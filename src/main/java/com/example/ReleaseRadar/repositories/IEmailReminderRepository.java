package com.example.ReleaseRadar.repositories;

import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.enums.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface IEmailReminderRepository extends JpaRepository<EmailReminder, Long> {


    List<EmailReminder> findBySendDatetimeBeforeAndStatus(LocalDateTime now, ReminderStatus status);
}
