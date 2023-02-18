package dev.cerus.sourceping;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.sourceping.listener.PlayersCommandListener;
import dev.cerus.sourceping.listener.PlaytimeCommandListener;
import dev.cerus.sourceping.model.Config;
import dev.cerus.sourceping.storage.ping.MariaDbPingStorageService;
import dev.cerus.sourceping.storage.ping.PingStorageService;
import dev.cerus.sourceping.storage.ping.SqlitePingStorageService;
import dev.cerus.sourceping.task.DiscordPresenceTask;
import dev.cerus.sourceping.task.PingTask;
import dev.cerus.sourceping.util.SlashCommandUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    public static final Logger LOGGER = LoggerFactory.getLogger("source-ping");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.STRING).create();
    private static AutoCloseable dbCloseable;

    public static void main(final String[] args) {
        final File configFile = new File("./config.json");
        final Config config;
        if (configFile.exists()) {
            try (final FileInputStream fin = new FileInputStream(configFile);
                 final InputStreamReader inr = new InputStreamReader(fin)) {
                config = GSON.fromJson(inr, Config.class);
            } catch (final IOException ex) {
                LOGGER.error("Failed to read config", ex);
                return;
            }
        } else {
            try (final FileOutputStream fout = new FileOutputStream(configFile)) {
                fout.write(GSON.toJson(Config.makeDefault()).getBytes(StandardCharsets.UTF_8));
                fout.flush();
            } catch (final IOException ex) {
                LOGGER.error("Failed to save default config", ex);
                return;
            }
            LOGGER.info("Default config has been saved, please edit and restart");
            return;
        }

        final JDA jda;
        try {
            jda = JDABuilder.create(config.getDiscordToken(), List.of())
                    .disableCache(CacheFlag.getPrivileged())
                    .build().awaitReady();
        } catch (final InterruptedException e) {
            LOGGER.error("Failed to launch Discord bot", e);
            return;
        }

        final ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();
        final ExecutorService dbExecutor = Executors.newCachedThreadPool();

        final PingStorageService pingStorageService;
        try {
            pingStorageService = getStorage(config, dbExecutor);
        } catch (final Throwable t) {
            LOGGER.error("Failed to initialize storage service", t);
            return;
        }

        jda.addEventListener(new PlayersCommandListener(pingStorageService));
        jda.addEventListener(new PlaytimeCommandListener(pingStorageService));
        SlashCommandUtil.registerCommands(jda);

        taskExecutor.scheduleAtFixedRate(new PingTask(config, pingStorageService), 0, config.getPingPeriod(), config.getPingPeriodUnit());
        taskExecutor.scheduleAtFixedRate(new DiscordPresenceTask(jda, pingStorageService), 0, 30, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dbCloseable.close();
            } catch (final Exception e) {
                LOGGER.error("Failed to close the database connection", e);
            }

            jda.shutdown();
            taskExecutor.shutdown();
            dbExecutor.shutdown();
            LOGGER.info("See ya");
        }));
    }

    private static PingStorageService getStorage(final Config config, final ExecutorService executorService) throws SQLException {
        return switch (config.getDatabase().getDb().toLowerCase()) {
            case "sqlite":
                final Connection con = DriverManager.getConnection("jdbc:sqlite:" + config.getDatabase().getSqlite().getPath());
                dbCloseable = con;
                yield new SqlitePingStorageService(config, executorService, con);
            case "mariadb":
                final HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl(config.getDatabase().getMariaDb().getJdbcUrl());
                hikariConfig.setUsername(config.getDatabase().getMariaDb().getUsername());
                hikariConfig.setPassword(config.getDatabase().getMariaDb().getPassword());
                hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
                final HikariDataSource dataSource = new HikariDataSource(hikariConfig);
                dbCloseable = dataSource;
                yield new MariaDbPingStorageService(config, executorService, dataSource);
            default:
                throw new IllegalStateException("Invalid database configuration");
        };
    }

}
