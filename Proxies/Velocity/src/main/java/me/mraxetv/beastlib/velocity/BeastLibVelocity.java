package me.mraxetv.beastlib.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
        id = "beast_lib",
        name = "BeastLib",
        version = "1.1",
        description = "This is a lib for Beast plugins!",
        authors = {"MrAxeTv"}
)
public class BeastLibVelocity {


    private static BeastLibVelocity INSTANCE;

    @Getter
    private ProxyServer proxy;

    @Getter
    private final Logger logger;


    @Getter
    private final Path dataDirectory;

    @Inject
    public BeastLibVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {

        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        INSTANCE = this;

    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
    }

    public static BeastLibVelocity getInstance(){
        return INSTANCE;
    }
}