package ru.goncharov.mts_task;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class ConnectionPoolImpl implements ConnectionPool {

    // Collection contains opened sql-connections
    private ArrayBlockingQueue<InactiveConnection> connections;
    // Сonnections that are used from pool
    private CopyOnWriteArrayList<Connection> activeConnections = new CopyOnWriteArrayList<>();

    // SQL driver
    private String driver;
    private String url;
    private String user;
    private String password;

    // Number of open connections
    private int connectionAmount;
    // Number of inactive connections that don't need to remove
    private int inactiveConnAmount = 5;
    // Time in milliseconds to wait for a free connection
    private long waitConnMilliSec = 5000;   // Default 5 sec
    // time in milliseconds after which to remove excess inactive connections
    private long closeInactiveMilliSec = 10000;    // Default 10 sec

    public ConnectionPoolImpl(String driver, String url, String user,
                              String password, int connectionAmount) throws SQLException, InterruptedException, ClassNotFoundException {
        checkDriver(driver);

        this.driver = driver;
        this.url = url;
        this.user = user;
        this.password = password;
        this.connectionAmount = connectionAmount;

        connectionsInit();
        clearTimerInit();
    }

    // Check sql-driver on existence
    private void checkDriver(String driver) throws ClassNotFoundException {
        Class.forName(driver);
    }

    // Open sql-connections and add in pool
    private void connectionsInit() throws InterruptedException, SQLException {
        connections = new ArrayBlockingQueue<>(connectionAmount);
        for (int i = 0; i < connectionAmount; i++) {
            // Create sql-connection
            Connection connection = DriverManager.getConnection(url, user, password);
            // Add connection in pool
            connections.add(new InactiveConnection(connection));
        }
    }

    /**
     * Run Timer to close inactive connection and remove it from collection
     */
    private void clearTimerInit() {
        Timer clearTimer = new Timer(true);
        TimerTask clearTask = new TimerTask() {
            @Override
            public void run() {
                // Clear the pool of inactive connections
                inactiveConnClose();
            }
        };
        // Start the cleaner after each period
        clearTimer.schedule(clearTask, closeInactiveMilliSec, closeInactiveMilliSec);
    }

    /**
     * Get opened sql-connection from pool if it exist
     * @return null if pool is empty
     */
    public Connection getConnection() throws InterruptedException {
        // Poll connection from queue if it exist
        Connection connection = connections.poll(waitConnMilliSec, TimeUnit.MILLISECONDS)
                .getConnection();
        // If connection not exist, return null
        if (connection == null) {
            return null;
        }
        // Add connection in the used collection
        activeConnections.add(connection);
        return connection;
    }

    /**
     * Return connection into pool if it was there.
     * @return null if connection wasn't in pool.
     */
    @Override
    public boolean returnConn(Connection connection) {
        // If connection isn't exist in the collection of used connection,
        //      return false.
        if (!activeConnections.contains(connection)) {
            return false;
        }
        // Remove connection from the collection of used connection
        activeConnections.remove(connection);
        // Add connection into pool
        connections.add(new InactiveConnection(connection));
        return true;
    }

    /*
    * Close inactive connections and remove from pool
    */
    private void inactiveConnClose() {
        // Get current time int milliseconds
        long nanoTime = System.currentTimeMillis();
        // Counter of inactive connections
        int inactiveCounter = 0;
        // Walk through inactive pool connections
        for (InactiveConnection inactiveConn : connections) {
            inactiveCounter++;
            // If inactive counter is bigger then amount of allowed inactive connections and
            //      if live time for inactive connection was passed
            if (inactiveCounter > inactiveConnAmount
                    && nanoTime - inactiveConn.getLastActiveMilliSec() > closeInactiveMilliSec) {
                try {
                    // Close connection
                    inactiveConn.getConnection().close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                // Remove connection from pool
                connections.remove(inactiveConn);
            }
        }
    }

//    Getters and Setters

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConnectionAmount() {
        return connectionAmount;
    }

    public long getWaitConnMilliSec() {
        return waitConnMilliSec;
    }

    public void setWaitConnMilliSec(int waitConnMilliSec) {
        this.waitConnMilliSec = waitConnMilliSec;
    }

    public int getInactiveConnAmount() {
        return inactiveConnAmount;
    }

    public void setInactiveConnAmount(int inactiveConnAmount) {
        this.inactiveConnAmount = inactiveConnAmount;
    }

    public long getCloseInactiveMilliSec() {
        return closeInactiveMilliSec;
    }

    public void setCloseInactiveMilliSec(int closeInactiveMilliSec) {
        this.closeInactiveMilliSec = closeInactiveMilliSec;
    }
}
