package me.mraxetv.beastlib.bungee;

import com.alessiodp.libby.BungeeLibraryManager;
import com.alessiodp.libby.Library;
import me.mraxetv.beastlib.bungee.utils.PUtils;
import me.mraxetv.beastlib.bungee.utils.Utils;
import net.md_5.bungee.api.plugin.Plugin;



public class BeastLib extends Plugin {


    private static BeastLib INSTANCE;
    private Utils utils;


    public void onEnable() {

        INSTANCE = this;
        utils = new Utils(this);

        Library library1 = Library.builder().groupId("com{}zaxxer").artifactId("HikariCP").version("4.0.3").relocate("com{}zaxxer{}hikari", "me.mraxetv.beasttokens.libs.hikari").build();
        BungeeLibraryManager bungeeLibraryManager = new BungeeLibraryManager(this,getDataFolder().getPath());
        bungeeLibraryManager.addMavenCentral();
        bungeeLibraryManager.loadLibrary(library1);


       // this.getUtils().sendMessage(this.getProxy().getConsole(), "&2#-------------------------------#");
      //  this.getUtils().sendMessage(this.getProxy().getConsole(), "&4Beast&bTokensBungee &2Enabled");
      //  this.getUtils().sendMessage(this.getProxy().getConsole(), "&2Version 1.0");
      //  this.getUtils().sendMessage(this.getProxy().getConsole(), "&2By MrAxeTv");
       // this.getUtils().sendMessage(this.getProxy().getConsole(), "&2#-------------------------------#");
    }

    public void onDisable() {
        super.onDisable();

    }


    public PUtils getUtils() {
        return this.utils;
    }

    public static BeastLib getInstance() {
        return INSTANCE;
    }



}
