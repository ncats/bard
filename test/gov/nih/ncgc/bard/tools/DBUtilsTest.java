package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.entity.Assay;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class DBUtilsTest extends DBTest {

    public DBUtilsTest() throws ClassNotFoundException, SQLException {
        super();
    }

    @DataProvider
    public Object[][] aidDataProvider() {
        return new Object[][]{
                {new Long(2048), new Long(2048)},
                {new Long(399), new Long(399)},
                {new Long(2492), new Long(2492)}
        };
    }

    @Test(dataProvider = "aidDataProvider")
    public void getAssayByAid(Long aid, Long aid2) throws SQLException {
        Assay assay = db.getAssayByAid(aid);
        Assert.assertNotNull(assay.getAid());
        Assert.assertEquals(assay.getAid(), aid2);
    }
}
