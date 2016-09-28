package ui;

import interfaces.ViewPublisher;
import interfaces.ViewSubscriber;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import knowledgebase.Belief;
import knowledgebase.Justification;
import knowledgebase.Tms;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.jena.rdf.model.Statement;

public class BaseView implements ViewPublisher
{
    protected static final UrlValidator urlValidator;
    protected final JPanel panel = new JPanel();
    protected final List<BaseView> subviews = new ArrayList<BaseView>();
    protected final List<ViewSubscriber> subscribers = new ArrayList<ViewSubscriber>();
    private final Font defaultFont = new Font(Font.SANS_SERIF, 0, 16);

    // Provide an url validator for all views subclassing the baseview.
    static {
        String[] schemes = { "http", "https" };
        urlValidator = new UrlValidator(schemes);
    }

    /**
     * Add a subView. All subview children emits events through their parents.
     * 
     * @param subView
     *            to add
     */
    public void addSubview(BaseView subView)
    {
        subviews.add(subView);
    }

    /*
     * (non-Javadoc)
     * 
     * @see interfaces.ViewPublisher#addSubscriber(interfaces.ViewSubscriber)
     */
    public void addSubscriber(ViewSubscriber viewSubscriber)
    {
        subscribers.add(viewSubscriber);
    }

    /**
     * @param subscriber
     *            to remove from view
     */
    public void removeSubscriber(ViewSubscriber subscriber)
    {
        subscribers.remove(subscriber);
    }

    /**
     * Call render on all children views.
     */
    protected void renderSubviews()
    {
        for (BaseView b : subviews) {
            b.render();
        }
    }

    /**
     * @param b
     *            to render in all children views
     */
    protected void renderSubviews(Belief b)
    {
        for (BaseView bv : subviews) {
            bv.render(b);
        }
    }

    /**
     * @param j
     *            to render in all children views
     */
    protected void renderSubviews(Justification j)
    {
        for (BaseView b : subviews) {
            b.render(j);
        }
    }

    /**
     * @return panel of the view
     */
    public JPanel getPanel()
    {
        return panel;
    }

    /**
     * Default render function.
     */
    public void render()
    {
    }

    /**
     * @param b
     *            to render
     */
    public void render(Belief b)
    {
    }

    /**
     * @param j
     *            to render
     */
    public void render(Justification j)
    {
    }

    /**
     * @param text
     *            to add to button
     * @return a default button
     */
    public JButton createButton(String text)
    {
        JButton button = new JButton(text);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory
                .createMatteBorder(1, 1, 1, 1, new Color(255, 255, 255, 100)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        button.setFocusPainted(false);
        button.setBackground(new Color(108, 122, 137));
        button.setForeground(Color.white);
        button.setFont(getDefaultFont());
        return button;
    }

    /**
     * @return default panel (transparent)
     */
    public JPanel createPanel()
    {
        JPanel p = new JPanel();
        p.setBackground(new Color(0, 0, 0, 0));
        return p;
    }

    /**
     * @return the default font
     */
    public Font getDefaultFont()
    {
        return defaultFont;
    }

    public BaseView() {
    }

    /**
     * @param question
     *            to ask user
     * @param title
     *            of the dialog box
     * @param placeholder
     *            text
     * @param error
     *            when something goes wrong
     * @return a string representing the valid URI the user entered.
     */
    protected String promptForResource(String question, String title,
            String placeholder, String error)
    {

        if (error != null && !error.isEmpty()) {
            question += "\nError: " + error;
        }

        String s = (String) JOptionPane.showInputDialog(null, question, title,
                JOptionPane.PLAIN_MESSAGE, null, null, placeholder);

        if (s == null) {
            return s;
        }

        if (!s.isEmpty()
                && BaseView.urlValidator.isValid("http://example.org/" + s)) {
            return s;
        } else {
            return promptForResource(question, title, placeholder,
                    "The local name would not become a well-formed URI.");
        }
    }

    /**
     * Prompt the user for an N3 triple.
     * 
     * @return list of triples, or an empty list if one of the triples was
     *         empty/invalid N3
     */
    protected List<String> promptForTriple(List<Statement> potentialStatements)
    {
        JFrame owner = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class,
                panel);
        final TripleDialog dialog = new TripleDialog(owner, true,
                potentialStatements);
        dialog.setSize(1050, 550);
        dialog.setVisible(true);
        List<String> triple = new ArrayList<String>();
        int i = 0;
        for (String el : dialog.getTriple()) {
            if (!el.isEmpty() && BaseView.urlValidator.isValid(el) || i == 2) {
                triple.add(el);
            }
            i++;
        }

        // Return the triple if we had three URLs.
        if (triple.size() == 3) {
            return triple;
        } else {
            return new ArrayList<String>();
        }
    }

    /**
     * Prompt the user for belief triple.
     * 
     * @return list of triples, or an empty list if one of the triples was
     *         empty/invalid N3
     */
    protected String promptForBelief(List<Statement> potentialStatements)
    {
        JFrame owner = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class,
                panel);
        final BeliefDialog dialog = new BeliefDialog(owner, true,
                potentialStatements);
        dialog.setSize(1050, 550);
        dialog.setVisible(true);
        String belief = dialog.getBelief();

        // Ignore empty resources
        if (belief.equals(Tms.NS))
            return "";

        // Ignore empty strings, as well as invalid uris.
        if (!belief.isEmpty() && BaseView.urlValidator.isValid(belief))
            return belief;
        return "";
    }
}
