package gov.nih.ncgc.bard.pcparser;

public interface Constants {
    public enum Unit {
	ppt (1), //	-  Parts per Thousand
	    ppm (2), //	-  Parts per Million
	    ppb (3), //	-  Parts per Billion
	    mm (4), //	-  milliM
	    um (5), //	-  microM
	    nm (6), //	-  nanoM
	    pm (7), //	-  picoM
	    fm (8), //	-  femtoM
	    mgml (9), //	-  milligrams per mL
	    ugml (10), //	-  micrograms per mL
	    ngml (11), //	-  nanograms per mL
	    pgml (12), //	-  picograms per mL
	    fgml (13), //	-  femtograms per mL
	    m (14), //	-  Molar
	    percent (15), //	-  Percent
	    ratio (16), //	-  Ratio
	    sec (17), //	-  Seconds
	    rsec (18), //	-  Reciprocal Seconds
	    min	(19), // -  Minutes
	    rmin (20), //	-  Reciprocal Minutes
	    day (21), //	-  Days
	    rday (22), //	-  Reciprocal Days
	    none (254), 
	    unspecified (255);

	private final int ord;

	Unit (int ord) { this.ord = ord; }
	public int ord () { return this.ord; }

	static public Unit getInstance (int ord) {
	    for (Unit u : values ()) {
		if (ord == u.ord()) {
		    return u;
		}
	    }
	    return unspecified;
	}
    }

    /*
      linear	-  Linear Scale (x)
      ln	-  Natural Log Scale (ln x)
      log	-  Log Base 10 Scale (log10 x)
      reciprocal	-  Reciprocal Scale (1/x)
      negative	-  Negative Linear Scale (-x)
      nlog	-  Negative Log Base 10 Scale (-log10 x)
      nln	-  Negative Natural Log Scane (-ln x)
    */
    public enum Transform {
	None, Linear, Ln, Log, Reciprocal, Negative, Nlog, Nln;
	public static Transform getInstance (int ord) {
	    for (Transform e : values ()) {
		if (e.ordinal() == ord) {
		    return e;
		}
	    }
	    return None;
	}
    }

    /*
      enhanced version of PC-ResultType_type 
    */
    public enum Type {
	Unknown, Float, Int, Bool, String, DoseResponse;

	public static Type getInstance (int ord) {
	    for (Type e : values ()) {
		if (e.ordinal() == ord) {
		    return e;
		}
	    }
	    return Unknown;
	}
    }

    public enum Outcome {
	Unknown, Inactive, Active, Inconclusive, Unspecified, Probe;

        public static Outcome instanceOf (int ord) {
	    for (Outcome o : values ()) {
		if (o.ordinal() == ord) {
		    return o;
		}
	    }
	    return Unknown;
	}
    }

    enum DataType {
	Unknown ("unknown", -1), 
	CRpt ("percent activity", 986), 
	Ac50 ("AC50", 959), 
	Hslope ("hill coeff", 919), 
	Hinf ("hill sinf", 921), 
	Hzero ("hill s0", 920), 
	R2 ("R Squared", 980), 
	CurveClass ("curve-fit specification", 590), 
	ExcludedPoints ("excluded points", 1348);
	
	private String dictText;
	private int dictElem;
	DataType (String dictText, int dictElem) {
	    this.dictText = dictText;
	    this.dictElem = dictElem;
	}
	String getText() {return dictText;}
	int getElem() {return dictElem;}
    }
   
}
