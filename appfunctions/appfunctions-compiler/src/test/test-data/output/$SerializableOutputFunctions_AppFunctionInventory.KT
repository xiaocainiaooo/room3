package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * SerializableOutputFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableOutputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, AppFunctionMetadata> = mapOf()

  private object ComTestdataSerializableOutputFunctionsSimpleFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val OBJECT_RESPONSE_VALUE_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = OBJECT_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunction",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataSerializableOutputFunctionsSimpleFunctionSerializableListMetadataObject
      {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val OBJECT_RESPONSE_VALUE_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = OBJECT_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunctionSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object
      ComTestdataSerializableOutputFunctionsSimpleFunctionNullableSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val OBJECT_RESPONSE_VALUE_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = OBJECT_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunctionNullableSerializableList",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object
      ComTestdataSerializableOutputFunctionsSimpleFunctionNullableSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val OBJECT_RESPONSE_VALUE_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = OBJECT_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.SerializableOutputFunctions#simpleFunctionNullableSerializable",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }
}
