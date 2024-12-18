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

package androidx.build.kythe

import androidx.build.AndroidXExtension
import androidx.build.ProjectLayoutType
import androidx.build.checkapi.ApiTaskConfig
import androidx.build.checkapi.configureJavaInputsAndManifest
import androidx.build.checkapi.createReleaseApiConfiguration
import androidx.build.getDefaultTargetJavaVersion
import androidx.build.getSupportRootFolder
import java.io.File
import org.gradle.api.Project

/** Sets up tasks for generating kzip files that are used for generating xref support on website. */
fun Project.configureProjectForKzipTasks(config: ApiTaskConfig, extension: AndroidXExtension) {
    // We use the output of kzip tasks for the Kythe pipeline to generate xrefs in cs.android.com
    // This is not supported, nor needed in GitHub
    if (ProjectLayoutType.isPlayground(this)) {
        return
    }

    // TODO(b/379936315): Make these compatible with koltinc/javac that indexer is using
    if (
        project.path in
            listOf(
                // Uses Java 9+ APIs, which are not part of any dependency in the classpath
                ":room:room-compiler-processing",
                ":room:room-compiler-processing-testing",
                // KSP generated folders not visible to AGP variant api (b/380363756)
                ":privacysandbox:tools:integration-tests:testsdk",
                ":room:room-runtime",
                // Javac extractor throws an error: package exists in another module:
                // jdk.unsupported
                ":performance:performance-unsafe",
                // Depends on the generated output of the proto project
                // :wear:protolayout:protolayout-proto
                // which we haven't captured for Java Kzip generation.
                ":wear:tiles:tiles-proto"
            )
    ) {
        return
    }

    // afterEvaluate required to read extension properties
    afterEvaluate {
        val (javaInputs, _) = configureJavaInputsAndManifest(config) ?: return@afterEvaluate
        val compiledSources = createReleaseApiConfiguration()

        GenerateKotlinKzipTask.setupProject(
            project,
            javaInputs,
            compiledSources,
            extension.kotlinTarget,
            getDefaultTargetJavaVersion(extension.type, project.name)
        )

        GenerateJavaKzipTask.setupProject(project, javaInputs, compiledSources)
    }
}

internal const val ANDROIDX_CORPUS =
    "android.googlesource.com/platform/frameworks/support//androidx-main"

internal fun Project.getVnamesJson(): File =
    File(project.getSupportRootFolder(), "buildSrc/vnames.json")
