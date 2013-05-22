package gov.nih.ncgc.bard.resourcemgr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BardDBUtil {

	private static String dbURL = "jdbc:mysql://maxwell.ncats.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull";
	private static String driverName = "com.mysql.jdbc.Driver";
	private static String user = "bard_manager";
	private static String pw = "bard_manager";
	private static float dbMinUpdateThreshold = 0.98f;
	
	private static String diracDbURL = "jdbc:mysql://dirac.nhgri.nih.gov:3306/bard2?zeroDateTimeBehavior=convertToNull";	
	
	
	public BardDBUtil() {	}

	public BardDBUtil(String dbURL, String driverName, String user, String password) {	
		BardDBUtil.dbURL = dbURL;
		BardDBUtil.driverName = driverName;
		BardDBUtil.user = user;
		BardDBUtil.pw = password;
	}
	
	public static Connection connect() throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Class.forName(driverName);
		conn = DriverManager.getConnection(dbURL, user, pw);
		return conn;
	}

	public static Connection connectToDirac() throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Class.forName(driverName);
		conn = DriverManager.getConnection(diracDbURL, user, pw);
		return conn;
	}
	
	public static Connection connect(String dbURL) throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Class.forName(driverName);
		conn = DriverManager.getConnection(dbURL, user, pw);
		return conn;
	}

	public static Connection connect(String dbURL, String driverName) throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Class.forName(driverName);
		conn = DriverManager.getConnection(dbURL, user, pw);
		return conn;
	}
	
	public static Connection connect(String dbURL, String driverName, String user, String pw) throws ClassNotFoundException, SQLException {
		Connection conn = null;
		Class.forName(driverName);
		conn = DriverManager.getConnection(dbURL, user, pw);
		return conn;
	}
	
	public static long getTableRowCount(String tableName) throws ClassNotFoundException, SQLException {
		long rowCount = -1;		
		Connection conn = connect();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select count(*) from "+tableName);
		if(rs.next()) {
			rowCount = rs.getLong(1);
		}
		rs.close();
		conn.close();
		return rowCount;
	}

	public static long getTableRowCount(String tableName, String dbURL) throws ClassNotFoundException, SQLException {
	    long rowCount = -1;		
	    Connection conn = connect(dbURL);
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery("select count(*) from "+tableName);
	    if(rs.next()) {
		rowCount = rs.getLong(1);
	    }
	    rs.close();
	    conn.close();
	    return rowCount;
	}
	
	public static long getTableRowCount(String tableName, 
		String dbURL, String driverName, String user, String pw) throws ClassNotFoundException, SQLException {
		long rowCount = -1;		
		Connection conn = connect(dbURL,driverName, user, pw);
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select count(*) from "+tableName);
		if(rs.next()) {
			rowCount = rs.getLong(1);
		}
		rs.close();
		conn.close();
		return rowCount;
	}
	
	public static long getTableRowCount(String tableName, String column, boolean distinct) throws ClassNotFoundException, SQLException {
		long rowCount = -1;		
		Connection conn = connect(dbURL, driverName, user, pw);
		Statement stmt = conn.createStatement();
		String sql = "select count(";
		if(distinct) {
			sql += "distinct("+column+")) from "+tableName;
		} else {
			sql += column + ") from "+tableName;
		}
		ResultSet rs = stmt.executeQuery(sql);
		if(rs.next()) {
			rowCount = rs.getLong(1);
		}
		rs.close();
		conn.close();
		return rowCount;
	}
	
	public static long getTableRowCount(String tableName, String column, boolean distinct,
		String dbURL, String driverName, String user, String pw) throws ClassNotFoundException, SQLException {
		long rowCount = -1;		
		Connection conn = connect(dbURL, driverName, user, pw);
		Statement stmt = conn.createStatement();
		String sql = "select count(";
		if(distinct) {
			sql += "distinct("+column+")) from "+tableName;
		} else {
			sql += column + ") from "+tableName;
		}
		ResultSet rs = stmt.executeQuery(sql);
		if(rs.next()) {
			rowCount = rs.getLong(1);
		}
		rs.close();
		conn.close();
		return rowCount;
	}
	
	
	public static boolean swapTempTableToProductionIfPassesSizeDelta(String tempTableName, String prodTableName, double delta) throws ClassNotFoundException, SQLException {
		boolean swapped = false;
		Connection conn = connect();
		
		long tempTableSize = BardDBUtil.getTableRowCount(tempTableName);
		long prodTableSize = BardDBUtil.getTableRowCount(prodTableName);
		double sizeDelta = (double)tempTableSize/(double)prodTableSize;
		if(sizeDelta > delta) {
			Statement stmt = conn.createStatement();
			stmt.execute("alter table "+prodTableName+" rename "+prodTableName+"_swap");
			stmt.execute("alter table "+tempTableName+" rename "+prodTableName);
			stmt.execute("alter table "+prodTableName+"_swap rename "+tempTableName);
			swapped = true;
		}
		
		conn.close();
		return swapped;
	}
	
	public static boolean swapTempTableToProductionIfPassesSizeDelta(String tempTableName, String prodTableName, double delta,
		String dbURL, String driverName, String user, String pw) throws ClassNotFoundException, SQLException {
		boolean swapped = false;
		Connection conn = connect(dbURL, driverName, user, pw);
		
		long tempTableSize = BardDBUtil.getTableRowCount(tempTableName);
		long prodTableSize = BardDBUtil.getTableRowCount(prodTableName);
		double sizeDelta = (double)tempTableSize/(double)prodTableSize;
		if(sizeDelta > delta) {
			Statement stmt = conn.createStatement();
			stmt.execute("alter table "+prodTableName+" rename "+prodTableName+"_swap");
			stmt.execute("alter table "+tempTableName+" rename "+prodTableName);
			stmt.execute("alter table "+prodTableName+"_swap rename "+tempTableName);
			swapped = true;
		}
		
		conn.close();
		return swapped;
	}
	
	public static boolean swapTempTableToProductionIfPassesSizeDelta(String tempTableName, String prodTableName, double delta,
		String dbURL) throws ClassNotFoundException, SQLException {
		boolean swapped = false;
		Connection conn = connect(dbURL);
		
		long tempTableSize = BardDBUtil.getTableRowCount(tempTableName, dbURL);
		long prodTableSize = BardDBUtil.getTableRowCount(prodTableName, dbURL);
		double sizeDelta = (double)tempTableSize/(double)prodTableSize;
		if(sizeDelta > delta) {
			Statement stmt = conn.createStatement();
			stmt.execute("alter table "+prodTableName+" rename "+prodTableName+"_swap");
			stmt.execute("alter table "+tempTableName+" rename "+prodTableName);
			stmt.execute("alter table "+prodTableName+"_swap rename "+tempTableName);
			swapped = true;
		}
		
		conn.close();
		return swapped;
	}
	
	public static void cloneTableStructure(String sourceTable, String newTable) throws ClassNotFoundException, SQLException {
		Connection conn = connect();
		Statement stmt = conn.createStatement();
		String sql = "create table if not exists " + newTable + " like " + sourceTable;
		stmt.execute(sql);
		//truncate table data if it already existed
		stmt.execute("truncate table "+newTable);
		conn.close();
	}

	public static void cloneTableStructure(String sourceTable, String newTable, String dbURL) throws ClassNotFoundException, SQLException {
		Connection conn = connect(dbURL);
		Statement stmt = conn.createStatement();
		String sql = "create table if not exists " + newTable + " like " + sourceTable;
		stmt.execute(sql);
		//truncate table data if it already existed
		stmt.execute("truncate table "+newTable);
		conn.close();
	}
	
	public static void truncateTable(String table) throws ClassNotFoundException, SQLException {
		Connection conn = connect();
		Statement stmt = conn.createStatement();
		String sql = "truncate table " + table;
		stmt.execute(sql);
		conn.close();
	}
	
	public static void cloneTableStructure(String sourceTable, String newTable, 
		String dbURL, String driver, String user, String password) throws ClassNotFoundException, SQLException {
		Connection conn = connect(dbURL, driver, user, password);
		Statement stmt = conn.createStatement();
		String sql = "create table if not exists " + newTable + " like " + sourceTable;
		stmt.execute(sql);
		//truncate table data if it already existed
		stmt.execute("truncate table "+newTable);
		conn.close();
	}

	
}
