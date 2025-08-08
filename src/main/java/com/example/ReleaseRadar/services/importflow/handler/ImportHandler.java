package com.example.ReleaseRadar.services.importflow.handler;

import java.io.IOException;

public abstract class ImportHandler {

    private ImportHandler next;

    public ImportHandler linkWith(ImportHandler next) {
        this.next = next;
        return next;
    }

    public final void handle(ImportContext ctx) throws IOException {
        doHandle(ctx);
        if (next != null) next.handle(ctx);
    }

    protected abstract void doHandle(ImportContext ctx) throws IOException;
}
