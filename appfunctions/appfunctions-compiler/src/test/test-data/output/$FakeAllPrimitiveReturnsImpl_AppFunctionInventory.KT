package com.testdata

import androidx.appfunctions.`internal`.AppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
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
 * FakeAllPrimitiveReturnsImpl.....kt
 * FakeSchemas.....kt
 */
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$FakeAllPrimitiveReturnsImpl_AppFunctionInventory` : AppFunctionInventory {
  override val functionIdToMetadataMap: Map<String, AppFunctionMetadata> = mapOf()

  private object ComTestdataFakeAllPrimitiveReturnsImplUnitReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
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
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#unitReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplByteArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 2,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#byteArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplIntReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#intReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplLongReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#longReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplFloatReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#floatReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplDoubleReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#doubleReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplStringReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#stringReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplBooleanReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#booleanReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableByteArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 2,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableByteArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableIntReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableIntReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableLongReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableLongReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableFloatReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableFloatReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableDoubleReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableDoubleReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableStringReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableBooleanReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val PRIMITIVE_RESPONSE_VALUE_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = PRIMITIVE_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableBooleanReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplIntArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#intArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplByteArrayListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 2,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#byteArrayListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplLongArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#longArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplFloatArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#floatArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplDoubleArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#doubleArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplStringListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#stringListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplBooleanArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = false
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#booleanArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableStringListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 8,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableStringListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableByteArrayListReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 2,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableByteArrayListReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableIntArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 7,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableIntArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableLongArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 6,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableLongArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableDoubleArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 4,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableDoubleArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableBooleanArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 1,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableBooleanArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }

  private object ComTestdataFakeAllPrimitiveReturnsImplNullableFloatArrayReturnMetadataObject {
    private val SCHEMA_METADATA: AppFunctionSchemaMetadata? = AppFunctionSchemaMetadata(category=
        "fake_schema_category", name="allPrimitiveReturns", version=1)

    private val PARAMETER_METADATA_LIST: List<AppFunctionParameterMetadata> = listOf(
    )

    private val ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE: AppFunctionPrimitiveTypeMetadata =
        AppFunctionPrimitiveTypeMetadata(
            type = 5,
            isNullable = false
        )

    private val ARRAY_RESPONSE_VALUE_TYPE: AppFunctionArrayTypeMetadata =
        AppFunctionArrayTypeMetadata(
            itemType = ARRAY_RESPONSE_VALUE_TYPE_PRIMITIVE_ITEM_TYPE,
            isNullable = true
        )

    private val RESPONSE_METADATA: AppFunctionResponseMetadata = AppFunctionResponseMetadata(
            valueType = ARRAY_RESPONSE_VALUE_TYPE
        )

    public val APP_FUNCTION_METADATA: AppFunctionMetadata = AppFunctionMetadata(
            id = "com.testdata.FakeAllPrimitiveReturnsImpl#nullableFloatArrayReturn",
            isEnabledByDefault = true,
            schema =  SCHEMA_METADATA,
            parameters = PARAMETER_METADATA_LIST,
            response = RESPONSE_METADATA
        )
  }
}
