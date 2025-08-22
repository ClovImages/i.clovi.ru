package art.clovi.uploader.objects.auth;

import com.google.gson.JsonObject;

import static ru.kelcuprum.caffeinelib.utils.GsonUtils.getBooleanInJSON;
import static ru.kelcuprum.caffeinelib.utils.GsonUtils.getStringInJSON;

public class Role {
    public String name;
    public String title;
    // -=-=-=- Посты -=-=-=-
    public boolean UPLOAD_FILE;
    // -=-=-=- Административное -=-=-=-
    public boolean ADMINISTRATION_TOOL;

    public Role(String name, String title, JsonObject permissions){
        this.name = name;
        this.title = title;
        UPLOAD_FILE = getBooleanInJSON("upload_file", permissions, true);
        ADMINISTRATION_TOOL = getBooleanInJSON("administration_tool", permissions, false);
    }
    public Role(JsonObject data){
        this.name = getStringInJSON("name", data, "user");
        this.title = getStringInJSON("title", data, "Пользователь");
        if(data.has("permissions")){
            JsonObject permissions = data.getAsJsonObject("permissions");
            UPLOAD_FILE = getBooleanInJSON("upload_file", permissions, true);
            ADMINISTRATION_TOOL = getBooleanInJSON("administration_tool", permissions, false);
        }
    }
    // -=-=-=- Сайт -=-=-=-
    public JsonObject toJSON(){
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("title", title);
        // -=-=-=-
        JsonObject perms = new JsonObject();
        perms.addProperty("upload_file", UPLOAD_FILE);
        perms.addProperty("administration_tool", ADMINISTRATION_TOOL);
        // -=-=-=-
        json.add("permissions", perms);
        return json;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

}
