package dev.cerus.sourceping.task;

import static dev.cerus.sourceping.Launcher.LOGGER;
import dev.cerus.sourceping.storage.PlayerCountProvider;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class DiscordPresenceTask implements Runnable {

    private final JDA jda;
    private final PlayerCountProvider pingStorageService;

    public DiscordPresenceTask(final JDA jda, final PlayerCountProvider pingStorageService) {
        this.jda = jda;
        this.pingStorageService = pingStorageService;
    }

    @Override
    public void run() {
        this.pingStorageService.getLatestPlayerCount().whenComplete((players, throwable) -> {
            if (throwable != null) {
                LOGGER.warn("Failed to retrieve the latest player count from the database", throwable);
                return;
            }

            this.jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching("%d player%s".formatted(players, players == 1 ? "" : "s")));
        });
    }

}
