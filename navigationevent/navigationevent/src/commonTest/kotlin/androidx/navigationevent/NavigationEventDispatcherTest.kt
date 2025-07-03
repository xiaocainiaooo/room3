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

package androidx.navigationevent

import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.Test

class NavigationEventDispatcherTest {
    @Test
    fun cancel_is_sent_while_removing_a_callback_after_navigation_started() {
        val history = mutableListOf<String>()
        val dispatcher = NavigationEventDispatcher()

        val callback =
            TestNavigationEventCallback(
                onEventStarted = { history += "onEventStarted" },
                onEventCancelled = { history += "onEventCancelled" },
            )
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())

        history += "before remove()"
        callback.remove()
        history += "after remove()"

        assertThat(history)
            .containsExactly(
                "onEventStarted",
                "before remove()",
                "onEventCancelled",
                "after remove()",
            )
            .inOrder()
    }

    @Test
    fun cancel_is_NOT_sent_while_disabling_a_callback_in_onEventStarted() {
        val history = mutableListOf<String>()
        val dispatcher = NavigationEventDispatcher()

        val callback =
            TestNavigationEventCallback(
                onEventStarted = {
                    history += "onEventStarted start"
                    isEnabled = false
                    history += "callback disabled"
                    history += "onEventStarted finish"
                },
                onEventCompleted = { history += "onEventCompleted" },
            )
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())
        dispatcher.dispatchOnCompleted()

        assertThat(history)
            .containsExactly(
                "onEventStarted start",
                "callback disabled",
                "onEventStarted finish",
                // Event is still sent because the callback was in progress.
                "onEventCompleted",
            )
            .inOrder()
    }

    @Test
    fun cancel_is_NOT_sent_while_disabling_a_callback_after_navigation_started() {
        val dispatcher = NavigationEventDispatcher()
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())
        assertThat(callback.startedInvocations).isEqualTo(1)

        callback.isEnabled = false

        // Assert that disabling the callback does not trigger a cancellation.
        assertThat(callback.cancelledInvocations).isEqualTo(0)
    }

    @Test
    fun cancel_is_sent_while_removing_a_callback_in_onEventStarted() {
        val history = mutableListOf<String>()
        val dispatcher = NavigationEventDispatcher()

        val callback =
            TestNavigationEventCallback(
                onEventStarted = {
                    history += "onEventStarted start"
                    remove()
                    history += "callback removed"
                    history += "onEventStarted finish"
                },
                onEventCancelled = { history += "onEventCancelled" },
            )
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnStarted(TestNavigationEvent())

        assertThat(history)
            .containsExactly(
                "onEventStarted start",
                "onEventCancelled",
                "callback removed",
                "onEventStarted finish",
            )
            .inOrder()
    }

    @Test
    fun cancel_is_sent_after_double_start() {
        val history = mutableListOf<String>()
        val dispatcher = NavigationEventDispatcher()

        val callback1 =
            TestNavigationEventCallback(
                onEventStarted = { history += "callback1 onEventStarted" },
                onEventCancelled = { history += "callback1 onEventCancelled" },
            )
        dispatcher.addCallback(callback1)

        history += "navigation1 starting"
        dispatcher.dispatchOnStarted(TestNavigationEvent())

        val callback2 =
            TestNavigationEventCallback(
                onEventStarted = { history += "callback2 onEventStarted" },
                onEventCompleted = { history += "callback2 onEventCompleted" },
            )
        dispatcher.addCallback(callback2)

        history += "navigation2 starting without cancelling/completing navigation1"
        dispatcher.dispatchOnStarted(TestNavigationEvent())

        dispatcher.dispatchOnCompleted()

        assertThat(history)
            .containsExactly(
                "navigation1 starting",
                "callback1 onEventStarted",
                "navigation2 starting without cancelling/completing navigation1",
                "callback1 onEventCancelled",
                "callback2 onEventStarted",
                "callback2 onEventCompleted",
            )
    }

    @Test
    fun callback_added_during_navigation_does_not_receive_events_from_current_navigation() {
        val dispatcher = NavigationEventDispatcher()

        val callback1 = TestNavigationEventCallback()
        dispatcher.addCallback(callback1)
        dispatcher.dispatchOnStarted(TestNavigationEvent())
        assertThat(callback1.startedInvocations).isEqualTo(1)

        // Add a second callback while the first navigation is in progress.
        val callback2 = TestNavigationEventCallback()
        dispatcher.addCallback(callback2)

        // Complete the first navigation.
        dispatcher.dispatchOnCompleted()

        // Assert that only the first callback was affected.
        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.startedInvocations).isEqualTo(0)
        assertThat(callback2.completedInvocations).isEqualTo(0)

        // Start and complete a second navigation.
        dispatcher.dispatchOnStarted(TestNavigationEvent())
        dispatcher.dispatchOnCompleted()

        // Assert that the second navigation was handled by the new top callback (callback2).
        assertThat(callback1.startedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback1.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(callback2.startedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
    }

    @Test
    fun fallback_is_called_when_there_are_no_enabled_callbacks() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback = TestNavigationEventCallback()
        dispatcher.addCallback(callback)

        dispatcher.dispatchOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()

        // After disabling the only callback, the fallback should be called.
        callback.isEnabled = false
        dispatcher.dispatchOnCompleted()
        assertThat(callback.completedInvocations).isEqualTo(1) // Unchanged
        assertThat(fallbackCalled).isTrue()
    }

    @Test
    fun fallback_is_not_called_when_all_enabled_callbacks_are_pass_through() {
        var fallbackCalled = false
        val dispatcher =
            NavigationEventDispatcher(fallbackOnBackPressed = { fallbackCalled = true })
        val callback1 = TestNavigationEventCallback(isPassThrough = true)
        val callback2 = TestNavigationEventCallback(isPassThrough = true)
        dispatcher.addCallback(callback1)
        dispatcher.addCallback(callback2)

        dispatcher.dispatchOnCompleted()

        assertThat(callback1.completedInvocations).isEqualTo(1)
        assertThat(callback2.completedInvocations).isEqualTo(1)
        assertThat(fallbackCalled).isFalse()
    }

    @Test
    fun overlay_callbacks_gets_called_even_when_added_before_normal_callbacks() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback()
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)
        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)

        dispatcher.dispatchOnCompleted()

        // The overlay callback should handle the event, and the normal one should not.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(0)
    }

    @Test
    fun overlay_callbacks_pass_through_to_normal_callbacks() {
        val dispatcher = NavigationEventDispatcher()
        val overlayCallback = TestNavigationEventCallback(isPassThrough = true)
        val normalCallback = TestNavigationEventCallback()

        dispatcher.addCallback(normalCallback, NavigationEventPriority.Default)
        dispatcher.addCallback(overlayCallback, NavigationEventPriority.Overlay)

        dispatcher.dispatchOnCompleted()

        // Both callbacks should be invoked because the overlay callback is pass-through.
        assertThat(overlayCallback.completedInvocations).isEqualTo(1)
        assertThat(normalCallback.completedInvocations).isEqualTo(1)
    }

    @Test
    fun adding_a_callback_to_more_dispatchers_throws_exception() {
        val callback = TestNavigationEventCallback()
        val dispatcher1 = NavigationEventDispatcher()
        dispatcher1.addCallback(callback)

        val dispatcher2 = NavigationEventDispatcher()
        assertThrows<IllegalArgumentException> { dispatcher2.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun adding_a_callback_to_the_same_dispatcher_twice_throws_exception() {
        val callback = TestNavigationEventCallback()
        val dispatcher = NavigationEventDispatcher()
        dispatcher.addCallback(callback)

        assertThrows<IllegalArgumentException> { dispatcher.addCallback(callback) }
            .hasMessageThat()
            .contains("is already registered with a dispatcher")
    }

    @Test
    fun removing_callbacks_in_progress_does_not_throw_concurrent_exception() {
        val history = mutableListOf<String>()
        val dispatcher = NavigationEventDispatcher()

        val callback1 =
            TestNavigationEventCallback(
                isPassThrough = true,
                onEventStarted = { history += "callback1 onEventStarted" },
                onEventProgressed = { history += "callback1 onEventProgressed" },
            )
        val callback2 =
            TestNavigationEventCallback(
                isPassThrough = true,
                onEventStarted = { history += "callback2 onEventStarted" },
                onEventProgressed = { history += "callback2 onEventProgressed" },
            )
        val callback3 =
            TestNavigationEventCallback(
                isPassThrough = true,
                onEventStarted = { history += "callback3 onEventStarted" },
                onEventProgressed = {
                    history += "callback3 onEventProgressed"
                    dispatcher.removeCallback(this)
                },
            )
        dispatcher.addCallback(callback1)
        dispatcher.addCallback(callback2)
        dispatcher.addCallback(callback3)

        val event = TestNavigationEvent()
        dispatcher.dispatchOnStarted(event)
        dispatcher.dispatchOnProgressed(event)

        assertThat(history)
            .containsExactly(
                "callback3 onEventStarted",
                "callback2 onEventStarted",
                "callback1 onEventStarted",
                "callback3 onEventProgressed",
                "callback2 onEventProgressed",
                "callback1 onEventProgressed",
            )
            .inOrder()
    }
}
