package gov.nih.ncgc.bard.pcparser;
// $Id: PubChemAssaySource.java 3488 2009-10-29 15:49:42Z nguyenda $

import gov.nih.ncgc.bard.capextract.CAPConstants;
import gov.nih.ncgc.bard.capextract.CAPExtractor;
import gov.nih.ncgc.bard.capextract.CAPUtil;
import gov.nih.ncgc.bard.capextract.CapResourceHandlerRegistry;
import gov.nih.ncgc.bard.capextract.jaxb.Dictionary;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class PubChemAssayParser implements Constants {
    private static final Logger logger = Logger.getLogger
	(PubChemAssayParser.class.getName());

    static final int PUBCHEM_SID = 0;
    static final int PUBCHEM_EXT_REGID = 1;
    static final int PUBCHEM_CID = 2;
    static final int PUBCHEM_OUTCOME = 3;
    static final int PUBCHEM_RANK = 4;
    static final int PUBCHEM_URL = 5;
    static final int PUBCHEM_COMMENT = 6;
    static final int PUBCHEM_REVOKE = 7;
    static final int PUBCHEM_NUMCOLUMNS = 8;

    protected static final Pattern DOSE_RESPONSE_REGEX = 
	//Pattern.compile("(.+)([\\s_]+at|[\\s_]+@)?[\\s_]+((\\d*\\.\\d*)|(\\d+))[\\s_]*(uMol|MICROM|NANOM|microM|nanoM|nM|uM)(.+)?");
	Pattern.compile("((.+)([\\s_]+at|[\\s_]+@)?[\\s_]+)?((\\d*\\.\\d*)|(\\d+))[\\s_]*(uMol|MICROM|NANOM|microM|nanoM|nM|uM)(.+)?");


    // If the input expression is a parsable dose-response expression, then
    //  return an array of four elements denoting the prefix, concentration,
    //  unit, and postfix, respectively, of the expression.  For example,
    //  consider the following input expression:
    //     % Inhibition of E. Coli WT @ 10 uMol Rep 1
    //  the four elements returned are:
    //     prefix: % Inhibition of E. Coli WT
    //     concentration: 10
    //     unit: uMol
    //     postfix: Rep 1
    protected static Object[] parseDoseResponseExpr (String expr) {
	Matcher m = DOSE_RESPONSE_REGEX.matcher(expr);

	Object[] tokens = {};
	if (m.find()) {
	    String prefix = m.group(2);
	    if (prefix != null) {
		prefix = prefix.replaceAll
		    ("([\\s_]+at|_|[\\s_]+@)$", "").trim();
	    }
	    String postfix = m.group(8);
	    if (postfix != null) {
		postfix = postfix.trim();
	    }
	    Double conc = Double.valueOf(m.group(4));
	    String u = m.group(7);
	    Unit unit = Unit.unspecified;
	    if (u.startsWith("uM") || u.equalsIgnoreCase("microM")) {
		unit = Unit.um;
	    }
	    else if (u.equalsIgnoreCase("nM") || u.equalsIgnoreCase("nanoM")) {
		unit = Unit.nm;
	    }
	    tokens = new Object[]{prefix, conc, unit, postfix};
	}

	return tokens;
    }

    public static Assay parseBioassayXML (InputStream is) throws Exception {
	return parseBioassayXML (new XmlTwig (is));
    }

    public static Assay parseBioassayXML (Reader r) throws Exception {
	return parseBioassayXML (new XmlTwig (r));
    }

    protected static Assay parseBioassayXML (XmlTwig twig) throws Exception {

	String aid = twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_aid/PC-ID/PC-ID_id");
	if (aid == null) aid = twig.getElementValue("PC-AssayDescription/PC-AssayDescription_aid/PC-ID/PC-ID_id");
	Assay assay = new Assay (new Integer (aid));
	assay.setName(twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_name"));
	assay.setSourceName(twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_aid-source/PC-Source/PC-Source_db/PC-DBTracking/PC-DBTracking_name"));
	assay.setSourceID(twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_aid-source/PC-Source/PC-Source_db/PC-DBTracking/PC-DBTracking_source-id/Object-id/Object-id_str"));
	assay.setDescription(twig.getElementsAsText("PC-AssayDescription_description_E", "\n"));
	assay.setProtocol(twig.getElementsAsText("PC-AssayDescription_protocol_E", "\n"));
	assay.setComment(twig.getElementsAsText("PC-AssayDescription_comment_E", "\n"));
	String aom = twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_activity-outcome-method");
	if (aom != null) {
	    assay.setOutcomeMethod
		(Assay.AOM.getInstance(Integer.parseInt(aom)));
	}

	String grant = twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_grant-number/PC-AssayDescription_grant-number_E");
	if (grant != null) {
	    assay.setGrant(grant);
	}

	String cat = twig.getElementValue("PC-AssayContainer/PC-AssaySubmit/PC-AssaySubmit_assay/PC-AssaySubmit_assay_descr/PC-AssayDescription/PC-AssayDescription_project-category");
	if (cat != null) {
	    assay.setCategory(Integer.parseInt(cat));
	}

	/*
	 * xref
	 */
	NodeList xrefNodes = twig.getDocument().getDocumentElement()
	    .getElementsByTagName("PC-AnnotatedXRef");
	for (int i = 0; i < xrefNodes.getLength(); ++i) {
	    Element xrefElm = (Element)xrefNodes.item(i);
	    String pmid = XmlTwig.getElementValue
		(xrefElm, "PC-AnnotatedXRef/PC-AnnotatedXRef_xref/PC-XRefData/PC-XRefData_pmid");
	    if (pmid != null) {
		//System.out.println("PMID: " + pmid);
		assay.addPublication(Long.parseLong(pmid));
	    }
	    String gene  =XmlTwig.getElementValue
		(xrefElm, "PC-AnnotatedXRef/PC-AnnotatedXRef_xref/PC-XRefData/PC-XRefData_gene");
	    if (gene != null) {
		//System.out.println("GENE: " + gene);
		//System.out.print("\t" + gene);
		try {
		    assay.addGene(Integer.parseInt(gene));
		}
		catch (NumberFormatException ex) {
		    logger.log(Level.SEVERE, "Bad gene id: "+gene, ex);
		}
	    }
	    String url = XmlTwig.getElementValue
		(xrefElm, "PC-AnnotatedXRef/PC-AnnotatedXRef_xref/PC-XRefData/PC-XRefData_dburl");
	    if (url != null) {
		//System.out.println("URL: " + url);
		assay.setURL(url);
	    }
	    String raid = XmlTwig.getElementValue
		(xrefElm, "PC-AnnotatedXRef/PC-AnnotatedXRef_xref/PC-XRefData/PC-XRefData_aid");
	    if (raid != null) {
		try {
		    assay.addAID(Integer.parseInt(raid));
		}
		catch (NumberFormatException ex) {
		    logger.log(Level.SEVERE, "Bad aid: "+raid, ex);
		}
		//System.out.println("AID: " + raid);
	    }
	}

	/*
	 * now parse result type fields
	 */

	NodeList resultTypeNodes = twig.getDocument().getDocumentElement()
	    .getElementsByTagName("PC-ResultType");

	// dose concentrations
	final Map<Integer, Double> dose = new TreeMap<Integer, Double>();
	// dose response curve grouping
	Map<String, BitSet> drgrp = new TreeMap<String, BitSet>();
	Unit doseUnit = Unit.unspecified;

	Map<Integer, ResultType> results = new TreeMap<Integer, ResultType>();

	int maxTID = 0;
	for (int i = 0; i < resultTypeNodes.getLength(); ++i) {
	    Element resultTypeElm = (Element)resultTypeNodes.item(i);
	    ResultType type = new ResultType ();
	    type.setTID(Integer.parseInt
			(XmlTwig.getElementValue
			 (resultTypeElm, "PC-ResultType/PC-ResultType_tid")));
	    if (type.getTID() > maxTID) {
		maxTID = type.getTID();
	    }
	    type.setName(XmlTwig.getElementValue
			 (resultTypeElm, "PC-ResultType/PC-ResultType_name"));
	    type.setDescription(XmlTwig.getElementsAsText
				(resultTypeElm, 
				 "PC-ResultType_description_E", "\n"));
	    String rtype = XmlTwig.getElementValue
		(resultTypeElm, "PC-ResultType/PC-ResultType_type");
	    if (rtype != null) {
		int t = Integer.parseInt(rtype);
		for (Type v : Type.values()) {
		    if (v.ordinal() == t) {
			type.setType(v);
		    }
		}
	    }
	    String unit = XmlTwig.getElementValue
		(resultTypeElm, "PC-ResultType/PC-ResultType_unit");
	    if (unit != null) {
		type.setUnit(Unit.getInstance(Integer.parseInt(unit)));
	    }

	    Element acElm = XmlTwig.getElement
		(resultTypeElm, "PC-ResultType/PC-ResultType_ac");
	    if (acElm != null) {
		String ac = acElm.getAttribute("value");
		type.setActiveConcentration(ac.equalsIgnoreCase("true"));
	    }

	    String tcVal = XmlTwig.getElementValue
		(resultTypeElm, "PC-ResultType/PC-ResultType_tc/PC-ConcentrationAttr/PC-ConcentrationAttr_concentration");
	    if (tcVal != null && 
		(type.getType() == ResultType.Type.Float
		 || type.getType() == ResultType.Type.Int)) {
		dose.put(type.getTID(), Double.valueOf(tcVal));
		Double[] concList = {Double.valueOf(tcVal)};
		type.setTestConcentration(concList);
	    }
	    String tcUnit = XmlTwig.getElementValue
		(resultTypeElm, "PC-ResultType/PC-ResultType_tc/PC-ConcentrationAttr/PC-ConcentrationAttr_unit");
	    if (tcUnit != null) {
		Unit u = Unit.getInstance(Integer.parseInt(tcUnit));
		if (doseUnit != Unit.unspecified && doseUnit != u) {
		    throw new IllegalStateException 
			("Dose response concentrations have different units!");
		}
		doseUnit = u;
		type.setTestConcUnit(u);
	    }

	    String tcDrId = XmlTwig.getElementValue
		(resultTypeElm, "PC-ResultType/PC-ResultType_tc/PC-ConcentrationAttr/PC-ConcentrationAttr_dr-id");
	    if (tcDrId != null) {
		BitSet group = drgrp.get(tcDrId);
		if (group == null) {
		    drgrp.put(tcDrId, group = new BitSet ());
		}
		group.set(type.getTID());
	    }
	    else if (tcVal != null) {
		// if there is concentration but no grouping info, then we
		//  try to parse the name of this result type to get the
		//  grouping info
		Object[] expr = parseDoseResponseExpr (type.getName());
		if (expr.length <= 0) {
		    // do nothing
		}
		else if (expr[0] != null) {
		    String prefix = (String)expr[0];
		    BitSet group = drgrp.get(prefix);
		    if (group == null) {
			drgrp.put(prefix, group = new BitSet ());
		    }
		    group.set(type.getTID());
		}
		else if (expr[3] != null) {
		    String postfix = (String)expr[3];
		    BitSet group = drgrp.get(postfix);
		    if (group == null) {
			drgrp.put(postfix, group = new BitSet ());
		    }
		    group.set(type.getTID());
		}
	    }

	    results.put(type.getTID(), type);
	}

	/*
	  do we care what are the different DR???
	  NodeList assayDrNodes = twig.getDocument().getDocumentElement()
	  .getElementsByTagName("PC-AssayDRAttr");
	  for (int i = 0; i < assayDrNodes.getLength(); ++i) {
	  Element assayDrAttrElm = (Element)assayDrNodes.item(i);
	    
	  }
	*/

	if (dose.size() == 0) { //!!!! && assay.getOutcomeMethod() == Assay.AOM.Confirmatory) { 
	    // hmm... confirmation assay with no dose response curve?... 
	    //  perhaps older assay, so let's see if we can parse the 
	    //  TIDs to figure out if there exist columns that look like
	    //  dr columns
	    doseUnit = Unit.m; // convert everything to molar
	    for (ResultType rt : results.values()) {
		if (rt.getType() == ResultType.Type.Float 
		    && rt.getUnit() == ResultType.Unit.percent) {
		    Object[] expr = parseDoseResponseExpr (rt.getName());
		    if (expr.length > 0) {
			// match as dose-response header
			String prefix = (String)expr[0];
			Double conc = (Double)expr[1];
			Unit unit = (Unit)expr[2];
			String postfix = (String)expr[3];
			
			if (prefix != null) {
			    BitSet group = drgrp.get(prefix);
			    if (group == null) {
				drgrp.put(prefix, group = new BitSet ());
			    }
			    group.set(rt.getTID());
			}
			else if (postfix != null) {
			    BitSet group = drgrp.get(postfix);
			    if (group == null) {
				drgrp.put(postfix, group = new BitSet ());
			    }
			    group.set(rt.getTID());
			}
			
			if (unit == Unit.um) {
			    conc *= 1e-6;
			}
			else if (unit == Unit.nm) {
			    conc *= 1e-9;
			}
			dose.put(rt.getTID(), conc);
			Double[] concList = {conc};
			rt.setTestConcentration(concList);
			rt.setTestConcUnit(Unit.m);
		    }
		}
	    }
	}

	// add an additional column for dose response
	if (dose.size() > 0) { //!!! > 1) {
	    if (drgrp.size() <= 1) { // single dose response curve...
		ResultType rt = createDoseResponseType 
		    (dose, results, dose.keySet());
		rt.setTID(++maxTID);
		rt.setTestConcUnit(doseUnit);
		assay.addResult(rt);
	    }
	    else { // multiple dose-response curves
		for (BitSet e : drgrp.values()) {
		    // only create DoseResult type if we have at least 3 or 
		    //   more data columns
		    if (e.cardinality() > 2) {
			Vector<Integer> columns = new Vector<Integer>();
			for (int b = e.nextSetBit(0); b >=0; 
			     b = e.nextSetBit(b+1)) {
			    columns.add(b);
			}
			
			ResultType rt = createDoseResponseType 
			    (dose, results, columns);
			rt.setTID(++maxTID);
			rt.setTestConcUnit(doseUnit);
			assay.addResult(rt);
		    }
		}
	    }
	}
	dose.clear();

	// now add the rest of the types....
	for (Map.Entry<Integer, ResultType> e : results.entrySet()) {
	    assay.addResult(e.getValue());
	}
	results.clear();
	results = null;

	return assay;
    }

    private static ResultType createDoseResponseType 
	(final Map<Integer, Double> dose, Map<Integer, ResultType> results,
	 Collection<Integer> columns) {
	String title = "";
	StringBuffer desc = new StringBuffer ();
	// make sure the concentrations are sorted in ascending order
	Vector<Integer> sortedTID = new Vector<Integer>(columns);
	Collections.sort(sortedTID, new Comparator<Integer>() {
			     public int compare (Integer a, Integer b) {
				 Double da = dose.get(a);
				 Double db = dose.get(b);
				 if (da == null && db == null) return 0;
				 if (da == null) return -1;
				 if (db == null) return 1;
				 if (da < db) return -1;
				 else if (da > db) return 1;
				 return 0;
			     }
			 });
	for (Integer tid : sortedTID) {
	    if (title.length() == 0) {
		ResultType rt = results.get(tid);
		Object[] expr = parseDoseResponseExpr (rt.getName());
		if (expr.length > 0) {
		    if (expr[0] != null) {
			title = (String)expr[0]; // prefix
		    }
		    else if (expr[3] != null) {
			title = (String)expr[3]; // postfix
		    }
		}
	    }
	    desc.append((desc.length() == 0 ? "" : " ") + tid);
	}
	ResultType rt = new ResultType ();
	rt.setName(title);
	rt.setDescription(desc.toString());
	rt.setType(Type.DoseResponse);
	rt.setUnit(Unit.none);
	Double[] conc = new Double[sortedTID.size()];
	for (int i = 0; i < conc.length; ++i) {
	    conc[i] = dose.get(sortedTID.get(i));
	}
	rt.setTestConcentration(conc);

	return rt;
    }

    public static void assignDataTypes (Vector<ResultType> result) {

	// used TIDs that have been used by dose-response parameters..
	Set<Integer> drMask = new HashSet<Integer>();

	// first add dose-response data (if any)
	for (ResultType rt: result) {
	    if (rt.getType() == ResultType.Type.DoseResponse) {
		rt.setContextGroup(rt.getTID());
		Vector<Double> dose = new Vector<Double>();
		Vector<Double> response = new Vector<Double>();

		Double[] conc = rt.getTestConcentration();
		String[] toks = rt.getDescription().split("\\s");
		if (conc.length != toks.length) {
		    System.err.println("** fatal error: inconsistent "
			    +"dose-response data!!!");
		}

		for (int i = 0; i < toks.length; ++i) {
		    int tid = Integer.parseInt(toks[i]);
		    for (ResultType resultType: result) 
			if (resultType.getTID() == tid) {
			    if (resultType.getDataType().equals(Constants.DataType.Unknown)) {
				resultType.setDataType(Constants.DataType.CRpt);
				resultType.setContextGroup(rt.getTID());
			    } else {
				System.err.println("Can not be CRpt; Data type already assigned for TID:"+tid+" to type "+resultType.getDataType());
				System.exit(1);
			    }
			}
		}

		AssayDataDoseResponseHill4p dr = 
			new AssayDataDoseResponseHill4p 
			(rt.getTID(), response.toArray(new Double[0]));
		dr.setDose(dose.toArray(new Double[0]));

		// figure out the hill/fit parameters!!!!
		checkForHillParams 
		(result, rt.getName(), rt.getTID(), drMask);

		// reassign dose response context groups to AC50 item
		int oldContext = rt.getTID(), newContext = rt.getTID();
		for (ResultType rt2: result) {
		    if (DataType.Ac50.equals(rt2.getDataType()))
			newContext = rt2.getTID();
		}
		if (newContext != oldContext)
		for (ResultType rt2: result) {
		    if (rt2.getContextGroup() == oldContext)
			rt2.setContextGroup(newContext);
		}		
	    }
	}
    }

    public static void parse (Assay assay, InputStream is) throws IOException {

	BufferedReader br = new BufferedReader (new InputStreamReader (is));

	String line = br.readLine();
	if (line == null) {
	    return;
	}

	String[] header = tokenizer (line, ',');
	String[] field = new String[header.length];
	for (int rows = 1; (line = br.readLine()) != null; ++rows) {
	    boolean ok = tokenizer (field, line, ',');
	    String sid = field[PUBCHEM_SID];
	    String cid = field[PUBCHEM_CID];

	    if (!ok) {
		System.err.println
		    ("*** AID=" + assay.getAID() + ":line=" 
		     + rows + ": expected " + header.length 
		     + " columns but got " + field.length 
		     + " instead; skipping substance " + sid);
		continue;
	    }

	    long id = 0;
	    try {
		id = Long.parseLong(cid);
	    }
	    catch (NumberFormatException ex) {
	    }
	    AssayResults ar = new AssayResults (id);
	    ar.setSID(sid);

	    String value = field[PUBCHEM_OUTCOME];
	    if (value != null && value.length() > 0) {
		ar.setOutcome(Outcome.instanceOf
			      (Integer.parseInt(value)));
	    }
		
	    value = field[PUBCHEM_RANK];
	    if (value != null && value.length() > 0) {
		ar.setRank(Integer.parseInt(value));
	    }

	    value = field[PUBCHEM_URL];
	    if (value != null && value.length() > 0) {
		ar.setURL(value);
	    }

	    Map<Integer, AssayData> data = 
		new TreeMap<Integer, AssayData>();
	    for (int i = PUBCHEM_NUMCOLUMNS; i < field.length; ++i) {
		int tid = Integer.parseInt(header[i]);
		ResultType rt = assay.getResult(tid);
		AssayData ad = parseField (rt, field[i]);
		if (ad != null) {
		    data.put(tid, ad);
		}
	    }

	    // used TIDs that have been used by dose-response parameters..
	    Set<Integer> drMask = new HashSet<Integer>();

	    // first add dose-response data (if any)
	    Enumeration<ResultType> result = assay.getResults();
	    for (; result.hasMoreElements(); ) {
		ResultType rt = result.nextElement();
		if (rt.getType() == ResultType.Type.DoseResponse) {
		    Vector<Double> dose = new Vector<Double>();
		    Vector<Double> response = new Vector<Double>();
			
		    Double[] conc = rt.getTestConcentration();
		    String[] toks = rt.getDescription().split("\\s");
		    if (conc.length != toks.length) {
			System.err.println("** fatal error: inconsistent "
					   +"dose-response data!!!");
		    }

		    for (int i = 0; i < toks.length; ++i) {
			int tid = Integer.parseInt(toks[i]);
			AssayData ad = data.get(tid);
			if (ad != null) {
			    double x;
			    switch (rt.getTestConcUnit()) {
			    case um: x = conc[i]*1e-6; break;
			    case m: x = conc[i]; break;
			    case nm: x = conc[i]*1e-9; break;
			    default: throw new IllegalStateException 
				    ("Not supported concentration unit: " 
				     + rt.getTestConcUnit());
			    }
			    dose.add(x);
			    response.add
				(((Number)ad.getValue()).doubleValue());
			}
		    }

		    AssayDataDoseResponseHill4p dr = 
			new AssayDataDoseResponseHill4p 
			(rt.getTID(), response.toArray(new Double[0]));
		    dr.setDose(dose.toArray(new Double[0]));

		    // figure out the hill/fit parameters!!!!
		    getHillParams 
			(assay, data.values(), rt.getName(), dr, drMask);
			
		    ar.addData(dr);
		}
	    }

	    // now add the rest of the data
	    for (Map.Entry<Integer, AssayData> e : data.entrySet()) {
		AssayData ad = e.getValue();
		if (ad != null) {
		    ar.addData(ad);
		}
	    }

	    System.out.println("Hey!");
	}
    } // parse

    static String[] tokenizer (String line, char delim) {
	Vector<String> toks = new Vector<String>();

	int len = line.length(), parity = 0;
	StringBuffer curtok = new StringBuffer ();
	for (int i = 0; i < len; ++i) {
	    char ch = line.charAt(i);
	    if (ch == '"') {
		parity ^= 1;
	    }
	    if (ch == delim) {
		if (parity == 0) {
		    String tok = null;
		    if (curtok.length() > 0) {
			tok = curtok.toString();
		    }
		    toks.add(tok);
		    curtok.setLength(0);
		}
		else {
		    curtok.append(ch);
		}
	    }
	    else if (ch != '"') {
		curtok.append(ch);
	    }
	}

	if (curtok.length() > 0) {
	    toks.add(curtok.toString());
	}

	return toks.toArray(new String[0]);
    }

    static boolean tokenizer (String[] tokens, String line, char delim) {
	int len = line.length(), parity = 0;
	StringBuffer curtok = new StringBuffer ();
	int tokpos = 0;
	for (int i = 0; i < len && tokpos < tokens.length; ++i) {
	    char ch = line.charAt(i);
	    if (ch == '"') {
		parity ^= 1;
	    }
	    if (ch == delim) {
		if (parity == 0) {
		    String tok = null;
		    if (curtok.length() > 0) {
			tok = curtok.toString();
		    }
		    tokens[tokpos++] = tok;
		    curtok.setLength(0);
		}
		else {
		    curtok.append(ch);
		}
	    }
	    else if (ch != '"') {
		curtok.append(ch);
	    }
	}

	if (tokpos < tokens.length) {
	    tokens[tokpos++] = 
		curtok.length() > 0 ? curtok.toString() : null;
	}

	return tokpos == tokens.length;
    }

    // figure out which of the data is hill slope...
    static final Pattern FOC_REGEX = Pattern.compile("\\s+FOC(\\s|$)+");
    protected static void checkForHillParams 
	(Vector<ResultType> rtv, String name, int contextGroup, Set<Integer> mask) {

	if (FOC_REGEX.matcher(name).find()) {
	    // might be something like: 620 nm FOC
	    return;
	}

	HashMap<Constants.DataType, ResultType> params = new HashMap<Constants.DataType, ResultType>();
	ResultType ac50Rt = null, slopeRt = null, zeroRt = null, infRt = null;
	for (ResultType rt: rtv) {
	    if (mask.contains(rt.getTID())) {
		continue;
	    }

	    String text = rt.getName();

	    if (rt.getType() == ResultType.Type.Float) {
		if (isHillSlope(text) && slopeRt == null) {
		    params.put(Constants.DataType.Hslope, rt);
		    slopeRt = rt;
		    rt.setDataType(Constants.DataType.Hslope);
		    // sigh... Emory... e.g., AID 801
		    if (text.indexOf("Curve Curvature") >= 0
			    || text.indexOf("Curve Slope") >= 0) {
			rt.setTransform(Constants.Transform.Negative);
		    }
		}
		else if (isHillZero (text) && zeroRt == null) {
		    params.put(Constants.DataType.Hzero, rt);
		    zeroRt = rt;
		    rt.setDataType(Constants.DataType.Hzero);
		}
		else if (isHillInf (text) && infRt == null) {
		    params.put(Constants.DataType.Hinf, rt);
		    infRt = rt;
		    rt.setDataType(Constants.DataType.Hinf);
		}
		else if ((ac50Rt == null || ac50Rt.getType() == ResultType.Type.String)
			&& isHillXC50 (text) && 
			(isConcUnit (rt.getUnit()) 
			// a big exception to handle a small
			//   case of early ncgc's assays that
			//   don't have property unit
				|| text.equals("Qualified AC50"))) {
		        /// always assume it's in uM???
		    params.put(Constants.DataType.Ac50, rt);
		    ac50Rt = rt;
		    rt.setDataType(Constants.DataType.Ac50);
		}
		else {
		    DataType dataType = isOtherType(text);
		    if (dataType != DataType.Unknown && !params.containsKey(dataType)) {
			params.put(isOtherType(text), rt);
			rt.setDataType(dataType);
		    }
		}
	    }
	    else if (rt.getType() == ResultType.Type.String) {
		if (text.equals("Potency")) {
		    params.put(Constants.DataType.Ac50, rt);
		    ac50Rt = rt;
		    mask.add(ac50Rt.getTID());
		    rt.setDataType(Constants.DataType.Ac50);
		}
		else {
		    DataType dataType = isOtherType(text);
		    if (dataType != DataType.Unknown && !params.containsKey(dataType)) {
			params.put(isOtherType(text), rt);
			rt.setDataType(dataType);
		    }
		}
	    }
	}

	if (params.containsKey(Constants.DataType.Ac50)) {
	    for (DataType key: params.keySet()) {
		ResultType rt = params.get(key);
		if (!mask.contains(rt.getTID()))
		    mask.add(rt.getTID());
		rt.setContextGroup(contextGroup);
	    }
	}
	
//	if (ac50Rt != null) { // && slopeRt != null) {
//	    if (!mask.contains(ac50Rt.getTID())) {
//		mask.add(ac50Rt.getTID());
//	    }
//	    ac50Rt.setContextGroup(contextGroup);
//	    
//	    if (slopeRt != null) {
//		mask.add(slopeRt.getTID());
//		slopeRt.setContextGroup(contextGroup);
//	    }
//
//	    if (zeroRt != null) {
//		mask.add(zeroRt.getTID());
//		zeroRt.setContextGroup(contextGroup);
//	    }
//
//	    if (infRt != null) {
//		mask.add(infRt.getTID());
//		infRt.setContextGroup(contextGroup);
//	    }
//	}
    }

    protected static void getHillParams 
	(Assay assay, Collection<AssayData> data, 
	 String name, AssayDataDoseResponseHill4p dr, Set<Integer> mask) {

	if (FOC_REGEX.matcher(name).find()) {
	    // might be something like: 620 nm FOC
	    return;
	}

	Double ac50 = null, slope = null, zero = null, inf = null;
	ResultType ac50Rt = null, slopeRt = null, zeroRt = null, infRt = null;
	for (AssayData ad : data) {
	    if (mask.contains(ad.getTID())) {
		continue;
	    }

	    ResultType rt = assay.getResult(ad.getTID());
	    if (ad != null) {
		String text = rt.getName();

		if (rt.getType() == ResultType.Type.Float) {
		    if (isHillSlope(text) && slopeRt == null) {
			slope = (Double)ad.getValue();
			// sigh... Emory... e.g., AID 801
			if (text.indexOf("Curve Curvature") >= 0
			    || text.indexOf("Curve Slope") >= 0) {
			    slope *= -1;
			}
			slopeRt = rt;
		    }
		    else if (isHillZero (text) && zeroRt == null) {
			zero = (Double)ad.getValue();
			zeroRt = rt;
		    }
		    else if (isHillInf (text) && infRt == null) {
			inf = (Double)ad.getValue();
			infRt = rt;
		    }
		    else if (ac50Rt == null && isHillXC50 (text) && 
			     (isConcUnit (rt.getUnit()) 
			      // a big exception to handle a small
			      //   case of early ncgc's assays that
			      //   don't have property unit
			      || text.equals("Qualified AC50"))) {
			/// always assume it's in uM???
			double scale = 1.;
			switch (rt.getUnit()) {
			case um: scale = 1e-6; break;
			case nm: scale = 1e-9; break;
			case mm: scale = 1e-3; break;
			}
			ac50 = (Double)ad.getValue() * scale;
			ac50Rt = rt;
		    }
		}
		else if (text.equals("Potency") 
			 && rt.getType() == ResultType.Type.String) {
		    // parse the string version...
		    try {
			String value = (String)ad.getValue();
			int index = value.indexOf("(");
			ac50 = Double.parseDouble
			    (index < 0 ? value : value.substring(0, index));
			ac50Rt = rt;
		    }
		    catch (NumberFormatException ex) {}
		}
	    }
	}

	if (ac50 != null && slope != null) {
	    mask.add(ac50Rt.getTID());
	    mask.add(slopeRt.getTID());

	    if (zeroRt != null) {
		mask.add(zeroRt.getTID());
	    }

	    if (infRt != null) {
		mask.add(infRt.getTID());
	    }

	    if (inf == null || zero == null) {
		// set inf to be max response....
		Double[] res = (Double[])dr.getValue();
		if (res != null && res.length > 0) {
		    Double min = res[0], max = res[0];
		    for (int i = 1; i < res.length; ++i) {
			if (res[i] > max) max = res[i];
			if (res[i] < min) min = res[i];
		    }
		    if (inf == null) inf = max;
		    if (zero == null) zero = min;
		}
	    }

	    if (zero != null && inf != null) {
		/*
		System.err.println("** dose-response " + name
				   +": xc50=\""+ac50Rt.getName()+"\""
				   +" slope=\""+slopeRt.getName()+"\""
				   +" zero=\""
				   +(zeroRt!=null?zeroRt.getName():"")+"\""
				   +" inf=\""
				   +(infRt!=null?infRt.getName():"")+"\"");
		*/
		dr.setHillCoef(slope);
		dr.setZeroAct(zero);
		dr.setInfAct(inf);
		dr.setAc50(ac50);
	    }
	}
    }

    // different incarnations of hill slope
    static final String[] HILL_SLOPE_EXPRS = {
	"Hill Slope",
	"HillSlope",
	"HILLSLOPE",
	"Hill Coefficient",
	"Hill slope",
	"Hillslope",
	"Curve Curvature",
	"Curve Slope"
    };
    protected static boolean isHillSlope (String text) {
	for (int i = 0; i < HILL_SLOPE_EXPRS.length; ++i) {
	    if (text.indexOf(HILL_SLOPE_EXPRS[i]) >= 0) {
		return true;
	    }
	}
	return false;
    }

    static final String[] HILL_INF_EXPRS = {
	"Hill Sinf",
	"InfiniteActivity",
	"EC50 Max",
	"IC50 Max",
	"Curve Top",
	"TOP",
	"SInf"
    };
    protected static boolean isHillInf (String text) {
	for (int i = 0; i < HILL_INF_EXPRS.length; ++i) {
	    if (HILL_INF_EXPRS[i].equals(text)
		|| text.indexOf(HILL_INF_EXPRS[i]) >= 0) {
		return true;
	    }
	}
	return false;
    }

    static final String[] HILL_ZERO_EXPRS = {
	"Hill S0",
	"ZeroActivity",
	"EC50 Min",
	"IC50 Min",
	"Curve Bottom",
	"BOTTOM",
	"S0"
    };
    protected static boolean isHillZero (String text) {
	for (int i = 0; i < HILL_ZERO_EXPRS.length; ++i) {
	    if (HILL_ZERO_EXPRS[i].equals(text) 
		|| text.indexOf(HILL_ZERO_EXPRS[i]) >= 0) {
		return true;
	    }
	}
	return false;
    }

    static final String[] HILL_XC50_EXPRS = {
	"EC50",
	"IC50",
	"AC50",
	"Potency",
	"Qualified AC50"
    };
    protected static boolean isHillXC50 (String text) {
	for (int i = 0; i < HILL_XC50_EXPRS.length; ++i) {
	    if ((HILL_XC50_EXPRS[i].equals(text)
		 || text.indexOf(HILL_XC50_EXPRS[i]) >= 0)
		// AID 855
		&& (text.indexOf("Error") < 0 
		    && text.indexOf("Std") < 0
		    && text.indexOf("Relative") < 0)) {
		return true;
	    }
	}
	return false;
    }

    static final HashMap<String, Constants.DataType> OTHER_EXPRS = new HashMap<String, Constants.DataType>();
    static {
	OTHER_EXPRS.put("Fit_CurveClass", Constants.DataType.CurveClass);
	OTHER_EXPRS.put("Fit_R2", Constants.DataType.R2);
	OTHER_EXPRS.put("Excluded_Points", Constants.DataType.ExcludedPoints);
    }
    protected static Constants.DataType isOtherType(String text) {
	for (String key: OTHER_EXPRS.keySet())
	    if (key.equals(text) || text.indexOf(key) >= 0)
	    	return OTHER_EXPRS.get(key);
	return Constants.DataType.Unknown;
    }
    
    protected static boolean isConcUnit (ResultType.Unit u) {
	// only look at field that looks like concentration
	return u == ResultType.Unit.um 
	    || u == ResultType.Unit.m
	    || u == ResultType.Unit.nm;
    }

    protected static AssayData parseField (ResultType result, String value) {
	if (value != null && value.length() > 0) {
	    switch (result.getType()) {
	    case String: 
		return new AssayData (result.getTID(), value);
		
	    case Float:
		return new AssayData (result.getTID(), Double.valueOf(value));
		
	    case Int:
		return new AssayData (result.getTID(), Integer.valueOf(value));
		
	    case Bool:
		return new AssayData (result.getTID(), Boolean.valueOf(value));
	    }
	}
	return null;
    }

    private static Vector<ResultType> loadResultsFromJson(JsonNode node, ObjectMapper mapper) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, JsonParseException, JsonMappingException, IOException {
	if (node == null)
	    return null;
	
	HashMap<String,Method> rtSetMethods = new HashMap<String,Method>();
	for (Method method: ResultType.class.getMethods()) {
	    if (method.getName().startsWith("set"))
		rtSetMethods.put(method.getName().substring(3).toLowerCase(), method);
	}
	
	Vector<ResultType> rts = new Vector<ResultType>();
	for (Iterator<JsonNode> iter =node.iterator(); iter.hasNext();) {
	    JsonNode child = iter.next();
	    ResultType rt = new ResultType();
	    for (Iterator<String> iter2 = child.getFieldNames(); iter2.hasNext();) {
		String field = iter2.next();
		if (rtSetMethods.containsKey(field.toLowerCase())) {
		    Method method = rtSetMethods.get(field.toLowerCase());
		    Class<?>[] inputs = method.getParameterTypes();
		    if (inputs.length != 1) throw new IllegalArgumentException("Method has too many input arguements: "+method.getName());
		    Object input = mapper.readValue(child.get(field), inputs[0]);
		    method.invoke(rt, input);
		}
	    }
	    rts.add(rt);
	}

	return rts;
    }

    public static void main (String argv[]) throws Exception {
//	if (argv.length < 2) {
//	    System.err.println("usage: PubChemAssayParser AID.xml AID.csv");
//	    System.exit(1);
//	}
//
//        Assay assay = parseBioassayXML (new FileReader (argv[0]));
//        Enumeration<ResultType> rte = assay.getResults();
//        Vector<ResultType> rtv = new Vector<ResultType>();
//        while (rte.hasMoreElements()) {
//            rtv.add(rte.nextElement());
//        }
//        assignDataTypes (rtv);
//        for (Enumeration<ResultType> en = assay.getResults(); 
//                en.hasMoreElements(); ) {
//               System.out.println(en.nextElement());
//           }
//
//        System.exit(0);
        
//	CAPExtractor c = new CAPExtractor();
//	c.setHandlers();
//	Dictionary d = (Dictionary)CapResourceHandlerRegistry.getInstance().getHandler(CAPConstants.CapResource.DICTIONARY).
//		poll(CAPConstants.CAP_ROOT+"/dictionary", CAPConstants.CapResource.DICTIONARY).get(0);
//
	Connection conn;

	HashMap<Integer,Integer> exptIDLookup = new HashMap<Integer,Integer>();
	HashMap<String,Integer> capDict = new HashMap<String,Integer>();
	HashMap<Integer,String> capDictReverse = new HashMap<Integer,String>();
	HashMap<String,String> exptTIDName = new HashMap<String,String>();
	HashMap<String,String> exptTIDType = new HashMap<String,String>();
	HashMap<String,Integer> exptTIDElem = new HashMap<String,Integer>();
	HashMap<String,Integer> exptTIDGroup = new HashMap<String,Integer>();
	try {
	    conn = CAPUtil.connectToBARD();
	    conn.setAutoCommit(false);
	    Statement st = conn.createStatement();
	    st.execute("select bard_expt_id, pubchem_aid from bard_experiment"); // where pubchem_aid=2551");
	    ResultSet result = st.getResultSet();	    
	    while (result.next()) {
		int BardExptId = result.getInt(1);
		int PubChemAID = result.getInt(2);
		exptIDLookup.put(PubChemAID, BardExptId);
	    }
	    result.close();

	    st = conn.createStatement();
	    st.execute("select label, dictid from cap_dict_elem order by ins_date");
	    result = st.getResultSet();	    
	    while (result.next()) {
		int capDictID = result.getInt(2);
		String capDictLabel = result.getString(1);
		capDict.put(capDictLabel, capDictID);
		capDictReverse.put(capDictID, capDictLabel);
	    }
	    capDict.put("unknown", -1);
	    result.close();

	    st = conn.createStatement();
	    st.execute("select pubchem_aid, tid, data_type, data_type_elem, context_group, name from bard_experiment_tid");
	    result = st.getResultSet();	    
	    while (result.next()) {
		int PubChemAID = result.getInt(1);
		int tid = result.getInt(2);
		String key = PubChemAID+":"+tid;
		String name = result.getString(6);
		exptTIDName.put(key, name);
		String type = result.getString(3);
		exptTIDType.put(key, type);
		int typeElem = result.getInt(4);
		exptTIDElem.put(key, typeElem);
		if (!capDict.containsKey(type) || capDict.get(type) != typeElem) {
		    System.err.println("Stated type does not map to dict elem: " + type +" " + typeElem + ":" + capDict.get(type)+":"+capDictReverse.get(typeElem));
		}
		int group = result.getInt(5);
		exptTIDGroup.put(key, group);
	    }
	    result.close();

	    conn.close();
	} catch (Exception e) {e.printStackTrace();}	
	
	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("latest_result_mappings.txt")));
	String[] header = br.readLine().split("\t");
	String line;
	while ((line = br.readLine()) != null) {
	    String[] fline = line.split("\t");
	    String[] sline = new String[22];
	    for (int i=0; i<sline.length; i++) {
		if (fline.length > i)
		    sline[i] = fline[i];
		else sline[i] = "";
	    }
	    int PubChemAID = Integer.valueOf(sline[0]);
	    int PubChemTID = Integer.valueOf(sline[1]);
	    String PubChemTIDName = sline[2];
	    Integer series = sline[3].length() == 0 ? null : Integer.valueOf(sline[3]);
	    if (sline[4].equals("Derives")) {sline[5] = sline[4]; sline[4] = "";} /* sigh ... minor problem with mappings file */
	    if (sline[4].indexOf(',') > -1) {sline[4] = sline[4].substring(1, sline[4].indexOf(','));} /* sigh ... minor problems in mappings file */
	    Integer parentTID = sline[4].length() == 0 ? null : Integer.valueOf(sline[4]);
	    String relationship = sline[5];
	    Integer qualifierTID = sline[6].length() == 0 ? null : Integer.valueOf(sline[6]);
	    String resultType = sline[7];
	    String statsModifier = sline[8];
	    Integer contextTID = sline[9].length() == 0 ? null : Integer.valueOf(sline[9]);
	    String contextItem = sline[10];
	    Double concentration = sline[11].length() == 0 ? null : Double.valueOf(sline[11]);
	    String concUnits = sline[12];
	    Integer panelNumber = sline[16].length() == 0 ? null : Integer.valueOf(sline[16]);
	    String attr1 = sline[17];
	    String value1 = sline[18];
	    Integer excludedSeries = sline[19].length() == 0 ? null : Integer.valueOf(sline[19]);
	    String attr2 = sline[20];
	    String value2 = sline[21];
	    if (!exptIDLookup.containsKey(PubChemAID))
		System.err.println("Missing BARD Expt ID for PubChem AID: "+PubChemAID);
	    else if (!resultType.equals("") && !capDict.containsKey(resultType)) {
		System.err.println("Unknown result type:"+resultType+" "+line);
	    }
	    else {
		String key = PubChemAID+":"+PubChemTID;
		String dataType = "unknown";
		int dataTypeElem = -1;
		if (!resultType.equals("")) {
		    dataType = resultType;
		    dataTypeElem = capDict.get(resultType);
		    
		    if (!exptTIDType.containsKey(key) || (!exptTIDType.get(key).equals(dataType) && !exptTIDType.get(key).equals("unknown")))
			System.err.println("Update to exptTIDType: "+key+":"+dataType+":"+dataTypeElem+":"+exptTIDType.get(key));
		    if (!exptTIDElem.containsKey(key) || (!exptTIDElem.get(key).equals(dataTypeElem) && !exptTIDElem.get(key).equals(-1)))
			System.err.println("Update to exptTIDElem: "+key+":"+dataType+":"+dataTypeElem+":"+exptTIDElem.get(key));
		}
		int contextGroup = -1;
	    }
	}	
	
	System.exit(0);
	br = new BufferedReader(new InputStreamReader(new FileInputStream("ExptTIDs_merged.txt")));
	header = br.readLine().split("\t");
	Map<String, Map<String, String>> exptTIDs = new HashMap<String, Map<String,String>>();
	while ((line = br.readLine()) != null) {
	    String[] sline = line.split("\t");
	    String bardExptID = sline[0];
	    String pubChemAID = sline[1];
	    String TID = sline[2];
	    String key = bardExptID+":"+pubChemAID+":"+TID;
	    Map<String,String> entry = new HashMap<String,String>();
	    for (int i=0; i<sline.length; i++) {
		entry.put(header[i], sline[i]);
	    }
	    exptTIDs.put(key, entry);
	}
		
	System.exit(0);
	//Connection conn;
	try {
	    conn = CAPUtil.connectToBARD();
	    conn.setAutoCommit(false);
	    PreparedStatement erdUpdate = conn.prepareStatement("update bard_experiment set bard_expt_result_def=? where bard_expt_id=?");
	    Statement st = conn.createStatement();
	    st.execute("select bard_expt_id, pubchem_aid, expt_result_def from bard_experiment where bard_expt_id"); // where pubchem_aid=2551");
	    ResultSet result = st.getResultSet();
	    
//	    System.out.println("BardExptID|PubChemAID|"+ResultType.printHeader());
	    while (result.next()) {
		String json = result.getString(3);
		ObjectMapper mapper = new ObjectMapper ();
		if (json != null) {
		    JsonNode node = mapper.readTree(json);
		    Vector<ResultType> exptDef = loadResultsFromJson(node, mapper);
		    assignDataTypes(exptDef);
		    int bardExptId = result.getInt(1);
		    int pubchemAID = result.getInt(2);
//		    for (ResultType rt: exptDef) { 
//			System.out.println(bardExptId+"|"+pubchemAID+"|"+rt.print());
//		    }
		    for (ResultType rt: exptDef) {
			String key = bardExptId+":"+pubchemAID+":"+rt.getTID();
			Map<String,String> entry = exptTIDs.get(key);
			if (!entry.get("DataTypeElem").equals(String.valueOf(rt.getDataType().getElem()))) {
			    System.out.println("Updated data type: "+key+" |"+rt.getDataType()+":"+entry.get("DataType"));
			    DataType dataType = DataType.getDataType(entry.get("DataType"));
			    if (dataType == null) {
				System.err.println("Data Type not found:"+entry.get("DataType"));
//				System.exit(1);
			    } else {
				rt.setDataType(dataType);
			    }
			}
			if (!entry.get("ContextGroup").equals(String.valueOf(rt.getContextGroup()))) {
			    System.out.println("Updated ContextGroup: "+key+" |"+rt.getContextGroup()+":"+entry.get("ContextGroup"));
			    try {
				rt.setContextGroup(Integer.valueOf(entry.get("ContextGroup")));
			    } catch (Exception e) {e.printStackTrace();}
			}
			if (!entry.get("TestConcUnit").toLowerCase().equals(rt.getTestConcUnit().toString())) {
			    System.out.println("Updated TestConcUnit: "+key+" |"+rt.getTestConcUnit()+":"+entry.get("TestConcUnit"));
			    Unit unit = Unit.getUnit(entry.get("TestConcUnit").toLowerCase());
			    if (unit == null) {
				System.err.println("Unit not found:"+entry.get("TestConcUnit"));
				System.exit(1);
			    }
			    rt.setTestConcUnit(unit);
			}
			if (entry.get("TestConcValue") != null && entry.get("TestConcValue").length() > 0 && entry.get("TestConcValue").indexOf(',') == -1) {
			    Double[] entryValue = new Double[1];
			    entryValue[0] = Double.valueOf(entry.get("TestConcValue"));
			    Double[] rtValue = rt.getTestConcentration();
			    if (rtValue.length == 0 || rtValue.length == 1 && !entryValue[0].equals(rtValue[0])) {
				System.out.println("Updated TestConcValue: "+key+" |"+(rtValue.length == 0 ? "" : rtValue[0])+":"+entryValue[0]);
				rt.setTestConcentration(entryValue);
			    }
			}
		    }
		    
		    // remove DoseResponse resultTypes as they are now out-of-date anyway [contextTIDs, concs, etc.]
		    for (int i=exptDef.size()-1; i>-1; i--) {
			ResultType rt = exptDef.get(i);
			if (rt.getType().equals(Type.DoseResponse))
			    exptDef.remove(rt);
		    }
		    
		    erdUpdate.setInt(2, bardExptId);
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    mapper.writeValue(baos, exptDef);
		    String jsonOut = new String(baos.toByteArray());
		    System.out.println(bardExptId+":"+jsonOut);
		    erdUpdate.setString(1, jsonOut);
//		    erdUpdate.executeUpdate();
//		    conn.commit();
		}
	    }
	    result.close();
	    conn.close();
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
    
    }

	
}
