package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardEntity;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.ExperimentData;
import gov.nih.ncgc.bard.entity.Substance;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
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

    @DataProvider
    public Object[][] countProvider() {
        return new Object[][]{
                {"gov.nih.ncgc.bard.entity.Assay", 0},
                {"gov.nih.ncgc.bard.entity.Compound", 0},
                {"gov.nih.ncgc.bard.entity.Substance", 0},
                {"gov.nih.ncgc.bard.entity.Experiment", 0},
                {"gov.nih.ncgc.bard.entity.ExperimentData", 0},
                {"gov.nih.ncgc.bard.entity.Publication", 0},
                {"gov.nih.ncgc.bard.entity.Project", 0},
                {"gov.nih.ncgc.bard.entity.ProteinTarget", 0}
        };
    }

    @Test(dataProvider = "countProvider")
    public void getEntityCount(String className, int n) throws SQLException, ClassNotFoundException {
        Class<BardEntity> klass = (Class<BardEntity>) Class.forName(className);
        int count = db.getEntityCount(klass);
        Assert.assertTrue(count > n);
    }

    @Test
    public void getSinglePointExperimentData() throws IOException, SQLException {
        Long edid = 3891279L;
        ExperimentData ed = db.getExperimentDataByDataId(edid);
        Assert.assertNotNull(ed);
        Assert.assertEquals(ed.getResults().length, 21);
    }

    @Test
    public void getSubstance() throws SQLException {
        Long sid = 135010881L;
        Substance s = db.getSubstanceBySid(sid);
        Assert.assertNotNull(s);
        Assert.assertFalse(s.getSmiles().equals(""));
    }


    @DataProvider
    public Object[][] probeIdProvider() {
        return new Object[][]{
                {"ML099", new Long(888706)},
                {"ML043", new Long(5401876)},
                {"ML149", new Long(2331284)},
                {"ML017", new Long(4131581)}
        };
    }

    @Test(dataProvider = "probeIdProvider")
    public void getCompoundByProbeId(String probeId, Long cid) throws SQLException {
        Compound c = db.getCompoundByProbeId(probeId);
        Assert.assertNotNull(c);
        Assert.assertEquals(c.getCid(), cid);

    }
}
