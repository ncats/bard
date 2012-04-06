package gov.nih.ncgc.bard.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Download latest Uniprot text dump and generate an Oracle SQL loading file.
 *
 * @author Rajarshi Guha
 */
public class PopulateTargets {

    String ofilename = "uniprot.sql";

    public PopulateTargets(String ofilename) {
        if (ofilename != null)
            this.ofilename = ofilename;
    }

    public void run() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("uniprot.sql"));
        writer.write("load data\n" +
                "infile *\n" +
                "append\n" +
                "into table protein_target\n" +
                "fields terminated by '\\t'\n" +
                "trailing nullcols\n" +
                "(accession, gene_id, name, taxid, description, uniprot_status)\n" +
                "begindata\n");

        URL uniprot = new URL("ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_sprot.dat.gz");
        InputStream gzipStream = new GZIPInputStream(uniprot.openStream());
        Reader reader = new InputStreamReader(gzipStream);
        BufferedReader breader = new BufferedReader(reader);
        String line = null;
        StringBuffer sb = new StringBuffer();
        int n = 0;
        while ((line = breader.readLine()) != null) {
            if (line.trim().equals("//")) {
                String acc = "", status = "", name = "", geneid = "", desc = "", taxid = "";
                String[] toks = sb.toString().split("\n");
                for (String aline : toks) {
                    if (aline.startsWith("ID")) {
                        status = aline.split("\\s+")[2].replace(";", "");
                    } else if (aline.startsWith("AC")) {
                        acc = aline.split(";")[0].trim().replace("AC   ", "");
                    } else if (aline.startsWith("DE   RecName:")) {
                        name = aline.split("=")[1].replace(";", "");
                    } else if (aline.startsWith("DR   GeneID;")) {
                        geneid = aline.split(";")[1].trim();
                    } else if (aline.startsWith("OX   ")) {
                        taxid = aline.trim().replace("OX   NCBI_TaxID=", "").replace(";", "");
                    }
                }
                writer.write(Util.join(new String[]{acc, geneid, name, taxid, desc, status}, "\t") + "\n");
                sb = new StringBuffer();

                n++;
                if (n % 100 == 0) System.out.print("\rProcessed " + n + " entries");
            } else {
                sb.append(line).append("\n");
            }
        }
        System.out.println();
    }

    public static void main(String[] args) throws IOException {
        String ofilename = null;
        if (args.length == 1) {
            ofilename = args[0];
        }
        PopulateTargets pt = new PopulateTargets(ofilename);
        pt.run();
    }
}
