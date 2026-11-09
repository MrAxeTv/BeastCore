package me.mraxetv.beastlib.utils;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import dev.triumphteam.gui.guis.Gui;
import me.mraxetv.beastlib.BeastLib;
import net.kyori.adventure.text.Component;


import java.util.Arrays;

public class LibrarySetup extends AbstractLibraryLoader<Library, BukkitLibraryManager> {

    private BeastLib pl;
    private BukkitLibraryManager bukkitLibraryManager;

    public LibrarySetup(BeastLib pl) {
        this.pl = pl;
        bukkitLibraryManager = new BukkitLibraryManager(pl);
        bukkitLibraryManager.addMavenCentral();
        loadLibraries();
    }

    @Override
    public void loadLibraries() {

        Arrays.stream(Libraries.values()).forEach(lib -> {
            if (lib.hasRelocations()) bukkitLibraryManager.loadLibrary(lib.createRelocatedLibrary());
            else bukkitLibraryManager.loadLibrary(lib.createLibrary());
        });
    }

    @Override
    public BukkitLibraryManager getLibraryManager() {
        return bukkitLibraryManager;
    }


}
