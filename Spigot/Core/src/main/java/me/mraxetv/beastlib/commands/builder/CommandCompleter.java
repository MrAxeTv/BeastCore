package me.mraxetv.beastlib.commands.builder;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class CommandCompleter {

    private final List<SubCommand> commands;

    protected CommandCompleter(CommandBuilder handle) {
        this.commands = handle.getSubCommands();
    }

    private List<String> getMainSuggestions(CommandSender sender, String start) {
        return this.commands.stream().filter(command -> {
                    boolean match = command.getName().startsWith(start);
                    if(!match)
                        return false;
                    if(command.getName() == null || command.getName().isEmpty())
                        return false;
                    if(command.getPermission() == null || command.getPermission().isEmpty())
                        return true;
                    return sender.hasPermission(command.getPermission());
                })
                .map(SubCommand::getName)
                .collect(Collectors.toList());
    }

    private List<String> getSubSuggestions(CommandSender sender, String[] args) {
        SubCommand subCommand = this.commands.stream()
                .filter(command -> {
                    boolean match = command.getName().equalsIgnoreCase(args[0]);
                    if(!match)
                        return false;
                    if(command.getPermission() == null || command.getPermission().isEmpty())
                        return true;
                    return sender.hasPermission(command.getPermission());
                })
                .findAny().orElse(null);
        return subCommand == null ? null : subCommand.tabComplete(sender, args);
    }

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return this.getMainSuggestions(sender, args[0]);
        } else if (args.length > 1) {
            return this.getSubSuggestions(sender, args);
        }
        return null;
    }
}