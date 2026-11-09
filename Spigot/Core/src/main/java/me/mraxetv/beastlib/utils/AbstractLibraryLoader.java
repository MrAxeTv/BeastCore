package me.mraxetv.beastlib.utils;

public abstract class AbstractLibraryLoader<Library, LibraryManager> {


    public abstract void loadLibraries();

    public abstract LibraryManager getLibraryManager();
}