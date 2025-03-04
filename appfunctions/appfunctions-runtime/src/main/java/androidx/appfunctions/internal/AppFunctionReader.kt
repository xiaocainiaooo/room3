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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionSearchSpec
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * A class responsible for reading and searching for app functions based on a search specification.
 *
 * It searches for AppFunction documents using the [GlobalSearchSession] and converts them into
 * [AppFunctionMetadata] objects.
 *
 * @param context The context of the application, used for session creation.
 */
internal class AppFunctionReader(private val context: Context) {

    /**
     * Searches for app functions based on the provided search specification.
     *
     * @param searchFunctionSpec The search specification, which includes filters for searching
     *   matching documents.
     * @return A flow emitting a list of app function metadata matching the search criteria.
     * @see AppFunctionSearchSpec
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun searchAppFunctions(
        searchFunctionSpec: AppFunctionSearchSpec
    ): Flow<List<AppFunctionMetadata>> {
        // TODO: Use observer API to emit new values when underlying documents are changed.
        if (searchFunctionSpec.packageNames?.isEmpty() == true) {
            return emptyFlow()
        }
        return flow {
            createSearchSession(context = context).use { session ->
                emit(performSearch(session, searchFunctionSpec))
            }
        }
    }

    private suspend fun performSearch(
        session: GlobalSearchSession,
        searchFunctionSpec: AppFunctionSearchSpec,
    ): List<AppFunctionMetadata> {
        // TODO: Join with Runtime Spec.
        return session
            .search(searchFunctionSpec.toStaticMetadataAppSearchQuery(), STATIC_SEARCH_SPEC)
            .readAll(::convertSearchResultToAppFunctionMetadata)
            .filterNotNull()
    }

    private fun convertSearchResultToAppFunctionMetadata(
        searchResult: SearchResult
    ): AppFunctionMetadata? {

        // This is different from document id which for uniqueness is computed as packageName + "/"
        // + functionId.
        val functionId = checkNotNull(searchResult.genericDocument.getPropertyString("functionId"))
        val packageName =
            checkNotNull(searchResult.genericDocument.getPropertyString("packageName"))

        // TODO: Handle failures and log instead of throwing.
        val staticMetadataDocument =
            searchResult.genericDocument.toDocumentClass(AppFunctionMetadataDocument::class.java)

        return AppFunctionMetadata(
            id = functionId,
            packageName = packageName,
            // TODO: Compute effectively enabled.
            isEnabled = staticMetadataDocument.isEnabledByDefault,
            // TODO: Populate them separately for legacy indexer.
            schema = staticMetadataDocument.schema?.toAppFunctionSchemaMetadata(),
            parameters =
                // Since this is a list type it can be null for cases where an app function has no
                // parameters.
                if (staticMetadataDocument.response != null) {
                    staticMetadataDocument.parameters?.map { it.toAppFunctionParameterMetadata() }
                        ?: emptyList()
                } else {
                    TODO("Populate for legacy indexer")
                },
            response =
                staticMetadataDocument.response?.toAppFunctionResponseMetadata()
                    ?: TODO("Populate for legacy indexer"),
            components =
                staticMetadataDocument.components?.toAppFunctionComponentsMetadata()
                    ?: TODO("Populate for legacy indexer"),
        )
    }

    private companion object {
        const val SYSTEM_PACKAGE_NAME = "android"
        const val APP_FUNCTIONS_NAMESPACE = "app_functions"
        const val APP_FUNCTIONS_RUNTIME_NAMESPACE = "app_functions_runtime"

        val STATIC_SEARCH_SPEC =
            SearchSpec.Builder()
                .addFilterNamespaces(APP_FUNCTIONS_NAMESPACE)
                .addFilterDocumentClasses(AppFunctionMetadataDocument::class.java)
                .addFilterPackageNames(SYSTEM_PACKAGE_NAME)
                .setVerbatimSearchEnabled(true)
                .setNumericSearchEnabled(true)
                .build()
    }
}
