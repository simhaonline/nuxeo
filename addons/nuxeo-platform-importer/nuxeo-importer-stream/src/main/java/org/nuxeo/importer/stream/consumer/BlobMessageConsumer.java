/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.importer.stream.message.BlobMessage;
import org.nuxeo.lib.stream.pattern.consumer.AbstractConsumer;
import org.nuxeo.runtime.api.Framework;

/**
 * Import BlobMessage into a Nuxeo BlobProvider, persist BlobInformation.
 *
 * @since 9.1
 */
public class BlobMessageConsumer extends AbstractConsumer<BlobMessage> {
    protected static final AtomicInteger consumerCounter = new AtomicInteger(0);

    protected BlobProvider blobProvider;

    protected final String blobProviderName;

    protected final BlobInfoWriter blobInfoWriter;

    public BlobMessageConsumer(String consumerId, String blobProviderName, BlobInfoWriter blobInfoWriter) {
        super(consumerId);
        this.blobProviderName = blobProviderName;
        if (!StringUtils.isBlank(blobProviderName)) {
            this.blobProvider = Framework.getService(BlobManager.class).getBlobProvider(blobProviderName);
            if (blobProvider == null) {
                throw new IllegalArgumentException("Invalid blob provider: " + blobProviderName);
            }
        }
        // when there is no blob provider we don't upload the blobs
        this.blobInfoWriter = blobInfoWriter;
    }

    @Override
    public void begin() {

    }

    @Override
    public void accept(BlobMessage message) {
        try {
            MyBlob blob = getBlob(message);
            try {
                String digest = null;
                if (blobProvider != null) {
                    digest = blobProvider.writeBlob(blob.getBlob());
                }
                long length = blob.getBlob().getLength();
                saveBlobInfo(message, digest, length, blob.getBlob().getFile());
            } finally {
                blob.clean();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid blob: " + message, e);
        }
    }

    protected MyBlob getBlob(BlobMessage message) {
        Blob blob;
        if (message.getPath() != null) {
            blob = new FileBlob(new File(message.getPath()));
        } else {
            // we don't submit filename or encoding this is not saved in the binary store but in the document
            blob = new StringBlob(message.getContent(), null, null, null);
        }
        return new MyBlob(blob);
    }

    protected void saveBlobInfo(BlobMessage message, String digest, long length, File blobFile) {
        BlobInfo bi = new BlobInfo();
        bi.digest = digest;
        bi.key = blobProviderName + ":" + bi.digest;
        bi.length = length;
        if (digest == null) {
            // the blob is not uploaded use the blob info to pass the file path
            bi.filename = blobFile.getAbsolutePath();
        } else {
            bi.filename = message.getFilename();
        }
        bi.mimeType = message.getMimetype();
        bi.encoding = message.getEncoding();
        blobInfoWriter.save(null, bi);
    }

    @Override
    public void commit() {

    }

    @Override
    public void rollback() {

    }

    public class MyBlob {
        protected final Blob blob;

        protected final String fileToDelete;

        public MyBlob(Blob blob) {
            this(blob, null);
        }

        public MyBlob(Blob blob, String fileToDelete) {
            this.blob = blob;
            this.fileToDelete = fileToDelete;
        }

        public Blob getBlob() {
            return blob;
        }

        public String getFileToDelete() {
            return fileToDelete;
        }

        public void clean() {
            if (fileToDelete != null) {
                try {
                    Files.delete(Paths.get(fileToDelete));
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}