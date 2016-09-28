package ui;

import interfaces.ViewSubscriber;

import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import knowledgebase.Tms;
import net.miginfocom.swing.MigLayout;

/**
 * The head of the app.
 *
 */
public class Head extends BaseView
{
    private final JLabel beliefsLabel = new JLabel();
    private final JLabel justificationsLabel = new JLabel();

    private final JLabel statusLabel = new JLabel();
    private final Timer statusFadeTimer = new Timer();

    public Head() {
        Color trans = new Color(0, 0, 0, 0);
        panel.setLayout(new MigLayout("wrap 3",
                "20[20%, align left][60%, align center][20%, align right]20",
                "20[]0"));
        panel.setBackground(new Color(218, 223, 225));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(
                108, 122, 137)));

        JLabel titleLabel = new JLabel("Semantic Decision Support System");
        titleLabel.setFont(getDefaultFont().deriveFont(0, 22));
        titleLabel.setForeground(Color.black);
        titleLabel.setBackground(trans);
        panel.add(titleLabel, "align center");

        beliefsLabel.setForeground(Color.black);
        beliefsLabel.setFont(getDefaultFont());

        justificationsLabel.setForeground(Color.black);
        justificationsLabel.setFont(getDefaultFont());

        panel.add(beliefsLabel);

        // New belief button
        JPanel controls = new JPanel();
        JButton beliefButton = createButton("New Belief");
        beliefButton.addActionListener(e -> this.newBeliefClick());
        controls.add(beliefButton);

        // New justification button
        JButton justButton = createButton("New Justification");
        justButton.addActionListener(e -> this.newJustificationClick());
        controls.add(justButton);
        controls.setBackground(new Color(0, 0, 0, 0));

        panel.add(controls);

        panel.add(justificationsLabel);

        statusLabel.setFont(getDefaultFont());
        statusLabel.setBackground(trans);
        panel.add(statusLabel, "align center");

        render(0, 0, "Loaded.");
    }

    public void render(int beliefs, int justifications, String status)
    {
        String bplural = beliefs == 1 ? "" : "s";
        String jplural = justifications == 1 ? "" : "s";
        beliefsLabel.setText(String.format("%d belief%s.", beliefs, bplural));
        justificationsLabel.setText(String.format("%d justification%s.",
                justifications, jplural));
        statusLabel.setText(status);
        statusLabel.setVisible(true);
        statusFadeTimer.schedule(new TimerTask() {
            @Override
            public void run()
            {
                statusLabel.setVisible(false);
            }
        }, 4 * 1000);
    }

    private void newBeliefClick()
    {
        String belief = promptForBelief(null);
        if (belief != null && !belief.isEmpty()) {
            for (ViewSubscriber v : subscribers) {
                v.handleAddBelief(belief);
            }
        }
    }

    private void newJustificationClick()
    {
        String question = "Type in the local name of the new Justification.";
        String resource = promptForResource(question, "New Justification",
                "JustificationResource", null);

        // We should prompt for triples and give user suggestions, but no time.
        // So for now, we just slap on the TMS NS.
        resource = Tms.NS + resource;

        if (resource != null) {
            for (ViewSubscriber v : subscribers) {
                v.handleAddJustification(resource);
            }
        }
    }
}
