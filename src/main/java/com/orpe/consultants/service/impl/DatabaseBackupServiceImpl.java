package com.orpe.consultants.service.impl;



import com.orpe.consultants.service.DatabaseBackupService;
import com.orpe.consultants.utils.DatabaseBackupUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DatabaseBackupServiceImpl implements DatabaseBackupService {

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${backup.path}")
    private String backupPath;

    @Value("${mysql.dump.path}")
    private String dumpPath;

    @Override
    public File backupDatabase() {
        try {
            String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
            if (dbName.contains("?")) {
                dbName = dbName.substring(0, dbName.indexOf("?"));
            }

            return DatabaseBackupUtil.createBackup(
                    dumpPath,
                    dbUser,
                    dbPassword,
                    dbName,
                    backupPath
            );
        } catch (Exception e) {
            throw new RuntimeException("Backup failed", e);
        }
    }

}


