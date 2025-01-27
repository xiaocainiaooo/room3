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
package androidx.wear.protolayout.material3

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextTest {
    @Test
    fun text_inflates() {
        val provider =
            LayoutElementAssertionsProvider(
                materialScope(
                    context = ApplicationProvider.getApplicationContext(),
                    deviceConfiguration = DEVICE_PARAMETERS,
                ) {
                    text(text = TEXT, color = COLOR.argb)
                }
            )
        // Underlying implementation is just calling androidx.wear.protolayout.layout.basicText
        // which is fully tested for all fields
        provider.onElement(hasText(TEXT)).assertExists()
        provider.onElement(hasColor(COLOR)).assertExists()
    }

    private companion object {
        val TEXT = "Text test".layoutString
        const val COLOR = Color.YELLOW
    }
}
