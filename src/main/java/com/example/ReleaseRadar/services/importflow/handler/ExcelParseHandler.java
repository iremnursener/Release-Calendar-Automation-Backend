package com.example.ReleaseRadar.services.importflow.handler;

import com.example.ReleaseRadar.services.impl.ExcelParserServiceImpl;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class ExcelParseHandler extends ImportHandler {

    private final ExcelParserServiceImpl excelParserService;

    @Override
    protected void doHandle(ImportContext ctx) throws IOException {
        excelParserService.parseExcelByBatch(ctx.getBatchId(), ctx.getFile());
    }
}
