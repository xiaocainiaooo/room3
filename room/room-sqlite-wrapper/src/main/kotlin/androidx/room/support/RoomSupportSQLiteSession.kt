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

package androidx.room.support

import android.database.sqlite.SQLiteTransactionListener
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.room.RoomDatabase
import androidx.room.Transactor
import androidx.room.Transactor.SQLiteTransactionType
import androidx.room.util.activeThreadTransactionContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

internal class RoomSupportSQLiteSession(val roomDatabase: RoomDatabase) {
    private val isClosed = AtomicBoolean(false)

    // A single thread dispatcher used for drivers that have an internal connection pool. It is
    // unknown if the driver's internal pool has thread-confined semantics (like Android) or
    // something entirely different, but to avoid any assumptions all database operations will be
    // dispatched to this thread while there is an active transaction.
    // TODO(b/408010324, b/415006268): Combine with RoomDatabase.withTransaction semantics
    private val threadTransactionDispatcher: ExecutorCoroutineDispatcher? =
        if (roomDatabase.driver.hasConnectionPool) {
            Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        } else {
            null
        }

    // The thread confined transaction object. If this thread local is set, then there is an active
    // transaction in this thread and a coroutine holding the connection with the transaction is
    // active and awaiting for the transaction to end.
    private val threadTransaction = ThreadLocal<ThreadTransaction>()

    val path: String?
        get() = roomDatabase.path

    /** Use a reader connection blocking. */
    fun <T> useReaderBlocking(block: suspend (Transactor) -> T) =
        useConnectionBlocking(isReadOnly = true, block)

    /** Use a writer connection blocking. */
    fun <T> useWriterBlocking(block: suspend (Transactor) -> T) =
        useConnectionBlocking(isReadOnly = false, block)

    /**
     * Use a database connection blocking. If there is an active transaction in this thread, then
     * the transaction connection will be re-used.
     */
    private fun <T> useConnectionBlocking(
        isReadOnly: Boolean,
        block: suspend (Transactor) -> T
    ): T {
        val context = threadTransaction.get()?.getCoroutineContext() ?: EmptyCoroutineContext
        return runBlockingNonInterruptible(context) {
            roomDatabase.useConnection(isReadOnly, block)
        }
    }

    /** A version of [runBlocking] that is not interruptible. */
    private fun <T> runBlockingNonInterruptible(
        context: CoroutineContext,
        action: suspend CoroutineScope.() -> T
    ): T {
        val result = AtomicReference<Result<T>>()
        try {
            runBlocking(context) {
                withContext(NonCancellable) { result.set(runCatching { action() }) }
            }
        } catch (_: InterruptedException) {
            // Restore the interrupted flag
            Thread.currentThread().interrupt()
        }
        return result.get().getOrThrow()
    }

    fun beginTransaction(type: SQLiteTransactionType, listener: SQLiteTransactionListener? = null) {
        val currentTransaction =
            threadTransaction.get()
                ?: ThreadTransaction.create(
                        db = roomDatabase,
                        transactionDispatcher = threadTransactionDispatcher,
                        type = type
                    )
                    .also {
                        threadTransaction.set(it)
                        activeThreadTransactionContext.set(it.getCoroutineContext())
                    }
        currentTransaction.begin(listener)
    }

    fun endTransaction() {
        val currentTransaction = threadTransaction.get() ?: error("Not in a transaction")
        val transactionEnded = currentTransaction.end()
        if (transactionEnded) {
            activeThreadTransactionContext.set(null)
            threadTransaction.set(null)
        }
    }

    fun setTransactionSuccessful() {
        val currentTransaction = threadTransaction.get() ?: error("Not in a transaction")
        currentTransaction.markSuccessful()
    }

    fun inTransaction(): Boolean {
        return threadTransaction.get() != null
    }

    // TODO(b/409102321): Sync the isOpen / isClosed state with RoomDatabase
    fun isClosed(): Boolean {
        return isClosed.get()
    }

    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            threadTransaction.get()?.end()
            threadTransactionDispatcher?.close()
            roomDatabase.close()
        }
    }
}

