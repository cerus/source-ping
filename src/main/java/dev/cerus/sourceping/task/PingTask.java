package dev.cerus.sourceping.task;

import com.ibasco.agql.protocols.valve.source.query.SourceQueryClient;
import com.ibasco.agql.protocols.valve.source.query.SourceQueryOptions;
import com.ibasco.agql.protocols.valve.source.query.info.SourceServer;
import com.ibasco.agql.protocols.valve.source.query.players.SourcePlayer;
import static dev.cerus.sourceping.Launcher.LOGGER;
import dev.cerus.sourceping.model.Config;
import dev.cerus.sourceping.model.PingData;
import dev.cerus.sourceping.storage.ping.PingStorageService;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

public class PingTask implements Runnable {

    private final Config config;
    private final PingStorageService pingStorageService;

    public PingTask(final Config config, final PingStorageService pingStorageService) {
        this.config = config;
        this.pingStorageService = pingStorageService;
    }

    @Override
    public void run() {
        try (final SourceQueryClient client = new SourceQueryClient(SourceQueryOptions.builder().build())) {
            final InetSocketAddress address = new InetSocketAddress(this.config.getServerHost(), this.config.getServerPort());
            final SourceServer info = client.getInfo(address).join().getResult();
            final List<SourcePlayer> players = client.getPlayers(address).join().getResult();
            final List<String> playerNames = players.stream().map(SourcePlayer::getName).collect(Collectors.toList());

            //LOGGER.info("Ping: " + playerNames.size());
            this.pingStorageService.store(new PingData(System.currentTimeMillis(), info.getNumOfPlayers(), playerNames))
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            LOGGER.error("Failed to store ping result", throwable);
                        }
                    });
        } catch (final Throwable e) {
            LOGGER.error("Failed to ping server", e);
        }
    }

}
