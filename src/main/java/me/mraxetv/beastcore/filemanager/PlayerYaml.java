/*
 * Decompiled with CFR 0.145.
 * 
 * Could not load the following classes:
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Server
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package me.mraxetv.beastcore.filemanager;


import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class PlayerYaml {
    JavaPlugin pl;
    UUID u;
    File userFile;
    FileConfiguration userConfig;

    public PlayerYaml(UUID u, JavaPlugin plugin) {
        this.pl = plugin;
        this.u = u;
        this.userFile = new File(pl.getDataFolder() + "/Players", u + ".yml");
        this.userConfig = YamlConfiguration.loadConfiguration(userFile);
        this.createFile();
    }

    public boolean isExists() {
        return userFile.exists();
    }

    public void createFile() {
        if (!userFile.exists()) {
            try {
                YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
                OfflinePlayer p = pl.getServer().getOfflinePlayer(u);
                userConfig.set("Name", p.getName());
                userConfig.set("UUID", u.toString());
                //userConfig.set("Tokens", pl.getTokensManager().getStartAmount());
                userConfig.save(userFile);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getFile() {
        return userConfig;
    }

    public void saveFile() {
        try {
            getFile().save(userFile);
        }
        catch (Exception e) {
            pl.getLogger().info("ERROR WITH SAVING PLAYER TOKENS UUID: " + u + ", PLEASE REPORT THIS TO AUTHOR OF THE PLUGIN");
            e.printStackTrace();
        }
    }
}

