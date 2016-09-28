package knowledgebase;

import interfaces.ModelPublisher;
import interfaces.ModelSubscriber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import vocabs.JTMS;

/**
 * This is the reasoner. When the knowledge base (TMS) is changed, the reasoner
 * is able to answer questions about belief and justifications, through
 * inference.
 * 
 */
public final class Reasoner implements ModelSubscriber, ModelPublisher
{
    private final Fuseki db;
    private Model inferredModel;
    private static Model reasonerDb;
    private final List<ModelSubscriber> modelSubscribers = new ArrayList<ModelSubscriber>();
    private final org.apache.jena.reasoner.Reasoner reasoner;
    public static final String Graph = "http://apollo.nasa.gov/Reasoner";
    private final HashMap<Resource, Belief> resourceToBelief = new HashMap<Resource, Belief>();
    private final HashMap<Resource, Justification> resourceToJustification = new HashMap<Resource, Justification>();

    /**
     * The report of the last, failed inference. Is set to null every time
     * inference is successful.
     */
    private ValidityReport lastReport;

    public Reasoner(Fuseki db) {
        this.db = db;
        reasoner = ReasonerRegistry.getOWLMicroReasoner().bindSchema(
                JTMS.getSchema());

        reasonerDb = db.accessor.getModel(Graph);
    }

    private boolean updateInferences(Model knowledgeBase)
    {
        InfModel inf = ModelFactory.createInfModel(reasoner, knowledgeBase);

        ValidityReport rep = inf.validate();

        // Check if the validation was successful. If not, we produce an error
        // report.
        if (rep.isValid()) {
            lastReport = null;
            inferredModel = inf;
            reasonerDb.removeAll();

            reasonerDb.add(knowledgeBase.listStatements());
            reasonerDb.add(inferredModel.listStatements());

            db.accessor.putModel(Graph, reasonerDb);
            return true;
        } else {
            lastReport = rep;
            return false;
        }
    }

    /**
     * Make sure all Justifications and Beliefs in the knowledgebase are
     * represented in the reasoner.
     */
    private void updateCachedResources()
    {
        // Cache justifications.
        String justfQ = String.format("SELECT ?j WHERE { ?j a <%s> }",
                JTMS.JUSTIFICATION);
        for (Resource jRes : querySingle(justfQ, "j")) {
            if (!resourceToJustification.containsKey(jRes)) {
                cacheJustification(jRes);
            }
        }

        // Cache beliefs.
        String beliefQ = String.format("SELECT ?b WHERE { ?b a <%s> }",
                JTMS.BELIEF);
        for (Resource bRes : querySingle(beliefQ, "b")) {
            if (!resourceToBelief.containsKey(bRes)) {
                cacheBelief(bRes);
            }
        }

        // Propagate beliefs, from the leaves.
        for (Belief b : getLeafBeliefs()) {
            propagateBelief(b);
        }

        // For any leaf justifications, we'll mark all its consequents as
        // believed. A leaf justification always holds.
        for (Justification jLeaf : getJustifications()) {
            if (!jLeaf.getResource().hasProperty(JTMS.HAS_ANTECEDENT)) {
                StmtIterator ji = jLeaf.getResource().listProperties(
                        JTMS.JUSTIFIES);
                while (ji.hasNext()) {
                    Resource jr = ji.next().getResource();
                    if (resourceToBelief.containsKey(jr)) {
                        Belief b = resourceToBelief.get(jr);
                        setBeliefState(b, true);
                        propagateBelief(b);
                    }
                }
            }
        }
    }

    private void clearCachedResources()
    {
        resourceToBelief.clear();
        resourceToJustification.clear();
    }

    private List<Resource> querySingle(String sparql, String var)
    {
        return querySingle(sparql, var, reasonerDb);
    }

    /**
     * Provide an easy way to query the reasoner.
     * 
     * @param sparql
     *            to execute
     * @param var
     *            to return
     * @param m
     *            model to query
     * @return a list of resources produced by the query
     */
    private List<Resource> querySingle(String sparql, String var, Model m)
    {
        Query query = QueryFactory.create(sparql);
        List<Resource> list = new ArrayList<Resource>();
        QueryExecution qexec = QueryExecutionFactory.create(query, m);
        ResultSet rSet = qexec.execSelect();

        for (; rSet.hasNext();) {
            QuerySolution soln = rSet.next();
            Node n = soln.get(var).asNode();
            Resource res = reasonerDb.createResource(n.getURI());
            list.add(res);
        }
        qexec.close();
        return list;
    }

