/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.internal

import android.os.Build
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.core.AppFunctionMetadataTestHelper.FunctionIds
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class AppFunctionReaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val appFunctionReader = AppFunctionReader(context)
    private val appFunctionMetadataTestHelper = AppFunctionMetadataTestHelper(context)

    @Before
    fun setup() =
        runBlocking<Unit> {
            assumeTrue(appFunctionMetadataTestHelper.isLegacyAppFunctionIndexerAvailable())
            appFunctionMetadataTestHelper.awaitAppFunctionIndexed(
                setOf(FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED)
            )
        }

    @Test
    fun searchAppFunctions_emptyPackagesListInSearchSpec_emptyFlow() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(packageNames = emptySet())

            assertThat(appFunctionReader.searchAppFunctions(searchFunctionSpec).toList()).isEmpty()
        }

    @Test
    fun searchAppFunctions_packageListNotSetInSpec_returnsAllAppFunctions() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec()

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            // TODO: Populate other fields for legacy indexer.
            assertThat(appFunctions.map { it.id })
                .containsExactly(
                    FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                    FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    FunctionIds.NO_SCHEMA_EXECUTION_FAIL
                )
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions)
                .containsExactlyElementsIn(getTestAppFunctionMetadataList(context.packageName))
        }

    @Test
    fun searchAppFunctions_packageListSetInSpec_returnsAppFunctionsInPackage() =
        runBlocking<Unit> {
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            // TODO: Populate other fields for legacy indexer.
            assertThat(appFunctions.map { it.id })
                .containsExactly(
                    FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                    FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    FunctionIds.NO_SCHEMA_EXECUTION_FAIL
                )
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions)
                .containsExactlyElementsIn(getTestAppFunctionMetadataList(context.packageName))
        }

    private companion object {
        fun getTestAppFunctionMetadataList(packageName: String) =
            listOf(
                AppFunctionMetadata(
                    id = FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                    packageName = packageName,
                    isEnabled = true,
                    schema = null,
                    parameters = emptyList(),
                    response =
                        AppFunctionResponseMetadata(
                            valueType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = 8, // TYPE_STRING
                                    isNullable = false
                                )
                        ),
                    components = AppFunctionComponentsMetadata()
                ),
                AppFunctionMetadata(
                    id = FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT,
                    packageName = packageName,
                    isEnabled = true,
                    schema = null,
                    parameters = emptyList(),
                    response =
                        AppFunctionResponseMetadata(
                            valueType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = 0, // TYPE_UNIT
                                    isNullable = false
                                )
                        ),
                    components = AppFunctionComponentsMetadata()
                ),
                AppFunctionMetadata(
                    id = FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
                    packageName = packageName,
                    isEnabled = false,
                    schema = null,
                    parameters = emptyList(),
                    response =
                        AppFunctionResponseMetadata(
                            valueType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = 0, // TYPE_UNIT
                                    isNullable = false
                                )
                        ),
                    components = AppFunctionComponentsMetadata()
                ),
                AppFunctionMetadata(
                    id = FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                    packageName = packageName,
                    isEnabled = true,
                    schema = null,
                    parameters = emptyList(),
                    response =
                        AppFunctionResponseMetadata(
                            valueType =
                                AppFunctionPrimitiveTypeMetadata(
                                    type = 0, // TYPE_UNIT
                                    isNullable = false
                                )
                        ),
                    components = AppFunctionComponentsMetadata()
                )
            )
    }
}
