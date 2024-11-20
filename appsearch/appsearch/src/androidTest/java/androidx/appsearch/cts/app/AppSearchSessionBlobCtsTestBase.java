/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;
import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchCommitBlobResponse;
import androidx.appsearch.app.AppSearchOpenBlobForReadResponse;
import androidx.appsearch.app.AppSearchOpenBlobForWriteResponse;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.flags.CheckFlagsRule;
import androidx.appsearch.flags.DeviceFlagsValueProvider;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
public abstract class AppSearchSessionBlobCtsTestBase {
    static final String DB_NAME_1 = "";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    private final String mPackageName =
            ApplicationProvider.getApplicationContext().getPackageName();

    private AppSearchSession mDb1;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName) throws Exception;

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();
        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
    }

    @Test
    public void testWriteAndReadBlob() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] data2 = generateRandomBytes(20); // 20 Bytes
        byte[] digest1 = calculateDigest(data1);
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, mPackageName, DB_NAME_1, "namespace");
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, mPackageName, DB_NAME_1, "namespace");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle1, handle2)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(handle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
                outputStream.write(data1);
                outputStream.flush();
            }

            ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(handle2);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
                outputStream.write(data2);
                outputStream.flush();
            }
        }

        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(handle1, handle2)).get().getResult()
                .isSuccess());

        byte[] readBytes1 = new byte[10]; // 10 Bytes
        byte[] readBytes2 = new byte[20]; // 20 Bytes

        try (AppSearchOpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(handle1, handle2)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertTrue(readResult.isSuccess());

            ParcelFileDescriptor readPfd1 = readResult.getSuccesses().get(handle1);
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readPfd1)) {
                inputStream.read(readBytes1);
            }
            assertThat(readBytes1).isEqualTo(data1);

            ParcelFileDescriptor readPfd2 = readResult.getSuccesses().get(handle2);
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readPfd2)) {
                inputStream.read(readBytes2);
            }
            assertThat(readBytes2).isEqualTo(data2);
        }
    }

    @Test
    public void testWriteAndReadBlob_withoutCommit() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(handle);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(data);
                outputStream.flush();
            }
        }

        // Read blob without commit the blob first.
        try (AppSearchOpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertFalse(readResult.isSuccess());

            assertThat(readResult.getFailures().keySet()).containsExactly(handle);
            assertThat(readResult.getFailures().get(handle).getResultCode())
                    .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
            assertThat(readResult.getFailures().get(handle).getErrorMessage())
                    .contains("Cannot find the blob for handle");
        }
    }

    @Test
    public void testRewrite_notAllowed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(handle);
            // write wrong data into it.
            byte[] wrongData = generateRandomBytes(10); // 10 Bytes
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(wrongData);
                outputStream.flush();
            }
        }

        // Open a new write session and rewrite is allowed before commit.
        try (AppSearchOpenBlobForWriteResponse reWriteResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> reWriteResult =
                    reWriteResponse.getResult();
            assertTrue(reWriteResult.isSuccess());
            ParcelFileDescriptor rewritePfd = reWriteResult.getSuccesses().get(handle);

            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(rewritePfd)) {
                outputStream.write(data);
                outputStream.flush();
            }

            // Commit the blob
            assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(handle)).get().getResult().isSuccess());

            // Rewrite is not allowed once committed.
            reWriteResult = mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get().getResult();
            assertThat(reWriteResult.isSuccess()).isFalse();
            assertThat(reWriteResult.getFailures().get(handle).getResultCode())
                    .isEqualTo(AppSearchResult.RESULT_ALREADY_EXISTS);
            assertThat(reWriteResult.getFailures().get(handle).getErrorMessage())
                    .contains("Rewriting the committed blob is not allowed");
        }
    }

    @Test
    public void testOpenWriteForRead_allowed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(handle);

            // Read on openWrite is allowed since openWriteBlob returns read and write fd.
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    writePfd)) {
                inputStream.read(data);
            }
        }
    }

    @Test
    public void testOpenReadForWrite_notAllowed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());
            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(handle);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(data);
                outputStream.flush();
            }
        }

        // Commit the blob
        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(handle)).get().getResult().isSuccess());

        try (AppSearchOpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertTrue(readResult.isSuccess());
            ParcelFileDescriptor readPfd = readResult.getSuccesses().get(handle);
            // Cannot write on openRead since openRead returns read only fd.
            assertThrows(IOException.class, () -> {
                try (OutputStream outputStream =
                             new ParcelFileDescriptor.AutoCloseOutputStream(readPfd)) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            });
        }
    }

    @Test
    public void testCommitBlobWithWrongDigest() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] data2 = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data1);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());
            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(handle);

            // Write data2 to pfd which is opened by blob handle for data1.
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(data2);
                outputStream.flush();
            }
        }

        // Commit the blob
        AppSearchCommitBlobResponse commitBlobResponse =
                mDb1.commitBlobAsync(ImmutableSet.of(handle)).get();
        AppSearchBatchResult<AppSearchBlobHandle, Void> commitResult =
                commitBlobResponse.getResult();
        assertThat(commitResult.isSuccess()).isFalse();
        assertThat(commitResult.getFailures().get(handle).getResultCode())
                .isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(commitResult.getFailures().get(handle).getErrorMessage())
                .contains("The blob content doesn't match to the digest");
    }

    // TODO(b/273591938) add test to get storage after abandon a blob, when abandon blob API is
    //  ready.
    @Test
    public void testGetStorageInfo() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));

        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data1 = generateRandomBytes(10 * 1024); // 10 KiB
        byte[] data2 = generateRandomBytes(20 * 1024); // 20 KiB
        byte[] digest1 = calculateDigest(data1);
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, mPackageName, DB_NAME_1, "namespace");
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, mPackageName, DB_NAME_1, "namespace");

        AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle1, handle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                writeResponse.getResult();
        assertTrue(writeResult.isSuccess());

        ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(handle1);
        try (OutputStream outputStream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
            outputStream.write(data1);
            outputStream.flush();
        }

        ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(handle2);
        try (OutputStream outputStream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
            outputStream.write(data2);
            outputStream.flush();
        }
        writeResponse.close();

        StorageInfo storageInfo = mDb1.getStorageInfoAsync().get();
        assertThat(storageInfo.getBlobCount()).isEqualTo(2);
        assertThat(storageInfo.getBlobSizeBytes()).isEqualTo(30 * 1024);
    }

    @Test
    public void testCloseWriteResponse() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] data2 = generateRandomBytes(20); // 20 Bytes
        byte[] digest1 = calculateDigest(data1);
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, mPackageName, DB_NAME_1, "ns");
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, mPackageName, DB_NAME_1, "ns");

        AppSearchOpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(handle1, handle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                writeResponse.getResult();
        assertTrue(writeResult.isSuccess());

        // Close the response will also close all fds.
        writeResponse.close();

        assertThrows(IOException.class, () -> {
            try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
                    writeResult.getSuccesses().get(handle1))) {
                outputStream.write(data1);
            }
        });
        assertThrows(IOException.class, () -> {
            try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
                    writeResult.getSuccesses().get(handle2))) {
                outputStream.write(data2);
            }
        });
    }

    @Test
    public void testCloseReadResponse() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] data2 = generateRandomBytes(20); // 20 Bytes
        byte[] digest1 = calculateDigest(data1);
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle handle1 = AppSearchBlobHandle.createWithSha256(
                digest1, mPackageName, DB_NAME_1, "ns");
        AppSearchBlobHandle handle2 = AppSearchBlobHandle.createWithSha256(
                digest2, mPackageName, DB_NAME_1, "ns");

        try (AppSearchOpenBlobForWriteResponse writeResponse =
                     mDb1.openBlobForWriteAsync(ImmutableSet.of(handle1, handle2)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(handle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
                outputStream.write(data1);
                outputStream.flush();
            }

            ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(handle2);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
                outputStream.write(data2);
                outputStream.flush();
            }
        }

        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(handle1, handle2)).get().getResult()
                .isSuccess());

        byte[] readBytes1 = new byte[10]; // 10 Bytes
        byte[] readBytes2 = new byte[20]; // 20 Bytes

        AppSearchOpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(handle1, handle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                readResponse.getResult();
        assertTrue(readResult.isSuccess());

        // Close the response will also close all fds.
        readResponse.close();

        assertThrows(IOException.class, () -> {
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readResult.getSuccesses().get(handle1))) {
                inputStream.read(readBytes1);
            }
        });
        assertThrows(IOException.class, () -> {
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readResult.getSuccesses().get(handle2))) {
                inputStream.read(readBytes2);
            }
        });
    }

    @Test
    public void testSetBlobSchema() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setDescription("this is a blob.")
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
    }

    @Test
    public void testPutDocumentWithBlobProperty() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setDescription("this is a blob.")
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "Type")
                        .setPropertyBlobHandle("blob", handle)
                        .build();

        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(document).build())
                .get();

        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("id")
                        .build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id")).isEqualTo(document);
    }

    @Test
    public void testWriteAndReadBlob_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.openBlobForWriteAsync(ImmutableSet.of(handle)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.commitBlobAsync(ImmutableSet.of(handle)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.openBlobForReadAsync(ImmutableSet.of(handle)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testSetBlobSchema_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build())
                        .get());
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }

    @Test
    public void testPutDocumentWithBlobProperty_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));

        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyBlobHandle("blob", handle)
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.putAsync(new PutDocumentsRequest.Builder()
                                .addGenericDocuments(document).build()).get());
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }
}
