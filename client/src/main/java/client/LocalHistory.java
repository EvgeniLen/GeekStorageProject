package client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LocalHistory {
    private static final String DIR = "History" + File.separator;

    static {
        File directory = new File(DIR);
        if (!directory.exists())
            directory.mkdir();
    }

    public static void writeHistory(String filename, String msg){
        Writer writer = null;
        try {
            writer = new FileWriter(DIR + filename, true);
            writer.write(msg);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer!=null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> getHistory(String historyFile){
        if (new File(DIR + historyFile).exists()) {
            try {
                return Files.readAllLines(Paths.get(DIR + historyFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    public static String getFileName(String login){
        return String.format("history_%s.txt", login);

    }

}
