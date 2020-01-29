package ru.goncharov.mts_task;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool {

    // Get opened connection
    Connection getConnection() throws InterruptedException, SQLException;

    // Return the connection into pool if it was there
    boolean returnConn(Connection connection) throws SQLException;

}
