/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.build

import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

/** Simple description for an artifact that is released from this project. */
data class Artifact(
    @get:Input val mavenGroup: String,
    @get:Input val projectName: String,
    @get:Input val version: String
) {
    override fun toString() = "$mavenGroup:$projectName:$version"
}

/** Zips all artifacts to publish. */
@DisableCachingByDefault(because = "Zip tasks are not worth caching according to Gradle")
abstract class GMavenZipTask : DefaultTask() {

    /** Repository containing artifacts to include */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectRepositoryDir: DirectoryProperty

    /** Zip file to save artifacts to */
    @get:OutputFile abstract val archiveFile: RegularFileProperty

    @TaskAction
    fun createZip() {
        val sourceDir = projectRepositoryDir.get().asFile
        ZipOutputStream(FileOutputStream(archiveFile.get().asFile)).use { zipOut ->
            zipOut.putNextEntry(
                // Top-level of the ZIP to align with Maven's expected repository structure
                ZipEntry("m2repository/").also { it.time = CONSTANT_TIME_FOR_ZIP_ENTRIES }
            )
            zipOut.closeEntry()

            sourceDir.walkTopDown().forEach { fileOrDir ->
                if (fileOrDir == sourceDir) return@forEach

                val relativePath = fileOrDir.relativeTo(sourceDir).invariantSeparatorsPath
                val entryName =
                    "m2repository/$relativePath" + if (fileOrDir.isDirectory) "/" else ""

                zipOut.putNextEntry(
                    ZipEntry(entryName).also { it.time = CONSTANT_TIME_FOR_ZIP_ENTRIES }
                )
                if (fileOrDir.isFile) {
                    fileOrDir.inputStream().use { it.copyTo(zipOut) }
                }
                zipOut.closeEntry()
            }
        }
    }
}

/** Handles creating various release tasks that create zips for the maven upload and local use. */
object Release {
    @Suppress("MemberVisibilityCanBePrivate")
    const val PROJECT_ARCHIVE_ZIP_TASK_NAME = "createProjectZip"
    private const val FULL_ARCHIVE_TASK_NAME = "createArchive"
    private const val ALL_ARCHIVES_TASK_NAME = "createAllArchives"
    const val DEFAULT_PUBLISH_CONFIG = "release"
    const val PROJECT_ZIPS_FOLDER = "per-project-zips"
    private const val GLOBAL_ZIP_PREFIX = "top-of-tree-m2repository"

    /**
     * Registers the project to be included in its group's zip file as well as the global zip files.
     */
    fun register(project: Project, androidXExtension: AndroidXExtension) {
        if (!androidXExtension.shouldPublish()) {
            project.logger.info(
                "project ${project.name} isn't part of release," +
                    " because its \"publish\" property is explicitly set to Publish.NONE"
            )
            return
        }
        if (!androidXExtension.isPublishConfigured()) {
            project.logger.info(
                "project ${project.name} isn't part of release, because" +
                    " it does not set the \"publish\" property."
            )
            return
        }
        if (!androidXExtension.shouldRelease() && !isSnapshotBuild()) {
            project.logger.info(
                "project ${project.name} isn't part of release, because its" +
                    " \"publish\" property is SNAPSHOT_ONLY, but it is not a snapshot build"
            )
            return
        }
        if (!androidXExtension.versionIsSet) {
            throw IllegalArgumentException(
                "Cannot register a project to release if it does not have a mavenVersion set up"
            )
        }

        val projectZipTask =
            getProjectZipTask(project, androidXExtension.isIsolatedProjectsEnabled())
        val zipTasks =
            listOfNotNull(
                projectZipTask,
                getGlobalFullZipTask(project, androidXExtension.isIsolatedProjectsEnabled())
            )

        val artifacts = androidXExtension.publishedArtifacts
        val publishTask = project.tasks.named("publish")
        zipTasks.forEach { it.configure { zipTask -> zipTask.dependsOn(publishTask) } }

        val verifyInputs = getVerifyProjectZipInputsTask(project)
        verifyInputs.configure { verifyTask ->
            verifyTask.dependsOn(publishTask)
            artifacts.forEach { artifact -> verifyTask.addCandidate(artifact) }
        }
        val verifyOutputs = getVerifyProjectZipOutputsTask(project)
        verifyOutputs.configure { verifyTask ->
            verifyTask.dependsOn(projectZipTask)
            artifacts.forEach { artifact -> verifyTask.addCandidate(artifact) }
        }
        projectZipTask.configure { zipTask ->
            zipTask.dependsOn(verifyInputs)
            zipTask.finalizedBy(verifyOutputs)
            val verifyOutputsTask = verifyOutputs.get()
            verifyOutputsTask.addFile(zipTask.archiveFile.get().asFile)
        }
    }

