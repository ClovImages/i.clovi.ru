package art.clovi.uploader.objects.auth;

import art.clovi.uploader.objects.auth.role.BannedRole;
import com.google.gson.JsonObject;

public class User {
    public String nickname;
    public String username;
    public String id;
    public String followedIP;
    public String ban;
    public boolean verified;
    public Role role = new Role(new JsonObject());
    public User(JsonObject object){
        if(has(object, "nickname")) nickname = object.get("nickname").getAsString();
        if(has(object,"username")) username = object.get("username").getAsString();
        if(has(object,"id")) id = object.get("id").getAsString();
        if(has(object,"ban")) ban = object.get("ban").getAsString();
        if(has(object,"verified")) verified = object.get("verified").getAsBoolean();
        if(has(object,"followed_ip")) followedIP = object.get("followed_ip").getAsString();
        if(has(object,"role")) role = new Role(object.getAsJsonObject("role"));
    }

    public static boolean has(JsonObject jsonObject, String element){
        return jsonObject.has(element) && !jsonObject.get(element).isJsonNull();
    }

    public JsonObject toJSON(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("nickname", nickname);
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("ban", ban);
        jsonObject.addProperty("verified", verified);
        jsonObject.addProperty("followed_ip", followedIP);
        jsonObject.add("role", getRole().toJSON());
        return jsonObject;
    }
    
    public Role getRole(){
        return userBanned() ? new BannedRole() : role;
    }

    public void userBan(String reason){
        this.ban = reason;
    }
    public void userPardon(){
        this.ban = null;
    }
    public boolean userBanned(){
        return this.ban != null && !this.ban.isEmpty();
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
