package me.mraxetv.beastlib.utils;

import com.alessiodp.libby.Library;

public interface LibraryObject {
    Library createRelocatedLibrary();
    Library createLibrary();
    boolean hasRelocations();
}
