package com.autowizard.document.internal;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * Object storage of the document files ({@code app.storage.*}).
 *
 * @param bucket    the bucket holding the files; required, the application does not start without it
 * @param endpoint  the store's URL for an S3-compatible store (e.g. SeaweedFS); empty for Amazon S3
 * @param region    the AWS region (any value for an S3-compatible store)
 * @param accessKey static credentials; empty to use the default AWS chain (environment, instance role)
 * @param secretKey static credentials, with {@code accessKey}
 * @param pathStyle {@code http://host/bucket/key} instead of {@code http://bucket.host/key}, as
 *                  stores without per-bucket DNS names need
 */
@Validated
@ConfigurationProperties("app.storage")
record StorageProperties(
        @NotBlank String bucket,
        URI endpoint,
        @NotBlank String region,
        String accessKey,
        String secretKey,
        boolean pathStyle) {
}
