package dev.cerus.sourceping.storage.ping;

import dev.cerus.sourceping.model.Config;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import javax.sql.DataSource;

public class MariaDbPingStorageService extends SqlPingStorageService {

    private final DataSource dataSource;

    public MariaDbPingStorageService(final Config config, final ExecutorService executorService, final DataSource dataSource) {
        super(config, executorService);
        this.dataSource = dataSource;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    protected void disposeConnection(final Connection con) throws Exception {
        con.close();
    }

    @Override
    protected String getAutoIncrementString() {
        return "AUTO_INCREMENT";
    }

    @Override
    protected String getUpsertString(final String key) {
        return "ON DUPLICATE KEY UPDATE";
    }

}
