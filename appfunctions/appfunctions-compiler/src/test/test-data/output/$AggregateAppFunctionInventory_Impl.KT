package androidx.appfunctions.`internal`

import com.testdata.`$AllPrimitiveInputFunctions_AppFunctionInventory`
import com.testdata.`$SimpleFunction_AppFunctionInventory`
import javax.`annotation`.processing.Generated
import kotlin.collections.List

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$AggregateAppFunctionInventory_Impl` : AggregateAppFunctionInventory() {
  override val inventories: List<AppFunctionInventory> = listOf(
    `$AllPrimitiveInputFunctions_AppFunctionInventory`(),
    `$SimpleFunction_AppFunctionInventory`(),
  )
}
