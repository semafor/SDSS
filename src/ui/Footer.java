package ui;

import interfaces.ViewSubscriber;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import knowledgebase.Belief;
import net.miginfocom.swing.MigLayout;

import org.apache.jena.rdf.model.Statement;

/**
 * Footer of the app.
 *
 */
public class Footer extends BaseView
{

    public Footer() {
        panel.setLayout(new MigLayout("wrap 1", "10[100%, fill]10",
                "10[20]10[fill]"));
        panel.setBackground(new Color(218, 223, 225));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(
                108, 122, 137)));
    }

    public void render()
    {
        panel.removeAll();
    }

    public void render(Belief b, List<Statement> beliefStatements,
            List<Statement> allStatements)
    {
        render();

        JPanel controls = new JPanel();

        JLabel label = new JLabel("Annotation");
        controls.add(label);

        JButton newPred = new JButton("Add annotation");
        newPred.addActionListener(e -> this.addDoc(b, allStatements));
        controls.add(newPred);
        controls.setBackground(new Color(0, 0, 0, 0));
        panel.add(controls);

        if (beliefStatements.size() == 0)
            return;

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model) {
            // Implement table cell tool tips.
            public String getToolTipText(MouseEvent e)
            {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                try {
                    tip = getValueAt(rowIndex, colIndex).toString();
                } catch (RuntimeException e1) {
                    // catch null pointer exception if mouse is over an empty
                    // line
                }

                return tip;
            }
        };

        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e)
            {
                int row = e.getFirstRow();
                int column = e.getColumn();

                if (column == TableModelEvent.ALL_COLUMNS) {
                    // All columns changed, ignore for now.
                    return;
                }

                DefaultTableModel model = (DefaultTableModel) e.getSource();
                String columnName = model.getColumnName(column);
            }
        });

        model.addColumn("Subject");
        model.addColumn("Predicate");
        model.addColumn("Object");

        for (Statement s : beliefStatements) {
            model.addRow(new Object[] { s.getSubject().toString(),
                    s.getPredicate().toString(), s.getObject().toString() });
        }
        panel.add(new JScrollPane(table), "w 100%!");
        panel.validate();
        panel.repaint();

    }

    public void addDoc(Belief b, List<Statement> allStatements)
    {
        List<String> triple = promptForTriple(allStatements);
        if (triple.size() == 3) {
            for (ViewSubscriber v : subscribers) {
                v.handleAddDoc(b, triple, ViewType.BELIEF);
            }
        }
    }
}
