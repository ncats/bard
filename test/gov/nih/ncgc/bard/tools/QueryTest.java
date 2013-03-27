package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.Compound;
import gov.nih.ncgc.bard.entity.ProteinTarget;
import gov.nih.ncgc.bard.entity.Publication;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class QueryTest extends DBTest {

    public QueryTest() throws ClassNotFoundException, SQLException {
        super();
    }

    @Test
    public void queryPublications() throws IOException, SQLException {
        String query = "caspase";
        List<Publication> pubs = db.searchForEntity(query, -1, -1, Publication.class);
        System.out.println("Querying publications for '" + query + "' gave " + pubs.size() + " results");
        Assert.assertTrue(pubs.get(0).getClass().equals(Publication.class));
        Assert.assertTrue(pubs.size() > 0);
    }

    @Test
    public void queryTargets() throws IOException, SQLException {
        String query = "caspase";
        List<ProteinTarget> targets = db.searchForEntity(query, -1, -1, ProteinTarget.class);
        System.out.println("Querying targets for '" + query + "' gave " + targets.size() + " results");
        Assert.assertTrue(targets.get(0).getClass().equals(ProteinTarget.class));
        Assert.assertTrue(targets.size() > 0);
    }

    @Test
    public void queryTargetsWithField1() throws IOException, SQLException {
        String query = "Q6GZX3[accession]";
        List<ProteinTarget> targets = db.searchForEntity(query, -1, -1, ProteinTarget.class);
        System.out.println("Querying targets for '" + query + "' gave " + targets.size() + " results");
        Assert.assertTrue(targets.get(0).getClass().equals(ProteinTarget.class));
        Assert.assertEquals(1, targets.size(), "Should have gotten a single result when filtering by accession");
    }

    @Test
    public void queryTargetsWithField2() throws IOException, SQLException {
        String query = "Q6GZ[accession]";
        List<ProteinTarget> targets = db.searchForEntity(query, -1, -1, ProteinTarget.class);
        System.out.println("Querying targets for '" + query + "' gave " + targets.size() + " results");
        Assert.assertTrue(targets.get(0).getClass().equals(ProteinTarget.class));
        Assert.assertTrue(targets.size() > 0);
    }

    @Test
    public void queryTargetsWithField3() throws IOException, SQLException {
        String query = "Napin[name]";
        List<ProteinTarget> targets = db.searchForEntity(query, -1, -1, ProteinTarget.class);
        System.out.println("Querying targets for '" + query + "' gave " + targets.size() + " results");
        Assert.assertTrue(targets.get(0).getClass().equals(ProteinTarget.class));
        Assert.assertTrue(targets.size() > 0);
    }

    @Test(expectedExceptions = SQLException.class)
    public void queryTargetsWithInvalidField() throws IOException, SQLException {
        String query = "Q6GZ[accessionFOO]";
        List<ProteinTarget> targets = db.searchForEntity(query, -1, -1, ProteinTarget.class);
        System.out.println("Querying targets for '" + query + "' gave " + targets.size() + " results");
    }

    @Test
    public void getCompoundsByName() throws SQLException {
        String query = "lidocaine";
        List<Compound> compounds = db.getCompoundsByName(query);
        System.out.println("Querying compounds for '" + query + "' gave " + compounds.size() + " compounds");
        Assert.assertTrue(compounds.size() > 0);
    }

    @Test
    public void getAssaysWithNoFilter() throws SQLException, IOException {
        String filter = null;
        List<Assay> assays = db.searchForEntity(filter, 0, 10, Assay.class);
        System.out.println("Querying assays for null gave " + assays.size() + " assays");
        Assert.assertEquals(assays.size(), 10);
    }

}
