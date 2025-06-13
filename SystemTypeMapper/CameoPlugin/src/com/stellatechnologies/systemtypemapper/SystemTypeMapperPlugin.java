package com.stellatechnologies.systemtypemapper;

import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.ui.MainFrame;
import com.nomagic.magicdraw.ui.browser.ContainmentTree;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Profile;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.prefs.Preferences;

public class SystemTypeMapperPlugin extends Plugin {
    private static final String SYSTEM_MAPPER_ENDPOINT = "stella.backend.endpoint";
    private ContainmentPrintAction printAction;
    private final Preferences prefs = Preferences.userRoot().node("stella");

    // UI Constants
    private static final Color STELLA_BLUE = new Color(50, 82, 123);        // Brighter, modern blue
    private static final Color STELLA_DARK = new Color(33, 37, 41);         // Dark gray
    private static final Color STELLA_LIGHT = new Color(248, 249, 250);     // Light background
    private static final Color STELLA_ACCENT = new Color(255, 102, 102);      // Accent color for highlights
    private static final Font STELLA_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font STELLA_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);

    @Override
    public void init() {
        printAction = new ContainmentPrintAction();

        // Create Stella menu
        ActionsConfiguratorsManager.getInstance()
            .addMainMenuConfigurator(manager -> {
                // Create or get main Stella menu
                MDActionsCategory stellaCategory = (MDActionsCategory) manager.getCategory("STELLA_MENU");
                if (stellaCategory == null) {
                    stellaCategory = new MDActionsCategory(
                        "STELLA_MENU",    // ID
                        "Stella",         // Name
                        null,            // Icon
                        null             // Group
                    );
                    stellaCategory.setNested(true);
                    manager.addCategory(stellaCategory);
                }
                
                // Add search action to Stella menu
                stellaCategory.addAction(printAction);
            });

        // Also in browser context menu
        ActionsConfiguratorsManager.getInstance()
            .addContainmentBrowserContextConfigurator(new BrowserContextAMConfigurator() {
                @Override
                public void configure(ActionsManager manager, Tree tree) {
                    MDActionsCategory cat = new MDActionsCategory("contPrintContext", "Semantic Search");
                    cat.addAction(printAction);
                    manager.addCategory(cat);
                }
                @Override
                public int getPriority() { return MEDIUM_PRIORITY; }
            });
    }

    private String getFlaskApiUrl() {
        return prefs.get(SYSTEM_MAPPER_ENDPOINT, "http://localhost:5000/search");
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean close() { return true; }

    private static class SearchResult {
        final String label;
        final String definition;
        final double score;
        final String source;
        final String recommendationExplanation;
        final double recommendationConfidence;
        SearchResult(String label, String definition, double score, String recommendationExplanation, double recommendationConfidence) {
            this.label = label;
            this.definition = definition;
            this.score = score;
            this.source = null;
            this.recommendationExplanation = recommendationExplanation;
            this.recommendationConfidence = recommendationConfidence;
        }
        SearchResult(String label, String definition, double score, String source) {
            this.label = label;
            this.definition = definition;
            this.score = score;
            this.source = source;
            this.recommendationExplanation = null;
            this.recommendationConfidence = 0.0;
        }
    }

    private static class SearchResultsDialog extends JDialog {
        private static final int DIALOG_WIDTH = 1100;  // Increased from 900
        private static final int DIALOG_HEIGHT = 700;
        
        private final JPanel loadingPanel;
        private final Timer spinnerTimer;
        private int spinnerAngle = 0;
        
        SearchResultsDialog(Frame owner, Map<String, List<SearchResult>> resultsBySource, Map<String, Element> sourceElements) {
            super(owner, "Semantic Search Results", true);
            setLayout(new BorderLayout());
            
            // Set dialog styling with shadow border
            setBackground(SystemTypeMapperPlugin.STELLA_LIGHT);
            getRootPane().setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                new EmptyBorder(0, 0, 0, 0)
            ));
            
            // Create custom header panel with logo
            JPanel headerPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw gradient background
                    GradientPaint gradient = new GradientPaint(
                        0, 0, SystemTypeMapperPlugin.STELLA_BLUE,
                        getWidth(), 0, SystemTypeMapperPlugin.STELLA_BLUE.darker()
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            };
            headerPanel.setPreferredSize(new Dimension(DIALOG_WIDTH, 80));
            headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
            
            // Add company logo
            try {
                ImageIcon logoIcon = new ImageIcon(SystemTypeMapperPlugin.class.getResource("/resources/stella_logo.png"));
                Image img = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(img));
                logoLabel.setBorder(new EmptyBorder(0, 0, 0, 15));
                headerPanel.add(logoLabel, BorderLayout.WEST);
            } catch (Exception e) {
                // Fallback if logo loading fails
                JLabel logoText = new JLabel("STELLA");
                logoText.setFont(SystemTypeMapperPlugin.STELLA_FONT_BOLD);
                logoText.setForeground(Color.WHITE);
                logoText.setBorder(new EmptyBorder(0, 0, 0, 15));
                headerPanel.add(logoText, BorderLayout.WEST);
            }
            
            // Add title with custom font
            JLabel titleLabel = new JLabel("System Type Association");
            titleLabel.setFont(SystemTypeMapperPlugin.STELLA_FONT_BOLD);
            titleLabel.setForeground(Color.WHITE);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            add(headerPanel, BorderLayout.NORTH);

            // Create loading panel with spinner
            loadingPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int size = 50;
                    int x = (getWidth() - size) / 2;
                    int y = (getHeight() - size) / 2;
                    
                    // Draw spinner
                    g2d.setColor(SystemTypeMapperPlugin.STELLA_BLUE);
                    g2d.setStroke(new BasicStroke(4));
                    g2d.rotate(Math.toRadians(spinnerAngle), x + size/2, y + size/2);
                    g2d.drawArc(x, y, size, size, 0, 300);
                    
                    g2d.dispose();
                }
            };
            loadingPanel.setOpaque(false);
            loadingPanel.setVisible(false);
            add(loadingPanel, BorderLayout.CENTER);
            
            // Create spinner animation
            spinnerTimer = new Timer(50, e -> {
                spinnerAngle = (spinnerAngle + 30) % 360;
                loadingPanel.repaint();
            });

            // Create tabbed pane with custom styling
            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(SystemTypeMapperPlugin.STELLA_FONT);
            tabs.setBackground(SystemTypeMapperPlugin.STELLA_LIGHT);
            tabs.setBorder(new EmptyBorder(20, 20, 20, 20));
            
            // Add tabs with loading animation
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    for (Map.Entry<String, List<SearchResult>> entry : resultsBySource.entrySet()) {
                        tabs.addTab(entry.getKey(), createPanel(entry.getValue(), entry.getKey(), sourceElements));
                    }
                    return null;
                }
                
                @Override
                protected void done() {
                    loadingPanel.setVisible(false);
                    spinnerTimer.stop();
                    add(tabs, BorderLayout.CENTER);
                    revalidate();
                    repaint();
                }
            };
            
            // Show loading panel and start animation
            loadingPanel.setVisible(true);
            spinnerTimer.start();
            worker.execute();

            // Create styled button panel
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnPanel.setBackground(SystemTypeMapperPlugin.STELLA_LIGHT);
            btnPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
            
            JButton closeBtn = new JButton("Close");
            closeBtn.setOpaque(true);
            closeBtn.setFont(SystemTypeMapperPlugin.STELLA_FONT);
            closeBtn.setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setBorderPainted(false);
            closeBtn.setFocusPainted(false);
            closeBtn.setContentAreaFilled(true);
            closeBtn.setPreferredSize(new Dimension(100, 35));
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> dispose());
            btnPanel.add(closeBtn);
            add(btnPanel, BorderLayout.SOUTH);

            // Set size and position
            setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
            pack();
            setLocationRelativeTo(owner);
        }
        
        // Custom shadow border
        private static class ShadowBorder extends AbstractBorder {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw shadow
                for (int i = 0; i < 5; i++) {
                    g2d.setColor(new Color(0, 0, 0, 10 - 2*i));
                    g2d.drawRoundRect(x+i, y+i, width-2*i-1, height-2*i-1, 10, 10);
                }
                
                g2d.dispose();
            }
            
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(5, 5, 5, 5);
            }
        }

        private JPanel createPanel(List<SearchResult> results, String source, Map<String, Element> sourceElements) {
            String[] cols = {"Score", "Label", "Definition", "LLM Recommendation", "Action"};
            Object[][] data = new Object[results.size()][5];
            
            // Find the recommended result
            SearchResult recommendedResult = null;
            for (SearchResult r : results) {
                if (r.recommendationExplanation != null) {
                    recommendedResult = r;
                    break;
                }
            }
            
            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                data[i][0] = String.format("%.2f", r.score);
                data[i][1] = r.label;
                data[i][2] = r.definition;
                
                // Add recommendation info
                if (r == recommendedResult) {
                    data[i][3] = String.format(r.recommendationExplanation);
                } else {
                    data[i][3] = "";
                }
                
                data[i][4] = "Apply";
            }
            
            JTable table = new JTable(data, cols) {
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    
                    // Always keep Apply button cells Stella blue
                    if (column == 4) {  // Apply button column
                        c.setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
                        c.setForeground(Color.BLACK);
                        return c;
                    }
                    
                    // Normal cell coloring
                    if (!isRowSelected(row)) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    }
                    c.setForeground(Color.BLACK);
                    return c;
                }
                
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 4;  // Only allow editing the Action column
                }

                @Override
                public void setSelectionBackground(Color color) {
                    // Override selection background for Apply button column
                    if (getSelectedColumn() == 4) {
                        super.setSelectionBackground(SystemTypeMapperPlugin.STELLA_BLUE);
                    } else {
                        super.setSelectionBackground(color);
                    }
                }
            };
            
            // Configure table properties
            table.setFont(SystemTypeMapperPlugin.STELLA_FONT);
            table.setRowHeight(80);  // Increased height for recommendation text
            table.setIntercellSpacing(new Dimension(10, 0));
            table.setShowGrid(false);
            table.setSelectionBackground(new Color(230, 236, 255));
            
            // Set column widths
            TableColumnModel columnModel = table.getColumnModel();
            columnModel.getColumn(0).setPreferredWidth(60);   // Score
            columnModel.getColumn(1).setPreferredWidth(150);  // Label
            columnModel.getColumn(2).setPreferredWidth(300);  // Definition
            columnModel.getColumn(3).setPreferredWidth(250);  // Recommendation
            columnModel.getColumn(4).setPreferredWidth(80);   // Action
            
            // Create custom cell renderers for scrollable text areas
            TableCellRenderer scrollableRenderer = new TableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    if (value == null) return new JLabel("");
                    
                    JTextArea textArea = new JTextArea(value.toString());
                    textArea.setWrapStyleWord(true);
                    textArea.setLineWrap(true);
                    textArea.setFont(table.getFont());
                    textArea.setForeground(Color.BLACK);
                    
                    if (isSelected) {
                        textArea.setBackground(table.getSelectionBackground());
                    } else {
                        textArea.setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
                    }
                    
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setBorder(BorderFactory.createEmptyBorder());
                    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                    
                    return scrollPane;
                }
            };
            
            // Apply scrollable renderer to Definition and Recommendation columns
            columnModel.getColumn(2).setCellRenderer(scrollableRenderer);
            columnModel.getColumn(3).setCellRenderer(scrollableRenderer);
            
            // Set up button renderer with custom background
            ButtonRenderer buttonRenderer = new ButtonRenderer();
            buttonRenderer.setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
            columnModel.getColumn(4).setCellRenderer(buttonRenderer);
            
            ButtonEditor buttonEditor = new ButtonEditor(new JCheckBox(), results, source, sourceElements);
            buttonEditor.getComponent().setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
            columnModel.getColumn(4).setCellEditor(buttonEditor);
            
            // Create scroll pane
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(Color.WHITE);
            
            // Create panel with source element info
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(Color.WHITE);
            
            // Add source element info at top
            Element sourceElement = sourceElements.get(source);
            if (sourceElement != null) {
                JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                sourcePanel.setBackground(Color.WHITE);
                sourcePanel.setBorder(new EmptyBorder(0, 0, 10, 0));
                
                JLabel sourceLabel = new JLabel("Source Element: " + sourceElement.getHumanName());
                sourceLabel.setFont(SystemTypeMapperPlugin.STELLA_FONT.deriveFont(Font.BOLD));
                sourcePanel.add(sourceLabel);
                
                panel.add(sourcePanel, BorderLayout.NORTH);
            }
            
            panel.add(scrollPane, BorderLayout.CENTER);
            return panel;
        }
    }

    private static class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {
        WordWrapCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setFont(SystemTypeMapperPlugin.STELLA_FONT);
            setBorder(new EmptyBorder(5, 5, 5, 5));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(true);
            setFocusPainted(false);
            setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
            setForeground(Color.BLACK);
            setFont(SystemTypeMapperPlugin.STELLA_FONT);
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            setMargin(new Insets(5, 10, 5, 10));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());
            // Always maintain Stella blue background and black text
            setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
            setForeground(Color.BLACK);
            // Add cell padding
            table.setRowMargin(10);
            table.setIntercellSpacing(new Dimension(10, 10));
            return this;
        }
    }

    private static class ButtonEditor extends DefaultCellEditor {
        protected final JButton button;
        private final List<SearchResult> results;
        private final Map<String, Element> sourceElements;
        private final String source;
        private int editingRow;

        ButtonEditor(JCheckBox checkBox, List<SearchResult> results, String source, Map<String, Element> sourceElements) {
            super(checkBox);
            this.button = new JButton();
            this.button.setOpaque(true);
            this.button.setBorderPainted(true);
            this.button.setFocusPainted(false);
            this.button.setBackground(SystemTypeMapperPlugin.STELLA_BLUE);
            this.button.setForeground(Color.BLACK);
            this.button.setFont(SystemTypeMapperPlugin.STELLA_FONT);
            this.button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            this.button.setMargin(new Insets(5, 10, 5, 10));
            this.results = results;
            this.source = source;
            this.sourceElements = sourceElements;
            
            this.button.addActionListener(e -> {
                SearchResult r = results.get(editingRow);
                Element elem = sourceElements.get(source);
                if (elem != null) {
                    applyStereotype(elem, r.label);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            button.setText(value.toString());
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        private void applyStereotype(Element elem, String id) {
            Project project = Application.getInstance().getProject();
            boolean sessionCreated = false;
            try {
                if (!SessionManager.getInstance().isSessionCreated()) {
                    SessionManager.getInstance().createSession("Apply SystemType");
                    sessionCreated = true;
                }
                Stereotype st = getOrCreateStereotype("SystemType");
                if (st != null) {
                    if (!StereotypesHelper.hasStereotype(elem, st)) {
                        StereotypesHelper.addStereotype(elem, st);
                    }
                    StereotypesHelper.setStereotypePropertyValue(elem, st, "id", Collections.singletonList(id));
                    Application.getInstance().getGUILog().log("Applied SystemType id=" + id);
                    Application.getInstance().getMainFrame().getBrowser().getContainmentTree().updateUI();
                }
            } catch (Exception e) {
                if (sessionCreated) SessionManager.getInstance().cancelSession();
            } finally {
                if (sessionCreated) SessionManager.getInstance().closeSession();
            }
        }

        private Stereotype getOrCreateStereotype(String name) {
            Project project = Application.getInstance().getProject();
            if (project == null) return null;
            boolean sessionCreated = false;
            Stereotype st = null;
            try {
                if (!SessionManager.getInstance().isSessionCreated()) {
                    SessionManager.getInstance().createSession("Create SystemType");
                    sessionCreated = true;
                }
                Profile prof = StereotypesHelper.getProfile(project, "SystemTypeProfile");
                if (prof == null) {
                    Package root = project.getPrimaryModel();
                    if (root == null) return null;
                    prof = project.getElementsFactory().createProfileInstance();
                    prof.setName("SystemTypeProfile");
                    ModelElementsManager.getInstance().addElement(prof, root);
                }
                st = StereotypesHelper.getStereotype(project, name, prof);
                if (st == null) {
                    com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class meta =
                        StereotypesHelper.getMetaClassByName(project, "Element");
                    if (meta != null) {
                        st = StereotypesHelper.createStereotype(prof, name, Collections.singletonList(meta));
                    }
                }
                if (st != null && StereotypesHelper.getPropertyByName(st, "id") == null) {                    
                    Property p = project.getElementsFactory().createPropertyInstance();
                    p.setName("id");
                    st.getOwnedAttribute().add(p);
                }
            } catch (Exception e) {
                if (sessionCreated) SessionManager.getInstance().cancelSession();
            } finally {
                if (sessionCreated) SessionManager.getInstance().closeSession();
            }
            return st;
        }
    }

    private class ContainmentPrintAction extends MDAction {
        ContainmentPrintAction() {
            super("containmentprint.action", "Search Similar Elements", null, null);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            MainFrame mf = Application.getInstance().getMainFrame();
            ContainmentTree tree = mf.getBrowser().getContainmentTree();
            Node[] nodes = tree.getSelectedNodes();
            if (nodes.length == 0) {
                JOptionPane.showMessageDialog(null, "No elements selected");
                return;
            }
            Map<String, List<SearchResult>> bySrc = new HashMap<>();
            Map<String, Element> elems = new HashMap<>();
            for (Node n : nodes) {
                Object u = n.getUserObject();
                if (u instanceof Element) {
                    Element el = (Element) u;
                    String name = el.getHumanName();
                    StringBuilder builder = new StringBuilder(name);
                    for (Comment c : el.getOwnedComment()) {
                        String text = c.getBody();
                        if (text != null && !text.isEmpty()) builder.append(" ").append(text);
                    }
                    List<SearchResult> results = makeHttpRequest(builder.toString());
                    bySrc.put(name, results);
                    elems.put(name, el);
                }
            }
            new SearchResultsDialog(mf, bySrc, elems).setVisible(true);
        }
        private List<SearchResult> makeHttpRequest(String query) {
            try {
                URL url = new URL(getFlaskApiUrl());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject rq = new JSONObject().put("query", query).put("num_results", 5);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(rq.toString().getBytes("utf-8"));
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line.trim());
                }
                
                // Debug logging
                System.out.println("Raw response: " + sb.toString());
                
                JSONObject response = new JSONObject(sb.toString());
                JSONArray arr = response.getJSONArray("results");
                List<SearchResult> list = new ArrayList<>();
                
                // Get LLM recommendation if available
                String recommendedId = null;
                String recommendationExplanation = null;
                double recommendationConfidence = 0.0;
                
                if (response.has("recommendation")) {
                    JSONObject recommendation = response.getJSONObject("recommendation");
                    recommendedId = recommendation.optString("recommended_id");
                    recommendationExplanation = recommendation.optString("explanation");
                    recommendationConfidence = recommendation.optDouble("confidence", 0.0);
                    
                    // Debug logging
                    System.out.println("Recommendation found:");
                    System.out.println("  ID: " + recommendedId);
                    System.out.println("  Explanation: " + recommendationExplanation);
                    System.out.println("  Confidence: " + recommendationConfidence);
                }
                
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String resultId = o.optString("id", "");
                    boolean isRecommended = recommendedId != null && 
                                         !recommendedId.isEmpty() && 
                                         recommendedId.equals(resultId);
                    
                    // Debug logging
                    System.out.println("Processing result " + i + ":");
                    System.out.println("  ID: " + resultId);
                    System.out.println("  Label: " + o.optString("label"));
                    System.out.println("  Is Recommended: " + isRecommended);
                    
                    list.add(new SearchResult(
                        o.getString("label"),
                        o.getString("definition"),
                        o.getDouble("score"),
                        isRecommended ? recommendationExplanation : null,
                        isRecommended ? recommendationConfidence : 0.0
                    ));
                }
                return list;
            } catch (Exception ex) {
                ex.printStackTrace();
                return Collections.emptyList();
            }
        }
    }
}
