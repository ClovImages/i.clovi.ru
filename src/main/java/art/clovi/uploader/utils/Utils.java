package art.clovi.uploader.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.HashMap;

import static art.clovi.uploader.Uploader.fileDeletes;
import static art.clovi.uploader.Uploader.mainFolder;
import static java.lang.Long.parseLong;

public class Utils {
    public static String isToString(InputStream is) throws IOException {
        byte[] requestBodyBytes = is.readAllBytes();
        return new String(requestBodyBytes);
    }

    public static HashMap<String, File> files = new HashMap<>();
    public static File getFileByID(String id){
        File needFile = null;
        if(files.containsKey(id)) needFile = files.get(id);
        else {
            for (File file : mainFolder.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName().split("\\.")[0];
                    if (name.equals(id)) {
                        needFile = file;
                        files.put(id, file);
                        break;
                    }
                }
            }
        }
        if(needFile != null && !needFile.exists()) {
            files.remove(id);
            needFile = null;
        }
        return needFile;
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static String makeID(int length, boolean isDelete) {
        StringBuilder result = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int charactersLength = characters.length();
        int counter = 0;
        while (counter < length) {
            result.append(characters.charAt(SECURE_RANDOM.nextInt(charactersLength)));
            counter += 1;
        }
        return isIDCorrect(result.toString(), isDelete) ? result.toString() : makeID(length, isDelete);
    }
    public static String makeID(int length) {
        StringBuilder result = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        int charactersLength = characters.length();
        int counter = 0;
        while (counter < length) {
            result.append(characters.charAt(SECURE_RANDOM.nextInt(charactersLength)));
            counter += 1;
        }
        return result.toString();
    }

    public static boolean isIDCorrect(String id, boolean isDelete) {
        if (!isDelete) {
            for (File file : mainFolder.listFiles())
                if (file.isFile())
                    if (file.getName().split("\\.")[0].equals(id)) return false;
        } else return !fileDeletes.containsKey(id);
        return true;
    }

    static long kilo = 1024;
    static long mega = kilo * kilo;
    static long giga = mega * kilo;
    static long tera = giga * kilo;

    public static String getParsedFileSize(long size) {
        String s;
        double kb = (double) size / kilo;
        double mb = kb / kilo;
        double gb = mb / kilo;
        double tb = gb / kilo;
        if (size < kilo) s = size + " Bytes";
        else if (size < mega) s = String.format("%.2f", kb) + " KB";
        else if (size < giga) s = String.format("%.2f", mb) + " MB";
        else if (size < tera) s = String.format("%.2f", gb) + " GB";
        else s = String.format("%.2f", tb) + " TB";
        return s;
    }

    public static long getKilo(String size) {
        String type = size.replaceAll("[0-9]", "");
        String factSize = size.replaceAll("[^0-9]", "");
        return switch (type.toLowerCase()){
            case "mb" -> parseLong(factSize)*mega;
            case "gb" -> parseLong(factSize)*giga;
            case "tb" -> parseLong(factSize)*tera;
            default -> parseLong(factSize);
        };
    }
}
