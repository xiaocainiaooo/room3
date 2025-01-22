package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext

class AllPrimitiveInputFunctions {
    @AppFunction fun simpleFunctionInt(appFunctionContext: AppFunctionContext, intParam: Int) {}

    @AppFunction fun simpleFunctionLong(appFunctionContext: AppFunctionContext, longParam: Long) {}

    @AppFunction
    fun simpleFunctionFloat(appFunctionContext: AppFunctionContext, floatParam: Float) {}

    @AppFunction
    fun simpleFunctionDouble(appFunctionContext: AppFunctionContext, doubleParam: Double) {}

    @AppFunction
    fun simpleFunctionBoolean(appFunctionContext: AppFunctionContext, booleanParam: Boolean) {}

    @AppFunction
    fun simpleFunctionString(appFunctionContext: AppFunctionContext, stringParam: String) {}

    @AppFunction
    fun simpleFunctionIntArray(appFunctionContext: AppFunctionContext, intArrayParam: IntArray) {}

    @AppFunction
    fun simpleFunctionLongArray(
        appFunctionContext: AppFunctionContext,
        longArrayParam: LongArray
    ) {}

    @AppFunction
    fun simpleFunctionFloatArray(
        appFunctionContext: AppFunctionContext,
        floatArrayParam: FloatArray
    ) {}

    @AppFunction
    fun simpleFunctionDoubleArray(
        appFunctionContext: AppFunctionContext,
        doubleArrayParam: DoubleArray
    ) {}

    @AppFunction
    fun simpleFunctionBooleanArray(
        appFunctionContext: AppFunctionContext,
        booleanArrayParam: BooleanArray
    ) {}

    @AppFunction
    fun simpleFunctionByteArray(
        appFunctionContext: AppFunctionContext,
        byteArrayParam: ByteArray
    ) {}
}