    /**
     * A recursive propagation of the TMS tree from bottom to up.
     * 
     * @param belief
     */
    public void propagateBelief(Belief belief)
    {
        // List of Beliefs now OUT or IN.
        List<Belief> newOutBeliefs = new ArrayList<Belief>();
        List<Belief> newInBeliefs = new ArrayList<Belief>();

        // Walk up the justification tree and update beliefs in the parent
        // justifcations.
        List<Justification> consequences = castToJustification(getConsequences(belief));
        for (Justification justification : consequences) {
            boolean holds = holds(justification);
            List<Belief> consequents = castToBeliefs(getConsequents(justification));
            for (Belief aBelief : consequents) {
                if (holds) {
                    // We can immediately mark this belief as held, since
                    // any single justification will do.
                    newInBeliefs.add(aBelief);
                } else {
                    // We need to check this beliefs list of justifications,
                    // and see if we can find one that holds.
                    boolean aHolds = false;
                    for (Justification aJust : castToJustification(getJustifications(aBelief))) {
                        if (holds(aJust)) {
                            aHolds = true;
                        }
                    }

                    if (!aHolds) {
                        newOutBeliefs.add(aBelief);
                    }

                }
            }
        }

        // XXX: here be recursion.
        for (Belief b : newOutBeliefs) {
            setBeliefState(b, false);
            propagateBelief(b);
        }
        for (Belief b : newInBeliefs) {
            setBeliefState(b, true);
            propagateBelief(b);
        }
    }

    /**
     * Take a knowledgeBase and update the reasoner. Prints out errors from an
     * error report should inference fail.
     * 
     * @param knowledgeBase
     */
    public void update(Model knowledgeBase)
    {
        if (updateInferences(knowledgeBase)) {
            clearCachedResources();
            updateCachedResources();
            db.accessor.putModel(Graph, reasonerDb);
        } else {
            System.out.println("Updated Reasoner, but there were conflicts:");
            for (Iterator<?> i = lastReport.getReports(); i.hasNext();) {
                System.out.println(" - " + i.next());
            }
            // Don't process anything if inference fails.
            return;
        }
    }

    /**
     * @return a report on the most recent failed inference
     */
    public ValidityReport getLastReport()
    {
        return lastReport;
    }

    public Belief cacheBelief(Resource resource)
    {
        Belief b = new Belief(resource);
        setBeliefState(b, false);
        resourceToBelief.put(resource, b);
        return b;
    }

    /**
     * A belief is justified if it is justified by a Justification that holds. A
     * Justification holds if all IN-beliefs are justified, and all OUT-beliefs
     * are not justified. A belief's state is true if: - It is a premise - It is
     * justified. False if: - It is a contradiction. - It is not justified.
     * 
     * These terms are loosely based on Doyle ‎1979.
     * 
     * @param b
     *            to ask
     * @return state of belief
     */
    public boolean getState(Belief b)
    {
        // Is this a Premise, if so, believe it no matter what.
        if (b.isPremise()) {
            return true;
        }

        // If a Contradiction, never believe it.
        if (b.isContradiction()) {
            return false;
        }

        // Check all justifications of Belief. A Belief holds if some
        // justification of it holds.
        for (Justification j : castToJustification(getJustifications(b))) {
            if (holds(j)) {
                return true;
            }
        }

        // Empty justification list means that the belief is not held.
        return false;
    }

    /**
     * @param b
     *            to ask
     * @return whether or not b is in a justification list
     */
    public boolean isInSomeJustificationList(Belief b)
    {
        return b.getResource().hasProperty(JTMS.SUPPORTS)
                || b.getResource().hasProperty(JTMS.OPPOSES);
    }