    /** Registers an archive task as a dependency of the anchor task */
    private fun Project.addToAnchorTask(task: TaskProvider<GMavenZipTask>) {
        val archiveAnchorTask: TaskProvider<VerifyLicenseAndVersionFilesTask> =
            project.rootProject.maybeRegister(
                name = ALL_ARCHIVES_TASK_NAME,
                onConfigure = { archiveTask: VerifyLicenseAndVersionFilesTask ->
                    archiveTask.group = "Distribution"
                    archiveTask.description = "Builds all archives for publishing"
                    archiveTask.repositoryDirectory.set(
                        project.rootProject.getRepositoryDirectory()
                    )
                },
                onRegister = {}
            )
        archiveAnchorTask.configure { it.dependsOn(task) }
    }

    /**
     * Creates and returns the task that includes all projects regardless of their release status.
     */
    private fun getGlobalFullZipTask(
        project: Project,
        projectIsolationEnabled: Boolean
    ): TaskProvider<GMavenZipTask>? {
        if (projectIsolationEnabled) return null
        return project.rootProject.maybeRegister(
            name = FULL_ARCHIVE_TASK_NAME,
            onConfigure = { task: GMavenZipTask ->
                task.archiveFile.set(
                    File(project.getDistributionDirectory(), "${getZipName(GLOBAL_ZIP_PREFIX)}.zip")
                )
                task.projectRepositoryDir.set(project.getRepositoryDirectory())
            },
            onRegister = { taskProvider: TaskProvider<GMavenZipTask> ->
                project.addToAnchorTask(taskProvider)
            }
        )
    }

    private fun getProjectZipTask(
        project: Project,
        projectIsolationEnabled: Boolean
    ): TaskProvider<GMavenZipTask> {
        val taskProvider =
            project.tasks.register(PROJECT_ARCHIVE_ZIP_TASK_NAME, GMavenZipTask::class.java) {
                it.archiveFile.set(
                    File(project.getDistributionDirectory(), project.getProjectZipPath())
                )
                it.projectRepositoryDir.set(project.getPerProjectRepositoryDirectory())
            }
        if (!projectIsolationEnabled) project.addToAnchorTask(taskProvider)
        return taskProvider
    }

    private fun getVerifyProjectZipInputsTask(project: Project): TaskProvider<VerifyGMavenZipTask> {
        return project.tasks.register(
            "verifyInputs$PROJECT_ARCHIVE_ZIP_TASK_NAME",
            VerifyGMavenZipTask::class.java
        )
    }

    private fun getVerifyProjectZipOutputsTask(
        project: Project
    ): TaskProvider<VerifyGMavenZipTask> {
        return project.tasks.register(
            "verifyOutputs$PROJECT_ARCHIVE_ZIP_TASK_NAME",
            VerifyGMavenZipTask::class.java
        )
    }
}

// b/273294710
@DisableCachingByDefault(
    because = "This task only checks the existence of files and isn't worth caching"
)
open class VerifyGMavenZipTask : DefaultTask() {
    @Input val filesToVerify = mutableListOf<File>()

    /** Whether this build adds automatic constraints between projects in the same group */
    @get:Input val shouldAddGroupConstraints: Provider<Boolean>

    init {
        cacheEvenIfNoOutputs()
        shouldAddGroupConstraints = project.shouldAddGroupConstraints()
    }

    fun addFile(file: File) {
        filesToVerify.add(file)
    }

