package me.mraxetv.beastlib.commands.builder;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractHelpCommand extends SubCommand {

    public AbstractHelpCommand(CommandBuilder handle,String name) {
        super(handle,name);
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getPermission() {
        return "";
    }

    @Override
    public String getDescription() {
        return "Shows available commands.";
    }

    @Override
    public String getUsage() {
        return "&7Syntax: &6/" + handle.getName() + " help <page>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("?");
    }

    @Override
    public boolean allowConsole() {
        return true;
    }

    @Override
    public void playerExecute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            handleCommands(sender, 0);
            return;
        }
        int page = getPage(args[1]);
        handleCommands(sender, page);
    }

    @Override
    public void consoleExecute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            handleCommands(sender, 0);
            return;
        }
        int page = getPage(args[1]);
        handleCommands(sender, page);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            int max = handle.getSubCommands().size() / handle.getHelpSuggestions();
            List<Integer> index = Lists.newArrayList();
            for (int i = 0; i < max; i++) {
                index.add(i);
            }
            return index.stream().map(String::valueOf).filter(i -> i.startsWith(args[1])).collect(Collectors.toList());
        }
        return null;
    }

    private int getPage(String str) {
        try {
            int page = Integer.parseInt(str);
            return Math.max(page, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private void handleCommands(CommandSender sender, int page) {
        List<SubCommand> filter = subCommands.stream()
                .filter(command -> {
                    if(command.getPermission() == null || command.getPermission().isEmpty())
                        return true;
                    return sender.hasPermission(command.getPermission());
                })
                .collect(Collectors.toList());
        int max = Math.min(handle.getHelpSuggestions() * (page + 1), filter.size());
        if (handle.getHelpHeader() != null)
            sender.sendMessage(handle.getHelpHeader());
        for (int i = page * handle.getHelpSuggestions(); i < max; i++) {
            SubCommand command = filter.get(i);
            TextComponent clickableCommand = createClickableCommand(command);
            if(sender instanceof Player)
                ((Player) sender).spigot().sendMessage(clickableCommand);
            else
                sender.sendMessage(handle.getHelpCommandColor() + "/" + handle.getUsage() + ": "
                        + handle.getHelpDescriptionColor() + command.getDescription());
        }
        if(sender instanceof Player) {
            Player player = (Player) sender;
            TextComponent previousPage = createClickableButton("&e&l«",
                    "/" + handle.getName() + " help " + (page - 1),
                    "&7Previous page");
            TextComponent nextPage = createClickableButton("&e&l»",
                    "/" + handle.getName() + " help " + (page + 1),
                    "&7Next page");
            TextComponent pageInfo = createClickableButton(" &e&l" + (page + 1) + " ",
                    null, "&7You're in page " + (page + 1));
            CommandBuilder.ClickableText spacing = new CommandBuilder.ClickableText("                       ");
            boolean canNextPage = handle.getHelpSuggestions() * (page + 1) < filter.size();
            if (page < 1) {
                if (canNextPage)
                    player.spigot().sendMessage(spacing.build(), pageInfo, nextPage);
                else
                    player.spigot().sendMessage(spacing.build(), pageInfo);

            } else {
                if (canNextPage)
                    player.spigot().sendMessage(spacing.build(), previousPage, pageInfo, nextPage);
                else
                    player.spigot().sendMessage(spacing.build(), previousPage, pageInfo);
            }
        }
        if (handle.getHelpFooter() != null)
            sender.sendMessage(handle.getHelpFooter());
    }

    private TextComponent createClickableCommand(SubCommand command) {
        return new CommandBuilder.ClickableText(handle.getHelpCommandColor() + "/" + command.getUsage() + ": "
                + handle.getHelpDescriptionColor() + command.getDescription())
                .setHoverAction(HoverEvent.Action.SHOW_TEXT, "&7Click to get this command.")
                .setClickAction(ClickEvent.Action.SUGGEST_COMMAND, "/"+handle.getName()+" "+command.getName())
                .build();
    }

    private TextComponent createClickableButton(String name, String clickAction, String... hoverAction) {
        CommandBuilder.ClickableText clickableText = new CommandBuilder.ClickableText(name);
        if (hoverAction.length > 0)
            clickableText.setHoverAction(HoverEvent.Action.SHOW_TEXT, hoverAction);
        if (clickAction != null)
            clickableText.setClickAction(ClickEvent.Action.RUN_COMMAND, clickAction);
        return clickableText.build();
    }
}