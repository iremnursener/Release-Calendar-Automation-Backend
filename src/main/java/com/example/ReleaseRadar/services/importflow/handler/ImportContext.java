package com.example.ReleaseRadar.services.importflow.handler;


import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;


public record ImportContext(UUID batchId, MultipartFile file) {}
