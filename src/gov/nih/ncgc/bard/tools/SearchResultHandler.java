package gov.nih.ncgc.bard.tools;

import chemaxon.struc.Molecule;
import gov.nih.ncgc.search.SearchCallback;
import gov.nih.ncgc.search.SearchParams;
import gov.nih.ncgc.search.SearchService2;

import java.io.PrintWriter;

/**
 * A one line summary.
 *
 * @author Dac-Trung Nguyen
 */
public class SearchResultHandler implements SearchCallback<SearchService2.MolEntry> {
    PrintWriter pw;
    SearchParams params;

    public SearchResultHandler(SearchParams params, PrintWriter pw) {
        this.params = params;
        this.pw = pw;
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
                StringBuilder sb = new StringBuilder ();
                
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        // can happen when the query contains explicit Hs
                        if (h[i] < 0) { 
                        }
                        else {
                            mol.getAtom(h[i]).setAtomMap(i + 1);
                            if (sb.length() > 0) sb.append(",");
                            sb.append(h[i]+1);
                        }
                    }
                }
                mol.setProperty("AMAP", sb.toString());                    
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
        //logger.info(Thread.currentThread()+": "+mol.getName());
        pw.print(mol.toFormat("sdf"));
    }
}
