package gov.nih.ncgc.bard.resourcemgr.precomp;

public class CompoundSimilarityWorker {  
   
    static double tanimoto (int[] fp1, int[] fp2) {
	int a = 0, b = 0, c = 0;
	for (int i = 0; i < fp1.length; ++i) {
	    c += Integer.bitCount(fp1[i] & fp2[i]);
	    a += Integer.bitCount(fp1[i]);
	    b += Integer.bitCount(fp2[i]);
	}
	return (double)c/(a+b-c);
    }
    
    
    static double tanimoto (long[] fp1, long[] fp2) {
	int a = 0, b = 0, c = 0;
	for (int i = 0; i < fp1.length; ++i) {
	    c += Long.bitCount(fp1[i] & fp2[i]);
	    a += Long.bitCount(fp1[i]);
	    b += Long.bitCount(fp2[i]);
	}
	return (double)c/(a+b-c);
    }
    
}
