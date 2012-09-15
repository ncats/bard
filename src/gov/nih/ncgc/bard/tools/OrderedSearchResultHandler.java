package gov.nih.ncgc.bard.tools;

import chemaxon.struc.Molecule;
import gov.nih.ncgc.search.SearchCallback;
import gov.nih.ncgc.search.SearchParams;
import gov.nih.ncgc.search.SearchService2;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

public class OrderedSearchResultHandler implements SearchCallback<SearchService2.MolEntry> {
    List<Long> cids;

    PrintWriter pw;
    SearchParams params;
    public boolean debug = false;


    //Stores a canonical ordered list of MolEntries for buffer
    PriorityBlockingQueue<SearchService2.MolEntry> MolBuffer = new PriorityBlockingQueue<SearchService2.MolEntry>();
    volatile double[] minRank = null;    //minimum returned rank per partition
    //(each partition returns in descending order)
    volatile Boolean[] wLock = new Boolean[]{true};
    volatile Boolean[] tLock = new Boolean[]{true};

    double lastRank = Double.POSITIVE_INFINITY;
    Integer returnCap = Integer.MAX_VALUE;
    Integer returnStart = 0;
    int returned = 0;

    boolean theEnd = false;
    boolean finished = false;

    //Consumer thread
    private Thread t;

    public OrderedSearchResultHandler(SearchParams params, PrintWriter pw, Integer start, Integer resultCap) {
        cids = new ArrayList<Long>();

        this.params = params;
        this.pw = pw;
        if (start != null) returnStart = start;
        if (resultCap != null) this.returnCap = resultCap;
        Consumer consumer = new Consumer();
        t = new Thread(consumer);
        t.start();
    }

    private void initialize(int l) {
        minRank = new double[l];
        for (int i = 0; i < l; i++) {
            minRank[i] = Double.POSITIVE_INFINITY;
        }
    }

    /**
     * SearchCallback interface. Note that this method is called from
     * multiple thread, so it should be thread safe!
     */
    public boolean matched(SearchService2.MolEntry entry) {
        //logger.info("thread:" + entry.getPartitionSig() + " of " + entry.getPartitionCount());

        if (theEnd) return false;
        MolBuffer.add(entry);
        //if(true)return true;

        if (minRank == null) {
            synchronized (wLock) {
                if (minRank == null) {
                    initialize(entry.getPartitionCount());
                }
            }
        }
        minRank[entry.getPartitionSig()] = entry.getRank();
        return true;
    }

    public boolean consumeMol(SearchService2.MolEntry entry) {
        int[][] hits = entry.getAtomMappings();
        Molecule mol = entry.getMol();

        switch (params.getType()) {
            case Substructure: {
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        if (h[i] >= 0) {
                            mol.getAtom(h[i]).setAtomMap(i + 1);
                        }
                    }
                }
            }
            break;

            case Superstructure: {
                for (int[] h : hits) {
                    for (int i = 0; i < h.length; ++i) {
                        if (h[i] >= 0) {
                            mol.getAtom(i).setAtomMap(h[i] + 1);
                        }
                    }
                }
            }
            break;
        }
        mol.setProperty("SIMILARITY",
                String.format("%1$.3f", entry.getSimilarity()));
        mol.setProperty("RANKING",
                String.format("%1$.3f", entry.getRank()));
        writeOutput(mol);
        return true;
    }

    double calculateSafeRank() {
        if (finished) return Double.NEGATIVE_INFINITY;
        if (minRank == null) return Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < minRank.length; i++) {
            if (minRank[i] > max) max = minRank[i];
        }

        return max;
    }

    synchronized void writeOutput(Molecule mol) {
        pw.println(mol.getName());
        cids.add(Long.valueOf(mol.getName()));
    }

    // TODO this throws a concurrent modification exception (sometimes)
    synchronized public List<Long> getCids() {
        return cids;
    }

    public void complete() {
        if (minRank != null) {
            for (int i = 0; i < minRank.length; i++) {
                minRank[i] = Double.NEGATIVE_INFINITY;
            }
        }
        finished = true;
        try {
            t.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    class Consumer implements Runnable {
        public void consume() {
            while (!theEnd() && !MolBuffer.isEmpty()) {
                try {

                    SearchService2.MolEntry m = MolBuffer.take();
                    if (m.getRank() > lastRank) {
                        if (returned >= returnStart) consumeMol(m);
                        returned++;
                    } else {
                        MolBuffer.add(m);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        private boolean moreToConsume() {
            double safeRank = calculateSafeRank();
            if (safeRank < lastRank) {
                lastRank = safeRank;
                return true;
            }
            return false;
        }

        private boolean theEnd() {
            theEnd = ((returned - returnStart) >= returnCap);
            return theEnd || (MolBuffer.isEmpty() && finished);
        }

        public void run() {
            while (!theEnd()) {
                synchronized (tLock) {
                    while (!moreToConsume()) {
                        try {
                            tLock.wait(50);
                        } catch (InterruptedException e) {
                            Thread.interrupted();
                        }
                    }
                    consume();
                }
            }
        }
    }

}