package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedBaseSerializableFactory` :
    AppFunctionSerializableFactory<NestedBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedBaseSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = appFunctionData.getString("stringBaseName")
    val nestedBaseData = appFunctionData.getAppFunctionDataOrNull("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData) as
          NestedBaseSerializable?
    }

    return NestedBaseSerializable(stringBaseName, nestedBase)
  }

  override fun toAppFunctionData(nestedBaseSerializable: NestedBaseSerializable): AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NestedBaseSerializable")
    builder.setString("stringBaseName", nestedBaseSerializable.stringBaseName)
    if (nestedBaseSerializable.nestedBase != null) {
      builder.setAppFunctionData("nestedBase",
          nestedBaseSerializableFactory.toAppFunctionData(nestedBaseSerializable.nestedBase as
          NestedBaseSerializable))
    }

    return builder.build()
  }
}
