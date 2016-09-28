/**
 * 
 */
package ui;

import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import net.miginfocom.swing.MigLayout;

import org.apache.jena.rdf.model.Statement;

/**
 * Asks for a triple from the user.
 *
 */
public class TripleDialog extends JDialog
{
    private static final long serialVersionUID = 1L;

    JLabel labelSubj = new JLabel("Subject");
    JLabel labelPred = new JLabel("Predicate");
    JLabel labelObj = new JLabel("Object");

    JTextField subjField = new JTextField();
    JTextField predField = new JTextField();
    JTextField objField = new JTextField();

    String[] triple = new String[3];

    public TripleDialog(Frame owner, boolean modal,
            List<Statement> potentialStatements) {
        super(owner, modal);
        init(potentialStatements);
    }

    private void init(List<Statement> potentialStatements)
    {
        this.setTitle("Add triple");
        this.setLayout(new MigLayout("ins 20, wrap 2", "[para]20[800lp, fill]"));
        this.add(labelSubj);
        this.add(subjField);
        this.add(labelPred);
        this.add(predField);
        this.add(labelObj);
        this.add(objField);

        this.add(new JLabel("… or choose one from the knowledge base"),
                "span 2");

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);

        TableRowSorter sorter = new TableRowSorter<DefaultTableModel>(model);
        table.setRowSorter(sorter);

        JTextField filterField = new JTextField();
        this.add(new JLabel("Filter:"));
        this.add(filterField, "w 200lp!");

        filterField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e)
            {
                JTextField textField = (JTextField) e.getSource();
                String text = textField.getText();

                RowFilter<DefaultTableModel, Object> rf = null;
                // If current expression doesn't parse, don't update.
                try {
                    rf = RowFilter.regexFilter(textField.getText());
                } catch (java.util.regex.PatternSyntaxException ex) {
                    return;
                }
                sorter.setRowFilter(rf);
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row >= 0 && column >= 0) {
                    JTable t = ((JTable) e.getSource());
                    subjField.setText(t.getValueAt(row, 0).toString());
                    predField.setText(t.getValueAt(row, 1).toString());
                    objField.setText(t.getValueAt(row, 2).toString());
                }
            }
        });

        model.addColumn("Subject");
        model.addColumn("Predicate");
        model.addColumn("Object");

        for (Statement s : potentialStatements) {
            model.addRow(new Object[] { s.getSubject().toString(),
                    s.getPredicate().toString(), s.getObject().toString() });
        }

        table.setRowHeight(23);
        table.setDefaultEditor(Object.class, null);
        this.add(new JScrollPane(table), "span 2, w 100%");
        this.validate();
        this.repaint();

        JButton close = new JButton("Done");
        close.addActionListener(e -> this.done());
        this.add(close, "w 100lp, h 20lp");

        getRootPane().setDefaultButton(close);

        this.validate();
        this.repaint();
    }

    private void done()
    {
        this.setVisible(false);
    }

    public String[] getTriple()
    {
        triple[0] = subjField.getText();
        triple[1] = predField.getText();
        triple[2] = objField.getText();
        return triple;
    }
}
