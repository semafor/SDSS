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

import knowledgebase.Tms;
import net.miginfocom.swing.MigLayout;

import org.apache.jena.rdf.model.Statement;

import vocabs.JTMS;

/**
 * Create a dialog that takes a belief from the user.
 */
public class BeliefDialog extends JDialog
{
    private static final long serialVersionUID = 1L;

    JLabel labelSubj = new JLabel("Subject");

    JTextField subjField = new JTextField();

    public BeliefDialog(Frame owner, boolean modal,
            List<Statement> potentialStatements) {
        super(owner, modal);
        init(potentialStatements);
    }

    private void init(List<Statement> potentialStatements)
    {
        this.setTitle("Select Belief");
        this.setLayout(new MigLayout("ins 20, wrap 2", "[para]20[800lp, fill]"));
        this.add(labelSubj);

        subjField.setText(Tms.NS);
        this.add(subjField);

        if (potentialStatements != null && potentialStatements.size() > 0) {

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
                    }
                }
            });

            model.addColumn("Belief URI");

            for (Statement s : potentialStatements) {
                if (s.getObject().equals(JTMS.BELIEF))
                    model.addRow(new Object[] { s.getSubject().toString() });
            }

            table.setRowHeight(23);
            table.setDefaultEditor(Object.class, null);
            this.add(new JScrollPane(table), "span 2, w 100%");
        }
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> this.done());
        this.add(ok, "w 100lp, h 20lp");

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> this.cancel());
        this.add(cancel, "w 100lp!, h 20lp");

        getRootPane().setDefaultButton(ok);

        this.validate();
        this.repaint();
    }

    private void done()
    {
        this.setVisible(false);
    }

    private void cancel()
    {
        this.subjField.setText("");
        this.setVisible(false);
    }

    public String getBelief()
    {
        return subjField.getText();
    }
}
