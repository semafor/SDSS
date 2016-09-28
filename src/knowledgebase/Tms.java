package knowledgebase;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;

import vocabs.JTMS;
import vocabs.PROV;

/**
 * The TMS is the Truth Maintenance System. Here, the user interacts with the
 * knowledge base. The knowledge base is exclusively user input.
 *
 *
 */
public class Tms
{
    private final Fuseki db;
    private final Model knowledgeBase;
    public static final String NS = "http://apollo.nasa.gov/KB#";
    public static final String Graph = "http://apollo.nasa.gov/KnowledgeBase";

    /**
     * @param Initialize
     *            the knowledge base using this Fuseki instance.
     */
    public Tms(Fuseki db) {
        this.db = db;
        // knowledgeBase = db.ds.getNamedModel(Graph);
        knowledgeBase = db.accessor.getModel(Graph);
    }

    /**
     * @return the Model representing the knowledge base.
     */
    public Model getKnowledgeBase()
    {
        return knowledgeBase;
    }

    /**
     * Handler for changes. Currently updates the db.
     */
    public void changed()
    {
        // Only put the model if it contains anything
        db.accessor.putModel(Graph, knowledgeBase);
    }

    public void addBelief(String resourceIri)
    {
        if (resourceIri.isEmpty()) {
            System.out.println("Ignoring empty belief IRI");
            return;
        }
        String insert = String.format("INSERT DATA { <%s> a <%s> }",
                resourceIri, JTMS.BELIEF);

        UpdateRequest request = UpdateFactory.create();
        request.add(insert);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public void removeBelief(Belief b)
    {
        String deleteProps = String.format("DELETE WHERE { <%s> ?p ?o }",
                b.getResource());
        String deleteRefs = String.format("DELETE WHERE { ?s ?p <%s> }",
                b.getResource());
        UpdateRequest request = UpdateFactory.create();
        request.add(deleteProps).add(deleteRefs);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    /**
     * Remove a justification. Will delete every reference and property related
     * to this justification.
     * 
     * @param j
     *            - Justification to remove
     */
    public void removeJustification(Justification j)
    {
        String deleteProps = String.format("DELETE WHERE { <%s> ?p ?o }",
                j.getResource());
        String deleteRefs = String.format("DELETE WHERE { ?s ?p <%s> }",
                j.getResource());
        UpdateRequest request = UpdateFactory.create();
        request.add(deleteProps).add(deleteRefs);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    /**
     * Add justification. Note that Justifications can be added via inference.
     * 
     * @param resourceIri to add
     */
    public void addJustification(String resourceIri)
    {
        String insert = String.format("INSERT DATA { <%s> a <%s> }",
                resourceIri, JTMS.JUSTIFICATION);
        UpdateRequest request = UpdateFactory.create();
        request.add(insert);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    /**
     * @param justificationIri that will justify 
     * @param beliefIri to be justified
     */
    public void justifies(String justificationIri, String beliefIri)
    {
        String insert = String.format("INSERT DATA { <%s> <%s> <%s> }",
                justificationIri, JTMS.JUSTIFIES, beliefIri);
        UpdateRequest request = UpdateFactory.create();
        request.add(insert);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    /**
     * @param beliefIri to add to IN
     * @param justificationIri of which we will add to IN
     */
    public void addToIn(String beliefIri, String justificationIri)
    {
        addToIn(beliefIri, justificationIri, false);
    }

    /**
     * @param makePremise whether or not it's a premise
     */
    public void addToIn(String beliefIri, String justificationIri,
            boolean makePremise)
    {
        String addIn = String.format("INSERT DATA { <%s> <%s> <%s> }",
                justificationIri, JTMS.HAS_SUPPORT, beliefIri);
        UpdateRequest request = UpdateFactory.create();
        request.add(addIn);
        UpdateAction.execute(request, knowledgeBase);

        if (makePremise) {
            premise(beliefIri);
        }

        changed();
    }

    /**
     * Remove some belief from a justification's IN list.
     * 
     * @param belief
     *            - the local name of the belief (e.g. batteryOk)
     * @param justification
     *            - the local name of the justification (e.g. havePower)
     */
    public void removeFromIn(Belief b, Justification j)
    {
        String removeIn = String.format("DELETE DATA { <%s> <%s> <%s> }",
                j.getResource(), JTMS.HAS_SUPPORT, b.getResource());
        UpdateRequest request = UpdateFactory.create();
        request.add(removeIn);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public void addToOut(String beliefIri, String justificationIri)
    {
        String addOut = String.format("INSERT DATA { <%s> <%s> <%s> }",
                justificationIri, JTMS.HAS_OPPOSITION, beliefIri);
        UpdateRequest request = UpdateFactory.create();
        request.add(addOut);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    /**
     * Remove some belief to a justification's OUT list.
     * 
     * @param belief
     *            - the local name of the belief (e.g. batteryOk)
     * @param justification
     *            - the local name of the justification (e.g. havePower)
     */
    public void removeFromOut(Belief b, Justification j)
    {
        String removeOut = String.format("DELETE DATA { <%s> <%s> <%s> }",
                j.getResource(), JTMS.HAS_OPPOSITION, b.getResource());
        UpdateRequest request = UpdateFactory.create();
        request.add(removeOut);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public void premise(String beliefIri)
    {
        String query = String.format("INSERT DATA { <%s> a <%s> }", beliefIri,
                JTMS.PREMISE);

        UpdateRequest request = UpdateFactory.create();
        request.add(query);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public void unpremise(String beliefIri)
    {
        String query = String.format("DELETE DATA { <%s> a <%s> }", beliefIri,
                JTMS.PREMISE);

        UpdateRequest request = UpdateFactory.create();
        request.add(query);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public void contradict(String beliefIri)
    {
        String query = String.format("INSERT DATA { <%s> a <%s> }", beliefIri,
                JTMS.CONTRADICTION);

        UpdateRequest request = UpdateFactory.create();
        request.add(query);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public void uncontradict(String beliefIri)
    {
        String query = String.format("DELETE DATA { <%s> a <%s> }", beliefIri,
                JTMS.CONTRADICTION);

        UpdateRequest request = UpdateFactory.create();
        request.add(query);
        UpdateAction.execute(request, knowledgeBase);

        changed();
    }

    public List<Statement> getStatements(Belief b)
    {
        List<Statement> list = new ArrayList<Statement>();

        // Find all seeAlso triples for our belief b, then add the properties
        // of the seeAlso objects to the list of statements.
        StmtIterator seeAlsoIt = b.getResource().listProperties(RDFS.seeAlso);
        while (seeAlsoIt.hasNext()) {
            System.out.println("had seealso");
            Statement see = seeAlsoIt.next();
            StmtIterator seeProps = see.getObject().asResource()
                    .listProperties();
            while (seeProps.hasNext()) {
                list.add(seeProps.next());
            }
        }
        return list;
    }

    /**
     * @return all statements in the KB
     */
    public List<Statement> getStatements()
    {
        List<Statement> list = new ArrayList<Statement>();
        StmtIterator iter = knowledgeBase.listStatements();
        while (iter.hasNext()) {
            list.add(iter.nextStatement());
        }
        return list;
    }

    /**
     * Return statements not in the knowledge base, but that pertains to the
     * situation (apollo mission).
     * 
     * @return all auxiliary statements
     */
    public List<Statement> getAuxiliaryStatements()
    {
        List<Statement> list = new ArrayList<Statement>();

        String all = String.format("SELECT ?s ?p ?o"
                + " FROM <src/data/spacelog.ttl>" + " WHERE { ?s ?p ?o . } ");
        Query query = QueryFactory.create(all);
        QueryExecution qexec = QueryExecutionFactory.create(query);
        ResultSet rSet = qexec.execSelect();
        while (rSet.hasNext()) {
            QuerySolution soln = rSet.next();
            Resource subj = soln.get("s").asResource();
            Property pred = ResourceFactory.createProperty(soln.get("p")
                    .toString());
            RDFNode obj = soln.get("o");
            list.add(ResourceFactory.createStatement(subj, pred, obj));
        }
        qexec.close();

        return list;
    }

    public void addStatement(Belief b, List<String> triple)
    {
        // Insert the statement, as well as a link to that statement.
        System.out.println("Adding to KB:" + triple.toString());
        String addStatement = String.format("INSERT DATA { <%s> <%s> <%s> }",
                triple.get(0), triple.get(1), triple.get(0));
        String linkStatement = String.format("INSERT DATA { <%s> <%s> <%s> }",
                b.getResource(), RDFS.seeAlso, triple.get(0));
        UpdateRequest request = UpdateFactory.create();
        request.add(addStatement).add(linkStatement);
        UpdateAction.execute(request, knowledgeBase);

        // Insert all other statements relating to the subject of the inserted
        // statement.
        System.out.println("making req");
        String addAllStatements = String.format("SELECT ?p ?o"
                + " FROM <src/data/spacelog.ttl> WHERE { <%s> ?p ?o }",
                triple.get(0));
        Query query = QueryFactory.create(addAllStatements);
        QueryExecution qexec = QueryExecutionFactory.create(query);
        ResultSet rSet = qexec.execSelect();

        for (; rSet.hasNext();) {
            QuerySolution soln = rSet.next();
            Resource res = ResourceFactory.createResource(triple.get(0));
            Property pred = ResourceFactory.createProperty(soln.get("p")
                    .toString());
            RDFNode obj = soln.get("o");
            knowledgeBase.add(ResourceFactory.createStatement(res, pred, obj));
        }
        qexec.close();

        // If provenance is provided and it was used to make an attribution,
        // we will fetch relevant triples for this resource using dbpedia.
        Resource added = knowledgeBase.getResource(triple.get(0));
        if (added.hasProperty(PROV.WAS_ATTRIBUTED_TO)) {
            System.out.println("added thing had prov "
                    + added.getProperty(PROV.WAS_ATTRIBUTED_TO).getObject()
                            .toString());
            fetchFromDbpedia(added.getProperty(PROV.WAS_ATTRIBUTED_TO)
                    .getObject().toString());
        }

        changed();
    }

    /**
     * Attempt to populate the knowledge base with information about the given
     * resource from dbpedia. In our case, we'll do a federated query based on
     * 1) the given resource, 2) the nasa mission data, 3) dbpedia.
     * 
     * @param resource
     *            to fetch information about from dbpedia
     */
    public void fetchFromDbpedia(String resource)
    {

        System.out.println("fetchFromDbpedia will try " + resource + "...");

        // Use the nasa mission triples to fetch data from dbpedia via a
        // federated query. Note we're using all the data sets we have, which
        // are lifted spacelog and nasa mission triples.
        // Results are filtered to English only.
        String nasaSameAs = String.format(
                "SELECT ?s ?p ?o FROM <src/data/nasa.n3> FROM <src/data/spacelog.ttl>"
                        + " WHERE { <%s> (<%s>|^<%s>)* ?s . "
                        + " SERVICE <http://dbpedia.org/sparql> { ?s ?p ?o } "
                        + " FILTER(langMatches(lang(?o), \"EN\"))" + " }"
                        + " LIMIT 300", resource, OWL.sameAs, OWL.sameAs);
        Query query = QueryFactory.create(nasaSameAs);
        QueryExecution qexec = QueryExecutionFactory.create(query);
        ResultSet rSet = qexec.execSelect();
        while (rSet.hasNext()) {
            QuerySolution soln = rSet.next();
            Resource res = soln.get("s").asResource();
            Property pred = ResourceFactory.createProperty(soln.get("p")
                    .toString());
            RDFNode o = soln.get("o");

            // Create the dbpedia resource in KB.
            knowledgeBase
                    .add(ResourceFactory.createResource(resource), pred, o);

            // Internalize linking in knowledgeBase
            knowledgeBase.add(res, OWL.sameAs, resource);
        }
        qexec.close();

        changed();
    }
}
