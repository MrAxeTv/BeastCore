package me.mraxetv.beastlib;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import dev.dejvokep.boostedyaml.YamlDocument;

import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.api.client.ClientDisplayService;
import me.mraxetv.beastlib.api.client.ClientDisplayServices;
import me.mraxetv.beastlib.api.client.ClientEventService;
import me.mraxetv.beastlib.api.client.ClientEventServices;
import me.mraxetv.beastlib.client.BukkitClientDisplayService;
import me.mraxetv.beastlib.client.BukkitClientEventService;
import me.mraxetv.beastlib.api.scheduler.SchedulerService;
import me.mraxetv.beastlib.api.scheduler.SchedulerServices;
import me.mraxetv.beastlib.api.update.UpdateService;
import me.mraxetv.beastlib.api.update.UpdateServices;
import me.mraxetv.beastlib.filemanager.FileYml;
import me.mraxetv.beastlib.filemanager.MessagesYml;
import me.mraxetv.beastlib.listeners.JoinListener;
import me.mraxetv.beastlib.placeholderapi.PlaceHolderAPI;
import me.mraxetv.beastlib.scheduler.PlatformSchedulerService;
import me.mraxetv.beastlib.update.BukkitUpdateService;
import me.mraxetv.beastlib.utils.MCVersion;
import me.mraxetv.beastlib.utils.Utils;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;


public final class BeastLib extends JavaPlugin implements BeastLibAPI {


    private static BeastLib INSTANCE;
   // private ConfigYml configYml;
    private MessagesYml messagesYml;
    private static Utils utils;
    private FileYml headsYml;
    private BukkitUpdateService updateService;
    private PlatformSchedulerService schedulerService;
    private BukkitClientEventService clientEventService;
    private BukkitClientDisplayService clientDisplayService;

    @Override
    public void onEnable() {
        // Plugin startup logic
        INSTANCE = this;
        schedulerService = new PlatformSchedulerService(this);
        schedulerService.start();
        SchedulerServices.set(schedulerService);
        getServer().getServicesManager().register(
                SchedulerService.class,
                schedulerService,
                this,
                ServicePriority.Normal
        );
        updateService = new BukkitUpdateService(this);
        UpdateServices.set(updateService);
        MCVersion.getVersion();



        BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);
        libraryManager.addMavenCentral();
        libraryManager.addRepository("https://repo.codemc.org/repository/maven-public/");

        loadClientComponentsIfMissing(libraryManager);


        //MiniMessage
        Library minimessage = Library.builder().groupId("net{}kyori").artifactId("adventure-text-minimessage").version("4.26.1").relocate("net{}kyori","me.mraxetv.beastlib.lib.kyori").
                resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(minimessage);

        Library slf4jReload4j = Library.builder().groupId("org.slf4j").artifactId("slf4j-reload4j").version("2.0.20").resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(slf4jReload4j);
        Library log4jc = Library.builder().groupId("org.apache.logging.log4j").artifactId("log4j-core").version("2.26.1").resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(log4jc);
        Library log4j = Library.builder().groupId("org.apache.logging.log4j").artifactId("log4j-api").version("2.26.1").resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(log4j);
        Library boostedYml =
                Library.builder().groupId("dev.dejvokep").artifactId("boosted-yaml").relocate("dev{}dejvokep{}boostedyaml", "me.mraxetv.beastlib.lib.boostedyaml").version("1.3.7").resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(boostedYml);
        Library triumph =
                Library.builder().groupId("dev{}triumphteam").artifactId("triumph-gui").relocate("dev{}triumphteam", "me.mraxetv.beastlib.lib.tgui").version("3.1.13")
                        .relocate("net{}kyori","me.mraxetv.beastlib.lib.kyori").resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(triumph);
        //NBTAPI
        Library nbtapi = Library.builder().groupId("de.tr7zw").artifactId("item-nbt-api").version("2.16.1").relocate("de{}tr7zw{}changeme{}nbtapi", "me.mraxetv.beastlib.lib.nbtapi").build();
        libraryManager.loadLibrary(nbtapi);

        Library gson = Library.builder().groupId("com{}google{}code{}gson").artifactId("gson").version("2.14.0")
                .relocate("com{}google{}gson", "me.mraxetv.beastlib.lib.gson").build();
        libraryManager.loadLibrary(gson);

        Library mongoDB = Library.builder().groupId("org.mongodb").artifactId("mongo-java-driver").version("3.12.14").
                relocate("com{}mongodb", "me{}mraxetv{}beastlib{}lib{}mongodb").relocate("org{}bson", "me.mraxetv.beastlib.lib.bson").build();
        libraryManager.loadLibrary(mongoDB);

