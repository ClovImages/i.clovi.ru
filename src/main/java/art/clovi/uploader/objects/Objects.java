package art.clovi.uploader.objects;

import art.clovi.uploader.objects.auth.Role;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ru.kelcuprum.caffeinelib.utils.GsonHelper;

import java.util.HashMap;

import static art.clovi.uploader.Uploader.config;
import static ru.kelcuprum.caffeinelib.utils.GsonUtils.getStringInJSON;
import static ru.kelcuprum.caffeinelib.utils.GsonUtils.jsonElementIsNull;

public class Objects {
    public static JsonObject NOT_FOUND = GsonHelper.parseObject("{\"error\":{\"code\":404,\"codename\":\"Not found\",\"message\":\"Method not found\"}}");
    public static JsonObject INTERNAL_SERVER_ERROR = GsonHelper.parseObject("{\"error\":{\"code\":500,\"codename\":\"Internal Server Error\",\"message\":\"\"}}");
    public static JsonObject FORBIDDEN  = GsonHelper.parseObject("{\"error\": {\"code\": 403,\"codename\": \"Forbidden\",\"message\": \"You do not have permission to use this method\"}}");
    public static JsonObject UNAUTHORIZED = GsonHelper.parseObject("{\"error\": {\"code\": 401,\"codename\": \"Unauthorized\",\"message\": \"You not authorized\"}}");
    public static JsonObject BAD_REQUEST = GsonHelper.parseObject("{\"error\": {\"code\": 400,\"codename\": \"Bad Request\",\"message\": \"The required arguments are missing!\"}}");
    public static JsonObject getErrorObject(Exception ex){
        JsonObject object = INTERNAL_SERVER_ERROR;
        object.get("error").getAsJsonObject().addProperty("message", ex.getMessage() == null ? ex.getClass().toString() : ex.getMessage());
        return object;
    }
    public static JsonObject getJsonError(int code, String codename, String message){
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("codename", codename);
        error.addProperty("message", message);
        JsonObject resp = new JsonObject();
        resp.add("error", error);
        return resp;
    }

    // -=-=-=-=-=-=-=-=-=-=-=-=-
    public static HashMap<String, Role> roles = new HashMap<>();
    public static Role defaultRole = new Role("user", "Пользователь", new JsonObject());

    public static void loadRoles(){
        JsonArray rolesFile = config.getJsonArray("roles", new JsonArray());
        roles = new HashMap<>();
        for(JsonElement json : rolesFile){
            if(json.isJsonObject()){
                JsonObject data = (JsonObject) json;
                if(!jsonElementIsNull("name", data)){
                    JsonObject perms = data.has("permissions") ? data.getAsJsonObject("permissions") : new JsonObject();
                    roles.put(getStringInJSON("name", data, "user"), new Role(getStringInJSON("name", data, "user"), getStringInJSON("title", data, "Пользователь"), perms));
                }
            }
        }
    }
    public static JsonArray getRolesJSON(){
        loadRoles();
        JsonArray rolesArray =  new JsonArray();
        for(String key : roles.keySet())
            rolesArray.add(roles.get(key).toJSON());
        return rolesArray;
    }

    public static Role getRoleByName(String name){
        return roles.getOrDefault(name, roles.getOrDefault("user", defaultRole));
    }
}
