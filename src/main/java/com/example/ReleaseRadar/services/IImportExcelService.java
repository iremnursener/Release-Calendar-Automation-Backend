package com.example.ReleaseRadar.services;

import org.springframework.web.multipart.MultipartFile;

public interface IImportExcelService {

    public void importExcel(MultipartFile file);

}
