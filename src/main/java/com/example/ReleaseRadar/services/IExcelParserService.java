package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.Color;
import com.example.ReleaseRadar.entities.Event;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IExcelParserService {


    public List<Event> parseExcelByBatch(UUID batchId, MultipartFile file) throws IOException;
    public Map<String, Color> loadColorLegendFromDB();
    public String normalizeHex(String hex);
}
