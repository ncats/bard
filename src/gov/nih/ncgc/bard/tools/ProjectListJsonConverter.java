package gov.nih.ncgc.bard.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncgc.bard.entity.Assay;
import gov.nih.ncgc.bard.entity.BardLinkedEntity;
import gov.nih.ncgc.bard.entity.Experiment;
import gov.nih.ncgc.bard.entity.Project;
import gov.nih.ncgc.bard.entity.Publication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ProjectListJsonConverter implements IJsonConverter<BardLinkedEntity> {

    @Override
    public JsonNode convert(BardLinkedEntity o) throws Exception {
        DBUtils db = new DBUtils();
        ObjectMapper mapper = new ObjectMapper();
        if (!(o.getCollection() instanceof Collection)) throw new IllegalArgumentException("Must supply an object of type BardLinkedEntity");
        Collection coll = (Collection) o.getCollection();

        JsonNode list = mapper.valueToTree(o);
        ArrayNode items = mapper.createArrayNode();

        for (Object item : coll) {
            if (!(item instanceof Project)) throw new IllegalArgumentException("The BardLinkedEntity must contain Project objects as elements");
            Project p = (Project) item;

            List<Assay> assays = new ArrayList<Assay>();
            for (Long aid : p.getAids())
                assays.add(db.getAssayByAid(aid));

            List<Experiment> expts = new ArrayList<Experiment>();
            for (Long eid : p.getEids())
                expts.add(db.getExperimentByExptId(eid));

            List<Publication> pubs = new ArrayList<Publication>();
            for (Long pmid : p.getPublications())
                pubs.add(db.getPublicationByPmid(pmid));

            ArrayNode an = mapper.createArrayNode();
            for (Assay assay : assays) {
                an.add(mapper.valueToTree(assay));
            }
            ArrayNode en = mapper.createArrayNode();
            for (Experiment expt : expts) {
                en.add(mapper.valueToTree(expt));
            }
            ArrayNode pn = mapper.createArrayNode();
            for (Publication pub : pubs) {
                pn.add(mapper.valueToTree(pub));
            }

            JsonNode tree = mapper.valueToTree(p);
            ((ObjectNode)tree).put("eids", en);
            ((ObjectNode)tree).put("aids", an);
            ((ObjectNode)tree).put("publications", pn);

            items.add(tree);
        }
        ((ObjectNode)list).put("collection", items);
        db.closeConnection();
        return list;
    }
}