    fun addCandidate(artifact: Artifact) {
        val groupSubdir = artifact.mavenGroup.replace('.', '/')
        val projectSubdir = File("$groupSubdir/${artifact.projectName}")
        val androidxRepoOut = project.getRepositoryDirectory()
        val fromDir = project.file("$androidxRepoOut/$projectSubdir")
        addFile(File(fromDir, artifact.version))
    }

    @TaskAction
    fun execute() {
        verifySettings()
        verifyFiles()
    }

    private fun verifySettings() {
        if (!shouldAddGroupConstraints.get() && !isSnapshotBuild()) {
            throw GradleException(
                """
                Cannot publish artifacts without setting -P$ADD_GROUP_CONSTRAINTS=true

                This property is required when building artifacts to publish

                (but this property can reduce remote cache usage so it is disabled by default)

                See AndroidXGradleProperties.kt for more information about this property
                """
                    .trimIndent()
            )
        }
    }

    private fun verifyFiles() {
        val missingFiles = mutableListOf<String>()
        val emptyDirs = mutableListOf<String>()
        filesToVerify.forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.path)
            } else {
                if (file.isDirectory) {
                    if (file.listFiles().isEmpty()) {
                        emptyDirs.add(file.path)
                    }
                }
            }
        }

        if (missingFiles.isNotEmpty() || emptyDirs.isNotEmpty()) {
            val checkedFilesString = filesToVerify.toString()
            val missingFileString = missingFiles.toString()
            val emptyDirsString = emptyDirs.toString()
            throw FileNotFoundException(
                "GMavenZip ${missingFiles.size} missing files: $missingFileString, " +
                    "${emptyDirs.size} empty dirs: $emptyDirsString. " +
                    "Checked files: $checkedFilesString"
            )
        }
    }
}

val AndroidXExtension.publishedArtifacts: List<Artifact>
    get() {
        val groupString = mavenGroup?.group!!
        val versionString = project.version.toString()
        val artifacts =
            mutableListOf(
                Artifact(
                    mavenGroup = groupString,
                    projectName = project.name,
                    version = versionString
                )
            )

        // Add platform-specific artifacts, if necessary.
        artifacts +=
            publishPlatforms.map { suffix ->
                Artifact(
                    mavenGroup = groupString,
                    projectName = "${project.name}-$suffix",
                    version = versionString
                )
            }

        return artifacts
    }

private val AndroidXExtension.publishPlatforms: List<String>
    get() {
        val potentialTargets =
            project.multiplatformExtension
                ?.targets
                ?.asMap
                ?.filterValues { it.publishable }
                ?.keys
                ?.map {
                    it.lowercase()
                        // Remove when https://youtrack.jetbrains.com/issue/KT-70072 is fixed.
                        // MultiplatformExtension.targets includes `wasmjs` in its list, however,
                        // the publication folder for this target is named `wasm-js`. Not having
                        // this replace causes the verifyInputscreateProjectZip task to fail
                        // as it is looking for a file named wasmjs
                        .replace("wasmjs", "wasm-js")
                } ?: emptySet()
        val declaredTargets = potentialTargets.filter { it != "metadata" }
        return declaredTargets.toList()
    }

private fun Project.projectZipPrefix(): String {
    return "${project.group}-${project.name}"
}

private fun getZipName(fileNamePrefix: String) = "$fileNamePrefix-all-${getBuildId()}"

fun Project.getProjectZipPath(): String {
    return Release.PROJECT_ZIPS_FOLDER +
        "/" +
        // We pass in a "" because that mimics not passing the group to getParams() inside
        // the getProjectZipTask function
        getZipName(projectZipPrefix()) +
        "-${project.version}.zip"
}

/**
 * Strip timestamps from the zip entries to generate consistent output. Set to be ths same as what
 * Gradle uses:
 * https://github.com/gradle/gradle/blob/master/platforms/core-runtime/files/src/main/java/org/gradle/api/internal/file/archive/ZipEntryConstants.java
 */
private val CONSTANT_TIME_FOR_ZIP_ENTRIES =
    GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).timeInMillis
