package me.mraxetv.beastlib.listeners;

import me.mraxetv.beastlib.BeastLib;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    BeastLib pl;

    public JoinListener(BeastLib pl) {
        this.pl = pl;
        pl.getServer().getPluginManager().registerEvents(this,pl);
    }

    @EventHandler
    public void join(PlayerJoinEvent e){
        pl.getUtils().fetchHeadTexture(e.getPlayer().getName());
        //pl.getUtils().fetchHeadTexture("55D4A165D41AS5");
    }
}
