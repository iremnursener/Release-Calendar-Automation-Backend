package com.example.ReleaseRadar.services.importflow.handler;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public final class ImportContext {
    private final UUID batchId;
    private final MultipartFile file;

    public ImportContext(UUID batchId, MultipartFile file) {
        this.batchId = batchId;
        this.file = file;
    }

    public UUID getBatchId() { return batchId; }
    public MultipartFile getFile() { return file; }
}
