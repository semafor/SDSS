import interfaces.ModelSubscriber;
import interfaces.ViewSubscriber;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Map;

import knowledgebase.Belief;
import knowledgebase.Fuseki;
import knowledgebase.Justification;
import knowledgebase.Reasoner;
import knowledgebase.Tms;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.log4j.varia.NullAppender;

import ui.App;
import ui.ViewType;
import vocabs.JTMS;

public class Main implements ViewSubscriber, ModelSubscriber
{
    private final App ui;
    private final Fuseki db;
    private final Reasoner reasoner;
    private final Tms tms;

    /**
     * Set up the app.
     */
    public Main() {
        org.apache.log4j.BasicConfigurator.configure(new NullAppender());
        db = new Fuseki("http://localhost:3030/info216/query",
                "http://localhost:3030/info216/data");
        reasoner = new Reasoner(db);
        tms = new Tms(db);

        tms.changed();

        ui = new App();
        ui.addSubscriber(this);

        ui.frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e)
            {
                renderGraph();
            }
        });
    }

    /**
     * Render the app.
     * 
     * @param args
     *            (not supported)
     */
    public static void main(String[] args)
    {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        Main app = new Main();
        app.reasoner.update(app.tms.getKnowledgeBase());
        app.update();
        app.render();
        app.ui.frame.setVisible(true);
    }

    private void render()
    {
        renderGraph();
        ui.render();
    }

    private void render(Belief b)
    {
        // Render a Belief.
        List<Statement> all = tms.getStatements();
        all.addAll(tms.getAuxiliaryStatements());
        ui.render(b, reasoner.getStatus(b), reasoner.getStatements(b), all);
        tms.getKnowledgeBase().write(System.out, "NTRIPLES");
    }

    private void render(Justification j)
    {
        // Render a Justification.
        renderGraph();
        ui.render(j, reasoner.getInList(j), reasoner.getOutList(j),
                reasoner.getStatus(j), reasoner.getBeliefStatements());
    }

    private void renderGraph()
    {
        ui.renderGraph(reasoner.getBeliefs(), reasoner.getJustifications(),
                reasoner.getSupportListEdges(),
                reasoner.getJustificationEdges());
    }

    private void updateReasoner()
    {
        reasoner.update(tms.getKnowledgeBase());
    }

    private void update()
    {
        updateReasoner();
    }

    @Override
    public void handleTmsGraphClick(Map<String, Object> map, String id)
    {
        // Figure out what the user clicked, then render the corresponding
        // thing.
        if (map != null && id != null) {
            Object o = map.get(id);
            if (o instanceof Justification) {
                Justification j = (Justification) o;
                render(j);
            } else if (o instanceof Belief) {
                render((Belief) o);
            } else {
                ui.render();
            }
        } else {
            // Empty click.
            ui.render();
        }
    }

    @Override
    public void handleBeliefTypeChanged(Belief b, Resource newType)
    {
        if (newType == JTMS.BELIEF) {
            tms.unpremise(b.getResource().toString());
            tms.uncontradict(b.getResource().toString());
        } else if (newType == JTMS.CONTRADICTION) {
            tms.contradict(b.getResource().toString());
            tms.unpremise(b.getResource().toString());
        } else if (newType == JTMS.PREMISE) {
            tms.premise(b.getResource().toString());
            tms.uncontradict(b.getResource().toString());
        }

        updateReasoner();
        reasoner.propagateBelief(b);

        renderGraph();
        render(b);
    }

    @Override
    public void handleJustificationJustifiesBelief(Justification j,
            String belief, ViewType t)
    {
        // Make changes to TMS and reasoner, and then render what changed.
        tms.justifies(j.getResource().toString(), belief);
        update();
        reasoner.propagateBelief(reasoner.getBelief(belief));
        renderGraph();
        render(j);
    }

    @Override
    public void handleAddBelief(String localName)
    {
        // Add the belief, update and render.
        tms.addBelief(localName);
        update();
        renderGraph();
    }

    @Override
    public void handleAddJustification(String localName)
    {
        // Add justification, then update and render.
        tms.addJustification(localName);
        update();
        renderGraph();
    }

    @Override
    public void handleTmsGraphClickEmpty()
    {
        // Call default renderer.
        ui.render();
    }

    @Override
    public void handleBeliefUpdate(Belief belief)
    {
    }

    @Override
    public void handleJustificationUpdate(Justification justification)
    {
        // A justification was updated, so render it (if we were looking at it).
        if (ui.getViewType() == ViewType.JUSTIFICATION) {
            Justification j = justification;
            ui.render(j, reasoner.getInList(j), reasoner.getOutList(j),
                    reasoner.getStatus(j), tms.getStatements());
        }
    }

    @Override
    public void handleRemoveInBelief(Belief b, Justification j, ViewType type)
    {
        // Remove belief. Since this can be done from multiple places, we figure
        // out what to render.
        tms.removeFromIn(b, j);
        update();
        if (type.equals(ViewType.JUSTIFICATION)) {
            render(j);
        } else if (type.equals(ViewType.BELIEF)) {
            render(b);
        } else {
            render();
        }
    }

    @Override
    public void handleRemoveOutBelief(Belief b, Justification j, ViewType type)
    {
        // Remove from out. We can do this from multiple places, so figure out
        // what to render.
        tms.removeFromOut(b, j);
        update();
        if (type.equals(ViewType.JUSTIFICATION)) {
            render(j);
        } else if (type.equals(ViewType.BELIEF)) {
            render(b);
        } else {
            render();
        }
    }

    @Override
    public void handleAddInBelief(String iri, Justification j, ViewType type)
    {
        // Add a belief to IN, then render.
        tms.addToIn(iri, j.getResource().toString());
        update();
        if (type.equals(ViewType.JUSTIFICATION)) {
            render(j);
        } else if (type.equals(ViewType.BELIEF)) {
            Belief b = reasoner.getBelief(iri);
            render(b);
        } else {
            render();
        }

    }

    @Override
    public void handleAddOutBelief(String beliefLocalName, Justification j,
            ViewType type)
    {
        // Add a belief to out, then render.
        tms.addToOut(beliefLocalName, j.getResource().toString());
        update();
        if (type.equals(ViewType.JUSTIFICATION)) {
            render(j);
        } else if (type.equals(ViewType.BELIEF)) {
            Belief b = reasoner.getBelief(Tms.NS + beliefLocalName);
            render(b);
        } else {
            render();
        }
    }

    @Override
    public void handleAddDoc(Belief b, List<String> triple, ViewType type)
    {
        // Add annotation (doc) then render.
        tms.addStatement(b, triple);
        updateReasoner();
        render(b);
    }

    @Override
    public void handleRemoveBelief(Belief b, ViewType type)
    {
        // remove belief, then render
        tms.removeBelief(b);
        update();
        render();
    }

    @Override
    public void handleRemoveJustification(Justification j, ViewType type)
    {
        // Remove justification then render.
        tms.removeJustification(j);
        update();
        render();
    }
}
