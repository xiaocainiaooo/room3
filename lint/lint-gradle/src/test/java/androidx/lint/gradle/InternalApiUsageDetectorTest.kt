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

package androidx.lint.gradle

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InternalApiUsageDetectorTest :
    GradleLintDetectorTest(
        detector = InternalApiUsageDetector(),
        issues =
            listOf(
                InternalApiUsageDetector.INTERNAL_GRADLE_ISSUE,
                InternalApiUsageDetector.INTERNAL_AGP_ISSUE,
            )
    ) {
    @Test
    fun `Test usage of internal Gradle API`() {
        val input =
            kotlin(
                """
                import org.gradle.api.component.SoftwareComponent
                import org.gradle.api.internal.component.SoftwareComponentInternal

                fun getSoftwareComponent() : SoftwareComponent {
                    return object : SoftwareComponentInternal {
                        override fun getUsages(): Set<out UsageContext> {
                            TODO()
                        }
                    }
                }
            """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Adding import aliases adds new warnings and that is working as intended.
            .skipTestModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                    src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Adding import aliases adds new warnings and that is working as intended.
            .testModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                    src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/test.kt:4: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal as IMPORT_ALIAS_2_SOFTWARECOMPONENTINTERNAL
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal Android Gradle API`() {
        val input =
            kotlin(
                """
                import com.android.build.gradle.internal.lint.VariantInputs
            """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Import aliases mode is covered by other tests
            .skipTestModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                src/test.kt:1: Error: Avoid using internal Android Gradle Plugin APIs [InternalAgpApiUsage]
                import com.android.build.gradle.internal.lint.VariantInputs
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of Internal annotation`() {
        val input =
            kotlin(
                """
                import java.io.File
                import org.gradle.api.Task
                import org.gradle.api.tasks.Internal

                class MyTask : Task {
                    @get:Internal
                    val notInput: File
                }
            """
                    .trimIndent()
            )
        check(input).expectClean()
    }

    @Test
    fun `Test usage of internal gradle API methods`() {
        // Custom stubs that don't make sense to reuse for other tests.
        val stubs =
            arrayOf(
                kotlin(
                    """
                        package org.gradle.api
                        interface BasePublicInterface {
                            fun basePublicMethodOverriddenByInternal()
                            fun basePublicMethodOverriddenByBoth()
                        }
                    """
                ),
                kotlin(
                    """
                        package org.gradle.api.internal
                        import org.gradle.api.BasePublicInterface
                        abstract class InternalClass : BasePublicInterface {
                            open fun internalMethodOverridden() = Unit
                            open fun internalMethodNotOverridden() = Unit
                            override fun basePublicMethodOverriddenByInternal() = Unit
                            override fun basePublicMethodOverriddenByBoth() = Unit
                        }
                    """
                ),
                kotlin(
                    """
                        @file:Suppress("InternalGradleApiUsage")
                        package org.gradle.api
                        import org.gradle.api.internal.InternalClass
                        class PublicClass : InternalClass() {
                            override fun internalMethodOverridden() = Unit
                            override fun basePublicMethodOverriddenByBoth() = Unit
                        }
                    """
                ),
            )
        val input =
            kotlin(
                """
                    package test.pkg
                    import org.gradle.api.PublicClass
                    fun callMethods(publicClass: PublicClass) {
                        publicClass.basePublicMethodOverriddenByInternal()
                        publicClass.basePublicMethodOverriddenByBoth()
                        publicClass.internalMethodOverridden()
                        publicClass.internalMethodNotOverridden()
                    }
                """
            )

        check(*stubs, input)
            .expect(
                """
                src/test/pkg/test.kt:8: Error: Avoid using internal Gradle APIs (method internalMethodNotOverridden from org.gradle.api.internal.InternalClass) [InternalGradleApiUsage]
                                        publicClass.internalMethodNotOverridden()
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal gradle API properties`() {
        // Custom stubs that don't make sense to reuse for other tests.
        val stubs =
            arrayOf(
                kotlin(
                    """
                        package org.gradle.api
                        interface BasePublicInterface {
                            val basePublicPropertyOverriddenByInternal: Boolean
                            val basePublicPropertyOverriddenByBoth: Boolean
                        }
                    """
                ),
                kotlin(
                    """
                        package org.gradle.api.internal
                        import org.gradle.api.BasePublicInterface
                        abstract class InternalClass : BasePublicInterface {
                            open val internalPropertyOverridden = false
                            open val internalPropertyNotOverridden = false
                            override val basePublicPropertyOverriddenByInternal = false
                            override val basePublicPropertyOverriddenByBoth = false
                        }
                    """
                ),
                kotlin(
                    """
                        @file:Suppress("InternalGradleApiUsage")
                        package org.gradle.api
                        import org.gradle.api.internal.InternalClass
                        class PublicClass : InternalClass() {
                            override val internalPropertyOverridden = false
                            override val basePublicPropertyOverriddenByBoth = false
                        }
                    """
                ),
            )
        val input =
            kotlin(
                """
                    package test.pkg
                    import org.gradle.api.PublicClass
                    fun getProperties(publicClass: PublicClass) {
                        publicClass.basePublicPropertyOverriddenByInternal
                        publicClass.basePublicPropertyOverriddenByBoth
                        publicClass.internalPropertyOverridden
                        publicClass.internalPropertyNotOverridden
                    }
                """
            )

        check(*stubs, input)
            .expect(
                """
                src/test/pkg/test.kt:8: Error: Avoid using internal Gradle APIs (method getInternalPropertyNotOverridden from org.gradle.api.internal.InternalClass) [InternalGradleApiUsage]
                                        publicClass.internalPropertyNotOverridden
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal gradle API fields`() {
        // Custom stubs that don't make sense to reuse for other tests.
        val stubs =
            arrayOf(
                java(
                    """
                        package org.gradle.api;
                        public interface BasePublicInterface {
                            boolean basePublicField = false;
                        }
                    """
                ),
                java(
                    """
                        package org.gradle.api.internal;
                        import org.gradle.api.BasePublicInterface;
                        public abstract class InternalClass implements BasePublicInterface {
                            boolean internalField = false;
                        }
                    """
                ),
                java(
                    """
                        package org.gradle.api;
                        public class PublicClass extends org.gradle.api.internal.InternalClass {
                            boolean publicField = false;
                        }
                    """
                ),
            )
        val input =
            kotlin(
                """
                    package test.pkg
                    import org.gradle.api.PublicClass
                    fun getFields(publicClass: PublicClass) {
                        publicClass.basePublicField
                        publicClass.internalField
                        publicClass.publicField
                    }
                """
            )

        check(*stubs, input)
            .expect(
                """
                src/test/pkg/test.kt:6: Error: Avoid using internal Gradle APIs (field internalField from org.gradle.api.internal.InternalClass) [InternalGradleApiUsage]
                                        publicClass.internalField
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
