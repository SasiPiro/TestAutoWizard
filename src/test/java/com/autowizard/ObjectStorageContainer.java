package com.autowizard;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * An S3-compatible object store in Docker (SeaweedFS, the same image as compose.yaml), with one
 * bucket and signed requests only: unsigned ones are refused with 403, which is also how the
 * wait strategy knows the S3 endpoint is up.
 */
public class ObjectStorageContainer extends GenericContainer<ObjectStorageContainer> {

    public static final String BUCKET = "autowizard-documents";
    public static final String ACCESS_KEY = "test-access-key";
    public static final String SECRET_KEY = "test-secret-key";

    private static final int S3_PORT = 8333;

    public ObjectStorageContainer() {
        super("chrislusf/seaweedfs:4.45");
        withCommand("mini", "-dir=/data", "-bucket=" + BUCKET);
        withEnv("AWS_ACCESS_KEY_ID", ACCESS_KEY);
        withEnv("AWS_SECRET_ACCESS_KEY", SECRET_KEY);
        withExposedPorts(S3_PORT);
        waitingFor(Wait.forHttp("/" + BUCKET).forPort(S3_PORT).forStatusCode(403));
    }

    public String endpoint() {
        return "http://" + getHost() + ":" + getMappedPort(S3_PORT);
    }
}
