package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.entities.Event;

public interface IEmailReminderService {

    public EmailReminder createReminder(Event event);
}
