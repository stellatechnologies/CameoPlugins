package com.stellatechnologies.generictableexport;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.generictable.GenericTableManager;
import com.nomagic.magicdraw.properties.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import java.awt.GridLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.ContainmentTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.nomagic.actions.ActionsManager;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.ui.browser.Tree;

public class GenericTableExport extends Plugin {
    // Shared configuration keys
    public static final String STELLA_PREFS_NODE = "stella";
    public static final String DEFAULT_DIR_PROPERTY = "stella.export.defaultdir";
    public static final String INCLUDE_ROW_NUMBERS_PROPERTY = "stella.export.includeRowNumbers";
    public static final String SYSTEM_MAPPER_ENDPOINT = "stella.backend.endpoint";
    
    private ExportTableAction exportAction;
    private BatchExportAction batchExportAction;
    private final Preferences prefs = Preferences.userRoot().node(STELLA_PREFS_NODE);

    @Override
    public void init() {
        exportAction = new ExportTableAction();
        batchExportAction = new BatchExportAction();

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
                // Add export action directly to Stella menu
                stellaCategory.addAction(exportAction);
            });

        // Add context menu action for packages
        ActionsConfiguratorsManager.getInstance()
            .addContainmentBrowserContextConfigurator(new BrowserContextAMConfigurator() {
                @Override
                public void configure(ActionsManager manager, Tree tree) {
                    if (tree instanceof ContainmentTree) {
                        Node node = ((ContainmentTree)tree).getSelectedNode();
                        if (node != null && node.getUserObject() instanceof Package) {
                            MDActionsCategory category = new MDActionsCategory("STELLA_MENU_BROWSER", "Stella");
                            category.addAction(batchExportAction);
                            manager.addCategory(category);
                        }
                    }
                }

                @Override
                public int getPriority() {
                    return MEDIUM_PRIORITY;
                }
            });
    }

    @Override
    public boolean isSupported() { return true; }

    @Override
    public boolean close() { return true; }

    private class ExportTableAction extends MDAction {
        ExportTableAction() {
            super("generictableexport.action", "Export Table JSON", null, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DiagramPresentationElement pe = Application.getInstance().getProject().getActiveDiagram();
            if (pe == null) {
                Application.getInstance().getGUILog().log("No active diagram");
                return;
            }
            
            Diagram diagram = pe.getDiagram();
            if (diagram == null) {
                Application.getInstance().getGUILog().log("Invalid diagram");
                return;
            }

            List<Element> rows;
            List<String> colIds;
            try {
                rows = GenericTableManager.getRowElements(diagram);
                colIds = GenericTableManager.getColumnIds(diagram);
            } catch (Exception ex) {
                Application.getInstance().getGUILog().log("Diagram is not a valid Generic Table: " + ex.getMessage());
                return;
            }
            if (rows.isEmpty() || colIds == null || colIds.isEmpty()) {
                Application.getInstance().getGUILog().log("No table data found");
                return;
            }

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("[\n");
            boolean firstRow = true;
            boolean includeRowNumbers = prefs.getBoolean(INCLUDE_ROW_NUMBERS_PROPERTY, false);
            int rowNumber = 1;
            
            for (Element row : rows) {
                if (!firstRow) {
                    jsonBuilder.append(",\n");
                }
                firstRow = false;
                
                jsonBuilder.append("  {\n");
                boolean firstProp = true;

                for (String colId : colIds) {
                    try {
                        Property cell = GenericTableManager.getCellValue(diagram, row, colId);
                        if (cell != null) {
                            String colName = GenericTableManager.getColumnNameById(diagram, colId);
                            // Skip the "#" column unless row numbers are explicitly enabled
                            if (colName.equals("#") && !includeRowNumbers) {
                                continue;
                            }
                            
                            if (!firstProp) {
                                jsonBuilder.append(",\n");
                            }
                            firstProp = false;
                            
                            Object value = cell.getValue();
                            String valueStr;
                            if (value == null) {
                                valueStr = "";
                            } else if (value instanceof Property) {
                                valueStr = ((Property)value).getValue().toString();
                            } else if (value.getClass().isArray()) {
                                Object[] array = (Object[]) value;
                                StringBuilder arrayStr = new StringBuilder();
                                arrayStr.append("[");
                                for (int i = 0; i < array.length; i++) {
                                    if (i > 0) arrayStr.append(", ");
                                    if (array[i] instanceof Property) {
                                        Object propValue = ((Property)array[i]).getValue();
                                        arrayStr.append(propValue != null ? propValue.toString() : "");
                                    } else if (array[i] != null) {
                                        arrayStr.append(array[i].toString());
                                    }
                                }
                                arrayStr.append("]");
                                valueStr = arrayStr.toString();
                            } else {
                                valueStr = value.toString();
                            }
                            
                            jsonBuilder.append("    \"").append(escapeJson(colName)).append("\": \"")
                                     .append(escapeJson(valueStr)).append("\"");
                        }
                    } catch (Exception ignore) {
                        // skip unsupported/derived columns
                    }
                }
                jsonBuilder.append("\n  }");
                rowNumber++;
            }
            jsonBuilder.append("\n]");

            // Show save dialog
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save JSON Export");
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            
            // Set current directory to configured default or last used
            String defaultDir = prefs.get(DEFAULT_DIR_PROPERTY, System.getProperty("user.home"));
            chooser.setCurrentDirectory(new File(defaultDir));
            
            // Set default filename based on diagram name
            String defaultFileName = diagram.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json";
            chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), defaultFileName));

            if (chooser.showSaveDialog(Application.getInstance().getMainFrame()) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                // Add .json extension if not present
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write(jsonBuilder.toString());
                    Application.getInstance().getGUILog().log("Exported Generic Table JSON to: " + file.getAbsolutePath());
                    
                    // Save this directory as the last used
                    prefs.put(DEFAULT_DIR_PROPERTY, file.getParent());
                } catch (IOException io) {
                    Application.getInstance().getGUILog().log("Failed to write JSON: " + io.getMessage());
                }
            }
        }
    }

    private List<JSONObject> getTableData(Diagram diagram) {
        List<JSONObject> tableRows = new ArrayList<>();
        List<Element> rows;
        List<String> colIds;

        try {
            rows = GenericTableManager.getRowElements(diagram);
            colIds = GenericTableManager.getColumnIds(diagram);
        } catch (Exception ex) {
            return tableRows;
        }

        if (rows.isEmpty() || colIds == null || colIds.isEmpty()) {
            return tableRows;
        }

        boolean includeRowNumbers = prefs.getBoolean(INCLUDE_ROW_NUMBERS_PROPERTY, false);

        for (Element row : rows) {
            JSONObject rowJson = new JSONObject();
            for (String colId : colIds) {
                try {
                    Property cell = GenericTableManager.getCellValue(diagram, row, colId);
                    if (cell != null) {
                        String colName = GenericTableManager.getColumnNameById(diagram, colId);
                        if (colName.equals("#") && !includeRowNumbers) {
                            continue;
                        }

                        Object value = cell.getValue();
                        if (value instanceof Property) {
                            value = ((Property)value).getValue();
                        }
                        
                        if (value != null && value.getClass().isArray()) {
                            Object[] array = (Object[]) value;
                            JSONArray jsonArray = new JSONArray();
                            for (Object item : array) {
                                if (item instanceof Property) {
                                    jsonArray.put(((Property)item).getValue());
                                } else {
                                    jsonArray.put(item);
                                }
                            }
                            rowJson.put(colName, jsonArray);
                        } else {
                            rowJson.put(colName, value);
                        }
                    }
                } catch (Exception ignore) {
                    // ignore
                }
            }
            if (rowJson.length() > 0) {
                tableRows.add(rowJson);
            }
        }
        return tableRows;
    }

    private void findGenericTablesInPackage(Element element, Map<String, List<JSONObject>> results) {
        if (element instanceof Diagram) {
            Diagram diagram = (Diagram) element;
            try {
                // Check if it's a generic table by attempting to get its columns
                List<String> columns = GenericTableManager.getColumnIds(diagram);
                if (columns != null) {
                    List<JSONObject> tableData = getTableData(diagram);
                    if (!tableData.isEmpty()) {
                        // Get the owning package by traversing up the element hierarchy
                        Element owner = diagram.getOwner();
                        while (owner != null && !(owner instanceof Package)) {
                            owner = owner.getOwner();
                        }
                        String packageName = owner != null ? ((Package)owner).getName() : "Unknown";
                        results.computeIfAbsent(packageName, k -> new ArrayList<>()).addAll(tableData);
                    }
                }
            } catch (Exception e) {
                // Not a generic table or other error, ignore
            }
        }

        if (element instanceof Package) {
            Package pkg = (Package) element;
            for (Element ownedElement : pkg.getOwnedElement()) {
                findGenericTablesInPackage(ownedElement, results);
            }
        }
    }

    private class BatchExportAction extends MDAction {
        BatchExportAction() {
            super("generictableexport.batchexport", "Export All Tables in Package (JSON)", null, null);
        }

        @Override
        public void updateState() {
            Element selected = getSelectedElementInContainmentTree();
            setEnabled(selected instanceof Package);
        }

        private Element getSelectedElementInContainmentTree() {
            ContainmentTree tree = Application.getInstance().getMainFrame().getBrowser().getContainmentTree();
            if (tree != null) {
                Node selectedNode = tree.getSelectedNode();
                if (selectedNode != null && selectedNode.getUserObject() instanceof Element) {
                    return (Element) selectedNode.getUserObject();
                }
            }
            return null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Element selected = getSelectedElementInContainmentTree();
            if (!(selected instanceof Package)) {
                Application.getInstance().getGUILog().log("Please select a package in the containment browser.");
                return;
            }
            Package topPackage = (Package) selected;

            Map<String, List<JSONObject>> results = new HashMap<>();
            findGenericTablesInPackage(topPackage, results);

            if (results.isEmpty()) {
                Application.getInstance().getGUILog().log("No generic tables found in package '" + topPackage.getName() + "' or its sub-packages.");
                return;
            }

            JSONObject finalJson = new JSONObject();
            for (Map.Entry<String, List<JSONObject>> entry : results.entrySet()) {
                finalJson.put(entry.getKey(), new JSONArray(entry.getValue()));
            }

            // Show save dialog
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save JSON Export");
            chooser.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
            
            String defaultDir = prefs.get(DEFAULT_DIR_PROPERTY, System.getProperty("user.home"));
            chooser.setCurrentDirectory(new File(defaultDir));
            
            String defaultFileName = topPackage.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_export.json";
            chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), defaultFileName));

            if (chooser.showSaveDialog(Application.getInstance().getMainFrame()) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                    bw.write(finalJson.toString(2)); // Using 2 for indentation
                    Application.getInstance().getGUILog().log("Exported package tables to: " + file.getAbsolutePath());
                    prefs.put(DEFAULT_DIR_PROPERTY, file.getParent());
                } catch (IOException io) {
                    Application.getInstance().getGUILog().log("Failed to write JSON: " + io.getMessage());
                }
            }
        }
    }

    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
