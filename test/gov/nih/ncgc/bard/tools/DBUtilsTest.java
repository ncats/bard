package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.capextract.CAPAssayAnnotation;
import gov.nih.ncgc.bard.capextract.CAPDictionary;
import gov.nih.ncgc.bard.capextract.CAPDictionaryElement;
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
import java.util.List;
import java.util.Set;

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
    public void getSinglePointExperimentData2() throws IOException, SQLException {
        Long edid = 639196L;
        ExperimentData ed = db.getExperimentDataByDataId(edid);
        Assert.assertNotNull(ed);
        Assert.assertNull(ed.getPotency());
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
        List<Compound> c = db.getCompoundsByProbeId(probeId);
        Assert.assertNotNull(c);
        Assert.assertEquals(c.size(), 1);
    }

    @Test
    public void getMultipleCompoundsByCid() throws SQLException {
        Long[] ids = new Long[]{419533L, 667555L};
        List<Compound> c = db.getCompoundsByCid(ids);
        Assert.assertNotNull(c);
        Assert.assertEquals(c.size(), 2);
        Assert.assertEquals((Object) c.get(0).getCid(), 419533L);
        Assert.assertEquals((Object) c.get(1).getCid(), 667555L);
    }

    @Test
    public void getMultipleCompoundsBySid() throws SQLException {
        Long[] ids = new Long[]{4237471L, 4237472L};
        List<Compound> c = db.getCompoundsBySid(ids);
        Assert.assertNotNull(c);
        Assert.assertEquals(c.size(), 2);
        Assert.assertEquals((Object) c.get(0).getCid(), 3232584L);
        Assert.assertEquals((Object) c.get(1).getCid(), 3232585L);
    }

    @Test
    public void getExplainInfo() throws SQLException {
        int n1 = db.getEstimatedRowCount("explain select * from compound");
        int n2 = db.getEstimatedRowCount("explain select * from compound where cid < 100");
        Assert.assertTrue(n1 > 0);
        Assert.assertTrue(n2 > 0);
    }

    @Test
    public void getCAPDict() throws SQLException, ClassNotFoundException, IOException {
        CAPDictionary d = db.getCAPDictionary();
        Assert.assertNotNull(d);
        Assert.assertEquals(678, d.getNodes().size());

        Set<CAPDictionaryElement> nodes = d.getNodes();
        for (CAPDictionaryElement node : nodes) Assert.assertNotNull(node.getLabel());
    }

    @Test
    public void getCAPAnnots() throws SQLException {
        List<CAPAssayAnnotation> as = db.getAssayAnnotations(75L);
        Assert.assertNotNull(as);
        Assert.assertEquals(8, as.size());
    }
}
