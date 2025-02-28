package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NonNestedChildSerializableFactory` :
    AppFunctionSerializableFactory<NonNestedChildSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NonNestedChildSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = appFunctionData.getString("stringBaseName")
    val nestedBaseData = appFunctionData.getAppFunctionDataOrNull("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData) as
          NestedBaseSerializable?
    }
    val longBaseValue = appFunctionData.getLong("longBaseValue")

    return NonNestedChildSerializable(stringBaseName, nestedBase, longBaseValue)
  }

  override fun toAppFunctionData(nonNestedChildSerializable: NonNestedChildSerializable):
      AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NonNestedChildSerializable")
    builder.setString("stringBaseName", nonNestedChildSerializable.stringBaseName)
    if (nonNestedChildSerializable.nestedBase != null) {
      builder.setAppFunctionData("nestedBase",
          nestedBaseSerializableFactory.toAppFunctionData(nonNestedChildSerializable.nestedBase as
          NestedBaseSerializable))
    }
    builder.setLong("longBaseValue", nonNestedChildSerializable.longBaseValue)

    return builder.build()
  }
}
