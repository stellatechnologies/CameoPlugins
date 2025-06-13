/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.stellatechnologies.configuration;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.ui.MainFrame;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.prefs.Preferences;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/**
 *
 * @author Jason
 */
public class Configuration extends Plugin {
    // Shared configuration keys
    public static final String STELLA_PREFS_NODE = "stella";
    public static final String DEFAULT_DIR_PROPERTY = "stella.export.defaultdir";
    public static final String INCLUDE_ROW_NUMBERS_PROPERTY = "stella.export.includeRowNumbers";
    public static final String SYSTEM_MAPPER_ENDPOINT = "stella.backend.endpoint";
    
    private ConfigureAction configAction;
    private final Preferences prefs = Preferences.userRoot().node(STELLA_PREFS_NODE);

    @Override
    public void init() {
        configAction = new ConfigureAction();

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
                
                // Add configuration action directly to Stella menu
                stellaCategory.addAction(configAction);
            });
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public boolean close() {
        return true;
    }

    private class ConfigureAction extends MDAction {
        ConfigureAction() {
            super("configuration", "Configure Stella Settings", null, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Stella color palette
            Color stellaBlue = new Color(50, 82, 123);
            Color stellaDark = new Color(33, 37, 41);
            Color stellaLight = new Color(248, 249, 250);
            Color stellaAccent = new Color(255, 102, 102);
            Color stellaGreen = new Color(51, 189, 51);
            Font stellaFont = new Font("Segoe UI", Font.PLAIN, 14);
            Font stellaFontBold = new Font("Segoe UI", Font.BOLD, 14);

            // Create main panel with shadow border
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(stellaLight);
            mainPanel.setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                new EmptyBorder(0, 0, 0, 0)
            ));

            // Create header panel with gradient
            JPanel headerPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw gradient background
                    GradientPaint gradient = new GradientPaint(
                        0, 0, stellaBlue,
                        getWidth(), 0, stellaBlue.darker()
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            };
            headerPanel.setPreferredSize(new Dimension(800, 80));
            headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

            // Add company logo
            try {
                ImageIcon logoIcon = new ImageIcon(getClass().getResource("/resources/stella_logo.png"));
                Image img = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(img));
                logoLabel.setBorder(new EmptyBorder(0, 0, 0, 15));
                headerPanel.add(logoLabel, BorderLayout.WEST);
            } catch (Exception ex) {
                JLabel logoText = new JLabel("STELLA");
                logoText.setFont(stellaFontBold);
                logoText.setForeground(Color.WHITE);
                logoText.setBorder(new EmptyBorder(0, 0, 0, 15));
                headerPanel.add(logoText, BorderLayout.WEST);
            }

            // Add title and help button
            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.setOpaque(false);
            
            JLabel titleLabel = new JLabel("Stella Settings");
            titleLabel.setFont(stellaFontBold);
            titleLabel.setForeground(Color.WHITE);
            titlePanel.add(titleLabel, BorderLayout.CENTER);
            
            JButton helpButton = new JButton("Help & About >");
            helpButton.setFont(stellaFont);
            helpButton.setForeground(Color.WHITE);
            helpButton.setBorderPainted(false);
            helpButton.setContentAreaFilled(false);
            helpButton.setFocusPainted(false);
            helpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            helpButton.addActionListener(evt -> showHelpDialog());
            titlePanel.add(helpButton, BorderLayout.EAST);
            
            headerPanel.add(titlePanel, BorderLayout.CENTER);
            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Create content panel with tabs
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(stellaFont);
            tabbedPane.setBackground(stellaLight);
            
            // System Mapper Settings Panel
            JPanel systemMapperPanel = new JPanel(new GridBagLayout());
            systemMapperPanel.setBackground(stellaLight);
            systemMapperPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(10, 10, 10, 10);

            // Backend endpoint
            JLabel endpointLabel = new JLabel("Backend Endpoint:");
            endpointLabel.setFont(stellaFont);
            systemMapperPanel.add(endpointLabel, gbc);

            gbc.gridx = 1;
            String currentEndpoint = prefs.get(SYSTEM_MAPPER_ENDPOINT, "http://localhost:5000/search");
            JTextField endpointField = new JTextField(currentEndpoint, 28);
            endpointField.setFont(stellaFont);
            systemMapperPanel.add(endpointField, gbc);

            tabbedPane.addTab("System Type Mapper", systemMapperPanel);

            // Export Settings Panel
            JPanel exportPanel = new JPanel(new GridBagLayout());
            exportPanel.setBackground(stellaLight);
            exportPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
            
            gbc.gridx = 0;
            gbc.gridy = 0;
            
            JLabel dirLabel = new JLabel("Default Export Directory:");
            dirLabel.setFont(stellaFont);
            exportPanel.add(dirLabel, gbc);

            gbc.gridx = 1;
            String currentDir = prefs.get(DEFAULT_DIR_PROPERTY, System.getProperty("user.home"));
            JTextField dirField = new JTextField(currentDir, 28);
            dirField.setFont(stellaFont);
            dirField.setEditable(false);
            exportPanel.add(dirField, gbc);

            gbc.gridx = 2;
            JButton browseButton = new JButton("Browse");
            browseButton.setFont(stellaFont);
            browseButton.setBackground(stellaBlue);
            browseButton.setForeground(Color.WHITE);
            browseButton.setBorderPainted(false);
            browseButton.setFocusPainted(false);
            browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            browseButton.addActionListener(evt -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setDialogTitle("Select Default Export Directory");
                chooser.setCurrentDirectory(new File(currentDir));
                if (chooser.showDialog(exportPanel, "Select") == JFileChooser.APPROVE_OPTION) {
                    String selectedDir = chooser.getSelectedFile().getAbsolutePath();
                    dirField.setText(selectedDir);
                }
            });
            exportPanel.add(browseButton, gbc);

            gbc.gridy++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            boolean includeRowNumbers = prefs.getBoolean(INCLUDE_ROW_NUMBERS_PROPERTY, false);
            JCheckBox rowNumbersCheckbox = new JCheckBox("Include row numbers in table exports", includeRowNumbers);
            rowNumbersCheckbox.setFont(stellaFont);
            exportPanel.add(rowNumbersCheckbox, gbc);

            tabbedPane.addTab("Generic Table Export", exportPanel);
            
            mainPanel.add(tabbedPane, BorderLayout.CENTER);

            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(stellaLight);
            buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

            JButton saveButton = new JButton("Save Settings");
            saveButton.setFont(stellaFont);
            saveButton.setBackground(stellaGreen);
            saveButton.setForeground(Color.WHITE);
            saveButton.setBorderPainted(false);
            saveButton.setFocusPainted(false);
            saveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveButton.setPreferredSize(new Dimension(120, 35));
            saveButton.addActionListener(evt -> {
                prefs.put(SYSTEM_MAPPER_ENDPOINT, endpointField.getText());
                prefs.put(DEFAULT_DIR_PROPERTY, dirField.getText());
                prefs.putBoolean(INCLUDE_ROW_NUMBERS_PROPERTY, rowNumbersCheckbox.isSelected());
                Application.getInstance().getGUILog().log("Settings updated successfully");
                SwingUtilities.getWindowAncestor(saveButton).dispose();
            });
            buttonPanel.add(saveButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            // Show dialog
            MainFrame mainFrame = Application.getInstance().getMainFrame();
            try {
                JDialog dialog = new JDialog(mainFrame, "Stella Settings", true);
                dialog.setContentPane(mainPanel);
                dialog.setSize(800, 600);
                dialog.setLocationRelativeTo(mainFrame);
                dialog.setVisible(true);
            } catch (Exception ex) {
                Application.getInstance().getGUILog().log("Error opening Stella Settings dialog: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        private void showHelpDialog() {
            // Stella color palette
            Color stellaBlue = new Color(50, 82, 123);
            Color stellaDark = new Color(33, 37, 41);
            Color stellaLight = new Color(248, 249, 250);
            Font stellaFont = new Font("Segoe UI", Font.PLAIN, 14);
            Font stellaFontBold = new Font("Segoe UI", Font.BOLD, 14);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(stellaLight);
            mainPanel.setBorder(BorderFactory.createCompoundBorder(
                new ShadowBorder(),
                new EmptyBorder(0, 0, 0, 0)
            ));

            // Header
            JPanel headerPanel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gradient = new GradientPaint(
                        0, 0, stellaBlue,
                        getWidth(), 0, stellaBlue.darker()
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            };
            headerPanel.setPreferredSize(new Dimension(900, 80));
            headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

            // Add logo
            try {
                ImageIcon logoIcon = new ImageIcon(getClass().getResource("/resources/stella_logo.png"));
                Image img = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(img));
                logoLabel.setBorder(new EmptyBorder(0, 0, 0, 15));
                headerPanel.add(logoLabel, BorderLayout.WEST);
            } catch (Exception ex) {
                JLabel logoText = new JLabel("STELLA");
                logoText.setFont(stellaFontBold);
                logoText.setForeground(Color.WHITE);
                logoText.setBorder(new EmptyBorder(0, 0, 0, 15));
                headerPanel.add(logoText, BorderLayout.WEST);
            }

            JLabel titleLabel = new JLabel("Help & About");
            titleLabel.setFont(stellaFontBold);
            titleLabel.setForeground(Color.WHITE);
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Content
            JPanel contentPanel = new JPanel();
            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
            contentPanel.setBackground(stellaLight);
            contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

            // About section
            addSection(contentPanel, "About Stella Technologies", 
                "Stella Technologies provides advanced model-based systems engineering tools " +
                "for Cameo Systems Modeler / MagicDraw.", stellaFont, stellaFontBold);

            // Plugins section
            addSection(contentPanel, "Available Plugins", "", stellaFont, stellaFontBold);
            
            // Plugin cards container
            JPanel cardsPanel = new JPanel();
            cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
            cardsPanel.setOpaque(false);
            cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardsPanel.setBorder(new EmptyBorder(0, 20, 15, 0));

            // System Mapper Card
            addPluginCard(cardsPanel,
                "System Mapper",
                "Semantic Search & Type Management",
                "The System Mapper plugin provides semantic search capabilities for system types. " +
                "It helps you find and apply appropriate system types to your model elements " +
                "based on their descriptions.",
                new String[] {
                    "Semantic search for system types",
                    "Automatic type application",
                    "Smart suggestions based on descriptions"
                },
                stellaFont, stellaFontBold, stellaBlue, stellaLight);

            // Generic Table Export Card
            addPluginCard(cardsPanel,
                "Generic Table Export",
                "Table Data Management",
                "The Generic Table Export functionality allows you to export table data from your models " +
                "in various formats. It provides options for customizing the export format and including additional " +
                "information like row numbers.",
                new String[] {
                    "Flexible export formats",
                    "Row number inclusion option",
                    "Custom directory selection"
                },
                stellaFont, stellaFontBold, stellaBlue, stellaLight);

            contentPanel.add(cardsPanel);

            // Contact section
            addSection(contentPanel, "Contact Information", 
                "For support or inquiries:\n" +
                "Email: support@stellatechnologies.space\n" +
                "Website: https://stellatechnologies.space", stellaFont, stellaFontBold);

            // Version info
            addSection(contentPanel, "Version Information", 
                "System Mapper: 1.0.0\n" +
                "Configuration Plugin: 1.0.0", stellaFont, stellaFontBold);

            JScrollPane scrollPane = new JScrollPane(contentPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            // Close button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(stellaLight);
            buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

            JButton closeButton = new JButton("Close");
            closeButton.setFont(stellaFont);
            closeButton.setBackground(stellaBlue);
            closeButton.setForeground(Color.WHITE);
            closeButton.setBorderPainted(false);
            closeButton.setFocusPainted(false);
            closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeButton.setPreferredSize(new Dimension(100, 35));
            closeButton.addActionListener(e -> SwingUtilities.getWindowAncestor(closeButton).dispose());
            buttonPanel.add(closeButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            // Show dialog
            MainFrame mainFrame = Application.getInstance().getMainFrame();
            JDialog dialog = new JDialog(mainFrame, "Help & About", true);
            dialog.setContentPane(mainPanel);
            dialog.setSize(900, 700);
            dialog.setLocationRelativeTo(mainFrame);
            dialog.setVisible(true);
        }

        private void addSection(JPanel panel, String title, String content, Font regularFont, Font boldFont) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(boldFont);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleLabel.setBorder(new EmptyBorder(10, 0, 5, 0));
            panel.add(titleLabel);

            if (!content.isEmpty()) {
                JTextArea textArea = new JTextArea(content);
                textArea.setFont(regularFont);
                textArea.setEditable(false);
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setOpaque(false);
                textArea.setAlignmentX(Component.LEFT_ALIGNMENT);
                textArea.setBorder(new EmptyBorder(0, 0, 15, 0));
                panel.add(textArea);
            }
        }

        private void addPluginCard(JPanel panel, String title, String subtitle, String description, 
                String[] features, Font regularFont, Font boldFont, Color accentColor, Color bgColor) {
            JPanel card = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw card background
                    g2d.setColor(bgColor);
                    g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                    
                    // Draw subtle border
                    g2d.setColor(accentColor.darker());
                    g2d.setStroke(new BasicStroke(1f));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                    
                    g2d.dispose();
                }
            };
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setOpaque(false);
            card.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 0, 20, 0),
                new EmptyBorder(20, 25, 20, 25)
            ));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Title with icon
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            headerPanel.setOpaque(false);
            
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(boldFont.deriveFont(18f));
            titleLabel.setForeground(accentColor);
            headerPanel.add(titleLabel);
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(headerPanel);
            
            // Subtitle
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(regularFont.deriveFont(Font.ITALIC, 15f));
            subtitleLabel.setForeground(accentColor.darker());
            subtitleLabel.setBorder(new EmptyBorder(2, 0, 15, 0));
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(subtitleLabel);
            
            // Description
            JTextArea descArea = new JTextArea(description);
            descArea.setFont(regularFont.deriveFont(14f));
            descArea.setEditable(false);
            descArea.setWrapStyleWord(true);
            descArea.setLineWrap(true);
            descArea.setOpaque(false);
            descArea.setBorder(new EmptyBorder(0, 0, 15, 0));
            descArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(descArea);
            
            // Features
            if (features != null && features.length > 0) {
                JPanel featuresPanel = new JPanel();
                featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
                featuresPanel.setOpaque(false);
                featuresPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                
                for (String feature : features) {
                    JPanel featureRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
                    featureRow.setOpaque(false);
                    
                    // Custom bullet point
                    JLabel bullet = new JLabel("•");
                    bullet.setFont(boldFont);
                    bullet.setForeground(accentColor);
                    featureRow.add(bullet);
                    
                    JLabel featureLabel = new JLabel(feature);
                    featureLabel.setFont(regularFont);
                    featureRow.add(featureLabel);
                    
                    featuresPanel.add(featureRow);
                }
                card.add(featuresPanel);
            }
            
            panel.add(card);
        }

        private class ShadowBorder extends AbstractBorder {
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
    }
}
