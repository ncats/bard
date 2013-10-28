package gov.nih.ncgc.bard.resourcemgr;

import gov.nih.ncgc.bard.capextract.CAPUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


public class BardDBUpdateLogger {

    public static long logStart(String eventType) {
	long logId = -1;
	try {

	    Timestamp now = new Timestamp(System.currentTimeMillis());			
	    Connection conn = CAPUtil.connectToBARD();

	    PreparedStatement ps = conn.prepareStatement("insert into update_history_log (start_time, event) values (?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
	    ps.setTimestamp(1, now);
	    ps.setString(2, eventType);			
	    boolean inserted = ps.execute();

	    ResultSet rs = ps.getGeneratedKeys();

	    if(rs.next()) {
		System.out.println("have a last insert result");
		logId = rs.getInt(1);
	    }
	    conn.close();

	} catch (SQLException e) {
	    e.printStackTrace();
	    return -1;
	}
	return logId;
    }

    public static long logEnd(long logId, int status, String eventComment) {
	try {
	    Timestamp now = new Timestamp(System.currentTimeMillis());			
	    Connection conn = CAPUtil.connectToBARD();
	    PreparedStatement ps = conn.prepareStatement("update update_history_log set end_time = ?, status = ?, event_comment=? where log_id=?");
	    ps.setTimestamp(1, now);
	    ps.setInt(2, status);			
	    ps.setString(3, eventComment);			
	    ps.setLong(4, logId);
	    int inserted = ps.executeUpdate();

	    if(inserted==0)
		return -1;

	    conn.close();

	} catch (SQLException e) {
	    e.printStackTrace();
	    return -1;
	}
	return logId;
    }

    public static long logStart(String eventType, String dbURL) {
	long logId = -1;
	try {

	    Timestamp now = new Timestamp(System.currentTimeMillis());			
	    Connection conn = CAPUtil.connectToBARD(dbURL);

	    PreparedStatement ps = conn.prepareStatement("insert into update_history_log (start_time, event) values (?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
	    ps.setTimestamp(1, now);
	    ps.setString(2, eventType);			
	    boolean inserted = ps.execute();

	    ResultSet rs = ps.getGeneratedKeys();

	    if(rs.next()) {
		System.out.println("have a last insert result");
		logId = rs.getInt(1);
	    }
	    conn.commit();
	    conn.close();

	} catch (SQLException e) {
	    e.printStackTrace();
	    return -1;
	}
	return logId;
    }

    public static long logEnd(long logId, int status, String eventComment, String dbURL) {
	try {
	    Timestamp now = new Timestamp(System.currentTimeMillis());			
	    Connection conn = CAPUtil.connectToBARD(dbURL);
	    PreparedStatement ps = conn.prepareStatement("update update_history_log set end_time = ?, status = ?, event_comment=? where log_id=?");
	    ps.setTimestamp(1, now);
	    ps.setInt(2, status);			
	    ps.setString(3, eventComment);			
	    ps.setLong(4, logId);
	    int inserted = ps.executeUpdate();

	    if(inserted == 0)
		return -1;
	    conn.commit();
	    conn.close();

	} catch (SQLException e) {
	    e.printStackTrace();
	    return -1;
	}

	return logId;
    }		

    public static void main(String [] args) {
	long dbLogId = BardDBUpdateLogger.logStart("TEST_COMPOUND_UPDATE_START_LOG");
	System.out.println("dbLogId="+dbLogId);

	try {
	    Thread.sleep(10000);
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	BardDBUpdateLogger.logEnd(dbLogId, 1, "load logger is working!");
    }
}