        //MySQL
        Library protobuf = Library.builder().groupId("com{}google{}protobuf").artifactId("protobuf-java").version("4.36.2")
                .relocate("com{}google{}protobuf", "me.mraxetv.beastlib.lib.protobuf").build();
        libraryManager.loadLibrary(protobuf);
        Library mysql = Library.builder().groupId("com{}mysql").artifactId("mysql-connector-j").version("9.7.0")
                .relocate("com{}mysql", "me.mraxetv.beastlib.lib.mysql")
                .relocate("com{}google{}protobuf", "me.mraxetv.beastlib.lib.protobuf")
                .resolveTransitiveDependencies(true)
                .excludeTransitiveDependency("com{}google{}protobuf", "protobuf-java").build();
        libraryManager.loadLibrary(mysql);

        //MariaDB
        Library mariaDB = Library.builder().groupId("org{}mariadb{}jdbc").artifactId("mariadb-java-client").version("3.5.10")
                .relocate("org{}mariadb", "me.mraxetv.beastlib.lib.mariadb")
                .resolveTransitiveDependencies(true).build();
        libraryManager.loadLibrary(mariaDB);

        //H2
        Library h2 = Library.builder().groupId("com{}h2database").artifactId("h2").version("2.2.224")
                .relocate("org{}h2", "me.mraxetv.beastlib.lib.h2").build();
        libraryManager.loadLibrary(h2);

        //PostgreSQL
        Library checkerQual = Library.builder().groupId("org{}checkerframework").artifactId("checker-qual").version("3.55.1")
                .relocate("org{}checkerframework", "me.mraxetv.beastlib.lib.checkerframework").build();
        libraryManager.loadLibrary(checkerQual);
        Library postgresql = Library.builder().groupId("org{}postgresql").artifactId("postgresql").version("42.7.14")
                .relocate("org{}postgresql", "me.mraxetv.beastlib.lib.postgresql")
                .relocate("org{}checkerframework", "me.mraxetv.beastlib.lib.checkerframework")
                .resolveTransitiveDependencies(true)
                .excludeTransitiveDependency("org{}checkerframework", "checker-qual").build();
        libraryManager.loadLibrary(postgresql);

        //Redis/Jedis
        Library commonsPool = Library.builder().groupId("org{}apache{}commons").artifactId("commons-pool2").version("2.12.1")
                .relocate("org{}apache{}commons{}pool2", "me.mraxetv.beastlib.lib.pool2").build();
        libraryManager.loadLibrary(commonsPool);
        Library jsonJava = Library.builder().groupId("org{}json").artifactId("json").version("20260814")
                .relocate("org{}json", "me.mraxetv.beastlib.lib.jsonjava").build();
        libraryManager.loadLibrary(jsonJava);
        Library jedis = Library.builder().groupId("redis{}clients").artifactId("jedis").version("5.2.0")
                .relocate("redis{}clients{}jedis", "me.mraxetv.beastlib.lib.jedis")
                .relocate("org{}apache{}commons{}pool2", "me.mraxetv.beastlib.lib.pool2")
                .relocate("org{}json", "me.mraxetv.beastlib.lib.jsonjava")
                .relocate("com{}google{}gson", "me.mraxetv.beastlib.lib.gson")
                .resolveTransitiveDependencies(true)
                .excludeTransitiveDependency("org{}slf4j", "slf4j-api")
                .excludeTransitiveDependency("org{}apache{}commons", "commons-pool2")
                .excludeTransitiveDependency("org{}json", "json")
                .excludeTransitiveDependency("com{}google{}code{}gson", "gson").build();
        libraryManager.loadLibrary(jedis);

        //Hikari
        Library hikari = Library.builder().groupId("com{}zaxxer").artifactId("HikariCP").version("4.0.3").relocate("com{}zaxxer", "me{}mraxetv{}beastlib{}lib").build();
        libraryManager.loadLibrary(hikari);

        /*preloadSharedClasses(
                "me.mraxetv.beastlib.lib.hikari.HikariConfig",
                "me.mraxetv.beastlib.lib.hikari.HikariDataSource"
        );*/

        //MaterialX
        Library xSeries = Library.builder().groupId("com{}github{}cryptomorin").artifactId("XSeries").version("14.0.0").relocate("com{}cryptomorin{}xseries", "me.mraxetv.beastlib.lib.xmaterials").build();
        libraryManager.loadLibrary(xSeries);
        loadSqliteIfMissing(libraryManager);
        //configYml = new ConfigYml(this, "config.yml");

        //Start it after dynamical libs are loaded
        updateService.start();

        clientEventService = new BukkitClientEventService(this);
        clientEventService.start();
        ClientEventServices.set(clientEventService);
        getServer().getServicesManager().register(ClientEventService.class, clientEventService,
                this, ServicePriority.Normal);
        clientDisplayService = new BukkitClientDisplayService(this);
        clientDisplayService.start();
        ClientDisplayServices.set(clientDisplayService);
        getServer().getServicesManager().register(ClientDisplayService.class, clientDisplayService,
                this, ServicePriority.Normal);


        messagesYml = new MessagesYml<>(this);
        utils = new Utils(this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceHolderAPI(this).register();
        }

