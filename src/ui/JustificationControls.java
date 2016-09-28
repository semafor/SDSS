package ui;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import knowledgebase.Belief;
import knowledgebase.Justification;
import net.miginfocom.swing.MigLayout;

public class JustificationControls extends JPanel
{

    public JustificationControls(Justification j, List<Belief> inList, List<Belief> outList) {
        setLayout(new MigLayout("wrap 2", "[80%, fill]10[10]10", "10[]20[]"));
        add(new JLabel("IN-List"));
        add(new JButton("New"));
        
        for (Belief iB : inList) {
            add(new JLabel(iB.getName()));
            JButton del = new JButton("Delete");
//            del.addActionListener(e -> j.removeFromIn(iB));
            add(del);
        }
    }
    
    private void removeFromIn(Belief b, Justification j) {
//        for (ViewSubscriber s : )
//        handleRemoveInBelief
    }
}
