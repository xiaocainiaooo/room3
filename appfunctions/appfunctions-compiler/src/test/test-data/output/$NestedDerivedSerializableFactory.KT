package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedDerivedSerializableFactory` :
    AppFunctionSerializableFactory<NestedDerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedDerivedSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()
    val nestedDerivedSerializableFactory = `$NestedDerivedSerializableFactory`()

    val stringBaseName = appFunctionData.getString("stringBaseName")
    val nestedBaseData = appFunctionData.getAppFunctionDataOrNull("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData) as
          NestedBaseSerializable?
    }
    val longBaseValue = appFunctionData.getLong("longBaseValue")
    val nestedDerivedData = appFunctionData.getAppFunctionDataOrNull("nestedDerived")
    var nestedDerived: NestedDerivedSerializable? = null
    if (nestedDerivedData != null) {
      nestedDerived = nestedDerivedSerializableFactory.fromAppFunctionData(nestedDerivedData) as
          NestedDerivedSerializable?
    }

    return NestedDerivedSerializable(stringBaseName, nestedBase, longBaseValue, nestedDerived)
  }

  override fun toAppFunctionData(nestedDerivedSerializable: NestedDerivedSerializable):
      AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()
    val nestedDerivedSerializableFactory = `$NestedDerivedSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NestedDerivedSerializable")
    builder.setString("stringBaseName", nestedDerivedSerializable.stringBaseName)
    if (nestedDerivedSerializable.nestedBase != null) {
      builder.setAppFunctionData("nestedBase",
          nestedBaseSerializableFactory.toAppFunctionData(nestedDerivedSerializable.nestedBase as
          NestedBaseSerializable))
    }
    builder.setLong("longBaseValue", nestedDerivedSerializable.longBaseValue)
    if (nestedDerivedSerializable.nestedDerived != null) {
      builder.setAppFunctionData("nestedDerived",
          nestedDerivedSerializableFactory.toAppFunctionData(nestedDerivedSerializable.nestedDerived
          as NestedDerivedSerializable))
    }

    return builder.build()
  }
}
