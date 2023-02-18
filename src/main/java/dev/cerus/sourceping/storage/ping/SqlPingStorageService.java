package dev.cerus.sourceping.storage.ping;

import dev.cerus.sourceping.model.Config;
import dev.cerus.sourceping.model.PingData;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public abstract class SqlPingStorageService implements PingStorageService {

    private final Config config;
    private final ExecutorService executorService;

    public SqlPingStorageService(final Config config, final ExecutorService executorService) {
        this.config = config;
        this.executorService = executorService;
        this.init();
    }

    private void init() {
        this.exec(connection -> {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS sourceping_pings (timestamp BIGINT PRIMARY KEY, players INT)").executeUpdate();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS sourceping_playtime (username VARCHAR(256) PRIMARY KEY, playtime BIGINT)").executeUpdate();
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS sourceping_ping_players (id INTEGER PRIMARY KEY %s, username VARCHAR(256), ping_timestamp BIGINT)"
                    .formatted(this.getAutoIncrementString())).executeUpdate();
            return null;
        });
    }

    protected abstract Connection getConnection() throws SQLException;

    protected abstract void disposeConnection(Connection con) throws Exception;

    protected abstract String getAutoIncrementString();

    protected abstract String getUpsertString(String key);

    @Override
    public CompletableFuture<Void> store(final PingData pingData) {
        return this.insertPing(pingData)
                .thenCompose($ -> CompletableFuture.allOf(pingData.playerNames().stream()
                        .filter(user -> user.length() > 0 && !user.matches("\\s+"))
                        .map(user -> this.incrementPlaytime(user, this.config.getPingPeriodUnit().toMillis(this.config.getPingPeriod()))
                                .thenCompose($$ -> this.storePingPlayerAssociation(user, pingData.timestamp())))
                        .toList()
                        .toArray(new CompletableFuture<?>[0])));
    }

    @Override
    public CompletableFuture<Collection<String>> getPlayersForPing(final long timestamp) {
        return this.exec(connection -> {
            final PreparedStatement stmt = connection.prepareStatement("SELECT * FROM sourceping_ping_players WHERE ping_timestamp = ?");
            stmt.setLong(1, timestamp);
            final ResultSet resultSet = stmt.executeQuery();

            final List<String> names = new ArrayList<>();
            while (resultSet.next()) {
                names.add(resultSet.getString("username"));
            }
            return names;
        });
    }

    @Override
    public CompletableFuture<PingData> getPingData(final long timestamp) {
        return this.exec(connection -> {
            // https://stackoverflow.com/a/592230/10821925
            final PreparedStatement stmt = connection.prepareStatement("SELECT * FROM sourceping_pings ORDER BY ABS(timestamp - ?) ASC LIMIT 1");
            stmt.setLong(1, timestamp);
            final ResultSet resultSet = stmt.executeQuery();

            final int playerCount;
            final long correctTimestamp;
            if (resultSet.next()) {
                playerCount = resultSet.getInt("players");
                correctTimestamp = resultSet.getLong("timestamp");
            } else {
                return new PingData(timestamp, 0, List.of());
            }

            final Collection<String> playerNames = this.getPlayersForPing(correctTimestamp).join();
            return new PingData(correctTimestamp, playerCount, playerNames);
        });
    }

    @Override
    public CompletableFuture<Long> getPlaytime(final String username) {
        return this.exec(connection -> {
            final PreparedStatement stmt = connection.prepareStatement("SELECT * FROM sourceping_playtime WHERE username = ?");
            stmt.setString(1, username);
            final ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("playtime");
            }
            return 0L;
        });
    }

    private CompletableFuture<Void> insertPing(final PingData pingData) {
        return this.exec(connection -> {
            final PreparedStatement stmt = connection.prepareStatement("INSERT INTO sourceping_pings (timestamp, players) VALUES (?, ?)");
            stmt.setLong(1, pingData.timestamp());
            stmt.setInt(2, pingData.playerCount());
            stmt.executeUpdate();
            return null;
        });
    }

    private CompletableFuture<Void> incrementPlaytime(final String username, final long byMillis) {
        return this.exec(connection -> {
            final PreparedStatement stmt = connection.prepareStatement(("INSERT INTO sourceping_playtime (username, playtime) VALUES (?, ?) " +
                    "%s playtime = playtime + ?").formatted(this.getUpsertString("username")));
            stmt.setString(1, username);
            stmt.setLong(2, byMillis);
            stmt.setLong(3, byMillis);
            stmt.executeUpdate();
            return null;
        });
    }

    private CompletableFuture<Void> storePingPlayerAssociation(final String username, final long timestamp) {
        return this.exec(connection -> {
            final PreparedStatement stmt = connection.prepareStatement("INSERT INTO sourceping_ping_players (username, ping_timestamp) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setLong(2, timestamp);
            stmt.executeUpdate();
            return null;
        });
    }

    private <T> CompletableFuture<T> exec(final ThrowingRunnable<T> runnable) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        this.executorService.submit(() -> {
            try {
                final Connection con = this.getConnection();
                future.complete(runnable.run(con));
                this.disposeConnection(con);
            } catch (final Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @FunctionalInterface
    private interface ThrowingRunnable<T> {

        T run(Connection con) throws Throwable;

    }

}
