package art.clovi.uploader;

import art.clovi.uploader.utils.MultipartParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import express.Express;
import express.middleware.FileStatics;
import express.middleware.Middleware;
import express.utils.MediaType;
import express.utils.Status;
import ru.kelcuprum.caffeinelib.CoffeeLogger;
import ru.kelcuprum.caffeinelib.config.Config;
import ru.kelcuprum.caffeinelib.utils.GsonHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static art.clovi.uploader.utils.Utils.isToString;
import static art.clovi.uploader.utils.Utils.makeID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static art.clovi.uploader.DiscordWebhooks.sendBanUser;
import static art.clovi.uploader.Objects.*;

public class Uploader {
    public static Express server;
    public static Config config = new Config("./config.json");
    public static Config bans = new Config("./bans.json");
    public static Config links = new Config("./files.json");
    public static File mainFolder = new File("./files");
    public static String html = "";
    public static String embedHtml = "";
    public static String adminHtml = "";
    public static String banHtml = "";
    public static byte[] favicon = null;
    public static Config release = new Config(new JsonObject());
    public static HashMap<String, String> fileNames = new HashMap<>();
    public static HashMap<String, String> fileDeletes = new HashMap<>();
    public static HashMap<String, String> fileTypes = new HashMap<>();
    public static String secretKey = config.getString("secret_token", "");

