package com.autowizard.document.internal;

import com.autowizard.shared.BusinessRuleException;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Checks an uploaded file before it is stored.
 *
 * <p>
 * The type is detected from the first bytes of the file (its signature), not taken from the
 * client: the stored type is later sent back as the {@code Content-Type} of the download, and
 * a file declared as a PDF but containing HTML would be served as a page in the user's
 * browser. Only the formats the documents of a vehicle come in are accepted: PDF, JPEG, PNG.
 * </p>
 */
final class DocumentFiles {

    static final int MAX_NAME_LENGTH = 255;
    /** Bytes needed to recognize every accepted format: the longest signature, PNG's. */
    static final int SIGNATURE_LENGTH = 8;

    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private DocumentFiles() {
    }

    /**
     * The content type of an accepted file, detected from its first bytes (at least
     * {@value #SIGNATURE_LENGTH}, or the whole file if shorter).
     */
    static String detectContentType(byte[] content) {
        if (content.length == 0) {
            throw new BusinessRuleException("The file is empty");
        }
        return detect(content).orElseThrow(() ->
                new UnsupportedDocumentException("Only PDF, JPEG and PNG files are accepted"));
    }

    private static Optional<String> detect(byte[] content) {
        if (startsWith(content, PDF)) return Optional.of(MediaType.APPLICATION_PDF_VALUE);
        if (startsWith(content, PNG)) return Optional.of(MediaType.IMAGE_PNG_VALUE);
        if (startsWith(content, JPEG)) return Optional.of(MediaType.IMAGE_JPEG_VALUE);
        return Optional.empty();
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }

    /**
     * The name to store: the last path segment only (some clients send a full path),
     * without control characters, at most {@value #MAX_NAME_LENGTH} characters.
     */
    static String safeName(String originalName) {
        String name = originalName == null ? "" : originalName;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            name = "document";
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }
}
