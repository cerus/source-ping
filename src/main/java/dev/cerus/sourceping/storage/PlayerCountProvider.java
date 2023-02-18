package dev.cerus.sourceping.storage;

import java.util.concurrent.CompletableFuture;

public interface PlayerCountProvider {

    CompletableFuture<Integer> getLatestPlayerCount();

}
