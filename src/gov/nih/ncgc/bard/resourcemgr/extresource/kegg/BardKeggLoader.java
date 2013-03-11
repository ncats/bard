package gov.nih.ncgc.bard.resourcemgr.extresource.kegg;

import gov.nih.ncgc.bard.resourcemgr.BardDBUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.Logger;

public class BardKeggLoader {


	private final static Logger logger = Logger.getLogger(BardKeggLoader.class.getName());
	private String insertKeggDisease = "insert into temp_kegg_gene2disease (gene_id, disease_names, disease_id, disease_category)" +
			" values (?,?,?,?)";

	private Connection conn;
	private PreparedStatement insertKeggDiseasePS;
	
	/**
	 * Maps kegg diseases to genes
	 */
	public long loadKeggDisease(String keggDiseaseFilePath) {
		
		long tableSize = 0;
		
		try {
			conn = BardDBUtil.connect();
			conn.setAutoCommit(false);
		
			tableSize = BardDBUtil.getTableRowCount("kegg_gene2disease");
			
			
			File keggDiseaseFile = new File(keggDiseaseFilePath);

			if(!keggDiseaseFile.exists() || !keggDiseaseFile.isFile()) {
				logger.warning("ERROR: KEGG Disease File is Not Found.");
				return -1;
			}
			
			//make and truncate temp_kegg_gene2disease
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists temp_kegg_gene2disease like kegg_gene2disease");
			stmt.execute("truncate table temp_kegg_gene2disease");
			
			//load temp
			loadTempKeggDisease(keggDiseaseFile);
						
			logger.info("TEMP KEGG DISEASE FILE LOADED");

			//rename table from temp to prod
			stmt.execute("drop table kegg_gene2disease");
			stmt.execute("alter table temp_kegg_gene2disease rename to kegg_gene2disease");
			logger.info("RECREATED KEGG_GENE2DISEASE");

			//table growth
			tableSize = BardDBUtil.getTableRowCount("kegg_gene2disease") - tableSize;
			
			conn.close();
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return tableSize;
	}
	
	
	private void loadTempKeggDisease(File keggFile) throws IOException, SQLException, ClassNotFoundException {
						
		this.insertKeggDiseasePS = conn.prepareStatement(this.insertKeggDisease);
		
		BufferedReader br = new BufferedReader(new FileReader(keggFile));
		String line;
		String [] toks;
		String diseaseID = "";
		String names = "";
		String genes = null;
		String category = "";
		String description = "";
		String currField = "";

		Vector <String> geneIDs;
		
		int diseaseCount = 0;
		long geneCount = 0;
		
		logger.info("Loading KEGG Disease: File Parse");
		
		while((line = br.readLine()) != null) {


			if(line.startsWith(" ")) {
				if(currField.equals("NAME")) {
					names += line.trim();
				} else if(currField.equals("GENE")) {
					genes += line.trim()+";";
				}
			} else {
				currField = "";
				if(line.startsWith("ENTRY")) {
					currField = "ENTRY";
					toks = line.split("[\\s]+");
					if(toks.length < 2) {
						System.out.println("bad id parse");
						continue;
					}
					diseaseID = toks[1];
				} else if(line.startsWith("NAME")) {
					currField = "NAME";
					line = line.replace("including:", "");
					names = line.substring(4).trim();
				} else if(line.startsWith("GENE")) {
					currField = "GENE";
					genes = line.substring(4).trim()+";";				
				} else if(line.startsWith("CATEGORY")) {
					currField = "CATEGORY";
					category = line.substring(9).trim();
				} else if(line.startsWith("DESCRIPTION")) {
					currField = "DESCRIPTION";
					description = line.substring(11).trim();
				} else if(line.startsWith("///")) {
					diseaseCount++;
					//end of record

					//if there are no associated genes, continue with next line
					if(genes == null)
						continue;

					//parse genes
					geneIDs = parseGenes(genes);
					long geneIDNum;
					for(String geneID: geneIDs) {
						geneCount++;
						try {
							geneIDNum = Long.parseLong(geneID.trim());
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
						insertKeggDiseasePS.setLong(1, geneIDNum);
						insertKeggDiseasePS.setString(2, names);
						insertKeggDiseasePS.setString(3, diseaseID);
						insertKeggDiseasePS.setString(4, category);
						
						insertKeggDiseasePS.addBatch();
					
						if(geneCount % 135 == 0) {
							insertKeggDiseasePS.executeBatch();
							conn.commit();
						}
					}
					
					//parse symbols

					//set values

//					if(diseaseCount % 12 == 0) {
//						System.out.println(
//							//	"ID: " + diseaseID + "\n" +
//							//			"Names " + names + "\n" +
//							//			"Desc: " + description + "\n" +
//							//			"Category " + category + "\n" +
//										diseaseCount + " Genes " + genes //+ "\n" 
//							//			"*************************************"
//								);
//						Vector <String> g = parseGenes(genes);
//						System.out.println("Gene Count = "+g.size());
//						System.out.print("Genes: ");
//						for (String gene : g)
//							System.out.print(gene+ " ");
//						System.out.println();
//
//						Vector <String> s = parseGeneSymbols(genes);
//						System.out.println("Symbol Count = "+s.size());
//						System.out.print("Symbols: ");
//						for (String symbol : s)
//							System.out.print(symbol+ " ");
//						System.out.println();
//
//					}

					//insert

					//clear current values
					toks=null;
					names = "";				
					diseaseID = names = genes = category = description = null;
					currField = "";
				}
			}
		}

		insertKeggDiseasePS.executeBatch();
		conn.commit();		
	}

	private Vector <String> parseGenes (String geneStr) {
		Vector <String> genes = new Vector <String> ();
		String [] toks = geneStr.split("[;]+");
		int index;
		String gene;
		String [] geneToks;
		for(String tok: toks) {
			index = -1;
			tok = tok.trim();
			index = tok.indexOf("[HSA:");
			if(index == -1)
				continue;
			index += 5;
			gene = tok.substring(index, tok.indexOf(']'));
			
			geneToks = gene.split("[\\s]+");
			
			for(String g : geneToks)
				genes.add(g);
		}
		
		return genes;
	}

	
	private Vector <String> parseGeneSymbols (String geneStr) {
		Vector <String> symbols = new Vector <String> ();
		String [] toks = geneStr.split("[;]+");
		int index;
		String symbol;
		for(String tok: toks) {
			index = -1;
			tok = tok.trim();
			if(tok.startsWith("(")) {
				index = tok.indexOf(")");
				if(index == -1)
					continue;
				index++;
				tok = tok.substring(index).trim();
				//System.out.println("paren tok: "+ tok);
			}
			
			symbol = tok.split("[\\s]+")[0];				
			
			symbols.add(symbol);
		}
		
		return symbols;
	}
	
	private void countKeggDiseaseGenes(File keggFile) throws IOException {
		
	
		BufferedReader br = new BufferedReader(new FileReader(keggFile));
		String line;
		String [] toks;
		String diseaseID = "";
		String names = "";
		String genes = null;
		String category = "";
		String description = "";
		String currField = "";

		Vector <String> geneIDs;
		
		int diseaseCount = 0;
		long geneCount = 0;
		
		logger.info("Loading KEGG Disease: File Parse");
		
		while((line = br.readLine()) != null) {


			if(line.startsWith(" ")) {
				if(currField.equals("NAME")) {
					names += line.trim();
				} else if(currField.equals("GENE")) {
					genes += line.trim()+";";
				}
			} else {
				currField = "";
				if(line.startsWith("ENTRY")) {
					currField = "ENTRY";
					toks = line.split("[\\s]+");
					if(toks.length < 2) {
						System.out.println("bad id parse");
						continue;
					}
					diseaseID = toks[1];
				} else if(line.startsWith("NAME")) {
					currField = "NAME";
					line = line.replace("including:", "");
					names = line.substring(4).trim();
				} else if(line.startsWith("GENE")) {
					currField = "GENE";
					genes = line.substring(4).trim()+";";				
				} else if(line.startsWith("CATEGORY")) {
					currField = "CATEGORY";
					category = line.substring(9).trim();
				} else if(line.startsWith("DESCRIPTION")) {
					currField = "DESCRIPTION";
					description = line.substring(11).trim();
				} else if(line.startsWith("///")) {
					diseaseCount++;
					//end of record

					//if there are no associated genes, continue with next line
					if(genes == null)
						continue;

					//parse genes
					geneIDs = parseGenes(genes);
					geneCount += geneIDs.size();
					
					//parse symbols

					//set values

//					if(diseaseCount % 12 == 0) {
//						System.out.println(
//							//	"ID: " + diseaseID + "\n" +
//							//			"Names " + names + "\n" +
//							//			"Desc: " + description + "\n" +
//							//			"Category " + category + "\n" +
//										diseaseCount + " Genes " + genes //+ "\n" 
//							//			"*************************************"
//								);
//						Vector <String> g = parseGenes(genes);
//						System.out.println("Gene Count = "+g.size());
//						System.out.print("Genes: ");
//						for (String gene : g)
//							System.out.print(gene+ " ");
//						System.out.println();
//
//						Vector <String> s = parseGeneSymbols(genes);
//						System.out.println("Symbol Count = "+s.size());
//						System.out.print("Symbols: ");
//						for (String symbol : s)
//							System.out.print(symbol+ " ");
//						System.out.println();
//
//					}

					//insert

					//clear current values
					toks=null;
					names = "";				
					diseaseID = names = genes = category = description = null;
					currField = "";
				}
			}
		}
		logger.info("KEGG Gene Count = "+geneCount);
	}
	
	/**
	 * Maps kegg diseases to genes
	 */
	public void insertOrUpdateKeggDisease(String keggDiseaseFilePath) {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		BardKeggLoader loader = new BardKeggLoader();
		
		File file = new File("C:/Putty/disease");
		
		try {
			loader.countKeggDiseaseGenes(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

//		try {
//			loader.loadKeggDisease(file);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}

}
