package de.berlios.vch.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;

public class ConnectionManager {
    
    private static transient Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    private static ConnectionManager instance;
    
    private BasicDataSource dataSource;

    
    private String driver;
    private String url;
    private String baseurl;
    private String user;
    private String pass;
    
    private ConnectionManager() {
        reloadSettings();
        
        // create the pooling datasource
        dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUsername(user);
        dataSource.setPassword(pass);
        dataSource.setUrl(url);

        dataSource.setInitialSize(10);
        dataSource.setMinIdle(10);
        dataSource.setMaxActive(50);
        dataSource.setMaxWait(120000);
    }
    
    public void reloadSettings() {
        Properties props = Config.getInstance();
        driver = props.getProperty("db.connection.driver");
        url = props.getProperty("db.connection.url");
        baseurl = Config.getInstance().getUnevaluatedProperty("db.connection.url").replaceAll("<db.name>",  "");
        baseurl = Config.getInstance().evaluatePlaceHolders(baseurl);
        user = props.getProperty("db.user");
        pass = props.getProperty("db.pass");
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }
    
    public synchronized Connection getConnectionWithoutDbname() throws SQLException {
        logger.debug("Returning special connection {}", baseurl);
        return DriverManager.getConnection(baseurl, user, pass);
    }
    
    public Connection getConnection() throws SQLException {
        Connection con = dataSource.getConnection();
//        logger.info("Active DB connections: {}", dataSource.getNumActive());
//        logger.info("Idle DB connections: {}", dataSource.getNumIdle());
        return con;
    }

    public String getScriptPath() {
        return "/sql/" + driver + "/";
    }
    
    private List<String> embeddedDrivers = Arrays.asList("org.hsqldb.jdbcDriver"); 
    public boolean isEmbedded() {
        String driver = Config.getInstance().getProperty("db.connection.driver");
        return embeddedDrivers.contains(driver);
    }
}