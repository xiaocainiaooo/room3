package com.testdata

import androidx.appfunctions.AppFunctionSerializable

// Putting constructor parameter without getter will result in compiler error
@AppFunctionSerializable
class EntityWithNonPropertyParameter(nonPropertyParameter: String)