    /**
     * @return justifications which have this belief as an antecedent
     */
    public List<Resource> getConsequences(Belief b)
    {
        // Select all Justifications which have this belief as an antecedent
        String q = String.format("SELECT ?o WHERE { ?o <%s> <%s> }",
                JTMS.HAS_ANTECEDENT, b.getResource());
        return querySingle(q, "o");
    }

    /**
     * @return justifications that have this belief as a consequent
     */
    public List<Resource> getJustifications(Belief b)
    {
        // Select all Justifications which have this belief as a consequent
        String q = String.format("SELECT ?j WHERE { ?j <%s> <%s> }",
                JTMS.JUSTIFIES, b.getResource());
        return querySingle(q, "j");
    }

    public Justification cacheJustification(Resource resource)
    {
        Justification js = new Justification(resource);
        resourceToJustification.put(resource, js);
        js.setHolds(holds(js));
        return js;
    }

    /**
     * A Justification holds if: - Both IN and OUT lists are empty - or all
     * IN-list beliefs are justified AND all OUT-list beliefs are not justified.
     * 
     * @param j
     *            to ask
     * @return whether or not j holds
     */
    public boolean holds(Justification j)
    {
        boolean inlistHaveNotHeldBeliefs = ask(String
                .format("ASK { <%s> <%s> ?inBelief . ?inBelief <%s> false ."
                        + " FILTER NOT EXISTS { ?inBelief a <%s> } }",
                        j.getResource(), JTMS.HAS_SUPPORT, JTMS.HAS_STATE,
                        JTMS.PREMISE));
        boolean inlistHaveContradictions = ask(String.format(
                "ASK { <%s> <%s> ?inBelief . ?inBelief a <%s> . }",
                j.getResource(), JTMS.HAS_SUPPORT, JTMS.CONTRADICTION));

        boolean outlistHaveHeldBeliefs = ask(String.format(
                "ASK { <%s> <%s> ?outBelief . ?outBelief <%s> true ."
                        + " FILTER NOT EXISTS { ?outBelief a <%s> } }",
                j.getResource(), JTMS.HAS_OPPOSITION, JTMS.HAS_STATE,
                JTMS.CONTRADICTION));
        boolean outlistHavePremise = ask(String.format(
                "ASK { <%s> <%s> ?outBelief . ?outBelief a <%s> . }",
                j.getResource(), JTMS.HAS_OPPOSITION, JTMS.PREMISE));

        return !inlistHaveContradictions && !inlistHaveNotHeldBeliefs
                && !outlistHavePremise && !outlistHaveHeldBeliefs;
    }

    /**
     * Helper to perform asks.
     * 
     * @param query
     *            to execute
     * @return yes/no in bool form
     */
    private boolean ask(String query)
    {
        Query askQ = QueryFactory.create(query);
        QueryExecution askQE = QueryExecutionFactory.create(askQ, reasonerDb);
        boolean solution = askQE.execAsk();
        askQE.close();
        return solution;
    }

    /**
     * An antecedent is some belief that a Justification justifies.
     * 
     * @param j
     *            to ask
     * @return antecedents of j
     */
    public List<Resource> getAntecedents(Justification j)
    {
        String q = String.format("SELECT ?belief WHERE { ?belief <%s> <%s> }",
                JTMS.ANTECEDENT_OF, j.getResource());
        return querySingle(q, "belief");
    }

    /**
     * A consequent is some belief in either the IN or OUT list of a
     * Justification.
     * 
     * @param j
     *            to ask
     * @return consequents of j
     */
    public List<Resource> getConsequents(Justification j)
    {
        String q = String.format("SELECT ?belief WHERE { ?belief <%s> <%s> }",
                JTMS.JUSTIFIED_BY, j.getResource());
        return querySingle(q, "belief");
    }

    /**
     * @return all cached beliefs
     */
    public List<Belief> getBeliefs()
    {
        List<Belief> beliefs = new ArrayList<Belief>();

        for (Belief b : resourceToBelief.values()) {
            beliefs.add(b);
        }
        return beliefs;
    }

    /**
     * @return all cached beliefs that are leaves in the TMS tree (i.e. they are
     *         not consequents).
     */
    public List<Belief> getLeafBeliefs()
    {
        List<Belief> leaves = new ArrayList<Belief>();

        for (Belief leaf : getBeliefs()) {
            if (!leaf.getResource().hasProperty(JTMS.JUSTIFIED_BY)) {
                leaves.add(leaf);
            }
        }

        return leaves;
    }

