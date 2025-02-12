package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * AppFunctionWithInputFromDifferentPackage.....kt
 * DiffPackageSchemas.....kt
 * DiffPackageSerializable.....kt
 * AnotherDiffPackageSerializable.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$AppFunctionWithInputFromDifferentPackage_AppFunctionInventory` : AppFunctionInventory
    {
  override val functionIdToMetadataMap: Map<String, AppFunctionMetadata> = mapOf(
    "com.testdata.AppFunctionWithInputFromDifferentPackage#functionWithDiffPackageInput" to
        ComTestdataAppFunctionWithInputFromDifferentPackageFunctionWithDiffPackageInputMetadataObject.APP_FUNCTION_METADATA,
  )

  private object
      ComTestdataAppFunctionWithInputFromDifferentPackageFunctionWithDiffPackageInputMetadataObject
      {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "diff_package_schemas_category", name="diffPackageInputSchemas", version=1)

    private val PARAMETER_METADATA_INPUT_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = false
        )

    private val INPUT_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "input",
            isRequired = true,
            dataType = PARAMETER_METADATA_INPUT_OBJECT_DATA_TYPE
        )

    private val PARAMETER_METADATA_INPUTLIST_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = false
        )

    private val INPUTLIST_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "inputList",
            isRequired = true,
            dataType = PARAMETER_METADATA_INPUTLIST_OBJECT_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      INPUT_PARAMETER_METADATA,
      INPUTLIST_PARAMETER_METADATA,
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 0,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id =
            "com.testdata.AppFunctionWithInputFromDifferentPackage#functionWithDiffPackageInput",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }
}