/**
 * An object representing a database transaction.
 *
 * The transaction itself is an active coroutine to which database operations are dispatched and is
 * started via [RoomSupportSQLiteSession.beginTransaction]. Once a transaction is ended via
 * [RoomSupportSQLiteSession.endTransaction] the coroutine is finished which marks the end of the
 * transaction.
 *
 * @property context The connection confined coroutine context. If there is an on-going transaction,
 *   this context should be used when bridging blocking APIs with the [RoomDatabase] coroutine APIs.
 *   See [RoomSupportSQLiteSession.useConnectionBlocking] and [activeThreadTransactionContext].
 * @property dispatcher An optional dispatcher to execute database operations for the on-going
 *   transaction.
 * @property completable A completable to finish the transaction, the value will determine if the
 *   transaction is committed or rolled back.
 */
private class ThreadTransaction
private constructor(
    private val context: CoroutineContext,
    private val dispatcher: CoroutineDispatcher?,
    private val completable: TransactionCompletable,
) {
    private val stack = ArrayDeque<TransactionItem>()

    fun getCoroutineContext(): CoroutineContext {
        val baseContext = context.minusKey(ContinuationInterceptor)
        return dispatcher?.let { baseContext + it } ?: baseContext
    }

    fun begin(listener: SQLiteTransactionListener?) {
        stack.addLast(TransactionItem(listener))
    }

    fun markSuccessful() {
        check(stack.isNotEmpty())
        stack[stack.lastIndex].markedSuccessful = true
    }

    fun end(): Boolean {
        check(stack.isNotEmpty())
        val item = stack.removeLast()
        val successful = item.markedSuccessful && !item.childFailed
        if (stack.isEmpty()) {
            completable.complete(successful)
            return true
        } else {
            stack[stack.lastIndex].childFailed = !successful
            return false
        }
    }

    /**
     * A [CompletableDeferred] that also awaits for the [latch] which is only released when the
     * transaction coroutine is completed.
     */
    private class TransactionCompletable(
        private val delegate: CompletableDeferred<Boolean>,
        private val latch: CountDownLatch
    ) : CompletableDeferred<Boolean> by delegate {
        override fun complete(value: Boolean): Boolean {
            check(delegate.complete(value))
            latch.await()
            return true
        }
    }

    /**
     * An object that represent a transaction in the stack of nested transactions. All child nested
     * transactions must complete successfully for the actual transaction to be committed.
     */
    // TODO(b/408061556): Use and implement SQLiteTransactionListener
    private class TransactionItem(val listener: SQLiteTransactionListener?) {
        var markedSuccessful = false
        var childFailed = false
    }

    companion object {
        fun create(
            db: RoomDatabase,
            transactionDispatcher: CoroutineDispatcher?,
            type: SQLiteTransactionType
        ): ThreadTransaction {
            val transactionFuture =
                CallbackToFutureAdapter.getFuture { completer ->
                    // a latch to wait (blocking) for the coroutine to finish
                    val completionLatch = CountDownLatch(1)
                    val transactionBody: suspend (Transactor) -> Unit = { connection ->
                        connection.withTransaction(type) {
                            // a completable to commit or rollback the transaction
                            val successSignal = CompletableDeferred<Boolean>()
                            // the transaction object that will be stored in a thread local
                            ThreadTransaction(
                                    context = coroutineContext,
                                    dispatcher = transactionDispatcher,
                                    completable =
                                        TransactionCompletable(successSignal, completionLatch)
                                )
                                .also { completer.set(it) }
                            val success = successSignal.await()
                            if (!success) {
                                rollback(Unit)
                            }
                        }
                    }
                    val transactionScope =
                        transactionDispatcher?.let { CoroutineScope(it) } ?: db.getCoroutineScope()
                    // launch a transaction coroutine that will be kept alive until the transaction
                    // object ends.
                    transactionScope
                        .launch(CoroutineName("RoomSupportSQLiteTransaction")) {
                            db.useConnection(
                                isReadOnly = type == SQLiteTransactionType.DEFERRED,
                                block = transactionBody
                            )
                        }
                        .invokeOnCompletion { error ->
                            if (error != null) {
                                completer.setException(error)
                            }
                            completionLatch.countDown()
                        }
                }
            try {
                // wait for the transaction coroutine to be started, this includes waiting for an
                // available database connection to be acquired plus the start of the transaction
                // on the connection.
                return transactionFuture.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: IllegalStateException("Failed to begin transaction")
            }
        }
    }
}
