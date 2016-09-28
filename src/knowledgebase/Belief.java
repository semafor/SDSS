package knowledgebase;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import vocabs.JTMS;

/**
 * Class representing a belief in the TMS.
 *
 */
public class Belief extends Base
{
    public Belief(Resource belief) {
        super(belief);
    }

    public boolean getHeld()
    {
        return r.getProperty(JTMS.HAS_STATE).getBoolean();
    }

    public boolean isPremise()
    {
        return r.hasProperty(RDF.type, JTMS.PREMISE);
    }

    public boolean isContradiction()
    {
        return r.hasProperty(RDF.type, JTMS.CONTRADICTION);
    }
}
