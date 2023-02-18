package dev.cerus.sourceping.model;

import java.util.Collection;

public record PingData(long timestamp, int playerCount, Collection<String> playerNames) {

}
