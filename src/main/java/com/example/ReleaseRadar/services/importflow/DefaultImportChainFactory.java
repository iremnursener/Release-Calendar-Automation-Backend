package com.example.ReleaseRadar.services.importflow;

import com.example.ReleaseRadar.services.impl.EmailReminderServiceImpl;
import com.example.ReleaseRadar.services.impl.ExcelParserServiceImpl;
import com.example.ReleaseRadar.services.importflow.handler.CreateRemindersHandler;
import com.example.ReleaseRadar.services.importflow.handler.ExcelParseHandler;
import com.example.ReleaseRadar.services.importflow.handler.ImportHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultImportChainFactory implements IImportChainFactory{

    private final ExcelParserServiceImpl excelParserService;
    private final EmailReminderServiceImpl emailReminderService;

    @Override
    public ImportHandler create() {
        ImportHandler parse = new ExcelParseHandler(excelParserService);
        ImportHandler reminders = new CreateRemindersHandler(emailReminderService);
        parse.linkWith(reminders);
        return parse;
    }
}
