package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidPropertiesFactory` :
    AppFunctionSerializableFactory<EntityWithValidProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = appFunctionData.getLong("longParam")
    val doubleParam = appFunctionData.getDouble("doubleParam")
    val boolParam = appFunctionData.getBoolean("boolParam")
    val stringParam = appFunctionData.getString("stringParam")
    val longArray = appFunctionData.getLongArray("longArray") ?: LongArray(0)
    val doubleArray = appFunctionData.getDoubleArray("doubleArray") ?: DoubleArray(0)
    val boolArray = appFunctionData.getBooleanArray("boolArray") ?: BooleanArray(0)
    val stringList = appFunctionData.getStringList("stringList") ?: emptyList()
    val inputSerializableData = appFunctionData.getAppFunctionData("inputSerializable")
    val inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList") ?:
        emptyList()
    val serializableList = serializableListData.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    return EntityWithValidProperties(longParam, doubleParam, boolParam, stringParam, longArray,
        doubleArray, boolArray, stringList, inputSerializable, serializableList)
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithValidProperties):
      AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidProperties")
    builder.setLong("longParam", appFunctionSerializable.longParam)
    builder.setDouble("doubleParam", appFunctionSerializable.doubleParam)
    builder.setBoolean("boolParam", appFunctionSerializable.boolParam)
    builder.setString("stringParam", appFunctionSerializable.stringParam)
    builder.setLongArray("longArray", appFunctionSerializable.longArray)
    builder.setDoubleArray("doubleArray", appFunctionSerializable.doubleArray)
    builder.setBooleanArray("boolArray", appFunctionSerializable.boolArray)
    builder.setStringList("stringList", appFunctionSerializable.stringList)
    builder.setAppFunctionData("inputSerializable",
        inputSerializableFactory.toAppFunctionData(appFunctionSerializable.inputSerializable as
        InputSerializable))
    builder.setAppFunctionDataList("serializableList", appFunctionSerializable.serializableList.map{
        inputSerializable ->
      inputSerializableFactory.toAppFunctionData(inputSerializable)
    })

    return builder.build()
  }
}
