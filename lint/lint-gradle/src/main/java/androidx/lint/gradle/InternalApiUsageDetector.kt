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

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.UQualifiedReferenceExpression

class InternalApiUsageDetector : Detector(), Detector.UastScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(
            UImportStatement::class.java,
            UQualifiedReferenceExpression::class.java,
        )

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                if (node.importReference != null) {
                    var resolved = node.resolve()
                    if (resolved is PsiField) {
                        resolved = resolved.containingClass
                    } else if (resolved is PsiMethod) {
                        resolved = resolved.containingClass
                    }

                    if (resolved is PsiClass) {
                        checkClassUsage(resolved, node)
                    }
                }
            }

            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                val element = node.resolve() ?: return
                // PsiMethod covers both method calls and kotlin property access
                when (element) {
                    is PsiMethod -> checkMethodUsage(element, node)
                    is PsiField ->
                        element.containingClass?.let {
                            checkClassUsage(
                                it,
                                node,
                                " (field ${element.name} from ${it.qualifiedName})"
                            )
                        }
                }
            }

            fun checkMethodUsage(method: PsiMethod, node: UElement) {
                val cls = method.containingClass ?: return
                // Check that the class the method is implemented in is internal, and also all super
                // methods that this method overrides are also internal.
                val contextMsg = "(method ${method.name} from ${cls.qualifiedName})"
                if (cls.isInternalGradleApi() && method.hasOnlyInternalSuperMethods()) {
                    reportIncidentForNode(
                        INTERNAL_GRADLE_ISSUE,
                        node,
                        "Avoid using internal Gradle APIs $contextMsg"
                    )
                } else if (cls.isInternalAgpApi() && method.hasOnlyInternalSuperMethods()) {
                    reportIncidentForNode(
                        INTERNAL_AGP_ISSUE,
                        node,
                        "Avoid using internal Android Gradle Plugin APIs $contextMsg"
                    )
                }
            }

            fun checkClassUsage(cls: PsiClass, node: UElement, contextMsg: String = "") {
                if (cls.isInternalGradleApi()) {
                    reportIncidentForNode(
                        INTERNAL_GRADLE_ISSUE,
                        node,
                        "Avoid using internal Gradle APIs$contextMsg"
                    )
                } else if (cls.isInternalAgpApi()) {
                    reportIncidentForNode(
                        INTERNAL_AGP_ISSUE,
                        node,
                        "Avoid using internal Android Gradle Plugin APIs$contextMsg"
                    )
                }
            }

            private fun reportIncidentForNode(issue: Issue, node: UElement, message: String) {
                val incident =
                    Incident(context)
                        .issue(issue)
                        .location(context.getLocation(node))
                        .message(message)
                        .scope(node)
                context.report(incident)
            }

            private fun PsiClass.isInternalGradleApi(): Boolean {
                val className = qualifiedName ?: return false
                return className.startsWith("org.gradle.") && className.contains(".internal.")
            }

            private fun PsiClass.isInternalAgpApi(): Boolean {
                val className = qualifiedName ?: return false
                return className.startsWith("com.android.build.") &&
                    className.contains(".internal.")
            }

            /**
             * Checks that this method's super methods are all implemented in either internal gradle
             * or AGP classes.
             */
            private fun PsiMethod.hasOnlyInternalSuperMethods(): Boolean {
                return findSuperMethods().all {
                    it.containingClass?.isInternalGradleApi() == true &&
                        it.containingClass?.isInternalAgpApi() == true
                }
            }
        }

    companion object {
        val INTERNAL_GRADLE_ISSUE =
            Issue.create(
                "InternalGradleApiUsage",
                "Avoid using internal Gradle APIs",
                """
                Using internal APIs results in fragile plugin behavior as these types have no binary
                compatibility guarantees. It is best to create a feature request to open up these
                APIs if you find them useful.
            """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(InternalApiUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
        val INTERNAL_AGP_ISSUE =
            Issue.create(
                "InternalAgpApiUsage",
                "Avoid using internal Android Gradle Plugin APIs",
                """
                Using internal APIs results in fragile plugin behavior as these types have no binary
                compatibility guarantees. It is best to create a feature request to open up these
                APIs if you find them useful.
            """,
                Category.CORRECTNESS,
                5,
                Severity.ERROR,
                Implementation(InternalApiUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }
}
