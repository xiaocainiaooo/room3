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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.Composer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorTraceTests {
    @BeforeTest
    fun setUp() {
        Composer.setDiagnosticStackTraceEnabled(true)
    }

    @AfterTest
    fun tearDown() {
        Composer.setDiagnosticStackTraceEnabled(false)
    }

    @Test
    fun setContent() = exceptionTest {
        assertTrace(listOf("<lambda>(ErrorTraceTests.kt:<unknown line>)")) {
            compose { throwTestException() }
        }
    }

    @Test
    fun recompose() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            if (state) {
                throwTestException()
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentLinear() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:57)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "Linear(ErrorTraceComposables.kt:53)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose { Linear { throwTestException() } }
        }
    }

    @Test
    fun recomposeLinear() = exceptionTest {
        var state by mutableStateOf(false)
        compose {
            Linear {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:57)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "Linear(ErrorTraceComposables.kt:53)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentInlineLinear() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:67)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "InlineLinear(ErrorTraceComposables.kt:63)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose { InlineLinear { throwTestException() } }
        }
    }

    @Test
    fun recomposeInlineLinear() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            InlineLinear {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:67)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "InlineLinear(ErrorTraceComposables.kt:63)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentAfterTextInLoopInWrapper() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Wrapper(ErrorTraceComposables.kt:37)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose {
                Wrapper {
                    repeat(5) { it ->
                        Text("test")
                        if (it > 3) {
                            throwTestException()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun recomposeAfterTextInLoopInWrapper() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            Wrapper {
                repeat(5) { it ->
                    Text("test")
                    if (it > 3 && state) {
                        throwTestException()
                    }
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Wrapper(ErrorTraceComposables.kt:37)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentAfterTextInLoop() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Repeated(ErrorTraceComposables.kt:74)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            ),
        ) {
            compose {
                Repeated(List(10) { it }) {
                    Text("test")
                    throwTestException()
                }
            }
        }
    }

    @Test
    fun recomposeAfterTextInLoop() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            Repeated(List(10) { it }) {
                Text("test")
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Repeated(ErrorTraceComposables.kt:74)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentSubcomposition() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:46)",
                "Subcompose(ErrorTraceComposables.kt:42)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { Subcompose { throwTestException() } }
        }
    }

    @Test
    fun recomposeSubcomposition() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            Subcompose {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:46)",
                "Subcompose(ErrorTraceComposables.kt:42)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }

    @Test
    fun setContentDefaults() = exceptionTest {
        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "ComposableWithDefaults(ErrorTraceComposables.kt:89)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            compose { ComposableWithDefaults { throwTestException() } }
        }
    }

    @Test
    fun recomposeDefaults() = exceptionTest {
        var state by mutableStateOf(false)

        compose {
            ComposableWithDefaults {
                if (state) {
                    throwTestException()
                }
            }
        }

        assertTrace(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "ComposableWithDefaults(ErrorTraceComposables.kt:89)",
                "<lambda>(ErrorTraceTests.kt:<line number>)"
            )
        ) {
            state = true
            advance()
        }
    }
}

private class ExceptionTestScope(
    private val externalExceptions: List<Throwable>,
    private val scope: CompositionTestScope
) : CompositionTestScope by scope {
    fun assertTrace(expected: List<String>, block: () -> Unit) {
        var exception: TestComposeException? = null
        try {
            block()
        } catch (e: TestComposeException) {
            exception = e
        }
        exception =
            exception
                ?: externalExceptions.firstOrNull { it is TestComposeException }
                    as? TestComposeException
                ?: error("Composition exception was not caught or not thrown")

        val composeTrace =
            exception.suppressedExceptions.single { it is DiagnosticComposeException }
        val message = composeTrace.message.orEmpty()
        val frames =
            message
                .substringAfter("Composition stack when thrown:\n")
                .lines()
                .filter { it.isNotEmpty() }
                .map {
                    val trace = it.removePrefix("\tat ")
                    // Only keep the lines in the test file
                    if (trace.contains(TEST_FILE)) {
                        trace
                    } else {
                        val line = trace.substringAfter(':').substringBefore(')')
                        if (line == "<unknown line>") {
                            trace
                        } else {
                            trace.replace(line, "<line number>")
                        }
                    }
                }

        assertEquals(expected, frames)
    }
}

private fun throwTestException(): Nothing = throw TestComposeException()

private class TestComposeException : Exception("Test exception")

private const val TEST_FILE = "ErrorTraceComposables.kt"

private fun exceptionTest(block: ExceptionTestScope.() -> Unit) {
    val recomposeExceptions = mutableListOf<Exception>()
    compositionTest(
        recomposeInvoker = {
            try {
                it()
            } catch (e: TestComposeException) {
                recomposeExceptions += e
            }
        }
    ) {
        val scope = ExceptionTestScope(recomposeExceptions, this)
        with(scope) { block() }
    }
}
