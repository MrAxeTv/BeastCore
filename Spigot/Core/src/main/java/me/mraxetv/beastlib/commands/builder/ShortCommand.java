package me.mraxetv.beastlib.commands.builder;


import me.mraxetv.beastlib.api.BeastLibAPI;
import me.mraxetv.beastlib.utils.BUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShortCommand extends BukkitCommand {

    public JavaPlugin pl;
    private BeastLibAPI beastLibAPI;


    public ShortCommand(JavaPlugin pl, String cmd, List<String> aliases, String permission) {
        super(cmd.toLowerCase());
        this.pl = pl;
        this.beastLibAPI = (BeastLibAPI) pl;
        this.description = "Short command for " + cmd;
        this.usageMessage = "/"+cmd;
        this.setPermission(permission);
        setName(cmd.toLowerCase());
        setLabel(cmd);
        this.setAliases(aliases);
        register();
    }

    @Override
    public boolean execute(CommandSender sender, String alias, String[] args) {

        return true;
    }



    public void unRegisterBukkitCommand() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = getKnownCommands();
            String pluginPrefix = pl.getDescription().getName().toLowerCase() + ":";

            removeCommandKey(knownCommands, this.getName());
            removeCommandKey(knownCommands, pluginPrefix + this.getName());

            for (String alias : this.getAliases()) {
                removeCommandKey(knownCommands, alias);
                removeCommandKey(knownCommands, pluginPrefix + alias);
            }

            unregister(getCommandMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeCommandKey(Map<String, Command> knownCommands, String key) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }

        Command command = knownCommands.get(key);
        if (command == this) {
            knownCommands.remove(key);
        }
    }


    public void register() {
        try {
            if (!getKnownCommands().containsKey(getName())) {
                getKnownCommands().put(getName(), this);
                getKnownCommands().put(pl.getDescription().getName().toLowerCase() + ":" + getName(), this);
            }
            for (String alias : getAliases()) {
                if (getKnownCommands().containsKey(alias))
                    continue;
                getKnownCommands().put(alias, this);
                getKnownCommands().put(pl.getDescription().getName().toLowerCase() + ":" + alias, this);
            }
            register(getCommandMap());
            Bukkit.getConsoleSender().sendMessage(BUtils.setColor(beastLibAPI.getUtils().getPrefix()+" &2/" + getLabel() + " command and aliases &e" + getAliases() + " " +
                    "&2are " +
                    "registered."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private CommandMap getCommandMap() throws Exception {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (CommandMap) field.get(Bukkit.getServer());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() throws Exception {
        CommandMap cm = getCommandMap();
        Map<String, Command> knownCommands;
        try {
            knownCommands = (Map<String, Command>) cm.getClass().getDeclaredMethod("getKnownCommands").invoke(cm);
        }catch (Exception e) {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            knownCommands = (Map<String, Command>) field.get(cm);
        }
        return knownCommands;
    }


}
