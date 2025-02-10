package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map

/**
 * Source Files:
 * SerializableInputFunctions.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SerializableInputFunctions_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, AppFunctionMetadata> = mapOf()

  private object ComTestdataSerializableInputFunctionsSimpleFunctionMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = false
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )
  }

  private object ComTestdataSerializableInputFunctionsSimpleFunctionSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = false
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )
  }

  private object
      ComTestdataSerializableInputFunctionsSimpleFunctionNullableSerializableListMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = true
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )
  }

  private object
      ComTestdataSerializableInputFunctionsSimpleFunctionNullableSerializableMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = null

    private val PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE: AppFunctionObjectTypeMetadata =
        AppFunctionObjectTypeMetadata(
            properties = emptyMap(),
            required = emptyList(),
            qualifiedName = null,
            isNullable = true
        )

    private val SERIALIZABLE_PARAMETER_METADATA: AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = "serializable",
            isRequired = true,
            dataType = PARAMETER_METADATA_SERIALIZABLE_OBJECT_DATA_TYPE
        )

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
      SERIALIZABLE_PARAMETER_METADATA,
    )
  }
}
