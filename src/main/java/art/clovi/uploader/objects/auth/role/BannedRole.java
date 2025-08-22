package art.clovi.uploader.objects.auth.role;

import art.clovi.uploader.objects.auth.Role;
import com.google.gson.JsonObject;

public class BannedRole extends Role {
    public BannedRole() {
        super("ban", "Заблокированный пользователь", new JsonObject());
        UPLOAD_FILE = false;
        ADMINISTRATION_TOOL = false;
    }
}
