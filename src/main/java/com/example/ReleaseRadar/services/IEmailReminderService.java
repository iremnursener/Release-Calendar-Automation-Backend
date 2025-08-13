package com.example.ReleaseRadar.services;

import java.io.IOException;

public interface IEmailReminderService {

    public void createRemindersForImportBatch(String batchIdStr)throws IOException ;
}
