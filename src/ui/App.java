package ui;

import interfaces.ViewSubscriber;

import java.util.List;

import javax.swing.JFrame;

import knowledgebase.Belief;
import knowledgebase.Justification;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Statement;

public class App extends BaseView
{
    public final JFrame frame;

    private final Graph graph;
    private final Head head;
    private final Details details;
    private final Footer footer;

    private ViewType currentView = ViewType.NONE;

    public App() {
        frame = new JFrame("Semantic Decision Support System");
        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel.setLayout(new MigLayout());

        head = new Head();
        addSubview(head);
        panel.add(head.getPanel(), "dock north, width 100%, height 8%!");

        footer = new Footer();
        addSubview(footer);
        panel.add(footer.getPanel(), "dock south, width 100%!, height 22%!");

        graph = new Graph();
        addSubview(graph);
        panel.add(graph.getPanel(), "dock west, width 80%, height 69%");

        details = new Details();
        addSubview(details);
        panel.add(details.getPanel(), "dock east, width 20%!, height 85%");

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    public void renderGraph(List<Belief> beliefs,
            List<Justification> justifications,
            List<Pair<Justification, Belief>> sListEdges,
            List<Pair<Belief, Justification>> justificationEdges)
    {
        graph.render(beliefs, justifications, sListEdges, justificationEdges);
    }

    @Override
    public void addSubscriber(ViewSubscriber viewSubscriber)
    {
        subscribers.add(viewSubscriber);

        for (BaseView v : subviews) {
            v.addSubscriber(viewSubscriber);
        }
    }

    public ViewType getViewType()
    {
        return currentView;
    }

    public void render()
    {
        details.render();
        head.render();
        footer.render();
        currentView = ViewType.NONE;
    }

    public void render(Justification j)
    {
        System.err
                .println("Trying to render Justification without inlist/outlist.");
    }

    public void render(Justification j, List<Belief> inList,
            List<Belief> outList, String status,
            List<Statement> potentialStatements)
    {
        head.render(j);
        details.render(j, inList, outList, status, potentialStatements);
        footer.render();
        currentView = ViewType.JUSTIFICATION;
        panel.validate();
        panel.repaint();
    }

    public void render(Belief b, String status,
            List<Statement> beliefStatements, List<Statement> allStatements)
    {
        head.render(b);
        details.render(b, status, beliefStatements, allStatements);
        footer.render(b, beliefStatements, allStatements);
        currentView = ViewType.BELIEF;
        panel.validate();
        panel.repaint();
    }

    public void renderStats(int beliefs, int justifications, String status)
    {
        head.render(beliefs, justifications, status);
    }
}
