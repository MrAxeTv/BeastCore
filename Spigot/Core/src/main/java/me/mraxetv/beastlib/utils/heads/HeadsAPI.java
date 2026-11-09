package me.mraxetv.beastlib.utils.heads;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

public class HeadsAPI {

    private static final Requests requests = new Requests();
    private static Gson gson = new Gson();


    private static final String DEFAULT_UUID = "8667ba71-b85a-4004-af54-457a9734eed7"; // Steve

    public static String getMojangStatus() {
        return requests.GetRequest("https://status.mojang.com/check");
    }

    public static String getStringUUID(String username) {
        String uuidJson = getUUID(username);
        if (uuidJson == null || uuidJson.isEmpty()) {
            return DEFAULT_UUID;
        }
        JsonObject object = gson.fromJson(uuidJson, JsonObject.class);
        if (object == null || !object.has("id")) {
            return DEFAULT_UUID;
        }
        return object.get("id").getAsString();
    }

    public static String getUUID(String username) {
        String response = requests.GetRequest("https://api.mojang.com/users/profiles/minecraft/" + username);
        if (response == null || response.isEmpty()) {
            return DEFAULT_UUID;
        }
        return response;
    }

    public static String getNameHistoryByUsername(String username) {
        return requests.GetRequest("https://api.mojang.com/user/profiles/" + getStringUUID(username) + "/names");
    }

    public static String getNameHistory(String uuid) {
        return requests.GetRequest("https://api.mojang.com/user/profiles/" + uuid + "/names");
    }

    public static String getUUIDs(String... usernames) {
        return requests.PostRequest("https://api.mojang.com/profiles/minecraft", usernames);
    }

    public static String getTexturesByUsername(String username , boolean unsigned) {
        String Url = "https://sessionserver.mojang.com/session/minecraft/profile/" + getStringUUID(username);
        String confirmurl = unsigned ? Url : Url + "?unsigned=false";
        return requests.GetRequest(confirmurl);
    }

    public static String getTextures(String uuid , boolean unsigned) {
        String Url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;

        String confirmurl = unsigned ? Url : Url + "?unsigned=false";
        return requests.GetRequest(confirmurl);
    }

    public static String getSkinURLByUsername(String username) {
            String textures = getTexturesByUsername(username , false);
            JsonObject object = gson.fromJson(textures, JsonObject.class);
            JsonArray array = object.getAsJsonArray("properties");
            return array.get(0).getAsJsonObject().get("value").getAsString();


    }
    /*public static String getSkinURLByUsername(String username) {
        String textures = getTexturesByUsername(username , true);
        JsonObject object = gson.fromJson(textures, JsonObject.class);
        JsonArray array = object.getAsJsonArray("properties");
        return new String(Base64.getDecoder().decode(array.get(0).getAsJsonObject().get("value").getAsString()));
    }*/

    public static String getSkinURL(String uuid) {
        String textures = getTextures(uuid , false);
        JsonObject object = gson.fromJson(textures, JsonObject.class);
        JsonArray array = object.getAsJsonArray("properties");
        return array.get(0).getAsJsonObject().get("value").getAsString();
    }
    /*public static String getSkinURL(String uuid) {
        String textures = getTextures(uuid , false);
        JsonObject object = gson.fromJson(textures, JsonObject.class);
        JsonArray array = object.getAsJsonArray("properties");
        return new String(Base64.getDecoder().decode(array.get(0).getAsJsonObject().get("value").getAsString()));
    }*/

    public static String getSignature(String uuid) {
        String textures = getTextures(uuid , false);
        JsonObject object = gson.fromJson(textures , JsonObject.class);
        JsonArray array = object.getAsJsonArray("properties");
        return array.get(0).getAsJsonObject().get("signature").getAsString();
    }

    public static String getSignatureByUsername(String username) {
        String textures = getTexturesByUsername(username , false);
        JsonObject object = gson.fromJson(textures , JsonObject.class);
        JsonArray array = object.getAsJsonArray("properties");
        return array.get(0).getAsJsonObject().get("signature").getAsString();
    }


}