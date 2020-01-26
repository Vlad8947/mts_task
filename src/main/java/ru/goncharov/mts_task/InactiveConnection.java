package ru.goncharov.mts_task;

import java.sql.Connection;

public class InactiveConnection {

    private Connection connection;
    private long lastActiveMilliSec = System.nanoTime();

    public InactiveConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public long getLastActiveMilliSec() {
        return lastActiveMilliSec;
    }
}
