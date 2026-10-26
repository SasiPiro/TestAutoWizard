package com.autowizard.document.internal;

import com.autowizard.document.Document;
import com.autowizard.identity.UserRepository;
import com.autowizard.shared.NotFoundException;
import com.autowizard.shared.TimeService;
import com.autowizard.vehicle.Vehicle;
import com.autowizard.vehicle.VehicleAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Documents are a database row (metadata) plus a file in object storage. The two stores have no
 * common transaction, so the order of the writes keeps one rule: <b>a row never points to a
 * missing file</b>. The file is written before the row and deleted after it; when something fails
 * in between, what is left is at most a file without a row, which nobody can reach.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentStorage storage;
    private final TimeService timeService;
    private final VehicleAccess vehicleAccess;
    private final UserRepository userRepository;

    /**
     * Not transactional on purpose: the file goes to storage without holding a database
     * connection for the length of the transfer. The row is inserted afterwards in its own
     * transaction ({@code save}); if the insert fails, the file is removed again.
     */
    public DocumentResponse uploadDocument(MultipartFile file, Document.DocumentType type, Long vehicleId, Long userId) {
        Vehicle vehicle = vehicleAccess.requireOwned(vehicleId, userId);
        String contentType = DocumentFiles.detectContentType(firstBytes(file));

        String key;
        try (InputStream content = file.getInputStream()) {
            key = storage.put(content, file.getSize(), contentType);
        } catch (IOException e) {
            // The upload could not be read on the server side: a 500, not the client's fault
            throw new UncheckedIOException(e);
        }

        Document document = new Document();
        document.setName(DocumentFiles.safeName(file.getOriginalFilename()));
        document.setType(type);
        document.setStorageKey(key);
        document.setSize(file.getSize());
        document.setContentType(contentType);
        document.setVehicle(vehicle);
        document.setUploadDate(timeService.now());
        document.setUser(userRepository.getReferenceById(userId));

        Document saved;
        try {
            saved = documentRepository.save(document);
        } catch (RuntimeException e) {
            deleteFile(key);
            throw e;
        }
        log.info("[DOCUMENT_UPLOAD] outcome=success documentId={} vehicleId={} type={} contentType={} size={}",
                saved.getId(), vehicleId, type, contentType, file.getSize());
        return toResponse(saved);
    }

    /**
     * The metadata comes from the database, the content is a stream from storage opened after
     * the query: no transaction or connection is held while the file is sent.
     */
    public DocumentContent getDocument(Long id, Long userId) {
        DocumentRepository.StoredFile file = documentRepository.findFileByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        return new DocumentContent(file.name(), file.contentType(), file.size(), storage.get(file.storageKey()));
    }

    /** Metadata only: the files are not read. */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByVehicleId(Long vehicleId, Long userId) {
        vehicleAccess.requireOwned(vehicleId, userId);
        return documentRepository.findSummariesByVehicleId(vehicleId);
    }

    /** The row first; the file only once the deletion is committed. */
    @Transactional
    public void deleteDocument(Long id, Long userId) {
        String key = documentRepository.findStorageKeyByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new NotFoundException("Document not found"));
        documentRepository.deleteMetadataById(id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFile(key);
            }
        });
        log.info("[DOCUMENT_DELETE] outcome=success documentId={}", id);
    }

    /** A file left behind is unreachable and only takes space: logged, not an error for the caller. */
    private void deleteFile(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException e) {
            log.warn("[DOCUMENT_FILE_DELETE] outcome=failure storageKey={} reason={}", key, e.getClass().getSimpleName());
        }
    }

    /** The first bytes of the upload, enough to recognize its format. */
    private static byte[] firstBytes(MultipartFile file) {
        try (InputStream content = file.getInputStream()) {
            return content.readNBytes(DocumentFiles.SIGNATURE_LENGTH);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getName(), document.getType(),
                document.getContentType(), document.getUploadDate(), document.getVehicle().getId());
    }
}