    public static void main(String[] args) throws IOException {
        InputStream releaseFile = Uploader.class.getResourceAsStream("/index.html");
        if (releaseFile != null) html = new String(releaseFile.readAllBytes(), StandardCharsets.UTF_8);
        InputStream embedFile = Uploader.class.getResourceAsStream("/embed.html");
        if (embedFile != null) embedHtml = new String(embedFile.readAllBytes(), StandardCharsets.UTF_8);
        InputStream adminFile = Uploader.class.getResourceAsStream("/admin.html");
        if (adminFile != null) adminHtml = new String(adminFile.readAllBytes(), StandardCharsets.UTF_8);
        InputStream banFile = Uploader.class.getResourceAsStream("/ban.html");
        if (banFile != null) banHtml = new String(banFile.readAllBytes(), StandardCharsets.UTF_8);
        try {
            InputStream kek = Uploader.class.getResourceAsStream("/release.json");
            release = new Config(GsonHelper.parseObject(new String(kek.readAllBytes(), StandardCharsets.UTF_8)));
            favicon = Uploader.class.getResourceAsStream("/favicon.ico").readAllBytes();
        } catch (IOException e) {
            LOG.debug(e.getMessage());
        }
        JsonArray h = links.getJsonArray("names", new JsonArray());
        links.load();
        for (JsonElement element : h) {
            fileNames.put(((JsonObject) element).get("id").getAsString(), ((JsonObject) element).get("name").getAsString());
            if (((JsonObject) element).has("delete") && !((JsonObject) element).get("delete").isJsonNull())
                fileDeletes.put(((JsonObject) element).get("id").getAsString(), ((JsonObject) element).get("delete").getAsString());
            if (((JsonObject) element).has("type") && !((JsonObject) element).get("type").isJsonNull())
                fileTypes.put(((JsonObject) element).get("id").getAsString(), ((JsonObject) element).get("type").getAsString());
        }
        LOG.log("Hello, world!");
        mainFolder = new File(config.getString("folder", "./files"));
        checkFolders();
        server = new Express();
        server.use(Middleware.cors());
        server.use((req, res) -> LOG.log(String.format("%s сделал запрос на %s", req.getIp(), req.getPath())));
        server.use((req, res) -> {
            String jsonObject = bans.getString(req.getIp(), null);
            if (jsonObject != null) {
                LOG.log(String.format("[BANNED] %s сделал запрос на %s", req.getIp(), req.getPath()));
                res.setStatus(403);

                if (banHtml != null && !banHtml.isBlank()) {
                    res.setContentType(MediaType._html);
                    res.send(banHtml.replace("{reason}", jsonObject));
                } else {
                    res.send(String.format("You has been blocked! Reason: %s", jsonObject));
                }
            }
        });
        // -=-=-=-=-
        if (adminHtml != null && !adminHtml.isBlank()) {
            server.post("/admin/update_config", (req, res) -> {
                if(req.getCookie("uploader_peepohuy") != null && req.getCookie("uploader_peepohuy").getValue().equals(secretKey)) {
                    try {
                        InputStream IS = req.getBody();
                        LOG.log((IS == null) + "");
                        String isString = IS == null ? "{}" : isToString(IS);
                        LOG.log(isString);
                        if (IS != null && !isString.isEmpty()) {
                            Files.writeString(Path.of("./config.json"), isString, StandardCharsets.UTF_8);
                            DiscordWebhooks.sendRerunning(req);
                            res.json("{\"message\": \"Config has been updated!\", \"status\": true}");
                            server.stop();
                            System.exit(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            server.get("/admin/ban", (req, res) -> {
                if(req.getCookie("uploader_peepohuy") != null && req.getCookie("uploader_peepohuy").getValue().equals(secretKey)) {
                    if(req.getQuery("user") == null){
                        res.setStatus(400);
                        res.json(BAD_REQUEST);
                        return;
                    }
                    try {
                        String user = req.getQuery("user");
                        String reason = req.getQuery("reason");
                        String ban = bans.getString(user, null);
                        String status;
                        if(ban == null) {
                            bans.setString(user, reason == null ? "" : reason);
                            status = "Blocked";
                        } else if(reason != null) {
                            bans.setString(user, reason);
                            status = "Updated reason";
                        } else {
                            bans.setString(user, null);
                            status = "Unblocked";
                        }
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("status", status);
                        jsonObject.addProperty("user", user);
                        jsonObject.addProperty("reason", reason);
                        bans.save();
                        sendBanUser(user, reason, status, req);
                        res.json(jsonObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            server.all("/admin/delete/:id", (req, res) -> {
                if(req.getCookie("uploader_peepohuy") != null && req.getCookie("uploader_peepohuy").getValue().equals(secretKey)) {
                    String id = req.getParam("id").split("\\.")[0];
                    for (File file : mainFolder.listFiles()) {
                        if (file.isFile()) {
                            String name = file.getName().split("\\.")[0];
                            if (name.equals(id)) {
                                file.delete();
                                DiscordWebhooks.sendDeleteFile(fileNames.get(name), "/" + name, fileTypes.getOrDefault(name, "file"), name, req);
                                fileNames.remove(name);
                                fileDeletes.remove(name);
                                res.send("File deleted");
                                System.gc();
                                break;
                            }
                        }
                    }
                    res.send("File not deleted");
                }
            });
            server.get("/1984", (req, res) -> {
                if(req.getCookie("uploader_peepohuy") != null && req.getCookie("uploader_peepohuy").getValue().equals(secretKey)){
                    res.setContentType(MediaType._html);
                    String configString = config.toString();
                    try {
                        configString = Files.readString(Path.of("./config.json"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    res.send(adminHtml.replace("{config}", configString));
                }
            });
        }
        server.all((req, res) -> {
            String[] mainHostnames = config.getString("hostname", "").replace(" ", "").split(",");
            if(mainHostnames.length != 0){
                boolean isFollowed = false;
                for(String domain : mainHostnames){
                    if(req.getHost().equals(domain)){
                        isFollowed = true;
                        break;
                    }
                }
                if(!isFollowed){
                    String protocol = req.getProtocol().startsWith("HTTP/") ? "http" : "https";
                    res.redirect(String.format("%s://%s%s",protocol, mainHostnames[0], req.getPath()));
                }
            }
        });
        server.all("/release", (req, res) -> res.json(release.toJSON()));
        server.all("/:id", (req, res) -> {
            String id = req.getParam("id").split("\\.")[0];
            for (File file : mainFolder.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName().split("\\.")[0];
                    if (name.equals(id)) {
                        if (fileNames.containsKey(name))
                            res.setHeader("Content-Disposition", "filename=\"" + fileNames.get(name) + "\"");
                        if (fileTypes.containsKey(name)) {
                            res.setHeader("Content-Type", fileTypes.get(name));
                            if(fileTypes.get(name).startsWith("video") || fileTypes.get(name).startsWith("audio")){
                                res.setHeader("accept-ranges", "bytes");
                                if(!req.getHeader("range").isEmpty()) res.setHeader("content-range", "bytes "+req.getHeader("range").getFirst()+file.length()+"/"+(file.length()+1));
                            } else if(fileTypes.get(name).startsWith("text")) {
                                try {
                                    res.sendBytes(Files.readString(file.toPath(), UTF_8).getBytes(), MediaType._txt.getMIME());
                                    System.gc();
                                } catch (Exception EX){
                                    EX.printStackTrace();
                                }
                                return;
                            }
                        }
                        res.send(file.toPath());
                        System.gc();
                        break;
                    }
                }
            }
        });
        server.all("/e/:id", (req, res) -> {
            String id = req.getParam("id").split("\\.")[0];
            for (File file : mainFolder.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName().split("\\.")[0];
                    if (name.equals(id)) {
                        String cloviName = config.getString("name", "{host}")
                                .replace("{host}", req.getHost().contains("localhost") || req.getHost().contains("127.0.0.1") ? "Clovi > Uploader" : req.getHost());
                        String page = embedHtml;
                        File filePage = new File("./embed.html");
                        if (filePage.exists()) {
                            try {
                                page = Files.readString(filePage.toPath());
                            } catch (Exception ignored) {
                                ignored.printStackTrace();
                            }
                        }
                        String type = fileTypes.getOrDefault(name, "file");
                        String resHtml = page.replace("{hostname}", cloviName)
                                .replace("{filename}", fileNames.getOrDefault(name, name))
                                .replace("{id}", name)
                                .replace("{type}", type.startsWith("video") ? "video" : type.startsWith("image") ? "image" : type.startsWith("audio") ? "audio" : "file")
                                .replace("{filetype}", type)
                                .replace("{accent_color}", config.getString("accent_color", "#7f916f"))
                                .replace("{version}", release.getString("version", "1.98.4"));
                        res.setContentType(MediaType._html);
                        res.send(resHtml);
                        break;
                    }
                }
            }
        });
        server.post("/upload", (req, res) -> {
            if (req.getHeader("X-File-Name").isEmpty() || req.getBody() == null) {
                res.setStatus(Status._400);
                res.json(BAD_REQUEST);
            } else {
                try {
                    if (req.getContentLength() > 104857600*2.5) {
                        res.setStatus(413);
                        JsonObject error = new JsonObject();
                        error.addProperty("code", 413);
                        error.addProperty("codename", "Payload Too Large");
                        error.addProperty("message", "File is over 100mb!");
                        JsonObject resp = new JsonObject();
                        resp.add("error", error);
                        res.json(resp);
                        return;
                    }
                    byte[] bytes = req.getBody().readAllBytes();
                    try {
                        MultipartParser parser = new MultipartParser();
                        List<MultipartParser.Part> parts = parser.parse(new ByteArrayInputStream(bytes), "boundary");
                        for (MultipartParser.Part part : parts) {
                            bytes = part.getContent();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    String fileName = req.getHeader("X-File-Name").getFirst();
                    String fileType = fileName.split("\\.").length <= 1 ? "" : "." + fileName.split("\\.")[fileName.split("\\.").length - 1];
                    String fileTypeMedia = req.getHeader("Content-Type").getFirst();
                    String id = makeID(7, false);
                    String delete_id = makeID(21, true);
                    File file = mainFolder.toPath().resolve(id + fileType).toFile();
                    saveFile(bytes, file);
                    bytes = null;
                    System.gc();
                    addFilename(id, fileName, delete_id, fileTypeMedia);
                    JsonObject resp = new JsonObject();
                    resp.addProperty("id", id);
                    resp.addProperty("name", fileName);
                    resp.addProperty("type", fileTypeMedia);
                    resp.addProperty("url", String.format("%1$s/%2$s", config.getString("url", "https://i.clovi.ru"), id));
                    resp.addProperty("delete_url", String.format("%1$s/delete/%2$s", config.getString("url", "https://i.clovi.ru"), delete_id));
                    res.json(resp);
                    DiscordWebhooks.sendUploadFile(fileName, String.format("%1$s/%2$s", "http://"+req.getHost(), id), fileTypeMedia, id, delete_id, req);
                } catch (Exception e) {
                    e.printStackTrace();
                    res.setStatus(500);
                    res.json(getErrorObject(e));
                }
            }
        });
        server.all("/delete/:id", (req, res) -> {
            String id = req.getParam("id");
            String idFile = "";
            for (String name : fileDeletes.keySet()) {
                if (fileDeletes.get(name).contains(id)) idFile = name;
            }
            if (fileDeletes.containsValue(id)) {
                for (File file : mainFolder.listFiles()) {
                    if (file.isFile()) {
                        String name = file.getName().split("\\.")[0];
                        if (name.equals(idFile)) {
                            file.delete();
                            DiscordWebhooks.sendDeleteFile(fileNames.get(name), "/" + name, fileTypes.getOrDefault(name, "file"), name, req);
                            fileNames.remove(idFile);
                            fileDeletes.remove(idFile);
                            res.send("File deleted");
                            break;
                        }
                    }
                }
            } else {
                res.setStatus(404);
                res.json(NOT_FOUND);
            }
        });
        server.all("/favicon.ico", (req, res) -> {
            Path path = Path.of("files/favicon.ico");
            res.setContentType("image/x-icon");
            if (favicon != null && !path.toFile().exists()) {
                res.sendBytes(favicon);
            } else res.send(path);
        });
        server.all("/", (req, res) -> {
            String name = config.getString("name", "{host}")
                    .replace("{host}", req.getHost().contains("localhost") || req.getHost().contains("127.0.0.1") ? "Clovi > Uploader" : req.getHost());
            String page = html;
            File filePage = new File("./index.html");
            if (filePage.exists()) {
                try {
                    page = Files.readString(filePage.toPath());
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
            String resHtml = page.replace("{hostname}", name)
                    .replace("{display}", config.getString("message", "").isEmpty() ? "display: none;" : "")
                    .replace("{message}", config.getString("message", ""))
                    .replace("{accent_color}", config.getString("accent_color", "#7f916f"))
                    .replace("{version}", release.getString("version", "1.98.4"));
            res.setContentType(MediaType._html);
            res.send(resHtml);
        });
        boolean staticEnable = true;
        if(!Path.of("./static").toFile().exists()){
            try {
                Files.createDirectory(Path.of("./static"));
            } catch (Exception ex){
                ex.printStackTrace();
                staticEnable = false;
            }
        }
        if(staticEnable) server.all(new FileStatics("static"));
        server.all((req, res) -> {
            res.setStatus(404);
            res.send("File not found");
        });
        server.listen(config.getNumber("port", 1984).intValue());
        LOG.log("-=-=-=-=-=-=-=-=-=-=-=-=-");
        LOG.log("Uploader запущен!");
        LOG.log(String.format("Порт: %s", config.getNumber("port", 1984).intValue()));
        LOG.log("-=-=-=-=-=-=-=-=-=-=-=-=-");
        DiscordWebhooks.run();
        DiscordWebhooks.sendRunning();
        if(secretKey.isEmpty()){
            secretKey = String.format("%s-%s-%s-%s", makeID(20), makeID(20), makeID(20), makeID(20));
            config.setString("secret_token", secretKey);
            DiscordWebhooks.sendGeneratedSecretToken();
        }
    }

    public static void addFilename(String id, String name, String delete_id, String file_type_media) {
        fileNames.put(id, name);
        fileDeletes.put(id, delete_id);
        fileTypes.put(id, file_type_media);
        saveFilenames();
    }

    public static void saveFilenames() {
        JsonArray j = new JsonArray();
        for (String key : fileNames.keySet()) {
            JsonObject jj = new JsonObject();
            jj.addProperty("id", key);
            jj.addProperty("name", fileNames.get(key));
            jj.addProperty("type", fileTypes.get(key));
            if (fileDeletes.containsKey(key))
                jj.addProperty("delete", fileDeletes.get(key));
            j.add(jj);
        }
        links.setJsonArray("names", j);
        links.save();
    }

    public static void saveFile(byte[] is, File targetFile) throws IOException {
        Files.write(targetFile.toPath(), is);
    }

    public static void checkFolders() throws IOException {
        try {
            if (!mainFolder.exists()) Files.createDirectory(mainFolder.toPath());
        } catch (Exception ignored) {
            throw ignored;
        }
    }

    public static CoffeeLogger LOG = new CoffeeLogger("WaterFiles/Uploader");
}