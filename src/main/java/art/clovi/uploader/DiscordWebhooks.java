package art.clovi.uploader;

import art.clovi.uploader.auth.Users;
import art.clovi.uploader.objects.auth.User;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import express.http.request.Request;

import static art.clovi.uploader.Uploader.LOG;
import static art.clovi.uploader.Uploader.release;

public class DiscordWebhooks {
    public static boolean available = false;
    public static WebhookClient client;
    public static void run(){
        try {
            String url = Uploader.config.getString("webhook_url", "");
            if(url.isBlank()) return;;
            client =  WebhookClient.withUrl(url);
            available = true;
        } catch (Exception ex){
            LOG.error(ex.getMessage());
            ex.printStackTrace();
        }
    }
    public static void sendRunning(){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle("Клови была успешно запущена!", null));
        embed.setColor(release.getString("experiment", "").isEmpty() ? 0xFF7f916f : 0xFFe63946);
        embed.setDescription(String.format("Порт подключения: %1$s\nВерсия: %2$s\nРежим: %3$s",
                Uploader.config.getNumber("port", 3000).intValue(),
                release.getString("version", "не указан") + " ("+ release.getString("version_type", "Developer")+")",
                Uploader.config.getBoolean("technical_break", false) ? ("Тех. работы: \n " + Uploader.config.getString("message", "")) : "Обычный"));
        if(!release.getString("experiment", "").isEmpty()) embed.addField(new WebhookEmbed.EmbedField(true, "Эксперимент", release.getString("experiment", "")));
        client.send(embed.build());
    }
    public static void sendRerunning(Request req){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle("Конфиги были обновлены!", null));
        embed.setColor(0xFFe9c46a);
        embed.setDescription("Сервер будет перезапущен!");
        embed.addField(new WebhookEmbed.EmbedField(true, "IP-Адрес", "||"+req.getIp()+"||"));
        client.send(embed.build());
    }

    public static void sendGeneratedSecretToken(){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle("Был сгенерирован секретный токен!", null));
        embed.setDescription("Его можете найти в конфиге.");
        client.send(embed.build());
    }


    public static void sendBanUser(String user, String reason, String status, Request req){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle(String.format("IP %s был %s!", user, status.contains("Unblocked") ? "разблокирован" : "заблокирован"), null));
        embed.setDescription("Причина: "+reason);
        embed.addField(new WebhookEmbed.EmbedField(true, "IP-Адрес", "||"+req.getIp()+"||"));
        client.send(embed.build());
    }

    public static void sendBanUser(User moderator, User gandon, String reason, Request req){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle(String.format("%s заблокировал пользователя %s", moderator.nickname, gandon.nickname), null));
        embed.addField(new WebhookEmbed.EmbedField(true, "Причина", reason));
        embed.setColor(0xFFfc1a47);
        embed.addField(new WebhookEmbed.EmbedField(true, "IP-Адрес", "||"+req.getIp()+"||"));
        client.send(embed.build());
    }
    public static void sendPardonUser(User moderator, User negandon, Request req){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle(String.format("%s разблокировал пользователя %s", moderator.nickname, negandon.nickname), null));
        embed.setColor(0xFF7f916f);
        embed.addField(new WebhookEmbed.EmbedField(true, "IP-Адрес", "||"+req.getIp()+"||"));
        client.send(embed.build());
    }

    // -=-=-=-

    public static void sendUploadFile(String name, String url, String type, String id, String delete_id, Request req){
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setColor(0xFFe9c46a)
                .setFooter(new WebhookEmbed.EmbedFooter("Пользователь загрузил файл!", "https://wf.kelcu.ru/icons/clover.png"));
        embed.addField(new WebhookEmbed.EmbedField(true, "IP-Адрес", "||"+req.getIp()+"||"));
        User user = Users.getUserFromRequest(req);
        if(user != null){
            embed.setAuthor(new WebhookEmbed.EmbedAuthor(getName(user)+" ("+user.id+")", "", ""));
        }
        if(type.startsWith("video")){
            embed.setDescription(String.format("Название: %s\nТип: Видео\nContent-Type: %s\nID: %s\nDelete URL: %s", name, type, id, String.format("http://%s/delete/%s", req.getHost(), delete_id)));
            client.send(embed.build());
            client.send("||"+url+"||");
            return;
        } else if(type.startsWith("image")){
            embed.setDescription(String.format("Название: %s\nТип: Изображение\nContent-Type: %s\nID: %s\nDelete URL: %s", name, type, id, String.format("http://%s/delete/%s", req.getHost(), delete_id)));
            client.send(embed.build());
            client.send("||"+url+"||");
            return;
        } else if(type.startsWith("audio")){
            embed.setDescription(String.format("Название: %s\nТип: Аудио\nContent-Type: %s\nID: %s\nDelete URL: %s", name, type, id, String.format("http://%s/delete/%s", req.getHost(), delete_id)));
            embed.addField(new WebhookEmbed.EmbedField(true, "URL", url));
        } else {
            embed.setDescription(String.format("Название: %s\nContent-Type: %s\nID: %s\nDelete URL: %s", name, type, id, String.format("http://%s/delete/%s", req.getHost(), delete_id)));
            embed.addField(new WebhookEmbed.EmbedField(true, "URL", url));
        }
        client.send(embed.build());
    }
    public static void sendDeleteFile(String name, String url, String type, String id, Request req){
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        User user = Users.getUserFromRequest(req);
        if(user != null){
            embed.setAuthor(new WebhookEmbed.EmbedAuthor(getName(user)+" ("+user.id+")", "", ""));
        }
        embed.setColor(0xFFfc1a47)
                .setFooter(new WebhookEmbed.EmbedFooter("Пользователь удалил файл!", "https://wf.kelcu.ru/icons/clover.png"));
        embed.setDescription(String.format("Название: %s\nContent-Type: %s\nID: %s", name, type, id));
        embed.addField(new WebhookEmbed.EmbedField(true, "URL", url));
        embed.addField(new WebhookEmbed.EmbedField(true, "IP-Адрес", "||"+req.getIp()+"||"));
        client.send(embed.build());
    }
    public static void sendReport(Exception exception){
        if(!available) return;
        WebhookEmbedBuilder embed = new WebhookEmbedBuilder();
        embed.setTitle(new WebhookEmbed.EmbedTitle("Произошла ошибка!!!!", null));
        embed.setColor(0xFFfc1a47);
        StringBuilder builder = new StringBuilder().append("```").append("\n").append(exception.getMessage());
        for(StackTraceElement arg : exception.getStackTrace())
            builder.append("\n- [").append(arg.getLineNumber()).append("] ").append(arg.getFileName()).append(" - ").append(arg.getMethodName());
        builder.append("\n```");
        embed.setDescription(builder.toString());
        client.send(embed.build());
    }

    public static String getName(User user){
        return user.nickname.isBlank() ? user.username : user.nickname;
    }
}
