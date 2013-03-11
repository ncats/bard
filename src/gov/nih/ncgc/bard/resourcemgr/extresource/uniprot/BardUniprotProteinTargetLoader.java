package gov.nih.ncgc.bard.resourcemgr.extresource.uniprot;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class BardUniprotProteinTargetLoader {


	private final static Logger logger = Logger.getLogger(BardUniprotProteinTargetLoader.class.getName());
	
	private String sqlCreateTempProteinTarget = "create table temp_protein_target like protein_target";
	private String sqlReplaceIntoProteinTarget = "replace into protein_target (accession, gene_id, name, taxid, uniprot_status)" +
			" values (?,?,?,?,?)";
	private String sqlInsertIntoAccToPrimaryAcc = "insert into uniprot_acc2primary_acc (primary_acc, acc) values (?,?)";
	private String replaceIntoUniprotMap = "replace into uniprot_map (uniprot_acc, acc, acc_type) values (?,?,?)";
	
	private Connection conn;
	private PreparedStatement createTempTablePS;
	private PreparedStatement insertTargetPS;
	private PreparedStatement insertAccToPrimaryAcc;
	
	private long accMapCnt;
	private long targetCnt;
	private int relatedAccCnt;
	private String relatedAccType;
	private int index;
	private String [] accToks;
	
	//patterns
	Pattern refseqPattern;
	Pattern genbankPattern;
	Pattern geneIDPattern;
	Matcher matcher;
	
	public BardUniprotProteinTargetLoader() {
	    refseqPattern = Pattern.compile("\\DP_");
	    genbankPattern = Pattern.compile("\\D{3}\\d");
	    geneIDPattern = Pattern.compile("\\d+");	    
	}
	
	public void loadUniprotToProteinTarget(String uniprotFilePath, String dbUrl, String dbDriverName) {
		try {

			conn = BardDBUtil.connect(dbUrl, dbDriverName, "bard_manager", "bard_manager");
			conn.setAutoCommit(false);

			//make a temp table for protein target
//			createTempTablePS = conn.prepareStatement(sqlCreateTempProteinTarget);
//			createTempTablePS.execute();
//			createTempTablePS.close();

			insertTargetPS = conn.prepareStatement(sqlReplaceIntoProteinTarget);
			insertAccToPrimaryAcc = conn.prepareStatement(sqlInsertIntoAccToPrimaryAcc);

			accMapCnt = targetCnt = 0;
			GZIPInputStream gzipStream = new GZIPInputStream(new FileInputStream(uniprotFilePath));			
			BufferedReader br = new BufferedReader(new InputStreamReader(gzipStream));

			String line = null;
			StringBuffer sb = new StringBuffer();
			int n = 0;
			//need acc to be unique
			HashMap <String, String> accMap = new HashMap <String, String> ();
			Vector <String> accessions = new Vector <String> ();
			boolean haveAcc = false;

			while ((line = br.readLine()) != null) {
				if (line.trim().equals("//")) {
					String acc = "", status = "", name = "", geneid = "", desc = "", taxid = "";
					String[] toks = sb.toString().split("\n");
					haveAcc = false;
					for (String aline : toks) {
						if (aline.startsWith("ID")) {
							status = aline.split("\\s+")[2].replace(";", "");
						} else if (aline.startsWith("AC")) {
							//take just the first AC
							if(!haveAcc) {
								acc = aline.split(";")[0].trim().replace("AC   ", "");
								haveAcc = true;
							}
						} else if (aline.startsWith("DE   RecName:")) {
							name = aline.split("=")[1].replace(";", "");
						} else if (aline.startsWith("DR   GeneID;")) {
							geneid = aline.split(";")[1].trim();
						} else if (aline.startsWith("OX   ")) {
							taxid = aline.trim().replace("OX   NCBI_TaxID=", "").replace(";", "");
						}
						
						//handle acc to other accessions
						if(aline.startsWith("DR   GeneID")
							|| aline.startsWith("RefSeq")
							|| aline.startsWith("EMBL")
							|| aline.startsWith("PDB")) {
						    processRelatedAcc(acc, aline);
						}
					}

					if(accMap.get(acc) == null) {                 
						//if not unique, don't insert
						//accession, gene_id, name, taxid, description, uniprot_status

						insertTargetPS.setString(1, acc);
						if(!geneid.equals(""))
							insertTargetPS.setLong(2, Long.parseLong(geneid));
						else
							insertTargetPS.setNull(2, java.sql.Types.INTEGER);

						insertTargetPS.setString(3, name);
						
						if(!taxid.equals(""))	
							insertTargetPS.setLong(4, Long.parseLong(taxid));
						else
							insertTargetPS.setNull(4, java.sql.Types.INTEGER);
							
						
						insertTargetPS.setString(5, status);

						insertTargetPS.addBatch();
						targetCnt++;

						if(targetCnt % 100 == 0) {
							insertTargetPS.executeBatch();
							conn.commit();
						}
						
						if(targetCnt % 1000 == 0) {
							logger.info("Load Count="+targetCnt);
						}

						accMap.put(acc, "");
					}

					sb = new StringBuffer();

				} else {
					sb.append(line).append("\n");
				}
			}



			insertTargetPS.executeBatch();
			conn.commit();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void processRelatedAcc(String uniprotAcc, String line) {
	    //Have a line starting with 'DR', then source string
	    //then source string (RefSeq | EMBL | GeneID | PDB);
	    
	    //remove the DR and space
	    line = line.substring(line.indexOf(' ')).trim();
	    
	    accToks = line.split(";");
	    for(int i = 0; i < accToks.length; i++) {
		accToks[i]=accToks[i].trim();
	    }
		
	    relatedAccType = accToks[0];
	   
	    if(relatedAccType.equals("EMBL")) {
		//DR   EMBL; X56494; CAA39849.1; -; Genomic_DNA.
		//look for ^three letters and number
		for(int i = 1; i < accToks.length; i++) {
		    matcher = genbankPattern.matcher(accToks[i]);
		    if(matcher.find()) {
			System.out.println(uniprotAcc+" "+accToks[i]+" "+"EMBL-GenBank");
		    }
		}
		
	    } else if(relatedAccType.equals("RefSeq")) {
		//DR   RefSeq; NP_001193725.1; NM_001206796.1.
		//look for ^*P_
		for(int i = 1; i < accToks.length; i++) {
		    matcher = refseqPattern.matcher(accToks[i]);
		    if(matcher.find()) {
			System.out.println(uniprotAcc+" "+accToks[i]+" "+relatedAccType);
		    }  
		}
	    } else if(relatedAccType.equals("PDB")) {
		//DR   PDB; 1ZJH; X-ray; 2.20 A; A=3-530.
		//just take tok 1

	    } else {
		//DR   GeneID; 5315; -.  //not sure if it can be many to 1
//		for(int i = 1; i < accToks.length; i++) {
//		    matcher = refseqPattern.matcher(accToks[i]);
//		    if(matcher.find()) {
//			System.out.println(uniprotAcc+" "+accToks[i]+" "+relatedAccType);
//		    }  
//		}

	    }
	    
	}

}
