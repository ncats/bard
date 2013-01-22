package gov.nih.ncgc.bard.tools;

import chemaxon.struc.Molecule;
import gov.nih.ncgc.search.SearchCallback;
import gov.nih.ncgc.search.SearchParams;
import gov.nih.ncgc.search.SearchService2;
import static gov.nih.ncgc.search.SearchService2.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * A one line summary.
 *
 * @author Dac-Trung Nguyen
 */
public class SearchResultHandler implements SearchCallback<MolEntry> {

    static class MolEntryComparator implements Comparator<MolEntry> {
        MolEntryComparator () {
        }

        public int compare (MolEntry e1, MolEntry e2) {
            if (e1.getRank() < e2.getRank()) return -1;
            if (e1.getRank() > e2.getRank()) return 1;
            return e1.getPartitionSig() - e2.getPartitionSig();
        }
    }

    PrintWriter pw;
    SearchParams params;

    volatile int start, top, matches;
    volatile PriorityQueue<MolEntry> queue = new PriorityQueue<MolEntry>
        (100, new MolEntryComparator ());
    volatile Map<Integer, Integer> partitions = 
        new HashMap<Integer, Integer>();

    public SearchResultHandler(SearchParams params, PrintWriter pw,
                               Integer start, Integer top) {
        this.params = params;
        this.pw = pw;
        this.start = start != null ? start : 0;
        this.top = top != null ? top : 10000;
    }

    /**
     * SearchCallback interface. Note that this method is called from
     * multiple thread, so it should be thread safe!
     */
    public synchronized boolean matched (SearchService2.MolEntry entry) {
        if (start >= 0 && queue.size() >= start) {
            // remove these elements
            for (int i = 0; i < start; ++i) {
                queue.poll();
            }
            start = -1; // don't execute this block again
        }

        queue.add(entry);
        
        Integer c = partitions.get(entry.getPartitionSig());
        partitions.put(entry.getPartitionSig(), c!=null ? (c+1) : 1);
        if (partitions.size() >= entry.getPartitionCount()) {
            // now empty the queue
            for (SearchService2.MolEntry e; 
                 (e = queue.poll()) != null; ++matches) {
                writeOutput (getMol (e));
            }
            partitions.clear();
        }

        return matches < top;
    }

    public void complete () {
        for (SearchService2.MolEntry e; 
             matches < top && (e = queue.poll()) != null; ++matches) {
            writeOutput (getMol (e));
        }
    }

    protected Molecule getMol (SearchService2.MolEntry entry) {
        int[][] hits = entry.getAtomMappings();
        Molecule mol = entry.getMol();

        switch (params.getType()) {
            case Substructure: {
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        // can happen when the query contains explicit Hs
                        if (h[i] >= 0) { 
                            mol.getAtom(h[i]).setAtomMap(i + 1);
                        }
                    }
                }
                mol.setProperty("HIGHLIGHT", mol.toFormat("smiles:q"));
            }
            break;

            case Superstructure: {
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        if (h[i] >= 0)
                            mol.getAtom(i).setAtomMap(h[i] + 1);
                    }
                }
                mol.setProperty("HIGHLIGHT", mol.toFormat("smiles:q"));
            }
            break;
        }
        mol.setProperty("SIMILARITY",
                        String.format("%1$.3f",  entry.getSimilarity()));
        mol.setProperty("RANKING",
                        String.format("%1$.3f", entry.getRank()));

        return mol;
    }

    void writeOutput (Molecule mol) {
        //logger.info(Thread.currentThread()+": "+mol.getName());
        //pw.print(mol.toFormat("sdf"));
        String highlight = mol.getProperty("HIGHLIGHT");
        pw.println(mol.getName()+(highlight != null ? ("\t"+highlight) : ""));
    }
}
