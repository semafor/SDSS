package interfaces;

import java.util.List;
import java.util.Map;

import knowledgebase.Belief;
import knowledgebase.Justification;

import org.apache.jena.rdf.model.Resource;

import ui.ViewType;

/**
 * Interface for view subscribers.
 *
 */
public interface ViewSubscriber
{
    public void handleTmsGraphClick(Map<String, Object> map, String id);

    public void handleTmsGraphClickEmpty();

    public void handleBeliefTypeChanged(Belief b, Resource newType);

    public void handleJustificationJustifiesBelief(Justification j,
            String beliefIri, ViewType t);

    public void handleAddBelief(String beliefIri);

    public void handleAddJustification(String justificationIri);

    public void handleRemoveInBelief(Belief b, Justification j, ViewType type);

    public void handleAddInBelief(String beliefIri, Justification j,
            ViewType type);

    public void handleRemoveOutBelief(Belief b, Justification j, ViewType type);

    public void handleRemoveBelief(Belief b, ViewType type);

    public void handleRemoveJustification(Justification j, ViewType type);

    public void handleAddOutBelief(String beliefIri, Justification j,
            ViewType type);

    public void handleAddDoc(Belief b, List<String> triple, ViewType type);
}
