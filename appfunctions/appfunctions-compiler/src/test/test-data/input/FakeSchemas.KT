package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition

private const val FAKE_CATEGORY = "fake_schema_category"

@AppFunctionSchemaDefinition(name = "noArg", version = 1, category = FAKE_CATEGORY)
interface FakeNoArg {
    fun noArg(appFunctionContext: AppFunctionContext)
}

@AppFunctionSchemaDefinition(name = "allPrimitiveParams", version = 1, category = FAKE_CATEGORY)
interface FakeAllPrimitiveParams {
    fun allPrimitiveParams(
        appFunctionContext: AppFunctionContext,
        arg1: Int,
        arg2: Long,
        arg3: Float,
        arg4: Double,
        arg5: String,
        arg6: Boolean,
    )
}

@AppFunctionSchemaDefinition(name = "allPrimitiveReturns", version = 1, category = FAKE_CATEGORY)
interface FakeAllPrimitiveReturns {
    fun unitReturn(
        appFunctionContext: AppFunctionContext,
    )

    fun intReturn(
        appFunctionContext: AppFunctionContext,
    ): Int

    fun longReturn(
        appFunctionContext: AppFunctionContext,
    ): Long

    fun floatReturn(
        appFunctionContext: AppFunctionContext,
    ): Float

    fun doubleReturn(
        appFunctionContext: AppFunctionContext,
    ): Double

    fun stringReturn(
        appFunctionContext: AppFunctionContext,
    ): String

    fun booleanReturn(
        appFunctionContext: AppFunctionContext,
    ): Boolean

    fun nullableIntReturn(
        appFunctionContext: AppFunctionContext,
    ): Int?

    fun nullableLongReturn(
        appFunctionContext: AppFunctionContext,
    ): Long?

    fun nullableFloatReturn(
        appFunctionContext: AppFunctionContext,
    ): Float?

    fun nullableDoubleReturn(
        appFunctionContext: AppFunctionContext,
    ): Double?

    fun nullableStringReturn(
        appFunctionContext: AppFunctionContext,
    ): String?

    fun nullableBooleanReturn(
        appFunctionContext: AppFunctionContext,
    ): Boolean?
}
