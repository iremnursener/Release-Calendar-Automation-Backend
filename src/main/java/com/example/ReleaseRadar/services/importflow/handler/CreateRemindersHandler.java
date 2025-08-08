package com.example.ReleaseRadar.services.importflow.handler;

import com.example.ReleaseRadar.services.impl.EmailReminderServiceImpl;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class CreateRemindersHandler extends ImportHandler {

    private final EmailReminderServiceImpl emailReminderService;

    @Override
    protected void doHandle(ImportContext ctx) throws IOException {
        emailReminderService.createRemindersForImportBatch(ctx.getBatchId().toString());
    }
}
