package com.example.ReleaseRadar.services.importflow.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@AllArgsConstructor
@Getter

public final class ImportContext {
    private final UUID batchId;
    private final MultipartFile file;
}
