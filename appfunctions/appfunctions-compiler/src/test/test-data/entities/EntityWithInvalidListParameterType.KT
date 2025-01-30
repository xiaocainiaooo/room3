package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
class EntityWithInvalidListParameterType(val badParam: List<Any>)
