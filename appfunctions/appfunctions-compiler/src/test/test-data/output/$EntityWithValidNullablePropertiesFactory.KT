package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.util.NoSuchElementException
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Double
import kotlin.Long
import kotlin.String

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidNullablePropertiesFactory` :
    AppFunctionSerializableFactory<EntityWithValidNullableProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData):
      EntityWithValidNullableProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    var longParam: Long?
    try {
      longParam = appFunctionData.getLong("longParam")
    } catch (e: NoSuchElementException) {
      longParam = null
    }
    var doubleParam: Double?
    try {
      doubleParam = appFunctionData.getDouble("doubleParam")
    } catch (e: NoSuchElementException) {
      doubleParam = null
    }
    var boolParam: Boolean?
    try {
      boolParam = appFunctionData.getBoolean("boolParam")
    } catch (e: NoSuchElementException) {
      boolParam = null
    }
    var stringParam: String?
    try {
      stringParam = appFunctionData.getString("stringParam")
    } catch (e: NoSuchElementException) {
      stringParam = null
    }
    val longArray = appFunctionData.getLongArray("longArray")
    val doubleArray = appFunctionData.getDoubleArray("doubleArray")
    val boolArray = appFunctionData.getBooleanArray("boolArray")
    val stringList = appFunctionData.getStringList("stringList")
    var inputSerializable: InputSerializable?
    try {
      inputSerializable =
          inputSerializableFactory.fromAppFunctionData(appFunctionData.getAppFunctionData("inputSerializable"))
    } catch (e: NoSuchElementException) {
      inputSerializable = null
    }
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList")
    val serializableList = serializableListData?.map {data ->
        inputSerializableFactory.fromAppFunctionData(data)}

    return EntityWithValidNullableProperties(longParam, doubleParam, boolParam, stringParam,
        longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
  }

  override
      fun toAppFunctionData(entityWithValidNullableProperties: EntityWithValidNullableProperties):
      AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidNullableProperties")
    if (entityWithValidNullableProperties.longParam != null) {
      builder.setLong("longParam", entityWithValidNullableProperties.longParam)
    }
    if (entityWithValidNullableProperties.doubleParam != null) {
      builder.setDouble("doubleParam", entityWithValidNullableProperties.doubleParam)
    }
    if (entityWithValidNullableProperties.boolParam != null) {
      builder.setBoolean("boolParam", entityWithValidNullableProperties.boolParam)
    }
    if (entityWithValidNullableProperties.stringParam != null) {
      builder.setString("stringParam", entityWithValidNullableProperties.stringParam)
    }
    if (entityWithValidNullableProperties.longArray != null) {
      builder.setLongArray("longArray", entityWithValidNullableProperties.longArray)
    }
    if (entityWithValidNullableProperties.doubleArray != null) {
      builder.setDoubleArray("doubleArray", entityWithValidNullableProperties.doubleArray)
    }
    if (entityWithValidNullableProperties.boolArray != null) {
      builder.setBooleanArray("boolArray", entityWithValidNullableProperties.boolArray)
    }
    if (entityWithValidNullableProperties.stringList != null) {
      builder.setStringList("stringList", entityWithValidNullableProperties.stringList)
    }
    if (entityWithValidNullableProperties.inputSerializable != null) {
      builder.setAppFunctionData("inputSerializable",
          inputSerializableFactory.toAppFunctionData(entityWithValidNullableProperties.inputSerializable))
    }
    if (entityWithValidNullableProperties.serializableList != null) {
      builder.setAppFunctionDataList("serializableList",
          entityWithValidNullableProperties.serializableList.map{ inputSerializable ->
          inputSerializableFactory.toAppFunctionData(inputSerializable) })
    }

    return builder.build()
  }
}
