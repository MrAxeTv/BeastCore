/*
 * Decompiled with CFR 0.145.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package me.mraxetv.beastcore.filemanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class FileYml {
    private JavaPlugin pl;
    private String name;
    public File configf;
    public FileConfiguration config;

    public FileYml(JavaPlugin plugin, String n) {
        this.pl = plugin;
        this.name = n;
        createFiles(name);
        configUpdate();
    }
    public FileYml(JavaPlugin plugin, String n, boolean setDefaults) {
        this.pl = plugin;
        this.name = n;
        createFiles(name);
        configUpdate();
        if(setDefaults) addDefaults();

    }

    public void reloadConfig() {
        createFiles(name);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {

        YamlCommentor.saveCommented(config,configf);
    }


    public void addDefaults(){

        InputStreamReader defaultConfigStream = null;
        YamlConfiguration defaultConfig = null;
        try {
            defaultConfigStream = new InputStreamReader(pl.getResource(this.name), "UTF-8");

            if (defaultConfigStream != null) {
                defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
                //keys
                Set<String> keys = config.getKeys(true);

                for(String s : defaultConfig.getKeys(true)){
                    if(!keys.contains(s)){
                        config.setDefaults(defaultConfig);
                        config.options().copyDefaults(true);
                        saveConfig();
                        return;
                    }
                }
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public void createFiles(String name) {

        configf = new File(pl.getDataFolder(), name);
        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            pl.saveResource(name, false);
        }
        this.config = new YamlConfiguration();
        try {
            config.load(configf);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configUpdate() {
        InputStreamReader oldConfigStream = null;
        YamlConfiguration oldConfig = null;
        try {
            oldConfigStream = new InputStreamReader(pl.getResource(name), "UTF-8");
            if (oldConfigStream != null) {
                oldConfig = YamlConfiguration.loadConfiguration(oldConfigStream);
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (getConfig().getDouble("ConfigVersion") < oldConfig.getDouble("ConfigVersion") || !getConfig().isSet("ConfigVersion")) {
            double oldVersion = getConfig().getDouble("ConfigVersion");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HH.mm.ss");
            Date date = new Date();
            File file = new File(pl.getDataFolder(), name);
            boolean renameResult = file.renameTo(new File(pl.getDataFolder(), name.replaceAll("\\.yml", "") + "_" + dateFormat.format(date) + "_old.yml"));
            if (renameResult) {
                pl.saveResource(name, false);
                this.createFiles(name);
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ("&7"+pl.getDescription().getPrefix()+" &4Old " + name + " " + oldVersion + " has been replaced with new version!")));
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ("&7"+pl.getDescription().getPrefix()+" &4Server has failed to replace old version of " + config + " please contact author MrAxeTv!")));
            }
        }
    }
}

