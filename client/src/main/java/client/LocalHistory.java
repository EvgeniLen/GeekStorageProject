package client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class LocalHistory {
    private static final String DIR = "History" + File.separator;
    private static Writer writer;

    static {
        File directory = new File(DIR);
        if (!directory.exists())
            directory.mkdir();
    }

    public static void start(String filename){
        try {
            writer = new FileWriter(DIR + filename, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        try {
            if (writer!=null) {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeHistory(String msg){
        try {
            writer.write(msg + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getHistory(String historyFile){
        if (!new File(DIR + historyFile).exists()){
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            List<String> history = Files.readAllLines(Paths.get(DIR + historyFile));
            for (int i = history.size() > 100? history.size() - 100 : 0; i < history.size(); i++) {
                sb.append(history.get(i)).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getFileName(String login){
        return String.format("history_%s.txt", login);

    }

}
