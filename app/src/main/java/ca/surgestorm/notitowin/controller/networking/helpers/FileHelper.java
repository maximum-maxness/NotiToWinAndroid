package ca.surgestorm.notitowin.controller.networking.helpers;

import java.io.File;
import java.io.IOException;

public class FileHelper {

    public static final String NAME = "testDir";
    public static final String USER_HOME = System.getProperty("user.home");
    public static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String WORKING_DIRECTORY = System.getProperty("user.dir");
    public static final String OS = System.getProperty("os.name");

    public static String getWorkingDirectoryPath() {
        return WORKING_DIRECTORY;
    }

    public static File makeTempFile(String name, String extension) throws IOException {
        File tmpDirectory = new File(TEMP_DIR);
        File outFile = File.createTempFile(name, extension, tmpDirectory);
        outFile.deleteOnExit();
        return outFile;
    }

    public static String getStorePath() {
        String result;
        switch (OS.toLowerCase()) {
            case "windows":
                result = USER_HOME + "\\" + NAME + "\\";
                break;
            case "linux":
            case "mac":
                result = USER_HOME + "/" + NAME + "/";
                break;
            default:
                result = WORKING_DIRECTORY;
        }
        verifyDirPath(result);
        return result;
    }

    private static void verifyDirPath(String path) {
        File directory = new File(path);
        if (directory.exists()) {
            directory.mkdir();
        }
    }

    public static boolean fileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static File verifyFilePath(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.getParentFile().mkdir();
            file.createNewFile();
        }
        file.deleteOnExit();
        return file;
    }
}
