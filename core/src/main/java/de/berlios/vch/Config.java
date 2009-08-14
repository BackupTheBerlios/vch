package de.berlios.vch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ConfigDAO;
import de.berlios.vch.http.HandlerMapping;

public class Config extends Properties {
    
    private static transient Logger logger = LoggerFactory.getLogger(Config.class);
    
    private static Config instance;
    
    private static final String CONFIG_FILE = "vodcatcherhelper.properties";
    
    private HandlerMapping mapping = new HandlerMapping();
    
    private String baseUrl;

    private Config() {
        load();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String getProperty(String key) {
        String prop = super.getProperty(key);
        
        for (Iterator iterator = keySet().iterator(); iterator.hasNext();) {
            String _key = (String) iterator.next();
            if(_key.equals(key)) {
                continue;
            }
            
            String placeholder = "<" + _key + ">";
            if(prop.indexOf(placeholder) > 0) {
                String placeholderProp = getProperty(_key);
                prop = prop.replaceAll(placeholder, placeholderProp);
                logger.debug("Found " + placeholder + " in " + key 
                        + " and replaced it with " + placeholderProp);
            }
        }
        return prop;
    }
    
    public String evaluatePlaceHolders(String value) {
        for (Iterator<?> iterator = keySet().iterator(); iterator.hasNext();) {
            String _key = (String) iterator.next();
            
            String placeholder = "<" + _key + ">";
            if(value.indexOf(placeholder) > 0) {
                String placeholderProp = getProperty(_key);
                value = value.replaceAll(placeholder, placeholderProp);
            }
        }
        return value;
    }
    
    public String getUnevaluatedProperty(String key) {
        return super.getProperty(key);
    }
    
    private void load() {
        logger.debug("Loading configuration properties");
        loadDefaults();
        loadFileProperties();
    }

    @SuppressWarnings("unchecked")
    private void insertNewIntoDB() {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            
            ConfigDAO dao = new ConfigDAO(conn);
            for (Iterator iterator = keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                de.berlios.vch.model.Config config = new de.berlios.vch.model.Config();
                config.setParameterKey(key);
                config.setParameterValue(getUnevaluatedProperty(key));
                if(!dao.exists(config)) {
                    dao.save(config);
                }
            }
            
            // commit all changes
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            logger.error("Couldn't save new config parameter", e);
            if(conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    logger.error("Couldn't rollback transaction",e);
                }
            }
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    private void loadDBProperties() {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            
            // get all config params
            List<de.berlios.vch.model.Config> params = new ConfigDAO(conn).getAll();
            for (Iterator<de.berlios.vch.model.Config> iterator = params.iterator(); iterator.hasNext();) {
                de.berlios.vch.model.Config config = iterator.next();
                logger.debug("Loading property from DB: " + config.getParameterKey() + "=" + config.getParameterValue());
                setProperty(config.getParameterKey(), config.getParameterValue());
            }
            
        } catch (SQLException e) {
            logger.error("Couldn't load config from DB", e);
            if(conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    logger.error("Couldn't rollback transaction",e);
                }
            }
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    private void loadDefaults() {
        InputStream in = Config.class.getResourceAsStream("/vodcatcherhelper.properties");
        try {
            load(in);
        } catch (IOException e) {
            logger.error("Couldn't load default configuration", e);
            System.exit(1);
        } finally {
            if(in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
        
    }
    
    private void loadFileProperties() {
        File configFile = new File(CONFIG_FILE);
        if(configFile.exists()) {
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(CONFIG_FILE);
                load(fin);
            } catch (FileNotFoundException e) {
                logger.warn("Couldn't find configuration file " + CONFIG_FILE, e);
            } catch (IOException e) {
                logger.error("Couldn't load configuration file " + CONFIG_FILE, e);
            } finally {
                if(fin != null) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        logger.error("Couldn't close configuration file", e);
                    }
                }
            }
        }
    }
    
    public boolean getBoolValue(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }
    
    public int getIntValue(String key) {
        int value = 0;
        try {
            value = Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            logger.error("Tried to retrieve an int value, which appears not to be one", e);
            logger.warn("Returning 0 - This can lead to unexpected behaviour!");
        }
        return value;
    }

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
            
            // loading and updating of the DB properties has to be done here,
            // because the db access needs config properties, which have to be loaded
            // first. so we have to wait, until the constructor has finished
            instance.insertNewIntoDB();
            instance.loadDBProperties();
        }
        return instance;
    }

    public void reload() {
        load();
        insertNewIntoDB();
        loadDBProperties();
    }
    
    @SuppressWarnings("unchecked")
    public void save() throws SQLException {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            
            // get all config params
            for (Iterator iterator = keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                String value = getUnevaluatedProperty(key);
                
                de.berlios.vch.model.Config config = new de.berlios.vch.model.Config();
                config.setParameterKey(key);
                config.setParameterValue(value);
                new ConfigDAO(conn).saveOrUpdate(config);
            }
            
            // commit all changes
            conn.commit();
        } catch (SQLException e) {
            logger.error("Couldn't save config into DB", e);
            if(conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    logger.error("Couldn't rollback transaction",e);
                }
            }
            throw e;
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    public HandlerMapping getHandlerMapping() {
        return mapping;
    }

    /**
     * Returns the application context URL
     * @return
     */
    public String getBaseUrl() {
        String url = baseUrl;
        try {
            url = "http://" + InetAddress.getLocalHost().getHostAddress() + ":"
                            + Config.getInstance().getProperty("webserver.listenport");
        } catch (UnknownHostException e) {}
        return url;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    
    private Manifest manifest = null;
    public String getManifestProperty(String key) {
        if(manifest == null) {
            loadManifest();
        }

        return manifest.getMainAttributes().getValue(key);
    }
    
    private void loadManifest() {
        manifest = new Manifest();

        URL url = getClass().getResource(getClass().getSimpleName()+".class");
        String temp = "";
        try {
            temp = URLDecoder.decode(url.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UTF-8 not supported", e);
        }
        
        if(temp.indexOf("!") > 0) {
            try {
                URL mfURL = new URL(temp.substring(0, temp.indexOf("!")+1) + "/META-INF/MANIFEST.MF");
                manifest.read(mfURL.openStream());
            } catch (Exception e) {
                logger.error("Couldn't load properties from manifest", e);
            }
        }
    }
}