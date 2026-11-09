/*
 * Decompiled with CFR 0.145.
 *
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 */
package me.mraxetv.beastlib.placeholderapi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import me.mraxetv.beastlib.BeastLib;
import me.mraxetv.beastlib.utils.Utils;
import me.mraxetv.beastlib.utils.XpUtils;
import org.bukkit.entity.Player;

public class PlaceHolderAPI extends PlaceholderExpansion {
    private BeastLib pl;

    public PlaceHolderAPI(BeastLib pl) {
        this.pl = pl;
    }

    public boolean canRegister() {
        return true;
    }

    public boolean persist() {
        return true;
    }

    public String getAuthor() {
        return "MrAxeTv";
    }

    public String getIdentifier() {
        return "beastlib";
    }

    public String getVersion() {
        return "1.0";
    }

    public String onPlaceholderRequest(Player p, String identifier) {
        if (identifier.equals("exp")) {
            return Utils.formatNumber(XpUtils.getTotalExperience(p));
        }

        return "";
    }
}

