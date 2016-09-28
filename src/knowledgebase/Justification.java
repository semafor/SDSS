package knowledgebase;

import org.apache.jena.rdf.model.Resource;

/**
 * Represents a justification in the TMS.
 * 
 */
public class Justification extends Base
{
    private boolean holds;

    public Justification(Resource justification) {
        super(justification);
    }

    public Resource getResource()
    {
        return r;
    }

    public void setHolds(boolean holds)
    {
        this.holds = holds;
    }

    public boolean getHolds()
    {
        return holds;
    }
}
