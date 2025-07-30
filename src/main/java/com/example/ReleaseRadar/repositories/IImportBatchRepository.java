package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IImportBatchRepository extends JpaRepository<ImportBatch, UUID> {
}
