package com.orpe.consultants.utils;



import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseBackupUtil {

    private DatabaseBackupUtil() {}

    public static File createBackup(
            String dumpPath,
            String dbUser,
            String dbPassword,
            String dbName,
            String backupDir) throws Exception {

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        File dir = new File(backupDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Unable to create backup directory");
        }

        File backupFile = new File(dir,
                "db_backup_" + dbName + "_" + timestamp + ".sql");

        String command = String.format(
                "\"%s\" -u%s -p%s %s",
                dumpPath, dbUser, dbPassword, dbName
        );

        ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", command
        );

        pb.redirectOutput(backupFile);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Database backup failed, exit code: " + exitCode);
        }

        return backupFile;
    }
}

 
