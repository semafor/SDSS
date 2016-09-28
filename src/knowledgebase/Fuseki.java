package knowledgebase;

import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;

/**
 * Expose a dataset accessor to the fuseki db. We also normalize the behaviour
 * of the app when it starts (populate both reasoner and KB db with version
 * info).
 * 
 *
 */
public class Fuseki
{
    private final String serviceURI;

    private final String service;

    public final DatasetAccessor accessor;

    public Fuseki(String service, String serviceURI) {
        this.serviceURI = serviceURI;
        this.service = service;
        accessor = DatasetAccessorFactory.createHTTP(serviceURI);

        // If the knowledge base was empty, create it and populate it with
        // something.
        if (!accessor.containsModel(Tms.Graph)) {
            Model kb = ModelFactory.createDefaultModel();
            Resource v = kb.createResource();
            v.addProperty(OWL.versionInfo, "1");
            accessor.putModel(Tms.Graph, kb);
        }

        // If the reasoner was empty, create it and populate it with something.
        if (!accessor.containsModel(Reasoner.Graph)) {
            Model rb = ModelFactory.createOntologyModel();
            Resource v = rb.createResource();
            v.addProperty(OWL.versionInfo, "1");
            accessor.putModel(Reasoner.Graph, rb);
        }
    }

    public String getServiceURI()
    {
        return serviceURI;
    }

    public String getService()
    {
        return service;
    }
}
