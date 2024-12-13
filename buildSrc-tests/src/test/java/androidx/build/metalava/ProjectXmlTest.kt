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

import java.io.File
import java.io.StringWriter
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectXmlTest {
    private fun checkElementXml(element: Element, expectedXml: String) {
        val stringWriter = StringWriter()
        CreateProjectXmlTask.writeXml(element, stringWriter)
        // Clean up the output for comparison
        val xml =
            stringWriter
                .toString()
                .removePrefix("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .trim()
        assertEquals(expectedXml, xml)
    }

    // Fake files for testing
    private val root = File("/fake/path/root")
    private val compiledSources = File(root, "compiledSources.jar")
    private val sourceDir = File(root, "sources")
    private val classpathDir = File(root, "classpath")

    @Test
    fun testEmptySourceSetXml() {
        val element =
            CreateProjectXmlTask.createSourceSetXml(
                "androidMain",
                emptyList(),
                emptyList(),
                emptyList(),
                compiledSources,
                root
            )

        checkElementXml(
            element,
            """
                <module name="androidMain" android="true">
                  <src jar="compiledSources.jar"/>
                </module>
            """
                .trimIndent()
        )
    }

    @Test
    fun testSourceSetXml() {
        val element =
            CreateProjectXmlTask.createSourceSetXml(
                "androidMain",
                listOf("commonMain", "jvmMain"),
                listOf(
                    File(sourceDir, "Foo.kt"),
                    File(sourceDir, "Bar.kt"),
                    File(sourceDir, "Baz.java")
                ),
                listOf(
                    File(classpathDir, "jarDependency.jar"),
                    File(classpathDir, "androidDependency.aar"),
                    File(classpathDir, "klibDependency.klib"),
                    File(classpathDir, "directoryDependency")
                ),
                compiledSources,
                root
            )
        checkElementXml(
            element,
            """
                <module name="androidMain" android="true">
                  <dep module="commonMain" kind="dependsOn"/>
                  <dep module="jvmMain" kind="dependsOn"/>
                  <src file="sources/Foo.kt"/>
                  <src file="sources/Bar.kt"/>
                  <src file="sources/Baz.java"/>
                  <classpath jar="classpath/jarDependency.jar"/>
                  <classpath aar="classpath/androidDependency.aar"/>
                  <klib file="classpath/klibDependency.klib"/>
                  <classpath dir="classpath/directoryDependency"/>
                  <src jar="compiledSources.jar"/>
                </module>
            """
                .trimIndent()
        )
    }

    @Test
    fun testEmptyProjectXml() {
        val element = CreateProjectXmlTask.createProjectXml(emptyList())
        checkElementXml(
            element,
            """
                <project>
                  <root dir="."/>
                </project>
            """
                .trimIndent()
        )
    }

    @Test
    fun testProjectXml() {
        val element =
            CreateProjectXmlTask.createProjectXml(
                listOf(
                    DocumentHelper.createElement("someElement"),
                    DocumentHelper.createElement("anotherElement")
                )
            )
        checkElementXml(
            element,
            """
                <project>
                  <root dir="."/>
                  <someElement/>
                  <anotherElement/>
                </project>
            """
                .trimIndent()
        )
    }
}
