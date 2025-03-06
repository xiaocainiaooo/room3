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
import androidx.appfunctions.AppFunctionManagerCompat
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.core.AppFunctionMetadataTestHelper.FunctionIds
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(TestParameterInjector::class)
class AppFunctionReaderTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val appFunctionReader = AppFunctionReader(context)
    private val appFunctionMetadataTestHelper = AppFunctionMetadataTestHelper(context)
    private val appFunctionManagerCompat = AppFunctionManagerCompat(context)

    @Before
    fun setup() =
        runBlocking<Unit> {
            assumeTrue(appFunctionMetadataTestHelper.isLegacyAppFunctionIndexerAvailable())
            appFunctionMetadataTestHelper.awaitAppFunctionIndexed(
                setOf(FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED)
            )

            // Reset enabled state for all test ids
            for (functionIds in getTestFunctionIdToMetadataMap(context.packageName).keys) {
                appFunctionManagerCompat.setAppFunctionEnabled(
                    functionIds,
                    AppFunctionManagerCompat.APP_FUNCTION_STATE_DEFAULT
                )
            }
        }

    @Test
    fun searchAppFunctions_emptyPackagesListInSearchSpec_noResults() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(packageNames = emptySet())

            assertThat(appFunctionReader.searchAppFunctions(searchFunctionSpec).first()).isEmpty()
        }

    @Test
    fun searchAppFunctions_emptySchemaNameInSearchSpec_noResults() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaName = "")

            assertThat(appFunctionReader.searchAppFunctions(searchFunctionSpec).first()).isEmpty()
        }

    @Test
    fun searchAppFunctions_emptySchemaCategoryInSearchSpec_noResults() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaCategory = "")

            assertThat(appFunctionReader.searchAppFunctions(searchFunctionSpec).first()).isEmpty()
        }

    @Test
    fun searchAppFunctions_packageListNotSetInSpec_returnsAllAppFunctions() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec()

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            // TODO: Populate other fields for legacy indexer.
            val expectedMetadata = getTestFunctionIdToMetadataMap(context.packageName)
            assertThat(appFunctions.map { it.id }).containsExactlyElementsIn(expectedMetadata.keys)
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions)
                .containsExactlyElementsIn(
                    getTestFunctionIdToMetadataMap(context.packageName).values
                )
        }

    @Test
    fun searchAppFunctions_packageListSetInSpec_returnsAppFunctionsInPackage() =
        runBlocking<Unit> {
            val searchFunctionSpec =
                AppFunctionSearchSpec(packageNames = setOf(context.packageName))

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            // TODO: Populate other fields for legacy indexer.
            val expectedMetadata = getTestFunctionIdToMetadataMap(context.packageName)
            assertThat(appFunctions.map { it.id }).containsExactlyElementsIn(expectedMetadata.keys)
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions)
                .containsExactlyElementsIn(
                    getTestFunctionIdToMetadataMap(context.packageName).values
                )
        }

    @Test
    fun searchAppFunctions_schemaNameInSpec_returnsMatchingAppFunctions() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaName = "print")

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            val expectedFunctionIds =
                setOf(
                    FunctionIds.MEDIA_SCHEMA_PRINT,
                    FunctionIds.NOTES_SCHEMA_PRINT,
                    FunctionIds.MEDIA_SCHEMA2_PRINT
                )
            val expectedMetadata =
                getTestFunctionIdToMetadataMap(context.packageName)
                    .filterKeys { it in expectedFunctionIds }
                    .values
            // TODO: Populate other fields for legacy indexer.
            assertThat(appFunctions.map { it.id }).containsExactlyElementsIn(expectedFunctionIds)
            assertThat(appFunctions.map { it.schema })
                .containsExactlyElementsIn(expectedMetadata.map { it.schema })
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions).containsExactlyElementsIn(expectedMetadata)
        }

    @Test
    fun searchAppFunctions_schemaCategoryInSpec_returnsMatchingAppFunctions() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(schemaCategory = "media")

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            val expectedFunctionIds =
                setOf(FunctionIds.MEDIA_SCHEMA_PRINT, FunctionIds.MEDIA_SCHEMA2_PRINT)
            val expectedMetadata =
                getTestFunctionIdToMetadataMap(context.packageName)
                    .filterKeys { it in expectedFunctionIds }
                    .values
            // TODO: Populate other fields for legacy indexer.
            assertThat(appFunctions.map { it.id }).containsExactlyElementsIn(expectedFunctionIds)
            assertThat(appFunctions.map { it.schema })
                .containsExactlyElementsIn(expectedMetadata.map { it.schema })
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions).containsExactlyElementsIn(expectedMetadata)
        }

    @Test
    fun searchAppFunctions_minSchemaVersionInSpec_returnsAppFunctionsWithSchemaVersionGreaterThanMin() =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec(minSchemaVersion = 2)

            val appFunctions = appFunctionReader.searchAppFunctions(searchFunctionSpec).first()

            val expectedFunctionIds = setOf(FunctionIds.MEDIA_SCHEMA2_PRINT)
            val expectedMetadata =
                getTestFunctionIdToMetadataMap(context.packageName)
                    .filterKeys { it in expectedFunctionIds }
                    .values
            // TODO: Populate other fields for legacy indexer.
            assertThat(appFunctions.map { it.id }).containsExactlyElementsIn(expectedFunctionIds)
            assertThat(appFunctions.map { it.schema })
                .containsExactlyElementsIn(expectedMetadata.map { it.schema })
            // Only check for all fields when dynamic indexer is enabled.
            assumeTrue(appFunctionMetadataTestHelper.isDynamicIndexerAvailable())
            assertThat(appFunctions).containsExactlyElementsIn(expectedMetadata)
        }

    @Test
    fun searchAppFunctions_isDisabledInRuntime_returnsIsEnabledFalse() =
        runBlocking<Unit> {
            val functionIdToTest = FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
            val searchFunctionSpec = AppFunctionSearchSpec()
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_DISABLED
            )

            val appFunctionMetadata =
                appFunctionReader.searchAppFunctions(searchFunctionSpec).first().single {
                    it.id == functionIdToTest
                }

            assertThat(appFunctionMetadata.isEnabled).isFalse()
        }

    @Test
    fun searchAppFunctions_isEnabledInRuntime_returnsIsEnabledTrue() =
        runBlocking<Unit> {
            val functionIdToTest = FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT
            val searchFunctionSpec = AppFunctionSearchSpec()
            appFunctionManagerCompat.setAppFunctionEnabled(
                functionIdToTest,
                AppFunctionManagerCompat.APP_FUNCTION_STATE_ENABLED
            )

            val appFunctionMetadata =
                appFunctionReader.searchAppFunctions(searchFunctionSpec).first().single {
                    it.id == functionIdToTest
                }

            assertThat(appFunctionMetadata.isEnabled).isTrue()
        }

    @Test
    fun searchAppFunctions_isEnabledSetByDefault_returnsIsEnabledDefaultValue(
        @TestParameter(
            FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT,
            FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT
        )
        functionIdToTest: String
    ) =
        runBlocking<Unit> {
            val searchFunctionSpec = AppFunctionSearchSpec()

            val appFunctionMetadata =
                appFunctionReader.searchAppFunctions(searchFunctionSpec).first().single {
                    it.id == functionIdToTest
                }

            val expectedMetadataWithIsEnabledDefault =
                getTestFunctionIdToMetadataMap(context.packageName).getValue(functionIdToTest)
            assertThat(appFunctionMetadata.isEnabled)
                .isEqualTo(expectedMetadataWithIsEnabledDefault.isEnabled)
        }

    private companion object {
        fun getTestFunctionIdToMetadataMap(packageName: String) =
            mapOf(
                FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED to
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
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_STRING,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
                FunctionIds.NO_SCHEMA_ENABLED_BY_DEFAULT to
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
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
                FunctionIds.NO_SCHEMA_DISABLED_BY_DEFAULT to
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
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
                FunctionIds.NO_SCHEMA_EXECUTION_FAIL to
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
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
                FunctionIds.NOTES_SCHEMA_PRINT to
                    AppFunctionMetadata(
                        id = FunctionIds.NOTES_SCHEMA_PRINT,
                        packageName = packageName,
                        isEnabled = true,
                        schema =
                            AppFunctionSchemaMetadata(
                                category = "notes",
                                name = "print",
                                version = 1
                            ),
                        parameters = emptyList(),
                        response =
                            AppFunctionResponseMetadata(
                                valueType =
                                    AppFunctionPrimitiveTypeMetadata(
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
                FunctionIds.MEDIA_SCHEMA_PRINT to
                    AppFunctionMetadata(
                        id = FunctionIds.MEDIA_SCHEMA_PRINT,
                        packageName = packageName,
                        isEnabled = true,
                        schema =
                            AppFunctionSchemaMetadata(
                                category = "media",
                                name = "print",
                                version = 1
                            ),
                        parameters = emptyList(),
                        response =
                            AppFunctionResponseMetadata(
                                valueType =
                                    AppFunctionPrimitiveTypeMetadata(
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
                FunctionIds.MEDIA_SCHEMA2_PRINT to
                    AppFunctionMetadata(
                        id = FunctionIds.MEDIA_SCHEMA2_PRINT,
                        packageName = packageName,
                        isEnabled = true,
                        schema =
                            AppFunctionSchemaMetadata(
                                category = "media",
                                name = "print",
                                version = 2
                            ),
                        parameters = emptyList(),
                        response =
                            AppFunctionResponseMetadata(
                                valueType =
                                    AppFunctionPrimitiveTypeMetadata(
                                        type = AppFunctionPrimitiveTypeMetadata.TYPE_UNIT,
                                        isNullable = false
                                    )
                            ),
                        components = AppFunctionComponentsMetadata()
                    ),
            )
    }
}
