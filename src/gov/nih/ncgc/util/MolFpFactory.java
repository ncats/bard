// $Id: MolFpFactory.java 3501 2009-10-29 16:01:09Z nguyenda $

package gov.nih.ncgc.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;

/**
 * Factory for generating molecular fingerprint
 */
public class MolFpFactory {
    final static Map<Integer, MolFpFactory> cache
        = new ConcurrentHashMap<Integer, MolFpFactory>();

    /**
     * default fingerprint configuration
     */
    static private final int FP_SIZE = 16;
    static private final int FP_BITS = 2;
    static private final int FP_DEPTH = 6;
    
    final private Cache<Molecule, int[]> fps = new Cache<Molecule, int[]>();
    private int size, bits, depth;

    protected MolFpFactory (int size, int bits, int depth) {
        this.size = size;
        this.bits = bits;
        this.depth = depth;
    }

    public int[] generate (Molecule mol) {
        int[] fp = fps.get(mol);
        if (fp == null) {
            synchronized (this) {
                MolHandler mh = new MolHandler (mol);
                fp = mh.generateFingerprintInInts(size, bits, depth);
                fps.put(mol, fp);
            }
        }
        return fp;
    }

    // similarity metric
    public double tanimotoSim (Molecule a, Molecule b) {
        return tanimotoSim (generate (a), generate (b));
    }
    public static double tanimotoSim (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }
        int c = 0, a = 0, b = 0;
        for (int i = 0; i < fpa.length; ++i) {
            c += ChemUtil.countBits(fpa[i] & fpb[i]);
            a += ChemUtil.countBits(fpa[i]);
            b += ChemUtil.countBits(fpb[i]);
        }
        return (double)c/(a + b - c);
    }

    public double tanimotoDist (Molecule a, Molecule b) {
        return tanimotoDist (generate (a), generate (b));
    }
    public static double tanimotoDist (int[] fpa, int[] fpb) {
        return 1. - tanimotoSim (fpa, fpb);
    }

    public double euclidean (Molecule a, Molecule b) {
        return euclidean (generate (a), generate (b));
    }
    public static double euclidean (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }
        int c = 0, a = 0, b = 0;
        for (int i = 0; i < fpa.length; ++i) {
            c += ChemUtil.countBits(fpa[i] & fpb[i]);
            a += ChemUtil.countBits(fpa[i]);
            b += ChemUtil.countBits(fpb[i]);
        }
        return Math.sqrt(a + b - 2.*c);
    }

    // similarity metric
    public double diceSim (Molecule a, Molecule b) {
        return diceSim (generate (a), generate (b));
    }
    public static double diceSim (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }
        int c = 0, a = 0, b = 0;
        for (int i = 0; i < fpa.length; ++i) {
            c += ChemUtil.countBits(fpa[i] & fpb[i]);
            a += ChemUtil.countBits(fpa[i]);
            b += ChemUtil.countBits(fpb[i]);
        }
        return (double)2.*c/(a + b);
    }

    // similarity metric
    public double cosineSim (Molecule a, Molecule b) {
        return cosineSim (generate (a), generate (b));
    }
    public static double cosineSim (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }
        int c = 0, a = 0, b = 0;
        for (int i = 0; i < fpa.length; ++i) {
            c += ChemUtil.countBits(fpa[i] & fpb[i]);
            a += ChemUtil.countBits(fpa[i]);
            b += ChemUtil.countBits(fpb[i]);
        }
        return (double)c/Math.sqrt(a * b);
    }

    // distance metric
    public double hammingDist (Molecule a, Molecule b) {
        return hammingDist (generate (a), generate (b));
    }
    public static double hammingDist (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }

        double dist = 0.;
        for (int i = 0; i < fpa.length; ++i) {
            dist += ChemUtil.countBits(fpa[i] ^ fpb[i]);
        }
        return dist;
    }

    // distance metric
    public double jaccardDist (Molecule a, Molecule b) {
        return jaccardDist (generate (a), generate (b));
    }
    public static double jaccardDist (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }
        int d = 0, s = 0;
        for (int i = 0; i < fpa.length; ++i) {
            d += ChemUtil.countBits(fpa[i] ^ fpb[i]);
            s += ChemUtil.countBits(fpa[i] & fpb[i]);
        }
        return (double)d / (s + d);
    }

    // distance metric
    public double rogersTanimotoDist (Molecule a, Molecule b) {
        return rogersTanimotoDist (generate (a), generate (b));
    }
    public static double rogersTanimotoDist (int[] fpa, int[] fpb) {
        if (fpa.length != fpb.length) {
            throw new IllegalArgumentException 
                ("Arrays are not of the same size");
        }
        int d = 0, s = 0, c = 0;
        for (int i = 0; i < fpa.length; ++i) {
            d += ChemUtil.countBits(fpa[i] ^ fpb[i]);
            s += ChemUtil.countBits(fpa[i] & fpb[i]);
            c += ChemUtil.countBits(~fpa[i] & ~fpb[i]);
        }
        return (double)2.*d/(s + c + 2.*d);
    }

    public int getNumInts () { return size; }
    public int getNumOnes () { return bits; }
    public int getNumEdges () { return depth; }

    public static MolFpFactory getInstance () {
        return getInstance (FP_SIZE, FP_BITS, FP_DEPTH);
    }

    public static MolFpFactory getInstance (int size, int bits, int depth) {
        int config = (size << 24) | (bits << 16) | depth;
        MolFpFactory factory = cache.get(config);
        if (factory == null) {
            synchronized (MolFpFactory.class) {
                factory = new MolFpFactory (size, bits, depth);
                cache.put(config, factory);
            }
        }
        return factory;
    }
}
