package dev.cerus.sourceping.listener;

import static dev.cerus.sourceping.Launcher.LOGGER;
import dev.cerus.sourceping.storage.ping.PingStorageService;
import dev.cerus.sourceping.util.SlashCommandUtil;
import java.awt.Color;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PlayersCommandListener extends ListenerAdapter {

    private final PingStorageService pingStorageService;

    public PlayersCommandListener(final PingStorageService pingStorageService) {
        this.pingStorageService = pingStorageService;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull final SlashCommandInteractionEvent event) {
        if (event.getCommandIdLong() != SlashCommandUtil.getCommandId("players")) {
            return;
        }

        event.deferReply(true).queue(h -> {
            this.pingStorageService.getPingData(System.currentTimeMillis()).whenComplete((pingData, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Failed to handle /players", throwable);
                    h.editOriginal("Unable to process request").queue();
                    return;
                }

                final StringBuilder msgBuilder = new StringBuilder("**").append(pingData.playerCount()).append(" players**");
                if (!pingData.playerNames().isEmpty()) {
                    msgBuilder.append("\n").append(pingData.playerNames().stream()
                            .map(s -> "`" + s + "`")
                            .collect(Collectors.joining(", ")));
                }
                h.editOriginalEmbeds(new EmbedBuilder()
                        .setTitle("Players")
                        .setDescription(msgBuilder.toString())
                        .setColor(new Color(0xE39017))
                        .build()).queue();
            });
        });
    }

}
