package gov.nih.ncgc.bard.capextract;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

/**
 * The CAP dictionary represented as a directed graph.
 * <p/>
 * Each node is a {@link CAPDictionaryElement} and has two sets of edges,
 * incoming (analogous to parent relationships) and outgoing (analogous to
 * child relationships). A node is considered a <i>leaf</i> if it has no
 * outgoing edges.
 *
 * @author Rajarshi Guha
 */
public class CAPDictionary implements Serializable {
    static final long serialVersionUID = 5435103181432997247L;

    Set<CAPDictionaryElement> nodes;
    Set<Edge> inEdges, outEdges;

    public CAPDictionary() {
        nodes = new HashSet<CAPDictionaryElement>();
        outEdges = new HashSet<Edge>();
        inEdges = new HashSet<Edge>();
    }

    public void addNode(CAPDictionaryElement node) {
        nodes.add(node);
    }

    /**
     * Create an edge in the graph.
     *
     * @param parent a node (analogous to parent). If node does not exists in the node list, it is added.
     * @param child  a node (analogous to child). If node does not exists in the node list, it is added.
     * @param data   arbitrary data to be associated with the outgoing edge
     */
    public void addOutgoingEdge(CAPDictionaryElement parent, CAPDictionaryElement child, Object data) {
        if (!nodes.contains(parent)) addNode(parent);
        if (!nodes.contains(child)) addNode(child);
        outEdges.add(new Edge(parent, child, data));
    }

    public void addIncomingEdge(CAPDictionaryElement child, CAPDictionaryElement parent, Object data) {
        if (!nodes.contains(child)) addNode(child);
        if (!nodes.contains(parent)) addNode(parent);
        inEdges.add(new Edge(child, parent, data));
    }

    public Set<CAPDictionaryElement> getNodes() {
        return nodes;
    }

    public CAPDictionaryElement getNode(String label) {
        for (CAPDictionaryElement node : nodes) {
            if (node.getLabel().equals(label)) return node;
        }
        return null;
    }

    public CAPDictionaryElement getNode(BigInteger id) {
        for (CAPDictionaryElement node : nodes) {
            if (node.getElementId().equals(id)) return node;
        }
        return null;
    }

    public Set<CAPDictionaryElement> getChildren(BigInteger nodeId) {
        return getChildren(getNode(nodeId));
    }

    /**
     * Is this a lead node (i.e., no child nodes).
     *
     * @param node the node in question
     * @return true if a lead node, false otherwise
     */
    public boolean isLeaf(CAPDictionaryElement node) {
        return getChildren(node).size() == 0;
    }

    public Set<CAPDictionaryElement> getChildren(CAPDictionaryElement node) {
        // just look at the outgoing edges
        Set<CAPDictionaryElement> children = new HashSet<CAPDictionaryElement>();
        for (Edge e : outEdges) {
            if (e.from.equals(node)) children.add(e.to);
        }
        return children;
    }

    public Set<CAPDictionaryElement> getParents(BigInteger nodeId) {
        return getParents(getNode(nodeId));
    }

    public Set<CAPDictionaryElement> getParents(CAPDictionaryElement node) {
        // just look at the outgoing edges
        Set<CAPDictionaryElement> parents = new HashSet<CAPDictionaryElement>();
        for (Edge e : inEdges) {
            if (e.from.equals(node)) parents.add(e.to);
        }
        return parents;
    }


    public int size() {
        return nodes.size();
    }


    class Edge implements Serializable {
        static final long serialVersionUID = -964324348571286627L;

        CAPDictionaryElement from, to;
        Object data;

        Edge(CAPDictionaryElement from, CAPDictionaryElement to) {
            this.from = from;
            this.to = to;
            data = null;
        }

        Edge(CAPDictionaryElement from, CAPDictionaryElement to, Object data) {
            this.from = from;
            this.to = to;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge)) return false;
            Edge e = (Edge) o;
            return e.from == from && e.to == to && e.data == data;
        }
    }
}