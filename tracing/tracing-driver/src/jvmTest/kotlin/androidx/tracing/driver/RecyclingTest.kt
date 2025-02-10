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

package androidx.tracing.driver

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test
import perfetto.protos.MutableCounterDescriptor
import perfetto.protos.MutableProcessDescriptor
import perfetto.protos.MutableThreadDescriptor
import perfetto.protos.MutableTracePacket
import perfetto.protos.MutableTrackDescriptor
import perfetto.protos.MutableTrackEvent

class RecyclingTest {
    private val sink = NoOpSink()

    private val context: TraceContext =
        TraceContext(sequenceId = 1, sink = sink, isEnabled = true, isDebug = true)

    @Test
    internal fun testProcessTrackEvents() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val thread = process.getOrCreateThreadTrack(1, "thread")
            thread.trace("section") {}
        }
        assertTrue(context.isDebug)
        assertEquals(0, context.poolableCount())
    }

    @Test
    internal fun testProcessTrackFlows() = runTest {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val thread = process.getOrCreateThreadTrack(1, "thread")
            thread.traceFlow("section") {}
        }
        assertTrue(context.isDebug)
        assertEquals(0, context.poolableCount())
    }

    @Test
    internal fun pooledTracePacketRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val packet = process.beginPacket("section")
            assertNotNull(packet) // Tracing is enabled so the packet needs to be non-null
            packet.recycle()
            // Should be equal to the default packet
            assertEquals(packet.tracePacket, MutableTracePacket(timestamp = INVALID_LONG))
            assertTrue { packet.nested.all { it == null } }
            assertEquals(0, process.pool.poolableCount())
        }
    }

    @Test
    internal fun pooledTrackEventRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val pooledTrackEvent = process.pool.obtainTrackEvent()
            pooledTrackEvent.trackEvent.name = "Test"
            pooledTrackEvent.trackEvent.type = MutableTrackEvent.Type.TYPE_COUNTER
            pooledTrackEvent.trackEvent.track_uuid = 1L
            pooledTrackEvent.trackEvent.counter_value = 100L
            pooledTrackEvent.trackEvent.double_counter_value = 0.25
            pooledTrackEvent.trackEvent.flow_ids = listOf(1L)
            assertEquals(1, process.pool.poolableCount())
            pooledTrackEvent.recycle()
            assertEquals(pooledTrackEvent.trackEvent, MutableTrackEvent())
            assertEquals(0, process.pool.poolableCount())
        }
    }

    @Test
    internal fun pooledTrackDescriptorRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val pooledTrackDescriptor = process.pool.obtainTrackDescriptor()
            pooledTrackDescriptor.trackDescriptor.uuid = 2L
            pooledTrackDescriptor.trackDescriptor.parent_uuid = 1L
            pooledTrackDescriptor.trackDescriptor.name = "Test"
            pooledTrackDescriptor.trackDescriptor.process = buildMutableProcessDescriptor()
            pooledTrackDescriptor.trackDescriptor.thread = buildMutableThreadDescriptor()
            pooledTrackDescriptor.trackDescriptor.counter = buildMutableCounterDescriptor()
            pooledTrackDescriptor.recycle()
            assertEquals(MutableTrackDescriptor(), pooledTrackDescriptor.trackDescriptor)
        }
    }

    @Test
    internal fun pooledProcessDescriptorRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val pooledProcessDescriptor = process.pool.obtainProcessDescriptor()
            fillInMutableProcessDescriptor(pooledProcessDescriptor.processDescriptor)
            pooledProcessDescriptor.recycle()
            assertEquals(
                MutableProcessDescriptor(pid = INVALID_INT),
                pooledProcessDescriptor.processDescriptor
            )
            assertEquals(0, process.pool.poolableCount())
        }
    }

    @Test
    internal fun pooledThreadDescriptorRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val pooledThreadDescriptor = process.pool.obtainThreadDescriptor()
            fillInMutableThreadDescriptor(pooledThreadDescriptor.threadDescriptor)
            pooledThreadDescriptor.recycle()
            assertEquals(
                MutableThreadDescriptor(pid = INVALID_INT, tid = INVALID_INT),
                pooledThreadDescriptor.threadDescriptor
            )
            assertEquals(0, process.pool.poolableCount())
        }
    }

    @Test
    internal fun pooledCounterDescriptorRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val pooledCounterDescriptor = process.pool.obtainCounterDescriptor()
            fillInMutableCounterDescriptor(pooledCounterDescriptor.counterDescriptor)
            pooledCounterDescriptor.recycle()
            assertEquals(MutableCounterDescriptor(), pooledCounterDescriptor.counterDescriptor)
            assertEquals(0, process.pool.poolableCount())
        }
    }

    @Test
    internal fun pooledTracePacketArrayRecyclingTest() {
        context.use {
            val process = context.getOrCreateProcessTrack(id = 1, name = "process")
            val pooledTracePacketArray = process.pool.obtainTracePacketArray()
            val pooledTracePacket = process.beginPacket("section")
            assertNotNull(pooledTracePacket)
            pooledTracePacketArray.pooledTracePacketArray[0] = pooledTracePacket
            pooledTracePacketArray.recycle()
            assertTrue { pooledTracePacketArray.pooledTracePacketArray.all { it == null } }
            // PooledTracePacketArray recycling does not recycle the trace packets.
            // That is typically performed by the sink.
            pooledTracePacket.recycle()
            assertEquals(0, process.pool.poolableCount())
        }
    }

    // Test support methods

    private fun buildMutableProcessDescriptor(): MutableProcessDescriptor {
        val processDescriptor = MutableProcessDescriptor(pid = INVALID_INT)
        return fillInMutableProcessDescriptor(processDescriptor)
    }

    private fun fillInMutableProcessDescriptor(
        processDescriptor: MutableProcessDescriptor
    ): MutableProcessDescriptor {
        processDescriptor.pid = 10
        processDescriptor.process_name = "process"
        processDescriptor.cmdline = listOf("cmdline")
        return processDescriptor
    }

    private fun buildMutableThreadDescriptor(): MutableThreadDescriptor {
        val threadDescriptor = MutableThreadDescriptor(pid = INVALID_INT, tid = INVALID_INT)
        return fillInMutableThreadDescriptor(threadDescriptor)
    }

    private fun fillInMutableThreadDescriptor(
        threadDescriptor: MutableThreadDescriptor
    ): MutableThreadDescriptor {
        threadDescriptor.pid = 10
        threadDescriptor.tid = 20
        threadDescriptor.thread_name = "thread"
        return threadDescriptor
    }

    private fun buildMutableCounterDescriptor(): MutableCounterDescriptor {
        val counterDescriptor = MutableCounterDescriptor()
        return fillInMutableCounterDescriptor(counterDescriptor)
    }

    private fun fillInMutableCounterDescriptor(
        counterDescriptor: MutableCounterDescriptor
    ): MutableCounterDescriptor {
        counterDescriptor.type = MutableCounterDescriptor.BuiltinCounterType.COUNTER_THREAD_TIME_NS
        counterDescriptor.unit = MutableCounterDescriptor.Unit.UNIT_TIME_NS
        counterDescriptor.unit_name = "ns"
        counterDescriptor.is_incremental = true
        counterDescriptor.unit_multiplier = 1
        return counterDescriptor
    }
}