    /**
     * @return all cached justifications
     */
    public List<Justification> getJustifications()
    {
        List<Justification> justifications = new ArrayList<Justification>();

        for (Justification j : resourceToJustification.values()) {
            justifications.add(j);
        }
        return justifications;
    }

    /**
     * @return IN-list edges
     */
    public List<Pair<Justification, Belief>> getSupportListEdges()
    {
        List<Pair<Justification, Belief>> edges = new ArrayList<Pair<Justification, Belief>>();

        for (Justification j : getJustifications()) {
            List<Belief> antecedents = castToBeliefs(getAntecedents(j));
            for (Belief b : antecedents) {

                Pair<Justification, Belief> p = new MutablePair<Justification, Belief>(
                        j, b);
                edges.add(p);
            }
        }
        return edges;
    }

    public Model getInferredModel()
    {
        return reasonerDb;
    }

    /**
     * @return justification edges
     */
    public List<Pair<Belief, Justification>> getJustificationEdges()
    {
        List<Pair<Belief, Justification>> edges = new ArrayList<Pair<Belief, Justification>>();

        for (Belief b : getBeliefs()) {
            List<Justification> justifications = castToJustification(getJustifications(b));
            for (Justification j : justifications) {
                Pair<Belief, Justification> p = new MutablePair<Belief, Justification>(
                        b, j);
                edges.add(p);
            }
        }
        return edges;
    }

    public List<Belief> castToBeliefs(List<Resource> list)
    {
        List<Belief> l = new ArrayList<Belief>();
        for (Resource n : list) {
            l.add(resourceToBelief.get(n));
        }
        return l;
    }

    public List<Justification> castToJustification(List<Resource> list)
    {
        List<Justification> l = new ArrayList<Justification>();
        for (Resource n : list) {
            l.add(resourceToJustification.get(n));
        }
        return l;
    }

    @Override
    public void handleBeliefUpdate(Belief belief)
    {
        propagateBelief(belief);
    }

    @Override
    public void handleJustificationUpdate(Justification justification)
    {
        // If we got new INs, let's propagate all our antecedents
        for (Resource b : getAntecedents(justification)) {
            propagateBelief(resourceToBelief.get(b));
        }

        for (ModelSubscriber s : modelSubscribers) {
            s.handleJustificationUpdate(justification);
        }
    }

    /**
     * @param b
     *            to ask
     * @return justification status of b in human readable form
     */
    public String getJustificationStatus(Belief b)
    {
        List<String> reasons = new ArrayList<String>();
        for (Justification js : castToJustification(getJustifications(b))) {
            if (!holds(js)) {
                reasons.add(js.getResource().getLocalName());
            }
        }
        if (reasons.size() == 0) {
            return "is justified";
        } else {
            String s = "not justified due to: ";
            s += StringUtils.join(reasons, ",");
            return s;
        }
    }

    /**
     * @param j
     *            to ask
     * @return status of j in human readable form
     */
    public String getStatus(Justification j)
    {
        List<String> ins = new ArrayList<String>();
        List<String> outs = new ArrayList<String>();
        for (Belief iB : getInList(j)) {
            if (!getState(iB)) {
                ins.add("“" + iB.getName() + "”");
            }
        }

        for (Belief oB : getOutList(j)) {
            if (getState(oB)) {
                outs.add("“" + oB.getName() + "”");
            }
        }

        if (ins.isEmpty() && outs.isEmpty()) {
            return "Justified";
        } else if (!ins.isEmpty() && !outs.isEmpty()) {
            return String.format(
                    "Not justified:\n • %s in IN-list %s not believed."
                            + "\n • %s in OUT-list %s believed",
                    ins.size() == 1 ? "is" : "are",
                    StringUtils.join(ins, ", "), outs.size() == 1 ? "is"
                            : "are", StringUtils.join(outs, ", "));
        } else if (!ins.isEmpty()) {
            return String
                    .format("Not justified:\n • %s %s currently not held",
                            StringUtils.join(ins, ", "), ins.size() == 1 ? "is"
                                    : "are");
        } else {
            return String.format("Not justified:\n • %s %s currently held",
                    StringUtils.join(outs, ", "), outs.size() == 1 ? "is"
                            : "are");
        }
    }

