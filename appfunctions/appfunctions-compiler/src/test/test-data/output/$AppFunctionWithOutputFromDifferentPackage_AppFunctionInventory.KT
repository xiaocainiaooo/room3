package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * AppFunctionWithOutputFromDifferentPackage.....kt
 * DiffPackageSchemas.....kt
 * DiffPackageSerializable.....kt
 * AnotherDiffPackageSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$AppFunctionWithOutputFromDifferentPackage_AppFunctionInventory` :
    AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, AppFunctionMetadata> = mapOf(
    "com.testdata.AppFunctionWithOutputFromDifferentPackage#functionWithDiffPackageOutput" to
        ComTestdataAppFunctionWithOutputFromDifferentPackageFunctionWithDiffPackageOutputMetadataObject.APP_FUNCTION_METADATA,
    "com.testdata.AppFunctionWithOutputFromDifferentPackage#functionWithAnotherDiffPackageOutput" to
        ComTestdataAppFunctionWithOutputFromDifferentPackageFunctionWithAnotherDiffPackageOutputMetadataObject.APP_FUNCTION_METADATA,
  )

  private object
      ComTestdataAppFunctionWithOutputFromDifferentPackageFunctionWithDiffPackageOutputMetadataObject
      {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "diff_package_schemas_category", name="diffPackageOutputSchemas", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val REFERENCE_RESPONSE_VALUE_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType = "com.testdata.differentPackage.DiffPackageSerializable",
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = REFERENCE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id =
            "com.testdata.AppFunctionWithOutputFromDifferentPackage#functionWithDiffPackageOutput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object
      ComTestdataAppFunctionWithOutputFromDifferentPackageFunctionWithAnotherDiffPackageOutputMetadataObject
      {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "diff_package_schemas_category", name="diffPackageOutputSchemas", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE: AppFunctionReferenceTypeMetadata =
        AppFunctionReferenceTypeMetadata(
            referenceDataType =
            "com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable",
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_REFERENCE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id =
            "com.testdata.AppFunctionWithOutputFromDifferentPackage#functionWithAnotherDiffPackageOutput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }
}
