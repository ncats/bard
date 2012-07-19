package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.handler.DictionaryHandler;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class DictionaryHandlerTest {

    @Test
    public void testCAPDict() throws IOException {
        DictionaryHandler handler = new DictionaryHandler();
        handler.process("https://bard.broadinstitute.org/dataExport/api/dictionary", CAPConstants.CapResource.DICTIONARY);
        CAPDictionary dict = CAPConstants.getDictionary();
        Assert.assertNotNull(dict);

        CAPDictionaryElement elem = dict.getNode("assay format");
        Assert.assertNotNull(elem);
        Set<CAPDictionaryElement> children = dict.getChildren(elem);
        Assert.assertNotNull(children);
        Assert.assertEquals(children.size(), 5);
        Set<CAPDictionaryElement> parents = dict.getParents(elem);
        Assert.assertNotNull(parents);
        Assert.assertEquals(parents.size(), 0);
    }
}
