package knowledgebase;

import org.apache.jena.rdf.model.Resource;

/**
 * Base class for justifications and beliefs. Normalizes access to resource and
 * the local name.
 * 
 *
 */
public class Base
{
    protected final Resource r;

    public Base(Resource r) {
        this.r = r;
    }

    public Resource getResource()
    {
        return r;
    }

    public String getName()
    {
        return r.getLocalName();
    }
}
