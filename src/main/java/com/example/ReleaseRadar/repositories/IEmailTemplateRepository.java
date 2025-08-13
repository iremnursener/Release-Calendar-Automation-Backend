package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IEmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByName(String templateName);
}
