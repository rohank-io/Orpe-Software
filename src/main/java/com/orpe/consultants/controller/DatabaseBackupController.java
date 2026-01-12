package com.orpe.consultants.controller;



import com.orpe.consultants.service.DatabaseBackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;

@Controller
@RequestMapping("/backup")
public class DatabaseBackupController {

    private final DatabaseBackupService backupService;

    public DatabaseBackupController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public String backupPage() {
        return "backup"; // backup.html / backup.jsp
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBackup() {

        File file = backupService.backupDatabase();

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + file.getName())
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(resource);
    }
}
