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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FolderYaml {
    private JavaPlugin pl;
    private String name;
    private String folder;
    public File configf;
    public FileConfiguration config;

    public FolderYaml(JavaPlugin plugin, String folder , String n) {
        this.pl = plugin;
        this.name = n;
        this.folder = folder;
        createFiles(name);
        configUpdate();
    }

    public void reloadConfig() {
        createFiles(name);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configf);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createFiles(String name) {

        configf = new File(pl.getDataFolder()+"/"+folder, name);
        if (!configf.exists()) {
            configf.getParentFile().mkdirs();
            pl.saveResource(folder+"\\"+name, false);
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
        InputStreamReader jarInputStream = null;
        YamlConfiguration jarConfig = null;
        /**
         * Reading file from plugin jar
         */
        try {

            jarInputStream = new InputStreamReader(pl.getResource(folder+"/"+this.name), "UTF-8");
            if (jarInputStream != null) {
                jarConfig = YamlConfiguration.loadConfiguration((Reader)jarInputStream);
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        /**
         * Checking if jar file config is newer
         */

        if (getConfig().getDouble("ConfigVersion") < jarConfig.getDouble("ConfigVersion") || !getConfig().isSet("ConfigVersion")) {
            double oldVersion = getConfig().getDouble("ConfigVersion");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HH.mm.ss");
            Date date = new Date();
            boolean renameResult = configf.renameTo(
                    new File(pl.getDataFolder()+"/"+folder, name.replaceAll("\\.yml", "") + "_" + dateFormat.format(date) + "_old.yml"));
            if (renameResult) {
                pl.saveResource(folder+"\\"+name, false);
                this.createFiles(name);
                Bukkit.getConsoleSender().sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                                ("&7["+pl.getDescription().getPrefix()+"] &4Old " + name + " " + oldVersion + " has been replaced with new "+jarConfig.getDouble("ConfigVersion") +" version!")));
            } else {
                Bukkit.getConsoleSender().sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                                ("&7["+pl.getDescription().getPrefix()+"] &4Server has failed to replace old version of " + config + " please contact author MrAxeTv!")));
            }
        }
    }
}