    /**
     * @param b
     *            to ask
     * @return status of b in human readable form
     */
    public String getStatus(Belief b)
    {
        List<String> l = new ArrayList<String>();
        for (Justification j : castToJustification(getJustifications(b))) {
            l.add(String.format("“%s”", j.getName()));
        }

        String justified;
        if (l.isEmpty()) {
            justified = "";
        } else {
            justified = String.format(" Justified by %s, which %s", StringUtils
                    .join(l, ", "), (getState(b) ? "holds" : "does not hold"));
        }

        return String.format("%s%s",
                (getBeliefState(b) ? "held." : "not held."), justified);
    }

    /**
     * @param j
     *            to ask
     * @return IN list of j
     */
    public List<Belief> getInList(Justification j)
    {
        String q = String.format("SELECT ?belief WHERE { ?belief <%s> <%s> }",
                JTMS.SUPPORTS, j.getResource());
        List<Belief> getIn = castToBeliefs(querySingle(q, "belief"));
        return getIn;
    }

    /**
     * @param j
     *            to ask
     * @return OUT list of j
     */
    public List<Belief> getOutList(Justification j)
    {
        String q = String.format("SELECT ?belief WHERE { ?belief <%s> <%s> }",
                JTMS.OPPOSES, j.getResource());
        return castToBeliefs(querySingle(q, "belief"));
    }

    @Override
    public void addSubscriber(ModelSubscriber modelSubscriber)
    {
        modelSubscribers.add(modelSubscriber);
    }

    public Belief getBelief(String resourceURI)
    {
        Resource r = reasonerDb.getResource(resourceURI);
        return resourceToBelief.get(r);
    }

    /**
     * We do not infer the belief state (we should in a future version).
     * 
     * @param b to alter
     * @param newState of belief
     */
    public void setBeliefState(Belief b, boolean newState)
    {
        String beliefURI = b.getResource().getURI();
        String deleteOldState = String.format("DELETE DATA { <%s> <%s> %s }",
                beliefURI, JTMS.HAS_STATE, new Boolean(!newState).toString());

        String insertNewState = String.format("INSERT DATA { <%s> <%s> %s }",
                beliefURI, JTMS.HAS_STATE, new Boolean(newState).toString());

        UpdateRequest request = UpdateFactory.create();
        request.add(deleteOldState).add(insertNewState);
        UpdateAction.execute(request, reasonerDb);
    }

    /**
     * @param b to ask
     * @return return state of belief
     */
    public boolean getBeliefState(Belief b)
    {
        String askStateSparql = String.format("ASK { <%s> <%s> %s }", b
                .getResource().getURI(), JTMS.HAS_STATE, new Boolean(true)
                .toString());
        Query askStateQuery = QueryFactory.create(askStateSparql);
        QueryExecution exec = QueryExecutionFactory.create(askStateQuery,
                reasonerDb);
        boolean heldState = exec.execAsk();
        exec.close();
        return heldState;
    }

    // Return ALL statements about a belief, including those inferred via
    // owl:sameAs
    public List<Statement> getStatements(Belief b)
    {
        Resource br = reasonerDb.getResource(b.getResource().getURI());
        List<Statement> list = new ArrayList<Statement>();

        // Find all seeAlso triples for our belief b, then add the properties
        // of the seeAlso objects to the list of statements.
        StmtIterator seeAlsoIt = br.listProperties(RDFS.seeAlso);
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
     * @return all belief statements
     */
    public List<Statement> getBeliefStatements()
    {
        List<Statement> list = new ArrayList<Statement>();
        StmtIterator iter = reasonerDb.listStatements();
        while (iter.hasNext()) {
            Statement n = iter.next();
            StmtIterator types = n.getSubject().listProperties(RDF.type);
            while (types.hasNext()) {
                Statement type = types.next();
                if (type.getObject().equals(JTMS.BELIEF))
                    list.add(n);
            }
        }
        return list;
    }
}
