package interfaces;

import knowledgebase.Belief;
import knowledgebase.Justification;

/**
 * Normalizes behaviour of a model subscriber.
 * 
 *
 */
public interface ModelSubscriber
{
    public void handleBeliefUpdate(Belief belief);

    public void handleJustificationUpdate(Justification justification);
}
