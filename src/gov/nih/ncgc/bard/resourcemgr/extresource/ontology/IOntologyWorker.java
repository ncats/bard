package gov.nih.ncgc.bard.resourcemgr.extresource.ontology;

public interface IOntologyWorker {
	public Object getSuccessorNodes(Object focusNode);
	public Object getAncestorNodes(Object focusNode);
}
