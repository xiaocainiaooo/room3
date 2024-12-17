/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.java.CompilationInputs
import androidx.build.java.MultiplatformCompilationInputs
import androidx.build.java.SourceSetInputs
import com.google.common.annotations.VisibleForTesting
import java.io.File
import java.io.Writer
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates an XML file representing the structure of a KMP project, to be used by metalava.
 *
 * For more information see go/metalavatask-kmp-spec.
 */
@CacheableTask
internal abstract class CreateProjectXmlTask : DefaultTask() {
    /**
     * Information about all source sets. This is marked as [Internal], but the source and classpath
     * files from the sources sets are listed as task inputs by [getSourceInputs] and
     * [getClasspathInputs]. Other properties of the source sets (name, dependsOn source sets) are
     * controlled by the project build file, and this task will already be rerun if the project
     * build file is updated.
     */
    @get:Internal abstract val sourceSets: ListProperty<SourceSetInputs>

    /** Output XML file */
    @get:OutputFile abstract val xmlFile: RegularFileProperty

    /** Android boot classpath for the project */
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val bootClasspath: ConfigurableFileCollection

    /** The compiled sources of this project. */
    @get:Classpath abstract val compiledSourceJar: ConfigurableFileCollection

    /** The project directory. This is used to construct relative paths. */
    @get:Internal abstract val rootDirectory: DirectoryProperty

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getSourceInputs(): List<FileCollection> {
        return sourceSets.get().map { it.sourcePaths }
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getClasspathInputs(): List<FileCollection> {
        return sourceSets.get().map { it.dependencyClasspath }
    }

    @TaskAction
    fun createXml() {
        val sourceSetElements =
            sourceSets.get().map { sourceSet ->
                createSourceSetXml(
                    sourceSet.sourceSetName,
                    sourceSet.dependsOnSourceSets,
                    sourceFiles(sourceSet.sourcePaths),
                    (sourceSet.dependencyClasspath + bootClasspath).files,
                    compiledSourceJar.singleFile,
                    rootDirectory.get().asFile
                )
            }
        val projectElement = createProjectXml(sourceSetElements)
        writeXml(projectElement, xmlFile.get().asFile.writer())
    }

    companion object {
        /**
         * Configures the project XML creation task and returns a provider for the output file.
         *
         * Returns null if [compilationInputs] is not a [MultiplatformCompilationInputs].
         */
        fun setupTask(
            project: Project,
            compilationInputs: CompilationInputs,
            compiledSourceJar: Configuration,
        ): Provider<RegularFile>? {
            val kmpInputs = compilationInputs as? MultiplatformCompilationInputs ?: return null
            val xmlDirectory = project.layout.buildDirectory.dir("metalava-lint-project-xml")
            val task =
                project.tasks.register("createProjectXml", CreateProjectXmlTask::class.java) { task
                    ->
                    task.sourceSets.set(kmpInputs.sourceSets)
                    task.xmlFile.set(xmlDirectory.map { it.file("project.xml") })
                    task.rootDirectory.set(project.projectDir)
                    task.compiledSourceJar.setFrom(compiledSourceJar)
                    task.bootClasspath.setFrom(kmpInputs.bootClasspath)
                }
            return task.flatMap { it.xmlFile }
        }

        /** Writes the [element] as XML to the [writer] and closes the stream. */
        @VisibleForTesting
        fun writeXml(element: Element, writer: Writer) {
            val document = DocumentHelper.createDocument(element)
            XMLWriter(writer, OutputFormat(/* indent= */ "  ", /* newlines= */ true)).apply {
                write(document)
                close()
            }
        }

        /** Constructs the XML [Element] for the project. */
        @VisibleForTesting
        fun createProjectXml(sourceSets: List<Element>): Element {
            val projectElement = DocumentHelper.createElement("project")

            // Setting "." for the root dir is equivalent to using the project directory path.
            // This allows all paths referenced in the XML file to be relative.
            // (With no root dir set, the location of the xml file is used as the root dir, and
            // relative paths don't work.)
            val rootDirElement = DocumentHelper.createElement("root")
            rootDirElement.addAttribute("dir", ".")
            projectElement.add(rootDirElement)

            for (sourceSet in sourceSets) {
                projectElement.add(sourceSet)
            }

            return projectElement
        }

        /** Constructs the XML [Element] representing one source set. */
        @VisibleForTesting
        fun createSourceSetXml(
            sourceSetName: String,
            dependsOnSourceSets: Collection<String>,
            sourceFiles: Collection<File>,
            allDependencies: Collection<File>,
            compiledSourceJar: File,
            rootDirectory: File,
        ): Element {
            val moduleElement = DocumentHelper.createElement("module")
            moduleElement.addAttribute("name", sourceSetName)
            moduleElement.addAttribute("android", "true")

            for (dependsOn in dependsOnSourceSets) {
                val depElement = DocumentHelper.createElement("dep")
                depElement.addAttribute("module", dependsOn)
                depElement.addAttribute("kind", "dependsOn")
                moduleElement.add(depElement)
            }

            for (sourceFile in sourceFiles) {
                val srcElement = DocumentHelper.createElement("src")
                srcElement.addAttribute("file", sourceFile.toRelativeString(rootDirectory))
                moduleElement.add(srcElement)
            }

            for (dependency in allDependencies) {
                val (elementType, fileType) =
                    when (dependency.extension) {
                        "jar" -> "classpath" to "jar"
                        "klib" -> "klib" to "file"
                        "aar" -> "classpath" to "aar"
                        "" -> "classpath" to "dir"
                        else -> continue
                    }

                val dependencyElement = DocumentHelper.createElement(elementType)
                dependencyElement.addAttribute(fileType, dependency.toRelativeString(rootDirectory))
                moduleElement.add(dependencyElement)
            }

            // Adding the compiled sources of this project fixes issues where annotations on some
            // elements aren't registered by metalava (e.g. in :ink:ink-rendering).
            val jarElement = DocumentHelper.createElement("src")
            jarElement.addAttribute("jar", compiledSourceJar.toRelativeString(rootDirectory))
            moduleElement.add(jarElement)

            return moduleElement
        }

        /** Lists all of the files from [sources]. */
        private fun sourceFiles(sources: FileCollection): List<File> {
            return sources.files.flatMap { gatherFiles(it) }
        }

        /**
         * If [file] is a normal file, returns a list containing [file].
         *
         * If [file] is a directory, returns a list of all normal files recursively contained in the
         * directory.
         *
         * Otherwise, returns an empty list.
         */
        private fun gatherFiles(file: File): List<File> {
            return if (file.isFile) {
                listOf(file)
            } else if (file.isDirectory) {
                file.listFiles()?.flatMap { gatherFiles(it) } ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
}
