package gov.nih.ncgc.bard.capextract;

import gov.nih.ncgc.bard.capextract.handler.CapResourceHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rajarshi Guha
 */
public class ScoreHandler extends CapResourceHandler {
    Connection conn;

    public ScoreHandler(Connection conn) {
        this.conn = conn;
    }

    public void updateScores(long bardExptId) throws SQLException {

        // determine assays associated with this experiment id
        PreparedStatement pst = conn.prepareStatement("select distinct bard_assay_id from bard_experiment where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        ResultSet rs = pst.executeQuery();
        List<Long> aids = new ArrayList<Long>();
        while (rs.next()) {
            aids.add(rs.getLong(1));
        }
        pst.close();

        // determine projects associated with this experiment id
        pst = conn.prepareStatement("select distinct bard_proj_id from bard_project_experiment where bard_expt_id = ?");
        pst.setLong(1, bardExptId);
        rs = pst.executeQuery();
        List<Long> pids = new ArrayList<Long>();
        while (rs.next()) {
            pids.add(rs.getLong(1));
        }
        pst.close();

        // for each assay id, find max score from all associated experiments
        pst = conn.prepareStatement("select distinct bard_expt_id, confidence_level from bard_experiment where bard_assay_id = ?");
        for (Long bardAssayId : aids) {
            pst.setLong(1, bardAssayId);
            rs = pst.executeQuery();
            float max = -1;
            while (rs.next()) {
                float conflevel = rs.getFloat(2);
                if (conflevel > max) max = conflevel;
            }
            pst.clearParameters();
            rs.close();

            // update assay score
            PreparedStatement update = conn.prepareStatement("update bard_assay set score = ? where bard_assay_id = ?");
            update.setInt(1, (int) max);
            update.setLong(2, bardAssayId);
            update.executeUpdate();
            update.close();
        }
        pst.close();
        log.info("## Update assay scores for bard experiment id " + bardExptId);


        // for each project, get the new project score and update
        PreparedStatement probe = conn.prepareStatement("select * from project_probe where bard_proj_id = ?");
        PreparedStatement confirm = conn.prepareStatement("select * from bard_project_experiment where bard_proj_id = ?");
        pst = conn.prepareStatement("select distinct a.bard_expt_id, a.confidence_level from bard_experiment a, bard_project_experiment b " +
                " where b.bard_proj_id = ? and a.bard_expt_id = b.bard_expt_id");
        for (Long bardProjId : pids) {

            // see whether we have a probe
            boolean hasProbe = false;
            probe.setLong(1, bardProjId);
            rs = probe.executeQuery();
            while (rs.next()) {
                hasProbe = rs.getString("probe_id") != null;
            }
            probe.clearParameters();
            rs.close();

            // see whether we have a confirmatory experiment
            boolean hasConfirmation = false;
            confirm.setLong(1, bardProjId);
            rs = confirm.executeQuery();
            while (rs.next()) {
                String type = rs.getString("expt_type");
                if (type != null && (type.equals("confirmatory assay") ||
                        type.equals("secondary assay") ||
                        type.equals("counter-screening assay") ||
                        type.equals("alternative confirmatory assay"))
                        ) {
                    hasConfirmation = true;
                    break;
                }
            }
            confirm.clearParameters();
            rs.close();

            // get all confidence scores
            pst.setLong(1, bardProjId);
            rs = pst.executeQuery();
            float avg = 0;
            int n = 0;
            while (rs.next()) {
                float conflevel = rs.getFloat(2);
                avg += conflevel;
                n++;
            }
            rs.close();
            pst.clearParameters();

            // calculate project score
            float projectScore = 0;
            avg /= n;
            if (avg >= 4) {
                if (hasProbe) projectScore = 4;
                else projectScore = 3;
            } else {
                if (hasProbe) projectScore = 3;
                else {
                    if (avg < 2) projectScore = avg;
                    else projectScore = 2;

                    if (hasConfirmation) projectScore += 0.5;
                }
            }

            // update project score
            PreparedStatement update = conn.prepareStatement("update bard_project set score = ? where bard_proj_id = ?");
            update.setFloat(1, projectScore);
            update.setLong(2, bardProjId);
            update.executeUpdate();
            update.close();

        }
        log.info("## Update project scores for bard experiment id " + bardExptId);
        pst.close();
        probe.close();
        confirm.close();
    }
}
