package dev.cerus.sourceping.model;

import java.util.concurrent.TimeUnit;

public class Config {

    private final String serverHost;
    private final int serverPort;
    private final String discordToken;
    private final Database database;
    private final int pingPeriod;
    private final TimeUnit pingPeriodUnit;

    public Config(final String serverHost, final int serverPort, final String discordToken, final Database database, final int pingPeriod, final TimeUnit pingPeriodUnit) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.discordToken = discordToken;
        this.database = database;
        this.pingPeriod = pingPeriod;
        this.pingPeriodUnit = pingPeriodUnit;
    }

    public static Config makeDefault() {
        return new Config("localhost", 27015, "abcdef", new Database(
                "sqlite",
                new Database.Sqlite("./data.db"),
                new Database.MariaDb("jdbc:mariadb://localhost:3306/db", "foo", "bar")
        ), 30, TimeUnit.SECONDS);
    }

    public String getServerHost() {
        return this.serverHost;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public String getDiscordToken() {
        return this.discordToken;
    }

    public Database getDatabase() {
        return this.database;
    }

    public int getPingPeriod() {
        return this.pingPeriod;
    }

    public TimeUnit getPingPeriodUnit() {
        return this.pingPeriodUnit;
    }

    public static final class Database {

        private final String db;
        private final Sqlite sqlite;
        private final MariaDb mariaDb;

        public Database(final String db, final Sqlite sqlite, final MariaDb mariaDb) {
            this.db = db;
            this.sqlite = sqlite;
            this.mariaDb = mariaDb;
        }

        public String getDb() {
            return this.db;
        }

        public Sqlite getSqlite() {
            return this.sqlite;
        }

        public MariaDb getMariaDb() {
            return this.mariaDb;
        }

        public static class Sqlite {

            private final String path;

            public Sqlite(final String path) {
                this.path = path;
            }

            public String getPath() {
                return this.path;
            }
        }

        public static class MariaDb {

            private final String jdbcUrl;
            private final String username;
            private final String password;

            public MariaDb(final String jdbcUrl, final String username, final String password) {
                this.jdbcUrl = jdbcUrl;
                this.username = username;
                this.password = password;
            }

            public String getJdbcUrl() {
                return this.jdbcUrl;
            }

            public String getUsername() {
                return this.username;
            }

            public String getPassword() {
                return this.password;
            }

        }

    }

}
