package com.example.ReleaseRadar.services.importflow;

import com.example.ReleaseRadar.services.impl.EmailReminderServiceImpl;
import com.example.ReleaseRadar.services.impl.ExcelParserServiceImpl;
import com.example.ReleaseRadar.services.importflow.handler.ImportHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class DefaultImportChainFactoryTest {

    @Test
    void create_returnsNonNullChain() {
        ExcelParserServiceImpl excelParser = Mockito.mock(ExcelParserServiceImpl.class);
        EmailReminderServiceImpl emailReminder = Mockito.mock(EmailReminderServiceImpl.class);

        DefaultImportChainFactory factory = new DefaultImportChainFactory(excelParser, emailReminder);
        ImportHandler chain = factory.create();

        assertNotNull(chain);
    }
}