package com.autowizard.document.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
class StorageConfig {

    /**
     * One client for the application (it is thread-safe and holds the connection pool),
     * closed with the context.
     *
     * <p>
     * Checksums only where the S3 API requires them: since SDK 2.30 every upload carries a
     * CRC32 checksum by default, which S3-compatible stores do not all accept. The transfer is
     * still protected by TCP and, in production, by TLS.
     * </p>
     */
    @Bean
    S3Client documentStorageClient(StorageProperties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .httpClientBuilder(ApacheHttpClient.builder())
                .region(Region.of(properties.region()))
                .forcePathStyle(properties.pathStyle())
                .credentialsProvider(credentials(properties))
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
        if (properties.endpoint() != null) {
            builder.endpointOverride(properties.endpoint());
        }
        return builder.build();
    }

    private static AwsCredentialsProvider credentials(StorageProperties properties) {
        if (StringUtils.hasText(properties.accessKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        }
        return DefaultCredentialsProvider.builder().build();
    }
}
