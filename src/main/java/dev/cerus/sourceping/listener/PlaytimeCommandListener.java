package dev.cerus.sourceping.listener;

import static dev.cerus.sourceping.Launcher.LOGGER;
import dev.cerus.sourceping.storage.ping.PingStorageService;
import dev.cerus.sourceping.util.SlashCommandUtil;
import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

public class PlaytimeCommandListener extends ListenerAdapter {

    private final PingStorageService pingStorageService;

    public PlaytimeCommandListener(final PingStorageService pingStorageService) {
        this.pingStorageService = pingStorageService;
    }

    private static String formatMillis(final long l) {
        final long seconds = l / 1000 % 60;
        final long minutes = l / 1000 / 60 % 60;
        final long hours = l / 1000 / 60 / 60 % 24;
        final long days = l / 1000 / 60 / 60 / 24;

        final StringBuilder durationBuilder = new StringBuilder();
        if (days > 0) {
            durationBuilder.append(days).append("d ");
        }
        if (hours > 0) {
            durationBuilder.append(hours).append("h ");
        }
        if (minutes > 0) {
            durationBuilder.append(minutes).append("m ");
        }
        if (seconds > 0) {
            durationBuilder.append(seconds).append("s ");
        }
        if (durationBuilder.length() == 0) {
            durationBuilder.append("0s");
        }
        return durationBuilder.toString().trim();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull final SlashCommandInteractionEvent event) {
        if (event.getCommandIdLong() != SlashCommandUtil.getCommandId("playtime")) {
            return;
        }

        event.deferReply(true).queue(h -> {
            final String username = event.getOption("username", OptionMapping::getAsString);
            this.pingStorageService.getPlaytime(username).whenComplete((playtime, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to handle /players", throwable);
                    h.editOriginal("Unable to process request").queue();
                    return;
                }

                h.editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("Playtime of " + username)
                        .setDescription(formatMillis(playtime))
                        .setColor(new Color(0x13B544))
                        .build()).queue();
            });
        });
    }

}
