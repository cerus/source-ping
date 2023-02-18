package dev.cerus.sourceping.storage.ping;

import dev.cerus.sourceping.model.Config;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;

public class SqlitePingStorageService extends SqlPingStorageService {

    private final Connection connection;

    public SqlitePingStorageService(final Config config, final ExecutorService executorService, final Connection connection) {
        super(config, executorService);
        this.connection = connection;
    }

    @Override
    protected Connection getConnection() {
        return this.connection;
    }

    @Override
    protected void disposeConnection(final Connection con) throws Exception {
        // no op
    }

    @Override
    protected String getAutoIncrementString() {
        return "AUTOINCREMENT";
    }

    @Override
    protected String getUpsertString(final String key) {
        return "ON CONFLICT (%s) DO UPDATE SET".formatted(key);
    }

}
