package gov.nih.ncgc.bard.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The superclass for all DB related tests.
 * <p/>
 * This class sets up and tests the connection to the db.
 *
 * @author Rajarshi Guha
 */
public abstract class DBTest {
    DBUtils db;

    protected DBTest() throws ClassNotFoundException, SQLException {
        db = new DBUtils();
        Class.forName("com.mysql.jdbc.Driver");

        String line = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("database.parameters"));
            line = reader.readLine();
        } catch (FileNotFoundException e) {
            Assert.fail("Could not read database credentials. DB tests will fail");
        } catch (IOException e) {
            Assert.fail("Could not read database credentials. DB tests will fail");
        }
        String[] toks = line.split(",");
        if (toks.length != 3) Assert.fail("Database credentials are in the wrong format");
        Connection connection = DriverManager.getConnection(toks[0], toks[1], toks[2]);
        if (connection != null) db.setConnection(connection);
    }

    @Test
    public void dbIsReady() {
        Assert.assertTrue(db.ready());
    }
}
