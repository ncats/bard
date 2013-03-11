package gov.nih.ncgc.bard.tools;

import gov.nih.ncgc.search.SearchCallback;
import gov.nih.ncgc.search.SearchParams;
import gov.nih.ncgc.search.SearchService2;

import java.util.ArrayList;
import java.util.List;

import chemaxon.struc.Molecule;

/**
 * A one line summary.
 *
 * @author Rajarshi Guha
 */
public class CidSearchResultHandler implements SearchCallback<SearchService2.MolEntry> {
    List<Long> cids;
    SearchParams params;

    public CidSearchResultHandler(SearchParams params) {
        this.params = params;
        cids = new ArrayList<Long>();
    }

    /**
     * SearchCallback interface. Note that this method is called from
     * multiple thread, so it should be thread safe!
     */
    public boolean matched(SearchService2.MolEntry entry) {
        int[][] hits = entry.getAtomMappings();
        Molecule mol = entry.getMol();

        switch (params.getType()) {
            case Substructure: {
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        mol.getAtom(h[i]).setAtomMap(i + 1);
                    }
                }
            }
            break;

            case Superstructure: {
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        mol.getAtom(i).setAtomMap(h[i] + 1);
                    }
                }
            }
            break;
        }
        mol.setProperty("SIMILARITY",
                String.format("%1$.3f",
                        entry.getSimilarity()));
        writeOutput(mol);

        return true;
    }

    synchronized void writeOutput(Molecule mol) {
        cids.add(Long.valueOf(mol.getName()));
    }

    public List<Long> getCids() {
        return cids;
    }
}
