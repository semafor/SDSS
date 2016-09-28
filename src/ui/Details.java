package ui;

import interfaces.ViewSubscriber;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import knowledgebase.Belief;
import knowledgebase.Justification;
import net.miginfocom.swing.MigLayout;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import vocabs.JTMS;
import vocabs.PROV;

/**
 * The details column (right).
 *
 */
public class Details extends BaseView
{
    private final JTextArea currentDetailsLabel = new JTextArea();
    private final JPanel provenance;
    private final JPanel controlsPanel;

    public Details() {
        panel.setLayout(new MigLayout("wrap 1", "20[100%, fill]20",
                "20[20][100]20[100%, al top]"));
        panel.setBackground(new Color(218, 223, 225));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(
                108, 122, 137)));

        currentDetailsLabel.setBackground(new Color(0, 0, 0, 0));
        currentDetailsLabel.setWrapStyleWord(true);
        currentDetailsLabel.setLineWrap(true);
        currentDetailsLabel.setFont(getDefaultFont());
        panel.add(currentDetailsLabel);

        controlsPanel = new JPanel(new MigLayout("wrap 2",
                "[80%, fill]10[10]10", "10[]20[]"));
        controlsPanel.setBackground(new Color(0, 0, 0, 0));
        panel.add(controlsPanel);

        provenance = new JPanel(new MigLayout("wrap 1", "10[fill]10"));
        provenance.setBackground(new Color(0, 0, 0, 0));
        panel.add(provenance);

        render();
    }

    @Override
    public void render()
    {
        currentDetailsLabel.setVisible(false);
        controlsPanel.setVisible(false);
        provenance.setVisible(false);
    }

    public void render(Belief b, String status,
            List<Statement> beliefStatements, List<Statement> allStatements)
    {
        controlsPanel.removeAll();
        controlsPanel.setVisible(true);
        currentDetailsLabel.setText(String.format(
                "“%s” is a belief, currently %s.", b.getName(), status));
        currentDetailsLabel.setVisible(true);

        provenance.removeAll();

        JPanel chk = new JPanel(new MigLayout("ax left"));
        JRadioButton justificationRadio = new JRadioButton("Justify");
        JRadioButton premiseRadio = new JRadioButton("Believe");
        JRadioButton contradictionRadio = new JRadioButton("Do not believe");
        ButtonGroup bG = new ButtonGroup();
        bG.add(justificationRadio);
        bG.add(premiseRadio);
        bG.add(contradictionRadio);

        chk.add(justificationRadio);
        chk.add(premiseRadio);
        chk.add(contradictionRadio);

        justificationRadio.addActionListener(e -> this.toggleBeliefType(b,
                JTMS.BELIEF));
        premiseRadio.addActionListener(e -> this.toggleBeliefType(b,
                JTMS.PREMISE));
        contradictionRadio.addActionListener(e -> this.toggleBeliefType(b,
                JTMS.CONTRADICTION));

        if (b.isPremise()) {
            premiseRadio.setSelected(true);
        } else if (b.isContradiction()) {
            contradictionRadio.setSelected(true);
        } else {
            justificationRadio.setSelected(true);
        }
        chk.setBackground(new Color(0, 0, 0, 0));
        controlsPanel.add(chk);

        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> this.removeBelief(b));
        controlsPanel.add(delete, "skip");
        System.out.println("checking provenance...");
        if (beliefStatements.size() > 0) {
            provenance.setVisible(true);

            System.out.println("have reason for provenance");
            for (Statement stmt : beliefStatements) {
                if (stmt.getPredicate().equals(PROV.WAS_ATTRIBUTED_TO)) {
                    Property p = ResourceFactory
                            .createProperty("http://dbpedia.org/property/name");
                    if (stmt.getObject().asResource().hasProperty(p)) {
                        System.out.println(stmt.getObject());
                        provenance.add(new JLabel(stmt.getObject().asResource()
                                .getProperty(p).getObject().toString()));
                    } else {
                        provenance.add(new JLabel(stmt.getObject().toString()));
                    }
                    StmtIterator values = stmt.getSubject().listProperties(
                            PROV.VALUE);
                    while (values.hasNext()) {
                        Statement n = values.next();
                        if (n.getObject().isLiteral()) {
                            provenance.add(new JLabel(
                                    "<html><body><p style=\"width:240px; margin-left: 10px\">"
                                            + n.getObject().toString()
                                            + "</p></body></html>"));
                        }
                    }
                }
            }
        } else {
            // No provenance.
            provenance.setVisible(false);
        }

        panel.validate();
        panel.repaint();
    }

    public void render(Justification j, List<Belief> inList,
            List<Belief> outList, String status, List<Statement> kb)
    {
        controlsPanel.removeAll();

        currentDetailsLabel.setVisible(true);
        currentDetailsLabel.setText(String.format(
                "“%s” is a justification.\n\n%s.", j.getName(), status));

        JButton justifyButton = new JButton("Justify Belief");
        controlsPanel.add(justifyButton, "span 2");
        justifyButton.addActionListener(e -> this.justifyBelief(j, kb));

        controlsPanel.add(new JLabel("IN-List"));

        JButton newbutton = new JButton("New");
        newbutton.addActionListener(e -> this.addToIn(j, kb));
        controlsPanel.add(newbutton);

        for (Belief iB : inList) {
            JLabel inLabel = new JLabel(iB.getName());
            inLabel.setFont(new Font(Font.MONOSPACED, 0, 14));
            controlsPanel.add(inLabel);
            JButton del = new JButton("Delete");
            del.addActionListener(e -> this.removeFromIn(iB, j));
            controlsPanel.add(del);
        }

        controlsPanel.add(new JLabel("OUT-List"));

        JButton newoutbutton = new JButton("New");
        newoutbutton.addActionListener(e -> this.addToOut(j, kb));
        controlsPanel.add(newoutbutton);

        for (Belief oB : outList) {
            JLabel outLabel = new JLabel(oB.getName());
            outLabel.setFont(new Font(Font.MONOSPACED, 0, 14));
            controlsPanel.add(outLabel);
            JButton del = new JButton("Delete");
            del.addActionListener(e -> this.removeFromOut(oB, j));
            controlsPanel.add(del);
        }

        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> this.removeJustification(j));
        controlsPanel.add(delete, "span 2");

        panel.validate();
        panel.repaint();
    }

    public void removeFromIn(Belief b, Justification j)
    {
        for (ViewSubscriber s : subscribers) {
            s.handleRemoveInBelief(b, j, ViewType.JUSTIFICATION);
        }
    }

    public void addToIn(Justification j, List<Statement> suggestions)
    {
        String resource = promptForBelief(suggestions);
        if (resource != null && !resource.isEmpty()) {
            for (ViewSubscriber v : subscribers) {
                v.handleAddInBelief(resource, j, ViewType.JUSTIFICATION);
            }
        }
    }

    public void removeFromOut(Belief b, Justification j)
    {
        for (ViewSubscriber s : subscribers) {
            s.handleRemoveOutBelief(b, j, ViewType.JUSTIFICATION);
        }
    }

    public void addToOut(Justification j, List<Statement> suggestions)
    {
        String resource = promptForBelief(suggestions);
        if (resource != null && !resource.isEmpty()) {
            for (ViewSubscriber v : subscribers) {
                v.handleAddOutBelief(resource, j, ViewType.JUSTIFICATION);
            }
        }
    }

    public void toggleBeliefType(Belief b, Resource type)
    {
        for (ViewSubscriber v : subscribers) {
            v.handleBeliefTypeChanged(b, type);
        }
    }

    public void justifyBelief(Justification j, List<Statement> suggestions)
    {
        String belief = promptForBelief(suggestions);
        if (belief != null && !belief.isEmpty()) {
            for (ViewSubscriber v : subscribers) {
                v.handleJustificationJustifiesBelief(j, belief,
                        ViewType.JUSTIFICATION);
            }
        }
    }

    public void removeBelief(Belief b)
    {
        for (ViewSubscriber v : subscribers) {
            v.handleRemoveBelief(b, ViewType.BELIEF);
        }
    }

    public void removeJustification(Justification j)
    {
        for (ViewSubscriber v : subscribers) {
            v.handleRemoveJustification(j, ViewType.JUSTIFICATION);
        }
    }
}
