package ui;

import interfaces.ViewSubscriber;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import knowledgebase.Belief;
import knowledgebase.Justification;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Resource;

import vocabs.JTMS;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class Graph extends BaseView
{
    public Graph() {
        panel.setBackground(new Color(236, 240, 241));
    }

    public class GraphScrolled implements ChangeListener
    {
        public void stateChanged(ChangeEvent e)
        {
            panel.repaint();
        }
    }

    public void render(List<Belief> beliefs,
            List<Justification> justifications,
            List<Pair<Justification, Belief>> sListEdges,
            List<Pair<Belief, Justification>> justificationEdges)
    {
        panel.removeAll();
        panel.validate();

        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        graph.setBorder(0);

        HashMap<Resource, Object> resToObj = new HashMap<Resource, Object>();
        HashMap<String, Object> idToObj = new HashMap<String, Object>();
        mxHierarchicalLayout layout = new mxHierarchicalLayout(graph);
        graph.getModel().beginUpdate();
        try {
            int x = 0;
            int y = 0;

            for (Belief b : beliefs) {
                String style = "fontColor=white;";

                if (b.isPremise()) {
                    style += "strokeColor=black;fillColor=#19703e;";
                } else if (b.isContradiction()) {
                    style += "strokeColor=black;fillColor=#81261d;";
                } else {
                    // Neither Contradiction nor Premise, so color based on
                    // held/not held.
                    if (b.getHeld()) {
                        style += "rounded=1;strokeColor=darkGreen;fillColor=#27ae60;";
                    } else {
                        style += "rounded=1;strokeColor=darkRed;fillColor=#c0392b;";
                    }
                }

                String id = b.getResource().getLocalName();
                // Insert belief vertex, x, y, width, height, style.
                Object v = graph.insertVertex(parent, id, id, x, y, 140, 30,
                        style);
                resToObj.put(b.getResource(), v);
                idToObj.put(id, b);

                y = y + 40;
            }

            x = 200;
            y = 0;
            for (Justification j : justifications) {
                String id = j.getResource().getLocalName();
                Object v = graph.insertVertex(parent, id, id, x, y, 170, 30);
                resToObj.put(j.getResource(), v);
                idToObj.put(id, j);
                y = y + 60;
            }

            for (Pair<Justification, Belief> edge : sListEdges) {
                String id = String.format("%s_%s", edge.getLeft().getResource()
                        .getLocalName(), edge.getRight().getResource()
                        .getLocalName());
                Object t = resToObj.get(edge.getLeft().getResource());
                Object s = resToObj.get(edge.getRight().getResource());

                String label = edge.getRight().getResource()
                        .hasProperty(JTMS.SUPPORTS) ? "IN" : "OUT";

                graph.insertEdge(parent, id, label, t, s);
                idToObj.put(id, edge);
            }

            for (Pair<Belief, Justification> edge : justificationEdges) {
                String id = String.format("%s_%s", edge.getLeft().getResource()
                        .getLocalName(), edge.getRight().getResource()
                        .getLocalName());
                Object t = resToObj.get(edge.getLeft().getResource());
                Object s = resToObj.get(edge.getRight().getResource());

                String label = "Justified by";
                graph.insertEdge(parent, id, label, t, s);
                idToObj.put(id, edge);
            }
        } finally {
            layout.execute(graph.getDefaultParent());
            graph.getModel().endUpdate();
        }

        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setEnabled(false);
        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mxCell cell = (mxCell) graphComponent.getCellAt(e.getX(),
                            e.getY());
                    if (cell != null) {
                        for (ViewSubscriber l : subscribers) {
                            l.handleTmsGraphClick(idToObj, cell.getId());
                        }
                    } else {
                        for (ViewSubscriber l : subscribers) {
                            l.handleTmsGraphClickEmpty();
                        }
                    }
                }
            }
        });

        panel.add(graphComponent, "100%!, alignx center, aligny center");
        graphComponent.setAutoScroll(true);
        graphComponent.getViewport().setBackground(new Color(0, 0, 0, 0));
        graphComponent.getViewport().setBorder(null);

        graphComponent.setPreferredSize(panel.getSize());
        graphComponent.setMaximumSize(panel.getSize());

        graphComponent.getViewport().addChangeListener(new GraphScrolled());

        graphComponent.setBackground(new Color(0, 0, 0, 0));
        graphComponent.setBorder(null);
        graphComponent.scrollToCenter(true);
        graphComponent.setCenterPage(true);
        panel.validate();
        panel.repaint();
    }
}
