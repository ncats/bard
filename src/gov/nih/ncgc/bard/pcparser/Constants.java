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
	static Unit getUnit(String text) {
	    for (Unit unit: Unit.values())
		if (unit.toString().equals(text)) return unit;
	    return null;
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
	ExcludedPoints ("excluded points", 1348),
	 elem1318 ("Cell-line name", 1318),
	 elem979 ("chi squared", 979),
	 elem1329 ("comment", 1329),
	 elem1334 ("Compound QC", 1334),
	 elem981 ("confidence limit - 95%", 981),
	 elem1337 ("control mean", 1337),
	 elem978 ("Count", 978),
	 elem602 ("curve-fit model", 602),
	 elem590 ("curve-fit specification", 590),
	 elem1325 ("disaggregation rating", 1325),
	 elem961 ("EC50", 961),
	 elem1342 ("EC50 Standard Error", 1342),
	 elem983 ("Efficacy", 983),
	 elem1348 ("excluded points", 1348),
	 elem1023 ("FOC", 1023),
	 elem1020 ("fold change", 1020),
	 elem1195 ("Half life", 1195),
	 elem919 ("hill coeff", 919),
	 elem922 ("hill ds", 922),
	 elem920 ("hill s0", 920),
	 elem921 ("hill sinf", 921),
	 elem963 ("IC50", 963),
	 elem1345 ("IC50 Standard Error", 1345),
	 elem955 ("IC90", 955),
	 elem902 ("Kd", 902),
	 elem903 ("ki", 903),
	 elem960 ("log AC50", 960),
	 elem953 ("log CC50", 953),
	 elem962 ("log EC50", 962),
	 elem968 ("log IC50", 968),
	 elem1025 ("luminescence", 1025),
	 elem1333 ("Max_Response", 1333),
	 elem984 ("maximal inhibition", 984),
	 elem957 ("MIC", 957),
	 elem1338 ("number of control wells", 1338),
	 elem1307 ("number of exclusions", 1307),
	 elem1397 ("number of points", 1397),
	 elem1358 ("number of replicates", 1358),
	 elem1327 ("optical density", 1327),
	 elem1378 ("pAC50", 1378),
	 elem996 ("percent activation", 996),
	 elem986 ("percent activity", 986),
	 elem1359 ("percent area", 1359),
	 elem1322 ("percent cellular ATP content", 1322),
	 elem998 ("percent inhibition", 998),
	 elem1012 ("percent of control", 1012),
	 elem982 ("percent response", 982),
	 elem1324 ("percent side scatter shift", 1324),
	 elem992 ("percent viability", 992),
	 elem1326 ("presence", 1326),
	 elem898 ("PubChem activity score", 898),
	 elem896 ("pubchem outcome", 896),
	 elem980 ("R Squared", 980),
	 elem1321 ("saturation transfer difference", 1321),
	 elem1344 ("SI (bkgd / vps1&#916;)", 1344),
	 elem1343 ("SI mod", 1343),
	 elem625 ("signal to noise ratio", 625),
	 elem925 ("slope", 925),
	 elem937 ("solubility", 937),
	 elem613 ("standard deviation", 613),
	 elem1335 ("standard error", 1335),
	 elem1336 ("Validation Flag", 1336),
	 elem1346 ("Visual Selection Flag", 1346),
	 elem1341 ("activity_qualifier", 1341),
	 elem627 ("z-factor", 627),
	 elem628 ("z-prime factor", 628);
	
	private String dictText;
	private int dictElem;
	DataType (String dictText, int dictElem) {
	    this.dictText = dictText;
	    this.dictElem = dictElem;
	}
	String getText() {return dictText;}
	int getElem() {return dictElem;}
	static DataType getDataType(String text) {
	    for (DataType dataType: DataType.values())
		if (dataType.getText().equals(text)) return dataType;
	    return null;
	}
	static DataType getDataType(int elem) {
	    for (DataType dataType: DataType.values())
		if (dataType.getElem() == elem && dataType.getText().startsWith("elem")) return dataType;
	    return null;
	}
	public String toString() {
	    return this.dictText+":"+this.dictElem;
	}
    }
   
}
