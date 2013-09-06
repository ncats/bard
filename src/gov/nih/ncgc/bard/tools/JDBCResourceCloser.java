package gov.nih.ncgc.bard.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCResourceCloser {

    private static Logger logger = LoggerFactory.getLogger(JDBCResourceCloser.class);
    
    public static void close(ResultSet... resultSet) {
	if(resultSet != null) {
	    try {
		for(ResultSet rs : resultSet) {
		    rs.close();
		}
	    } catch (SQLException sqle) {
		logger.equals(sqle.getMessage());
		sqle.printStackTrace();
	    }
	}
    }

    public static void close(Statement... statement) {
	if(statement != null) {
	    try {
		for(Statement stmt : statement) {
		    stmt.close();
		}
	    } catch (SQLException sqle) {
		logger.equals(sqle.getMessage());
		sqle.printStackTrace();
	    }
	}
    }
    
    public static void close(Connection... connection) {
	if(connection != null) {
	    try {
		for(Connection conn : connection) {
		    conn.close();
		}
	    } catch (SQLException sqle) {
		logger.equals(sqle.getMessage());
		sqle.printStackTrace();
	    }
	}
    }
    
    //combo methods for resources
    public static void close (Statement statement, Connection connection) {
	try {
	    statement.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
	
	try {
	    connection.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
    }
    
    public static void close (ResultSet resultSet, Statement statement) {
	try {
	    resultSet.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
	
	try {
	    statement.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
    }
    
    public static void close (ResultSet resultSet, Statement statement, Connection connection) {
	try {
	    resultSet.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
	
	try {
	    statement.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
	
	try {
	    connection.close();
	} catch (SQLException sqle) {
	    logger.equals(sqle.getMessage());
	    sqle.printStackTrace();
	}
    }
 
}

