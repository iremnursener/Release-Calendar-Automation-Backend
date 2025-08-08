package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.entities.Event;

import java.io.IOException;

public interface IEmailReminderService {

    public void createRemindersForImportBatch(String batchIdStr)throws IOException ;
}
