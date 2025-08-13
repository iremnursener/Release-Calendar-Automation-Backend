package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.ImportBatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
public interface IImportBatchEventRepository extends JpaRepository<ImportBatchEvent, UUID> {

    List<ImportBatchEvent> findByBatch_Id(UUID batchId);


}
