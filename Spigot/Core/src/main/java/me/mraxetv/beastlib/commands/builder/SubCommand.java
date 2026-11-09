package me.mraxetv.beastlib.commands.builder;

import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class SubCommand {

    protected   List<SubCommand> subCommands;
    protected CommandBuilder handle;

    protected String name;

    public SubCommand(CommandBuilder handle,String name) {
        this.handle = handle;
        this.name = name;
        this.subCommands = handle.getSubCommands();
    }

    /**
     * Get name of sub command
     *
     * @return Sub command name
     */
    public String getName() {return name;}

    /**
     * Get permission of sub command
     *
     * @return Sub command permission
     */
    public abstract String getPermission();

    /**
     * Get description of sub command
     *
     * @return Sub command description
     */
    public abstract String getDescription();

    /**
     * Get usage of sub command
     *
     * @return Sub command usage
     */
    public String getUsage(){

        return handle.getName()+ " " + name;
    }

    /**
     * Get list of aliases of sub command
     *
     * @return Sub command aliases
     */
    public abstract List<String> getAliases();

    /**
     * Allow console to use this command
     *
     * @return True if allow, otherwise false
     */
    public abstract boolean allowConsole();

    /**
     * Player execution
     */
    public abstract void playerExecute(CommandSender sender, String[] args);

    /**
     * Console execution
     */
    public abstract void consoleExecute(CommandSender sender, String[] args);

    /**
     * Tab complete for sub command
     */
    public abstract List<String> tabComplete(CommandSender sender, String[] args);
}
