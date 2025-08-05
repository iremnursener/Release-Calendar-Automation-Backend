package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.entities.EmailTemplate;
import com.example.ReleaseRadar.enums.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IEmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByName(String templateName);
}