        headsYml = new FileYml(this, "heads-data.yml", true);

        registerEvent();


        schedulerService.runGlobalDelayed(this, () -> {
            // Create metrics later when the server is stable
            new Metrics(this, 23534);
        }, 1L);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (isAdventureComponentLoaded(classLoader)) {
            System.out.println("Adventure Component is loaded!");
        } else {
            System.out.println("Adventure Component is NOT loaded!");
        }

        /*ClassLoader classLoader = getClass().getClassLoader();

        List<Class<?>> loadedClasses = RuntimeClassLoaderScanner.getLoadedClassesUnderPackage(
                classLoader,
                "me.mraxetv.beastlib.lib"
        );

        for (Class<?> clazz : loadedClasses) {
            System.out.println(clazz.getName());
        }*/


    }





        private static final String ADVENTURE_COMPONENT_CLASS =
                "net.kyori.adventure.text.Component";

        public static boolean isAdventureComponentLoaded(ClassLoader classLoader) {
            try {
                Class.forName(ADVENTURE_COMPONENT_CLASS, false, classLoader);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }


    public static BeastLib getInstance() {
        return INSTANCE;
    }

    public void registerEvent() {
        new JoinListener(this);
    }

    public void onDisable() {

        if (clientDisplayService != null) {
            getServer().getServicesManager().unregister(ClientDisplayService.class, clientDisplayService);
            clientDisplayService.shutdown();
            clientDisplayService = null;
        }
        ClientDisplayServices.clear();
        if (clientEventService != null) {
            getServer().getServicesManager().unregister(ClientEventService.class, clientEventService);
            clientEventService.shutdown();
            clientEventService = null;
        }
        ClientEventServices.clear();

        if (updateService != null) {
            updateService.shutdown();
            updateService = null;
        }
        UpdateServices.clear();

        if (schedulerService != null) {
            getServer().getServicesManager().unregister(SchedulerService.class, schedulerService);
            schedulerService.shutdown();
            schedulerService = null;
        }
        SchedulerServices.clear();

        if (headsYml != null) {
            headsYml.saveConfig();
        }
    }

    private void preloadSharedClasses(String... classNames) {
        ClassLoader classLoader = getClass().getClassLoader();
        for (String className : classNames) {
            try {
                Class.forName(className, true, classLoader);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Failed to preload shared library class: " + className, ex);
            }
        }
    }

    private void loadSqliteIfMissing(BukkitLibraryManager libraryManager) {
        if (loadClass("org.sqlite.JDBC")) {
            return;
        }

        Library sqlite = Library.builder()
                .groupId("org{}xerial")
                .artifactId("sqlite-jdbc")
                .version("3.53.4.0")
                .build();
        libraryManager.loadLibrary(sqlite);

        if (!loadClass("org.sqlite.JDBC")) {
            throw new IllegalStateException("SQLite JDBC was not available after dynamic loading.");
        }
    }

    /** Spigot consumers share Adventure through their declared BeastLib dependency. */
    private void loadClientComponentsIfMissing(BukkitLibraryManager libraryManager) {
        String component = "net{}kyori{}adventure{}text{}Component".replace("{}", ".");
        try {
            Class.forName(component, false, Bukkit.class.getClassLoader());
            return;
        } catch (ClassNotFoundException spigot) {
            // Paper supplies its native Adventure API; plain Spigot needs a shared copy.
        }
        try {
            Class<?> constants = Class.forName("net.minecraft.SharedConstants", false,
                    Bukkit.class.getClassLoader());
            Object protocol = constants.getMethod("getProtocolVersion").invoke(null);
            if (!(protocol instanceof Number) || ((Number) protocol).intValue() < 776) return;
        } catch (ReflectiveOperationException legacyServer) {
            // Older BeastLib consumers retain their existing relocated Java 8 libraries.
            return;
        }
        for (String artifact : new String[] {
                "adventure-api", "adventure-text-serializer-gson", "adventure-text-minimessage"
        }) {
            libraryManager.loadLibrary(Library.builder().groupId("net{}kyori")
                    .artifactId(artifact).version("5.2.0")
                    .resolveTransitiveDependencies(true).build());
        }
        try {
            Class.forName(component, false, getClass().getClassLoader());
        } catch (ClassNotFoundException error) {
            throw new IllegalStateException("Shared client component API did not load", error);
        }
    }

    private boolean loadClass(String className) {
        try {
            Class.forName(className, true, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public FileConfiguration getHeadsConfig() {
        return headsYml.getConfig();
    }

    @Override
    public YamlDocument getMessages() {
        return messagesYml.getConfig();
    }

    @Override
    public UpdateService getUpdateService() {
        return updateService == null ? UpdateServices.get() : updateService;
    }

    @Override
    public SchedulerService getScheduler() {
        return schedulerService == null ? SchedulerServices.get() : schedulerService;
    }

    public Utils getUtils() {
        return utils;
    }
}
