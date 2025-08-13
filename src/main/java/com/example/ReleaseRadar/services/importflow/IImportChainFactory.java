package com.example.ReleaseRadar.services.importflow;

import com.example.ReleaseRadar.services.importflow.handler.ImportHandler;

public interface IImportChainFactory {
    ImportHandler create();
}
