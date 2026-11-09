package me.mraxetv.beastlib.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.utils.heads.HeadsAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public class Utils extends BUtils {

    private BeastLib pl;
    private UUID uuid;
    public Utils(JavaPlugin pl) {
        super(pl);
        this.pl = (BeastLib) pl;
        uuid = UUID.fromString("ab67e41a-1e38-4c7e-b586-f219f6dd93df");
    }

    @Override
    public void sendMessage(CommandSender sender, String message) {

    }

    @Override
    public String setPlaceholders(CommandSender sender, String message) {
        return "";
    }


    private URL getUrlFromBase64(String base64) throws MalformedURLException {
        String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);

        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        String url = jsonObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

        // Validate the URL
        if (url == null || !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new MalformedURLException("Invalid URL: " + url);
        }
        return new URL(url);
    }

    public void fetchHeadTexture(String playerName) {

        if (playerName.isEmpty()) return;
        if (!MCVersion.isAtLeastVersion(MCVersion.MC1_8_R3)) return;
                pl.getScheduler().runAsync(pl, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String uuid = HeadsAPI.getStringUUID(playerName);
                            if(uuid != null) pl.getHeadsConfig().set(playerName.toLowerCase(), HeadsAPI.getSkinURL(uuid));
                        }catch (Exception e){}
                    }
                });
    }

    public  boolean hasFetchedHeadTexture(String skullTexture){
        if(skullTexture.length() > 16) return true;
        if(pl.getHeadsConfig().isSet(skullTexture.toLowerCase())) return true;
        return false;
    }

    public String getFetchedTexture(String skullTexture){
        if(skullTexture.length() > 16) return skullTexture;
        return pl.getHeadsConfig().getString(skullTexture.toLowerCase(),"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ3ZDc0ODcyNTMwMzUwNTNjN2JhZmVlMzQwM2QzN2JiN2E3ZWNiMDE4MTljZmUyZTI3YmU2ZWM1MGI1MDZkYSJ9fX0=");
    }





    public void setHeadTexture(ItemStack item, String texture) {

        //Check one more time if value  which is provided is a  name it will grab coresponding Texture Value
        String textureValue = getFetchedTexture(texture);

        if (MCVersion.isAtLeastVersion(MCVersion.MC1_20_R4)) {
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid);
            PlayerTextures textures = profile.getTextures();
            URL urlObject;
            try {
                //urlObject = new URL(url); // The URL to the skin, for example: https://textures.minecraft.net/texture/18813764b2abc94ec3c3bc67b9147c21be850cdf996679703157f4555997ea63a
                urlObject = getUrlFromBase64(textureValue);
            } catch (MalformedURLException exception) {
                throw new RuntimeException("Invalid URL", exception);
            }
            textures.setSkin(urlObject); // Set the skin of the player profile to the URL
            profile.setTextures(textures); // Set the textures back to the profile
            skullMeta.setOwnerProfile(profile);
            item.setItemMeta(skullMeta);
        } else {
            NBT.modify(item, nbt -> {
                final ReadWriteNBT skullOwnerCompound = nbt.getOrCreateCompound("SkullOwner");
                skullOwnerCompound.setUUID("Id", uuid);
                skullOwnerCompound.getOrCreateCompound("Properties").getCompoundList("textures").addCompound().setString("Value", textureValue);
            });
        }
    }


}
