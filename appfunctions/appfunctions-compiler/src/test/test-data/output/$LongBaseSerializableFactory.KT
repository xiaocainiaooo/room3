package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LongBaseSerializableFactory` : AppFunctionSerializableFactory<LongBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LongBaseSerializable {

    val age = checkNotNull(appFunctionData.getLongOrNull("age"))
    val name = checkNotNull(appFunctionData.getString("name"))

    return LongBaseSerializable(age, name)
  }

  override fun toAppFunctionData(appFunctionSerializable: LongBaseSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.LongBaseSerializable")
    val age = appFunctionSerializable.age
    builder.setLong("age", age)
    val name = appFunctionSerializable.name
    builder.setString("name", name)

    return builder.build()
  }
}
