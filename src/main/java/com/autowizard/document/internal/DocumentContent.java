package com.autowizard.document.internal;

import java.io.InputStream;

/**
 * A file to download, streamed from object storage to the response: never held in memory and
 * never serialized as JSON. Whoever writes the content closes it.
 */
public record DocumentContent(String name, String contentType, long size, InputStream content) {
}
