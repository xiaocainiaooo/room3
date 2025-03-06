package com.testdata

import androidx.appfunctions.AppFunctionSchemaCapability
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSchemaCapability
public interface AppFunctionOpenable {
    public val stringToOpen: String
}

@AppFunctionSerializable
open class StringBaseSerializable(override val stringToOpen: String) : AppFunctionOpenable {}

@AppFunctionSerializable
open class LongBaseSerializable(
    open val age: Long,
    override val stringToOpen: String,
) : StringBaseSerializable(stringToOpen) {}

@AppFunctionSerializable
class DerivedSerializable(
    override val stringToOpen: String,
    override val age: Long,
) : LongBaseSerializable(age, stringToOpen)
