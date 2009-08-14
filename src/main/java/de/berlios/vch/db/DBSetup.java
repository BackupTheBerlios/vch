package de.berlios.vch.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.utils.PropertiesLoader;

public class DBSetup {
	
	private static transient Logger logger = LoggerFactory.getLogger(DBSetup.class);
	
	private Statement stat;
	
	private String SQL_CREATE_DATABASE;
	private String SQL_CHECK_DATABASE;
	
	public DBSetup() {
	    loadSqls();
	}
	
	private void loadSqls() {
	    String path = ConnectionManager.getInstance().getScriptPath();
	    path += this.getClass().getSimpleName() + ".sql.props";
        try {
            Properties sqls = PropertiesLoader.loadFromJar(path);
            SQL_CREATE_DATABASE = sqls.getProperty("SQL_CREATE_DATABASE");
            SQL_CHECK_DATABASE = sqls.getProperty("SQL_CHECK_DATABASE");
            String dbName = Config.getInstance().getProperty("db.name");
            SQL_CREATE_DATABASE = SQL_CREATE_DATABASE.replaceAll("\\{DB_NAME\\}", dbName);
            SQL_CHECK_DATABASE = SQL_CHECK_DATABASE.replaceAll("\\{DB_NAME\\}", dbName);
        } catch (IOException e) {
            logger.error("Couldn't load sql statements from " + path, e);
            System.exit(1);
        }
    }

    private void loadScript(String file) throws IOException, SQLException {
	    String driver = Config.getInstance().getProperty("db.connection.driver");
	    String scriptFile = "/sql/" + driver + "/" + file;
	    logger.debug("Trying to load script from " + scriptFile);
	    InputStream in = Config.class.getResourceAsStream(scriptFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		StringBuffer query = new StringBuffer();
		boolean queryEnds = false;

		while ((line = reader.readLine()) != null) {
			if (isComment(line))
				continue;
			queryEnds = checkStatementEnds(line);
			query.append(line);
			if (queryEnds) {
				logger.debug("query->" + query);
				stat.addBatch(query.toString());
				query.setLength(0);
			}
		}
	}
	
	private final static char QUERY_ENDS = ';';
		
	private static boolean isComment(String line) {
		if ((line != null) && (line.length() > 0))
			return (line.charAt(0) == '#');
		return false;
	}
	
	private void execute() throws IOException, SQLException {
		stat.executeBatch();
	}
	
	private boolean checkStatementEnds(String s) {
		return (s.indexOf(QUERY_ENDS) != -1);
	}
	
	public void createDB() {
        logger.debug("Create DB");
        Connection con = null;
        try {
            logger.debug("Executing sql {}", SQL_CREATE_DATABASE);
            con = ConnectionManager.getInstance().getConnectionWithoutDbname();
            QueryRunner run = new QueryRunner();
            run.update(con, SQL_CREATE_DATABASE);
        } catch (SQLException e) {
            logger.error("Couldn't create DB", e);
            System.exit(1);
        } finally {
            if(con != null) {
                try {
                    DbUtils.close(con);
                } catch (SQLException e) {
                    logger.error("Couldn't close db connection", e);
                }
            }
        }
    }

    public boolean checkDB() {
        if(ConnectionManager.getInstance().isEmbedded()) {
            return true;
        }
        
        Connection con = null;
        try {
            con = ConnectionManager.getInstance().getConnectionWithoutDbname();
            QueryRunner run = new QueryRunner();
            run.update(con, SQL_CHECK_DATABASE);
        } catch (SQLException e) {
            logger.debug("DB doesn't exist", e);
            return false;
        } finally {
            if(con != null) {
                try {
                    DbUtils.close(con);
                } catch (SQLException e) {
                    logger.error("Couldn't close db connection ", e);
                }
            }
        }
        
        return true;
    }
	
    public void createTables() {
		ConnectionManager ds = ConnectionManager.getInstance();
		Connection conn = null;
		try {

			conn = ds.getConnection();
			conn.setAutoCommit(false);
			
			stat = conn.createStatement();
			loadScript("schema_ddl.sql");
			execute();

			conn.commit();
			conn.setAutoCommit(true);
        	logger.info("DB Created. The previous errors (SQLExceptions) can be ignored");

		} catch (SQLException e) {
			logger.error("DB Initialize failed !", e);
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException e1) {
					logger.error("Couldn't rollback transaction", e);
				}
			}
		} catch (Exception e) {
			logger.error("DB Initialize failed !", e);
		}
		finally {
			try {
				DbUtils.close(conn);
			} catch (SQLException e) {
				logger.error("Couldn't close database connection", e);
			}
		}
	}
	
    public void runSetupIfNecessary() {
        // check if db exists
        if( !checkDB() ) {
            createDB();
        }
        
        // run updates
        runUpdates();
        
        // reload config
        Config.getInstance().reload();
    }
    
    private Queue<String> loadUpdateQueue() throws IOException {
        // load update order file
        String driver = Config.getInstance().getProperty("db.connection.driver");
        String orderFile = "/sql/" + driver + "/updates_order.txt";
        logger.debug("Trying to load update order from " + orderFile);
        InputStream in = Config.class.getResourceAsStream(orderFile);
        Queue<String> updateQueue = new LinkedList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while( (line=reader.readLine()) != null) {
            
            updateQueue.add(line);
        }
        
        return updateQueue;
    }
    
    /**
     * Runs DB updates.
     * The update sql scripts have to be in the updates directory in the
     * jdbc driver directory. The order of the updates is defined in the
     * updates_order.txt. In the configuration there is a param named db.version.
     * Updates, which appear after this param in the updates_order.txt will be
     * executed.
     */
    private void runUpdates() {
        String currentVersion = Config.getInstance().getProperty("db.version");
        currentVersion = currentVersion == null ? "initial" : currentVersion;
        logger.info("Current DB version is {}", currentVersion);
        
        // load update list
        Queue<String> updateFiles = null;
        try {
            updateFiles = loadUpdateQueue();
        } catch (Exception e) {
            logger.error("Couldn't load db update list", e);
            System.exit(1);
        }
        
        // skip unnecessary updates
        while(!updateFiles.peek().equals(currentVersion)) {
            updateFiles.poll();
        }
        updateFiles.poll();
        
        // execute update scripts
        Connection conn = null;
        String updateFile = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            for (String file : updateFiles) {
                logger.info("Executing DB update {}", file);
                updateFile = "updates/" + file + ".sql";
                stat = conn.createStatement();
                loadScript(updateFile);
                execute();
                
                // close statement
                stat.close();
                
                // store the new version in config
                Config.getInstance().setProperty("db.version", file);
                Config.getInstance().save();
            }
        } catch (Exception e) {
            logger.error("Couldn't execute database update " + updateFile, e);
            System.exit(1);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
    }
}
