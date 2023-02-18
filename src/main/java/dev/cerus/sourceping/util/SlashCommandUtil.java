package dev.cerus.sourceping.util;

import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class SlashCommandUtil {

    private static final Map<String, Long> commandMap = new HashMap<>();

    private SlashCommandUtil() {
    }

    public static void registerCommands(final JDA jda) {
        jda.upsertCommand(Commands.slash("players", "Get a list of players on the server"))
                .queue(command -> commandMap.put("players", command.getIdLong()));
        jda.upsertCommand(Commands.slash("playtime", "Get the playtime of a specific player")
                        .addOption(OptionType.STRING, "username", "Steam name of the player", true))
                .queue(command -> commandMap.put("playtime", command.getIdLong()));
    }

    public static long getCommandId(final String name) {
        return commandMap.getOrDefault(name, -1L);
    }

}
