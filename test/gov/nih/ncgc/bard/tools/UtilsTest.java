package gov.nih.ncgc.bard.tools;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class UtilsTest {
    @Test
    public void testChunk() {
        Integer[] array = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        List<List<Integer>> chunks = Util.chunk(array, 2);
        Assert.assertEquals(5, chunks.size());
        Assert.assertEquals(2, chunks.get(0).size());
        Assert.assertEquals(2, chunks.get(1).size());

        chunks = Util.chunk(array, 3);
        Assert.assertEquals(4, chunks.size());
        Assert.assertEquals(1, chunks.get(3).size());
    }
}
