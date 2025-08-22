package art.clovi.uploader.auth;

import art.clovi.uploader.DiscordWebhooks;
import art.clovi.uploader.objects.auth.User;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import express.http.request.Request;
import express.http.response.Response;
import express.utils.Status;
import ru.kelcuprum.caffeinelib.config.Config;
import ru.kelcuprum.caffeinelib.utils.GsonHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static art.clovi.uploader.objects.Objects.*;
import static ru.kelcuprum.caffeinelib.utils.GsonUtils.getStringInJSON;

public class Users {
    public static File dir = new File("users");

    public static void getUser(Request req, Response res) {
        String id = req.getParam("id") == null ? "" : req.getParam("id");
        try {
            JsonObject resp = NOT_FOUND;
            User user = getUser(id);
            if(user != null) resp = user.toJSON();
            if (resp == NOT_FOUND) res.setStatus(404);
            res.json(resp);
        } catch (Exception ex) {
            DiscordWebhooks.sendReport(ex);
            res.setStatus(500);
            res.json(getJsonError(500, "error", ex.getMessage()));
        }
    }

    public static void getUserByToken(Request req, Response res) {
        if (req.getHeader("Authorization").isEmpty()) {
            res.setStatus(Status._401);
            res.json(UNAUTHORIZED);
            return;
        }
        String token = req.getHeader("Authorization").getFirst().split(" ")[1];
        try {
            JsonObject resp = UNAUTHORIZED;
            User user = getUserByToken(token);
            if(user != null) resp = user.toJSON();
            if (resp == UNAUTHORIZED) res.setStatus(401);
            res.json(resp);
        } catch (Exception ex) {
            DiscordWebhooks.sendReport(ex);
            res.setStatus(500);
            res.json(getJsonError(500, "error", ex.getMessage()));
        }
    }

    public static void banUser(Request req, Response res) {
        User reqUser = Users.getUserFromRequest(req);
        if (reqUser == null) {
            res.setStatus(Status._401);
            res.json(UNAUTHORIZED);
            return;
        }
        if(req.getQuery("reason") == null){
            res.setStatus(Status._400);
            res.json(BAD_REQUEST);
            return;
        }
        if(!reqUser.role.ADMINISTRATION_TOOL){
            res.setStatus(Status._403);
            res.json(FORBIDDEN);
            return;
        }
        try {
            JsonObject resp = UNAUTHORIZED;
            User user = getUser(req.getParam("id"));

            if(user != null){
                File userFile = null;
                for (File file : dir.listFiles()) {
                    if (file.isFile()) {
                        User fileUser = new User(GsonHelper.parseObject(Files.readString(file.toPath())));
                        if(fileUser.id.equals(user.id)) {
                            userFile = file;
                            break;
                        }
                    }
                }
                if(userFile == null) throw new RuntimeException("userFile == null");
                user.userBan(req.getQuery("reason"));
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("status", "done");
                resp = jsonObject;
                Files.writeString(userFile.toPath(), user.toString(), StandardCharsets.UTF_8);
                DiscordWebhooks.sendBanUser(reqUser, user, req.getQuery("reason"), req);
            }
            if (resp == UNAUTHORIZED) res.setStatus(401);
            res.json(resp);
        } catch (Exception ex) {
            DiscordWebhooks.sendReport(ex);
            res.setStatus(500);
            res.json(getJsonError(500, "error", ex.getMessage()));
        }
    }
    public static void pardonUser(Request req, Response res) {
        User reqUser = Users.getUserFromRequest(req);
        if (reqUser == null) {
            res.setStatus(Status._401);
            res.json(UNAUTHORIZED);
            return;
        }
        if(!reqUser.role.ADMINISTRATION_TOOL){
            res.setStatus(Status._403);
            res.json(FORBIDDEN);
            return;
        }
        try {
            JsonObject resp = UNAUTHORIZED;
            User user = getUser(req.getParam("id"));

            if(user != null){
                File userFile = null;
                for (File file : dir.listFiles()) {
                    if (file.isFile()) {
                        User fileUser = new User(GsonHelper.parseObject(Files.readString(file.toPath())));
                        if(fileUser.id.equals(user.id)) {
                            userFile = file;
                            break;
                        }
                    }
                }
                if(userFile == null) throw new RuntimeException("userFile == null");
                user.userPardon();
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("status", "done");
                resp = jsonObject;
                Files.writeString(userFile.toPath(), user.toString(), StandardCharsets.UTF_8);
                DiscordWebhooks.sendPardonUser(reqUser, user, req);
            }
            if (resp == UNAUTHORIZED) res.setStatus(401);
            res.json(resp);
        } catch (Exception ex) {
            DiscordWebhooks.sendReport(ex);
            res.setStatus(500);
            res.json(getJsonError(500, "error", ex.getMessage()));
        }
    }

    public static User getUserByToken(String token) {
        try {
            File file = dir.toPath().resolve(token + ".json").toFile();
            if (file.exists()){
                JsonObject userFile = GsonHelper.parseObject(Files.readString(file.toPath()));
                userFile.add("role", getRoleByName(getType(getStringInJSON("id", userFile))).toJSON());
                return new User(userFile);
            }
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public static User getUser(String id) {
        try {
            for (File file : dir.listFiles()) {
                if (file.isFile()) {
                    JsonObject userFile = GsonHelper.parseObject(Files.readString(file.toPath()));
                    if (getStringInJSON("id", userFile, "").equals(id) || getStringInJSON("username", userFile, "").equals(id)) {
                        userFile.add("role", getRoleByName(getType(getStringInJSON("id", userFile))).toJSON());
                        userFile.remove("follows");
                        userFile.remove("followed_ip");
                        userFile.remove("saves");
                        return new User(userFile);
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static User getUserFromRequest(Request req){
        User user = null;
        if(!req.getHeader("Authorization").isEmpty()){
            try {
                user = getUserByToken(req.getHeader("Authorization").getFirst().split(" ")[1]);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        } else if(req.getCookies().containsKey("clovi_uploader_access_token")){
            user = getUserByToken(req.getCookie("clovi_uploader_access_token").getValue());
        }
        return user;
    }

    public static String getType(String id) {
        return new Config("./roles.json").getString(id, "user");
    }
}
