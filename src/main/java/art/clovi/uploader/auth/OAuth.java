package art.clovi.uploader.auth;

import art.clovi.uploader.Uploader;
import art.clovi.uploader.objects.Objects;
import com.google.gson.JsonObject;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.MediaType;
import express.utils.Status;
import ru.kelcuprum.caffeinelib.WebHelper;
import ru.kelcuprum.caffeinelib.utils.GsonHelper;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;

import static art.clovi.uploader.objects.Objects.getJsonError;
import static art.clovi.uploader.Uploader.LOG;
import static art.clovi.uploader.Uploader.config;
import static ru.kelcuprum.caffeinelib.utils.GsonUtils.getStringInJSON;

public class OAuth {
    public static File dir = new File("users");
    public static HashMap<String, String> redirectUrls = new HashMap<>();
    public static String uriEncode(String uri){
        return URLEncoder.encode(uri, StandardCharsets.UTF_8);
    }
    public static void auth(Request req, Response res){
        if(req.getContentType().toLowerCase().contains("application/json") || (req.getQuery("json") != null && req.getQuery("json").equalsIgnoreCase("true"))){
            if(req.getQuery("code") == null){
                res.setStatus(400);
                res.json(Objects.BAD_REQUEST);
                return;
            }
            if (req.getQuery("code").isEmpty()) {
                res.setStatus(Status._401);
                res.json(Objects.UNAUTHORIZED);
                return;
            }
            try {
                String accessToken = req.getQuery("code");
                JsonObject oauthData = GsonHelper.parse(WebHelper.getString(HttpRequest.newBuilder().uri(URI.create("https://api.twitch.tv/helix/users"))
                        .GET()
                        .header("Client-ID", config.getString("client_id", ""))
                        .header("Authorization", "Bearer "+accessToken)
                        .header("Content-Type", "application/json"))).getAsJsonObject();
                JsonObject userData = oauthData.get("data").getAsJsonArray().get(0).getAsJsonObject();
                LOG.log(userData.toString());

                JsonObject user = null;
                boolean registered = false;
                String token = makeID();
                for(File file : dir.listFiles()){
                    if(file.isFile()){
                        JsonObject userFile = GsonHelper.parseObject(Files.readString(file.toPath()));
                        if(userFile.get("id").equals(userData.get("id"))) {
                            token = file.getName().replace(".json", "");
                            user = userFile;
                            break;
                        }
                    }
                }
                if(user == null){
                    user = new JsonObject();
                    user.addProperty("id", getStringInJSON("id", userData, ""));
                    registered = true;
                    user.addProperty("registered", System.currentTimeMillis());
                }
                user.addProperty("username", getStringInJSON("login", userData, ""));
                user.addProperty("nickname", getStringInJSON("display_name", userData, ""));
                user.addProperty("verified", user.has("verified") && !user.get("verified").isJsonNull() && user.get("verified").getAsBoolean());
                String ips = user.has("followed_ip") && !user.get("followed_ip").isJsonNull() ? user.get("followed_ip").getAsString() : "";
                if(!ips.contains(req.getIp())) ips+= (ips.isBlank() ? "" : ", ") + req.getIp();
                user.addProperty("followed_ip", ips);
                Files.writeString(dir.toPath().resolve(token+".json"), user.toString(), StandardCharsets.UTF_8);

                JsonObject resp = new JsonObject();
                resp.addProperty("state", registered ? "Registered" : "Updated");
                resp.add("user", user);
                resp.addProperty("access", token);
                res.json(resp);

            } catch (Exception ex){
                res.setStatus(500);
                ex.printStackTrace();
                res.json(getJsonError(500, "Error", ex.getMessage()));
            }
            res.send("ok");
        } else {
            res.setContentType(MediaType._html);
            res.send(Uploader.authHtml);
        }
    }

    public static String makeID(){
        String token = String.format("%s-%s-%s-%s", makeOneID(7), makeOneID(7), makeOneID(7), makeOneID(7));
        if (new File("./users/"+token+".json").exists()) return makeID();
        else return token;
    }
    public static String makeOneID(int length){
        return makeOneID(length, false);
    }
    public static String makeOneID(int length, boolean followCharacters){
        StringBuilder result = new StringBuilder();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        if(followCharacters) characters+="-_+-";
        int charactersLength = characters.length();
        int counter = 0;
        while (counter < length) {
            result.append(characters.charAt((int) Math.floor(Math.random() * charactersLength)));
            counter += 1;
        }
        return result.toString();
    }
}
