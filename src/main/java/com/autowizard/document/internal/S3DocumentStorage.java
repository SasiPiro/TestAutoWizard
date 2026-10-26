package com.autowizard.document.internal;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.util.UUID;

/**
 * {@link DocumentStorage} on any S3-compatible store: Amazon S3 in the cloud, SeaweedFS in
 * Docker Compose and in the tests.
 *
 * <p>
 * Keys are random ({@code documents/<uuid>}), never derived from the user's file name: names
 * often contain personal data, and a key must not be guessable or collide.
 * </p>
 */
@Component
class S3DocumentStorage implements DocumentStorage {

    private static final String PREFIX = "documents/";

    private final S3Client s3;
    private final String bucket;

    S3DocumentStorage(S3Client s3, StorageProperties properties) {
        this.s3 = s3;
        this.bucket = properties.bucket();
    }

    @Override
    public String put(InputStream content, long size, String contentType) {
        String key = PREFIX + UUID.randomUUID();
        s3.putObject(request -> request.bucket(bucket).key(key).contentType(contentType).contentLength(size),
                RequestBody.fromInputStream(content, size));
        return key;
    }

    @Override
    public InputStream get(String key) {
        return s3.getObject(request -> request.bucket(bucket).key(key));
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(request -> request.bucket(bucket).key(key));
    }
}
