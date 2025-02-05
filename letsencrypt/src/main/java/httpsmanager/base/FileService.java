package httpsmanager.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;

public class FileService {

    private FileService() {
    }
    
    public static String loadTextFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            return null;
        }
    }

    public static void saveTextFile(File file, String text) {
        file.getParentFile().mkdirs();
        try (FileWriter w = new FileWriter(file)) {
            w.write(text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void copy(File sourceFile, File targetFile) {
        try {
            targetFile.getParentFile().mkdirs();
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Error copying file '" + sourceFile.getAbsolutePath() + "' to '"
                    + targetFile.getAbsolutePath() + "'", e);
        }
    }
    
    public static void deleteFolder(File folder) {
        try {
            FileUtils.deleteDirectory(folder);
        } catch (IOException ignore) {
        }
    }
}
