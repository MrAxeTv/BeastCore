package me.mraxetv.beastlib.commands.builder;

import com.google.common.collect.Lists;
import me.mraxetv.beastlib.api.BeastLibAPI;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CommandBuilder extends BukkitCommand {
    private static final String DEFAULT_NO_PERMISSIONS_MESSAGE = "&cYou do not have permission to do that.";
    private static final String DEFAULT_NO_SUB_COMMAND_FOUND_MESSAGE = "&cUnknown subcommand.";
    private static final String DEFAULT_NO_CONSOLE_ALLOWED_MESSAGE = "&cThis command cannot be used from console.";

    private final JavaPlugin plugin;
    private final BeastLibAPI beastLibAPI;
    private final UUID uniqueId;

    private final List<SubCommand> subCommands;

    private String noPermissionsMessage = normalizeMessage(null, DEFAULT_NO_PERMISSIONS_MESSAGE);

    private String noSubCommandFoundMessage = normalizeMessage(null, DEFAULT_NO_SUB_COMMAND_FOUND_MESSAGE);

    private String noConsoleAllowMessage = normalizeMessage(null, DEFAULT_NO_CONSOLE_ALLOWED_MESSAGE);

    private String helpHeader;

    private String helpFooter;

    private String helpCommandColor;

    private String helpDescriptionColor;

    private int helpSuggestions;

    public CommandBuilder(JavaPlugin plugin, String name, String description, String usageMessage, List<String> aliases) {
        super(name.toLowerCase(), description, usageMessage,
                aliases.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toList()));
        this.beastLibAPI = (BeastLibAPI) plugin;
        this.uniqueId = UUID.randomUUID();
        this.plugin = plugin;
        this.subCommands = Lists.newArrayList();
    }

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0) {
                String name = args[0];
                Optional<SubCommand> optCommand = this.subCommands.stream().filter(command -> {
                    boolean matchAliases = command.getAliases().contains(name);
                    return command.getName().equalsIgnoreCase(name) || matchAliases;
                }).findAny();

                if (!optCommand.isPresent()) {
                    sendNoSubCommand(sender);
                    return true;
                }

                SubCommand command = optCommand.get();

                if (!command.allowConsole()) {
                    sendNoConsole(sender);
                    return true;
                }

                command.consoleExecute(sender, args);
            } else {
                Optional<SubCommand> defaultHelp = this.subCommands.stream()
                        .filter(cmd -> cmd.getName().equalsIgnoreCase("help"))
                        .findFirst();

                if (defaultHelp.isPresent() && defaultHelp.get().allowConsole()) {
                    defaultHelp.get().consoleExecute(sender, new String[]{"help"});
                } else {
                    sendNoSubCommand(sender);
                }
            }

            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0) {
            String name = args[0];
            Optional<SubCommand> optCommand = this.subCommands.stream().filter(command -> {
                boolean matchAliases = command.getAliases().contains(name);
                return command.getName().equalsIgnoreCase(name) || matchAliases;
            }).findAny();

            if (!optCommand.isPresent()) {
                sendNoSubCommand(sender);
                return true;
            }

            SubCommand command = optCommand.get();
            String permission = command.getPermission();

            if (permission == null || player.hasPermission(permission)) {
                command.playerExecute(sender, args);
            } else {
                sendNoPermission(sender);
            }
        } else {
            Optional<SubCommand> defaultHelp = this.subCommands.stream()
                    .filter(cmd -> cmd.getName().equalsIgnoreCase("help"))
                    .findFirst();

            if (defaultHelp.isPresent()) {
                SubCommand helpCmd = defaultHelp.get();
                String permission = helpCmd.getPermission();

                if (permission == null || player.hasPermission(permission)) {
                    helpCmd.playerExecute(sender, new String[]{"help"});
                } else {
                    sendNoPermission(sender);
                }
            } else {
                sendNoSubCommand(sender);
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
        return new CommandCompleter(this).onTabComplete(sender, args);
    }

    /**
     * Add sub command to your main command
     *
     * @param commands Sub command to add
     */
    public CommandBuilder addSubCommand(SubCommand... commands) {
        List<SubCommand> filter = Arrays.asList(commands).stream()
                .filter(c -> !isRegistered(c)).collect(Collectors.toList());
        this.subCommands.addAll(filter);
        return this;
    }

    public boolean isRegistered(SubCommand command) {
        return this.subCommands.stream().anyMatch(c -> c.getName().equals(command.getName()));
    }

    /**
     * Register command to server in {@code onEnable()} method
     */
    public CommandBuilder register() {
        try {
            addSubCommand(new AbstractHelpCommand(this, "help"));
            if (!getKnownCommands().containsKey(getName())) {
                getKnownCommands().put(getName(), this);
                getKnownCommands().put(plugin.getDescription().getName().toLowerCase() + ":" + getName(), this);
            }
            for (String alias : getAliases()) {
                if (getKnownCommands().containsKey(alias)) {
                    continue;
                }
                getKnownCommands().put(alias, this);
                getKnownCommands().put(plugin.getDescription().getName().toLowerCase() + ":" + alias, this);
            }
            register(getCommandMap());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Unregister command from server in {@code onDisable()} method
     */
    public CommandBuilder unregister() {
        try {
            unregister(getCommandMap());
            Map<String, Command> knownCommands = getKnownCommands();
            List<String> keysToRemove = knownCommands.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof CommandBuilder)
                    .filter(entry -> ((CommandBuilder) entry.getValue()).getUniqueId().equals(this.getUniqueId()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (String key : keysToRemove) {
                knownCommands.remove(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    private CommandMap getCommandMap() throws Exception {
        Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        field.setAccessible(true);
        return (CommandMap) field.get(Bukkit.getServer());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands() throws Exception {
        Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
        cmField.setAccessible(true);
        CommandMap cm = (CommandMap) cmField.get(Bukkit.getServer());
        cmField.setAccessible(false);
        Map<String, Command> knownCommands;
        try {
            knownCommands = (Map<String, Command>) cm.getClass().getDeclaredMethod("getKnownCommands").invoke(cm);
        } catch (Exception e) {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            knownCommands = (Map<String, Command>) field.get(cm);
        }
        return knownCommands;
    }

    /**
     * Get unique id of this command
     *
     * @return Command unique id
     */
    public UUID getUniqueId() {
        return uniqueId;
    }

    /**
     * Set message when player doesn't have permission to access a sub command
     *
     * @param noPermissionsMessage Message to set
     */
    public CommandBuilder setNoPermissionsMessage(String noPermissionsMessage) {
        this.noPermissionsMessage = normalizeMessage(noPermissionsMessage, DEFAULT_NO_PERMISSIONS_MESSAGE);
        return this;
    }

    /**
     * Set message when player input matches no sub command
     *
     * @param noSubCommandFoundMessage Message to set
     */
    public CommandBuilder setNoSubCommandFoundMessage(String noSubCommandFoundMessage) {
        this.noSubCommandFoundMessage = normalizeMessage(noSubCommandFoundMessage, DEFAULT_NO_SUB_COMMAND_FOUND_MESSAGE);
        return this;
    }

    /**
     * Set message when command is not allowed for console use
     *
     * @param noConsoleAllowMessage Message to set
     */
    public CommandBuilder setNoConsoleAllowMessage(String noConsoleAllowMessage) {
        this.noConsoleAllowMessage = normalizeMessage(noConsoleAllowMessage, DEFAULT_NO_CONSOLE_ALLOWED_MESSAGE);
        return this;
    }

    private static String normalizeMessage(String message, String fallback) {
        String value = message == null || message.trim().isEmpty() ? fallback : message;
        return translateColorCodes(value);
    }

    private static String translateColorCodes(String value) {
        return value.replace("&", Character.toString(ChatColor.COLOR_CHAR));
    }

    private void sendNoPermission(CommandSender sender) {
        beastLibAPI.getUtils().sendMessage(sender, noPermissionsMessage);
    }

    private void sendNoSubCommand(CommandSender sender) {
        beastLibAPI.getUtils().sendMessage(sender, noSubCommandFoundMessage);
    }

    private void sendNoConsole(CommandSender sender) {
        beastLibAPI.getUtils().sendMessage(sender, noConsoleAllowMessage);
    }

    protected int getHelpSuggestions() {
        return helpSuggestions;
    }

    public CommandBuilder setHelpSuggestions(int helpSuggestions) {
        this.helpSuggestions = helpSuggestions;
        if (this.helpSuggestions < 1) {
            return setHelpSuggestions(5);
        }
        return this;
    }

    protected String getHelpHeader() {
        return this.helpHeader;
    }

    public CommandBuilder setHelpHeader(String helpHeader) {
        this.helpHeader = translateColorCodes(helpHeader);
        return this;
    }

    protected String getHelpFooter() {
        return this.helpFooter;
    }

    public CommandBuilder setHelpFooter(String helpFooter) {
        this.helpFooter = translateColorCodes(helpFooter);
        return this;
    }

    protected String getHelpCommandColor() {
        return helpCommandColor == null ? "&a" : this.helpCommandColor;
    }

    public CommandBuilder setHelpCommandColor(ChatColor color) {
        return setHelpCommandColor(String.valueOf(color.getChar()));
    }

    public CommandBuilder setHelpCommandColor(String color) {
        this.helpCommandColor = translateColorCodes(color);
        return this;
    }

    protected String getHelpDescriptionColor() {
        return helpDescriptionColor == null ? "&7" : this.helpDescriptionColor;
    }

    public CommandBuilder setHelpDescriptionColor(ChatColor color) {
        return setHelpCommandColor(String.valueOf(color.getChar()));
    }

    public CommandBuilder setHelpDescriptionColor(String color) {
        this.helpDescriptionColor = translateColorCodes(color);
        return this;
    }

    /**
     * Get list of registered sub commands
     *
     * @return List of sub commands
     */
    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    protected static class ClickableText {

        private String text;

        private HoverEvent hoverAction;

        private ClickEvent clickAction;

        public ClickableText(String text) {
            Objects.requireNonNull(text);
            this.text = translateColorCodes(text);
        }

        public ClickableText setHoverAction(HoverEvent.Action action, String... content) {
            TextComponent[] texts = Arrays.stream(content)
                    .map(e -> new TextComponent(translateColorCodes(e)))
                    .collect(Collectors.toList()).toArray(new TextComponent[content.length]);
            this.hoverAction = new HoverEvent(action, texts);
            return this;
        }

        public ClickableText setClickAction(ClickEvent.Action action, String value) {
            this.clickAction = new ClickEvent(action, value);
            return this;
        }

        public ClickableText setText(String text) {
            this.text = text;
            return this;
        }

        public TextComponent build() {
            TextComponent component = new TextComponent();
            component.setText(this.text);
            if (this.hoverAction != null) {
                component.setHoverEvent(this.hoverAction);
            }
            if (this.clickAction != null) {
                component.setClickEvent(this.clickAction);
            }
            return component;
        }
    }
}
