/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.test.uiautomator

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class UiAutomatorTestScopeTest {

    companion object {
        private const val ID_BUTTON_DENY =
            "com.android.permissioncontroller:id/permission_deny_button"
    }

    @Test
    @Ignore
    fun searchPlaceInAgrigentoFor2AdultGuests() = uiAutomator {
        startApp("com.airbnb.android")

        // Need to click on Don't allow to dismiss the permission dialog.
        onViewOrNull { view.viewIdResourceName == ID_BUTTON_DENY }?.click()

        // Need to click on the search bar to type the destination.
        onView { view.textAsString == "Start your search" }.click()

        // Click on the search bar to type the destination
        onView { view.className == "android.widget.EditText" }.click()

        // Wait for the animation to finish
        activeWindow().waitForStable()

        // Need to type the destination in the search bar.
        type("Agrigento")

        // The destination is written, press enter to proceed.
        pressEnter()

        // Need to proceed to the next screen.
        onView { view.textAsString == "Next" }.click()

        // Click on the increment button to set the number of adults to 2
        onView {
                view.className == "android.widget.Button" && view.contentDescription == "increment"
            }
            .click()

        // We need to increment the number of adults to 2
        onView {
                view.className == "android.widget.Button" && view.contentDescription == "increment"
            }
            .click()

        // Need to click on Search to proceed.
        onView { view.textAsString == "Search" }.click()
    }
}
