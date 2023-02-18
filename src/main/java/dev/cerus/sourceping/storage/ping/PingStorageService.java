package dev.cerus.sourceping.storage.ping;

import dev.cerus.sourceping.model.PingData;
import dev.cerus.sourceping.storage.PlayerCountProvider;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface PingStorageService extends PlayerCountProvider {

    CompletableFuture<Void> store(PingData pingData);

    CompletableFuture<Collection<String>> getPlayersForPing(long timestamp);

    @Override
    default CompletableFuture<Integer> getLatestPlayerCount() {
        return this.getPingData(System.currentTimeMillis()).thenApply(PingData::playerCount);
    }

    CompletableFuture<PingData> getPingData(long timestamp);

    CompletableFuture<Long> getPlaytime(String username);

}
