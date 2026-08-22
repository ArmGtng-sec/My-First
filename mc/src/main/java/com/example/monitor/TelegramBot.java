package com.example.monitor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TelegramBot {

    private static final String BASE = "https://api.telegram.org/bot";

    private final MonitorPlugin plugin;
    private final String token;
    private final String password;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private final Set<Long> authorized = ConcurrentHashMap.newKeySet();
    private final Map<Long, Boolean> authState = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;
    private long offset = 0;

    public TelegramBot(MonitorPlugin plugin, String token, String password) {
        this.plugin = plugin;
        this.token = token;
        this.password = password;
    }

    public void start() {
        running.set(true);
        thread = new Thread(this::pollLoop, "PlayerMonitor-Telegram");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                JsonArray updates = getUpdates();
                for (JsonElement e : updates) {
                    handleUpdate(e.getAsJsonObject());
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Telegram poll error: " + ex.getMessage());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private JsonArray getUpdates() throws Exception {
        String url = BASE + token + "/getUpdates?timeout=25&offset=" + offset;
        String body = request(url);
        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
        if (!obj.get("ok").getAsBoolean()) {
            return new JsonArray();
        }
        JsonArray result = obj.getAsJsonArray("result");
        for (JsonElement e : result) {
            long updateId = e.getAsJsonObject().get("update_id").getAsLong();
            if (updateId + 1 > offset) {
                offset = updateId + 1;
            }
        }
        return result;
    }

    private void handleUpdate(JsonObject update) {
        JsonObject message = update.getAsJsonObject("message");
        if (message == null) {
            return;
        }
        JsonObject chat = message.getAsJsonObject("chat");
        if (chat == null) {
            return;
        }
        long chatId = chat.get("id").getAsLong();
        String text = message.has("text") ? message.get("text").getAsString() : "";

        if (text.equals("/start")) {
            authState.put(chatId, true);
            sendMessage(chatId, "\ud83d\udd12 Input Password :", true);
            return;
        }

        if (authorized.contains(chatId)) {
            if (text.startsWith("/check")) {
                handleCheck(chatId, text);
            } else {
                sendMessage(chatId, quote("Perintah tidak dikenal. Pakai /check <username>"), false);
            }
            return;
        }

        if (authState.getOrDefault(chatId, false)) {
            authState.put(chatId, false);
            if (text.equals(password)) {
                authorized.add(chatId);
                sendMessage(chatId, quote("\u2705 Auth sukses. Sekarang kamu bisa pakai /check <username>"), false);
            } else {
                sendMessage(chatId, "\u274c Password salah. Kirim /start untuk coba lagi.", false);
            }
        } else {
            sendMessage(chatId, "Kirim /start dulu.", false);
        }
    }

    private void handleCheck(long chatId, String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2) {
            sendMessage(chatId, quote("Format: /check <username>"), false);
            return;
        }
        String name = parts[1].trim();
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) {
            sendMessage(chatId, quote("\u274c Player <b>" + escape(name) + "</b> tidak ditemukan / offline."), false);
            return;
        }
        String snapshot = PlayerSnapshot.render(p);
        sendMessage(chatId, quote(snapshot), false);
    }

    private String quote(String content) {
        return "<blockquote expandable>" + content + "</blockquote>";
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void sendMessage(long chatId, String text, boolean forceReply) {
        StringBuilder payload = new StringBuilder();
        payload.append("chat_id=").append(chatId)
                .append("&parse_mode=HTML")
                .append("&text=").append(urlEncode(text));
        if (forceReply) {
            payload.append("&reply_markup=").append(urlEncode("{\"force_reply\":true}"));
        }
        try {
            post(BASE + token + "/sendMessage", payload.toString());
        } catch (Exception ex) {
            plugin.getLogger().warning("Gagal kirim pesan Telegram: " + ex.getMessage());
        }
    }

    private String request(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private void post(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}