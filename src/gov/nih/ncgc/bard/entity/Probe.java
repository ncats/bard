package gov.nih.ncgc.bard.entity;

import gov.nih.ncgc.bard.rest.BARDConstants;

/**
 * Representation of a chemical probe.
 * <p/>
 * Since a probe is just a label applied to a compound, this class simply extends
 * {@link Compound} and does not anything else. It is primarily to indicate cases
 * where we are dealing with probe compounds rather than compounds in general, allowing
 * us to reuse the infrastructure that is based on entity class.
 *
 * @author Rajarshi Guha
 */
public class Probe extends Compound {

    public Probe(Compound c) {
        this.cid = c.cid;
        this.probeId = c.probeId;
    }

    public String getResourcePath() {
        return BARDConstants.API_BASE + "/probes/" + probeId;
    }
}
