package com.autowizard.document.internal;

import java.io.InputStream;

/**
 * Where document files live: an object store, not the database. A {@code documents} row keeps
 * the metadata and the key of its file (claim check: whoever needs the file, such as the AI
 * worker, gets the key and reads the file directly from the store).
 */
public interface DocumentStorage {

    /** Stores a file under a new random key and returns the key. */
    String put(InputStream content, long size, String contentType);

    /** The file stored under the key; the caller closes the stream. */
    InputStream get(String key);

    /** Removes the file stored under the key; nothing happens if there is none. */
    void delete(String key);
}
