package com.stellatechnologies.nodalanalysis;

import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.ui.MainFrame;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Relationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.BehavioredClassifier;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.Behavior;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import com.nomagic.magicdraw.ui.browser.Tree;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import java.util.stream.Collectors;
import java.awt.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.awt.BasicStroke;
import java.awt.Stroke;

import com.nomagic.magicdraw.ui.browser.Node;
import java.util.Queue;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.JCheckBox;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.stream.Collectors;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;

public class NodalAnalysis extends Plugin {

    private static String getFormattedElementName(NamedElement el) {
        if (el == null) return "";
        return String.format("(%s) %s", el.getHumanType(), el.getHumanName());
    }

    @Override
    public void init() {
        ActionsConfiguratorsManager.getInstance()
            .addMainMenuConfigurator(manager -> {
                MDActionsCategory stella = (MDActionsCategory) manager.getCategory("STELLA_MENU");
                if (stella == null) {
                    stella = new MDActionsCategory(
                        "STELLA_MENU", "Stella", null, null
                    );
                    stella.setNested(true);
                    manager.addCategory(stella);
                }
                stella.addAction(new RelationshipExplorerAction());
                stella.addAction(new PathFinderAction());
            });
    }

    @Override
    public boolean isSupported() { return true; }
    @Override
    public boolean close() { return true; }

    // Recursively collect NamedElements and Relationships
    private static void collectElements(Namespace ns,
                                        List<NamedElement> nodes,
                                        List<Relationship> rels,
                                        Map<Element, Element> ownershipRels,
                                        List<Property> typedAttributeRels,
                                        Map<Element, List<Stereotype>> stereotypeRels,
                                        Map<BehavioredClassifier, Behavior> classifierBehaviorRels) {
        for (Element el : ns.getOwnedElement()) {
            if (el instanceof Relationship) {
                rels.add((Relationship) el);
            } else if (el instanceof NamedElement) {
                nodes.add((NamedElement) el);
                // Track ownership relationship
                ownershipRels.put(el, ns);
            }

            // Get Stereotypes
            Collection<Stereotype> appliedStereotypes = el.getAppliedStereotype();
            if (appliedStereotypes != null && !appliedStereotypes.isEmpty()) {
                stereotypeRels.put(el, new ArrayList<>(appliedStereotypes));
            }

            // Get Classifier Behavior
            if (el instanceof BehavioredClassifier) {
                BehavioredClassifier bc = (BehavioredClassifier) el;
                Behavior behavior = bc.getClassifierBehavior();
                if (behavior != null) {
                    classifierBehaviorRels.put(bc, behavior);
                }
            }

            if (el instanceof Classifier) {
                for (Property prop : ((Classifier)el).getAttribute()) {
                    if (prop != null && prop.getType() != null) {
                        typedAttributeRels.add(prop);
                    }
                }
            }

            if (el instanceof Namespace) {
                collectElements((Namespace) el, nodes, rels, ownershipRels, typedAttributeRels, stereotypeRels, classifierBehaviorRels);
            }
        }
    }

    private class PathFinderAction extends MDAction {
        PathFinderAction() {
            super("path.finder.action", "Find Paths Between Elements", null, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Project project = Application.getInstance().getProject();
            if (project == null) {
                JOptionPane.showMessageDialog(null, "No open project.");
                return;
            }

            Tree selectionTree = Application.getInstance().getMainFrame().getBrowser().getActiveTree();
            if (selectionTree == null) {
                JOptionPane.showMessageDialog(null, "Please select two elements in the containment tree.");
                return;
            }
            Node[] selectedNodes = selectionTree.getSelectedNodes();
            if (selectedNodes == null || selectedNodes.length != 2) {
                JOptionPane.showMessageDialog(null, "Please select exactly two elements in the containment tree.");
                return;
            }

            Element[] selectedElements = new Element[2];
            for (int i = 0; i < 2; i++) {
                Object userObject = selectedNodes[i].getUserObject();
                if (userObject instanceof Element) {
                    selectedElements[i] = (Element) userObject;
                } else {
                     JOptionPane.showMessageDialog(null, "The selected items must be model elements.");
                     return;
                }
            }

            if (!(selectedElements[0] instanceof NamedElement) || !(selectedElements[1] instanceof NamedElement)) {
                JOptionPane.showMessageDialog(null, "The selected items must be named model elements.");
                return;
            }

            NamedElement startNode = (NamedElement) selectedElements[0];
            NamedElement endNode = (NamedElement) selectedElements[1];

            MainFrame mf = Application.getInstance().getMainFrame();
            Namespace root = project.getModel();
            List<NamedElement> allNodes = new ArrayList<>();
            List<Relationship> allRels = new ArrayList<>();
            Map<Element, Element> allOwnershipRels = new HashMap<>();
            List<Property> allTypedAttributeRels = new ArrayList<>();
            Map<Element, List<Stereotype>> allStereotypeRels = new HashMap<>();
            Map<BehavioredClassifier, Behavior> allClassifierBehaviorRels = new HashMap<>();
            collectElements(root, allNodes, allRels, allOwnershipRels, allTypedAttributeRels, allStereotypeRels, allClassifierBehaviorRels);

            List<List<NamedElement>> initialPaths = findPaths(startNode, endNode, 0, allNodes, allRels, allOwnershipRels, allTypedAttributeRels, allStereotypeRels, allClassifierBehaviorRels, false);

            if (initialPaths.isEmpty()) {
                JOptionPane.showMessageDialog(mf, "No paths found between " + startNode.getHumanName() + " and " + endNode.getHumanName());
            } else {
                PathFinderDialog dlg = new PathFinderDialog(mf, startNode, endNode, initialPaths, allNodes, allRels, allOwnershipRels, allTypedAttributeRels, allStereotypeRels, allClassifierBehaviorRels);
                dlg.setVisible(true);
            }
        }
    }
    
    private static void addEdge(Map<NamedElement, Set<NamedElement>> adj, Element e1, Element e2) {
        if (e1 instanceof NamedElement && e2 instanceof NamedElement) {
            NamedElement n1 = (NamedElement) e1;
            NamedElement n2 = (NamedElement) e2;
            adj.computeIfAbsent(n1, k -> new HashSet<>()).add(n2);
            adj.computeIfAbsent(n2, k -> new HashSet<>()).add(n1);
        }
    }

    private static List<List<NamedElement>> findPaths(NamedElement start, NamedElement end, int minLength, List<NamedElement> allNodes, List<Relationship> allRels, Map<Element, Element> allOwnershipRels, List<Property> allTypedAttributeRels, Map<Element, List<Stereotype>> allStereotypeRels, Map<BehavioredClassifier, Behavior> allClassifierBehaviorRels, boolean ignoreStereotypes) {
        Map<NamedElement, Set<NamedElement>> adj = new HashMap<>();
        List<NamedElement> graphNodes = new ArrayList<>(allNodes);
        // Also add stereotypes to the list of nodes for graph traversal
        if (!ignoreStereotypes) {
            allStereotypeRels.values().stream().flatMap(Collection::stream).distinct().forEach(s -> {
                if (!graphNodes.contains(s)) {
                    graphNodes.add(s);
                }
            });
        }
        
        for (NamedElement node : graphNodes) {
            adj.putIfAbsent(node, new HashSet<>());
        }
        for (Relationship r : allRels) {
            List<Element> ends = new ArrayList<>(r.getRelatedElement());
            if (ends.size() == 2) {
                addEdge(adj, ends.get(0), ends.get(1));
            }
        }
        for (Map.Entry<Element, Element> entry : allOwnershipRels.entrySet()) {
            addEdge(adj, entry.getKey(), entry.getValue());
        }
        for (Property prop : allTypedAttributeRels) {
            addEdge(adj, prop.getOwner(), prop.getType());
        }
        for (Map.Entry<BehavioredClassifier, Behavior> entry : allClassifierBehaviorRels.entrySet()) {
            addEdge(adj, entry.getKey(), entry.getValue());
        }
        if (!ignoreStereotypes) {
            for (Map.Entry<Element, List<Stereotype>> entry : allStereotypeRels.entrySet()) {
                for (Stereotype s : entry.getValue()) {
                    addEdge(adj, entry.getKey(), s);
                }
            }
        }

        List<List<NamedElement>> foundPaths = new ArrayList<>();
        Queue<List<NamedElement>> queue = new LinkedList<>();
        List<NamedElement> initialPath = new ArrayList<>();
        initialPath.add(start);
        queue.add(initialPath);

        int foundPathLength = -1;

        while (!queue.isEmpty()) {
            List<NamedElement> currentPath = queue.poll();
            NamedElement lastNode = currentPath.get(currentPath.size() - 1);

            if (foundPathLength != -1 && currentPath.size() > foundPathLength) {
                break; // We are looking for paths of the same shortest length
            }

            if (lastNode.equals(end)) {
                if (currentPath.size() -1 >= minLength) {
                    if (foundPathLength == -1) {
                        foundPathLength = currentPath.size();
                    }
                    foundPaths.add(currentPath);
                }
                continue;
            }
            
            Set<NamedElement> neighbors = adj.get(lastNode);
            if (neighbors != null) {
                for (NamedElement neighbor : neighbors) {
                    if (!currentPath.contains(neighbor)) { // Avoid cycles
                        List<NamedElement> newPath = new ArrayList<>(currentPath);
                        newPath.add(neighbor);
                        queue.add(newPath);
                    }
                }
            }
        }
        return foundPaths;
    }

    private static class PathFinderDialog extends JDialog {
        private final NamedElement startNode, endNode;
        private final List<NamedElement> allNodes;
        private final List<Relationship> allRels;
        private final Map<Element, Element> allOwnershipRels;
        private final List<Property> allTypedAttributeRels;
        private final Map<Element, List<Stereotype>> allStereotypeRels;
        private final Map<BehavioredClassifier, Behavior> allClassifierBehaviorRels;
        
        private List<List<NamedElement>> currentPaths;
        private int lastSearchedLength = 0;

        private final GraphPanel graphPanel;
        private final DefaultListModel<List<NamedElement>> pathListModel;
        private final JList<List<NamedElement>> pathList;
        private final JSpinner lengthSpinner;
        private final JButton findButton;
        private final JCheckBox ignoreStereotypesCheckbox;

        PathFinderDialog(Frame owner, NamedElement startNode, NamedElement endNode, List<List<NamedElement>> initialPaths, List<NamedElement> allNodes, List<Relationship> allRels, Map<Element, Element> allOwnershipRels, List<Property> allTypedAttributeRels, Map<Element, List<Stereotype>> allStereotypeRels, Map<BehavioredClassifier, Behavior> allClassifierBehaviorRels) {
            super(owner, "Path Finder", false);
            this.startNode = startNode;
            this.endNode = endNode;
            this.allNodes = allNodes;
            this.allRels = allRels;
            this.allOwnershipRels = allOwnershipRels;
            this.allTypedAttributeRels = allTypedAttributeRels;
            this.allStereotypeRels = allStereotypeRels;
            this.allClassifierBehaviorRels = allClassifierBehaviorRels;
            
            if (!initialPaths.isEmpty()) {
                this.lastSearchedLength = initialPaths.get(0).size() - 1;
            }

            setLayout(new BorderLayout());
            setSize(900, 700);

            // Top control panel
            JPanel controlPanel = new JPanel();
            SpinnerNumberModel spinnerModel = new SpinnerNumberModel(lastSearchedLength, 0, 100, 1);
            lengthSpinner = new JSpinner(spinnerModel);
            findButton = new JButton("Find Paths");
            
            controlPanel.add(new JLabel("Path Length:"));
            controlPanel.add(lengthSpinner);
            controlPanel.add(findButton);
            ignoreStereotypesCheckbox = new JCheckBox("Ignore Stereotypes");
            controlPanel.add(ignoreStereotypesCheckbox);

            // Main content
            pathListModel = new DefaultListModel<>();
            pathList = new JList<>(pathListModel);
            pathList.setCellRenderer(new PathRenderer());
            
            graphPanel = new GraphPanel(allNodes, allRels, allOwnershipRels, allStereotypeRels, allClassifierBehaviorRels, allTypedAttributeRels);

            JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(pathList),
                new JScrollPane(graphPanel)
            );
            splitPane.setDividerLocation(300);

            add(controlPanel, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
            
            // Listeners
            findButton.addActionListener(e -> findNewPaths());
            pathList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    List<NamedElement> selectedPath = pathList.getSelectedValue();
                    if (selectedPath != null) {
                        graphPanel.setHighlightedPath(selectedPath);
                    }
                }
            });

            // Initial population
            updatePathsList(initialPaths);
            setLocationRelativeTo(owner);
        }

        private void findNewPaths() {
            int desiredLength = (int) lengthSpinner.getValue();
            boolean ignoreStereotypes = ignoreStereotypesCheckbox.isSelected();
            List<List<NamedElement>> newPaths = findPaths(startNode, endNode, desiredLength, allNodes, allRels, allOwnershipRels, allTypedAttributeRels, allStereotypeRels, allClassifierBehaviorRels, ignoreStereotypes);
            
            if (newPaths.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No paths of length >= " + desiredLength + " found.");
                updatePathsList(newPaths); // Clear the list
            } else {
                int newLength = newPaths.get(0).size() - 1;
                lastSearchedLength = newLength;
                lengthSpinner.setValue(newLength);
                updatePathsList(newPaths);
            }
        }
        
        private void updatePathsList(List<List<NamedElement>> paths) {
            this.currentPaths = paths;
            pathListModel.clear();
            for (List<NamedElement> path : paths) {
                pathListModel.addElement(path);
            }

            graphPanel.setPaths(paths);

            if (!paths.isEmpty()) {
                pathList.setSelectedIndex(0);
            } else {
                graphPanel.setPaths(null); // Clear graph if no paths
            }
        }
    }

    private static class PathRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<NamedElement> path = (List<NamedElement>) value;
                String pathStr = path.stream()
                                     .map(NodalAnalysis::getFormattedElementName)
                                     .collect(Collectors.joining(" → "));
                setText(pathStr);
            }
            return this;
        }
    }

    private class RelationshipExplorerAction extends MDAction {
        RelationshipExplorerAction() {
            super("relationship.explorer.action", "Explore Relationships", null, null);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MainFrame mf = Application.getInstance().getMainFrame();
            Project project = Application.getInstance().getProject();
            if (project == null) {
                JOptionPane.showMessageDialog(null, "No open project.");
                return;
            }
            Namespace root = project.getModel();
            List<NamedElement> nodes = new ArrayList<>();
            List<Relationship> rels = new ArrayList<>();
            Map<Element, Element> ownershipRels = new HashMap<>();
            List<Property> typedAttributeRels = new ArrayList<>();
            Map<Element, List<Stereotype>> stereotypeRels = new HashMap<>();
            Map<BehavioredClassifier, Behavior> classifierBehaviorRels = new HashMap<>();
            collectElements(root, nodes, rels, ownershipRels, typedAttributeRels, stereotypeRels, classifierBehaviorRels);

            RelationshipExplorerDialog dlg = new RelationshipExplorerDialog(mf, nodes, rels, ownershipRels, stereotypeRels, classifierBehaviorRels, typedAttributeRels);
            dlg.setVisible(true);
        }
    }

    private static class RelationshipExplorerDialog extends JDialog {
        RelationshipExplorerDialog(Frame owner,
                                   List<NamedElement> nodes,
                                   List<Relationship> rels,
                                   Map<Element, Element> ownershipRels,
                                   Map<Element, List<Stereotype>> stereotypeRels,
                                   Map<BehavioredClassifier, Behavior> classifierBehaviorRels,
                                   List<Property> typedAttributeRels) {
            super(owner, "Relationship Explorer", false);
            setLayout(new BorderLayout());
            setSize(900, 600);

            JTabbedPane tabs = new JTabbedPane();

            // List view
            JPanel listPanel = new JPanel(new BorderLayout());
            String[] nodeNames = nodes.stream()
                                      .map(NodalAnalysis::getFormattedElementName)
                                      .toArray(String[]::new);
            
            // Create initial relationship strings including both explicit and ownership relationships
            List<String> allRelStrings = new ArrayList<>();
            
            // Add explicit relationships
            for (Relationship r : rels) {
                List<Element> ends = new ArrayList<>(r.getRelatedElement());
                if (ends.size() == 2 
                    && ends.get(0) instanceof NamedElement 
                    && ends.get(1) instanceof NamedElement) {
                    String a = getFormattedElementName((NamedElement) ends.get(0));
                    String b = getFormattedElementName((NamedElement) ends.get(1));
                    allRelStrings.add(r.getClass().getSimpleName() + ": " + a + " → " + b);
                } else {
                    allRelStrings.add(r.getClass().getSimpleName() + " (" + ends.size() + " ends)");
                }
            }
            
            // Add ownership relationships
            for (Map.Entry<Element, Element> entry : ownershipRels.entrySet()) {
                if (entry.getKey() instanceof NamedElement && entry.getValue() instanceof NamedElement) {
                    String child = getFormattedElementName((NamedElement) entry.getKey());
                    String parent = getFormattedElementName((NamedElement) entry.getValue());
                    allRelStrings.add("Ownership: " + parent + " owns → " + child);
                }
            }

            // Add typed attribute relationships
            for (Property prop : typedAttributeRels) {
                if (prop.getOwner() instanceof NamedElement && prop.getType() instanceof NamedElement) {
                    String ownerName = getFormattedElementName((NamedElement) prop.getOwner());
                    String propName = prop.getHumanName();
                    String typeName = getFormattedElementName((NamedElement) prop.getType());
                    allRelStrings.add("Attribute: " + ownerName + "." + propName + " : " + typeName);
                }
            }

            // Add stereotype relationships
            for (Map.Entry<Element, List<Stereotype>> entry : stereotypeRels.entrySet()) {
                if (entry.getKey() instanceof NamedElement) {
                    String elementName = getFormattedElementName((NamedElement) entry.getKey());
                    for (Stereotype stereotype : entry.getValue()) {
                        allRelStrings.add("Stereotype: «" + stereotype.getName() + "» applied to " + elementName);
                    }
                }
            }

            // Add classifier behavior relationships
            for (Map.Entry<BehavioredClassifier, Behavior> entry : classifierBehaviorRels.entrySet()) {
                String classifierName = getFormattedElementName(entry.getKey());
                String behaviorName = getFormattedElementName(entry.getValue());
                allRelStrings.add("Classifier Behavior: " + classifierName + " has behavior " + behaviorName);
            }

            String[] relNames = allRelStrings.toArray(new String[0]);

            JList<String> nodeList = new JList<>(nodeNames);
            JList<String> relList = new JList<>(relNames);
            
            // Create a map to store node name to element mapping for quick lookup
            Map<String, NamedElement> nameToElement = new HashMap<>();
            for (NamedElement node : nodes) {
                nameToElement.put(getFormattedElementName(node), node);
            }
            
            // Create graph panel
            GraphPanel graph = new GraphPanel(nodes, rels, ownershipRels, stereotypeRels, 
                                            classifierBehaviorRels, typedAttributeRels);

            // Add selection listener to update relationships and graph
            nodeList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    String selectedName = nodeList.getSelectedValue();
                    if (selectedName != null) {
                        final NamedElement selected = nameToElement.get(selectedName);
                        // Update graph view
                        graph.setSelectedElement(selected);
                        
                        // Update relationship list
                        List<String> filteredRelStrings = new ArrayList<>();
                        
                        // Filter explicit relationships
                        rels.stream()
                            .filter(r -> {
                                List<Element> ends = new ArrayList<>(r.getRelatedElement());
                                return ends.contains(selected);
                            })
                            .forEach(r -> {
                                List<Element> ends = new ArrayList<>(r.getRelatedElement());
                                if (ends.size() == 2 
                                    && ends.get(0) instanceof NamedElement 
                                    && ends.get(1) instanceof NamedElement) {
                                    String a = getFormattedElementName((NamedElement) ends.get(0));
                                    String b = getFormattedElementName((NamedElement) ends.get(1));
                                    filteredRelStrings.add(r.getClass().getSimpleName() + ": " + a + " → " + b);
                                } else {
                                    filteredRelStrings.add(r.getClass().getSimpleName() + " (" + ends.size() + " ends)");
                                }
                            });
                        
                        // Filter ownership relationships
                        for (Map.Entry<Element, Element> entry : ownershipRels.entrySet()) {
                            if (entry.getKey() == selected || entry.getValue() == selected) {
                                if (entry.getKey() instanceof NamedElement && entry.getValue() instanceof NamedElement) {
                                    String child = getFormattedElementName((NamedElement) entry.getKey());
                                    String parent = getFormattedElementName((NamedElement) entry.getValue());
                                    filteredRelStrings.add("Ownership: " + parent + " owns → " + child);
                                }
                            }
                        }

                        // Filter stereotype relationships
                        if (stereotypeRels.containsKey(selected)) {
                            for (Stereotype stereotype : stereotypeRels.get(selected)) {
                                filteredRelStrings.add("Stereotype: «" + stereotype.getName() + "» applied to " + getFormattedElementName(selected));
                            }
                        }

                        // Filter classifier behavior relationships
                        if (classifierBehaviorRels.containsKey(selected)) {
                            Behavior behavior = classifierBehaviorRels.get(selected);
                            filteredRelStrings.add("Classifier Behavior: " + getFormattedElementName(selected) + " has behavior " + getFormattedElementName(behavior));
                        }
                        for (Map.Entry<BehavioredClassifier, Behavior> entry : classifierBehaviorRels.entrySet()) {
                            if (entry.getValue() == selected) {
                                filteredRelStrings.add("Classifier Behavior: " + getFormattedElementName(entry.getKey()) + " has behavior " + getFormattedElementName(selected));
                            }
                        }

                        // Filter typed attribute relationships
                        typedAttributeRels.stream()
                            .filter(prop -> prop.getOwner() == selected || prop.getType() == selected)
                            .forEach(prop -> {
                                if (prop.getOwner() instanceof NamedElement && prop.getType() instanceof NamedElement) {
                                    String ownerName = getFormattedElementName((NamedElement) prop.getOwner());
                                    String propName = prop.getHumanName();
                                    String typeName = getFormattedElementName((NamedElement) prop.getType());
                                    filteredRelStrings.add("Attribute: " + ownerName + "." + propName + " : " + typeName);
                                }
                            });
                        
                        relList.setListData(filteredRelStrings.toArray(new String[0]));
                    }
                }
            });

            JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(nodeList),
                new JScrollPane(relList)
            );
            split.setDividerLocation(300);
            listPanel.add(split, BorderLayout.CENTER);
            tabs.addTab("Lists", listPanel);

            // Graph view
            tabs.addTab("Graph", graph);

            add(tabs, BorderLayout.CENTER);
            setLocationRelativeTo(owner);
        }
    }

    private static class GraphPanel extends JPanel {
        private final List<NamedElement> nodes;
        private final List<Relationship> rels;
        private final Map<Element, Element> ownershipRels;
        private final Map<Element, List<Stereotype>> stereotypeRels;
        private final Map<BehavioredClassifier, Behavior> classifierBehaviorRels;
        private final List<Property> typedAttributeRels;
        private NamedElement selectedElement;

        private Map<NamedElement, Point> nodePositions = new HashMap<>();
        private List<List<NamedElement>> allPaths = new ArrayList<>();
        private List<NamedElement> highlightedPath;

        private NamedElement draggingNode = null;
        private Point dragOffset;

        private static final class NodePair {
            private final NamedElement n1;
            private final NamedElement n2;
        
            public NodePair(NamedElement n1, NamedElement n2) {
                this.n1 = n1;
                this.n2 = n2;
            }
        
            public NamedElement n1() {
                return n1;
            }
        
            public NamedElement n2() {
                return n2;
            }
        
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                NodePair nodePair = (NodePair) o;
                return (Objects.equals(n1, nodePair.n1) && Objects.equals(n2, nodePair.n2)) ||
                       (Objects.equals(n1, nodePair.n2) && Objects.equals(n2, nodePair.n1));
            }
        
            @Override
            public int hashCode() {
                // Commutative hash code
                return Objects.hash(n1) + Objects.hash(n2);
            }
        }

        GraphPanel(List<NamedElement> nodes, List<Relationship> rels,
                  Map<Element, Element> ownershipRels,
                  Map<Element, List<Stereotype>> stereotypeRels,
                  Map<BehavioredClassifier, Behavior> classifierBehaviorRels,
                  List<Property> typedAttributeRels) {
            this.nodes = nodes;
            this.rels = rels;
            this.ownershipRels = ownershipRels;
            this.stereotypeRels = stereotypeRels;
            this.classifierBehaviorRels = classifierBehaviorRels;
            this.typedAttributeRels = typedAttributeRels;
            setBackground(Color.WHITE);

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int nodeDiameter = 40;
                    for (Map.Entry<NamedElement, Point> entry : nodePositions.entrySet()) {
                        Point pos = entry.getValue();
                        if (e.getPoint().distance(pos) < nodeDiameter / 2.0) {
                            draggingNode = entry.getKey();
                            dragOffset = new Point(e.getX() - pos.x, e.getY() - pos.y);
                            return;
                        }
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggingNode != null) {
                        Point newPos = e.getPoint();
                        newPos.translate(-dragOffset.x, -dragOffset.y);
                        nodePositions.put(draggingNode, newPos);
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggingNode = null;
                }
            };
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        public void setPaths(List<List<NamedElement>> paths) {
            this.selectedElement = null; // Ensure single-focus mode is off
            this.allPaths = (paths != null) ? paths : new ArrayList<>();
            this.nodePositions.clear(); // Force recalculation of layout
            this.highlightedPath = null;
            if (!this.allPaths.isEmpty()) {
                // The selection listener will call setHighlightedPath
            }
            repaint();
        }

        public void setHighlightedPath(List<NamedElement> path) {
            this.highlightedPath = path;
            repaint();
        }

        public void setPathToDraw(List<NamedElement> path) {
            // This is now handled by setPaths and setHighlightedPath
            if (path != null) {
                setPaths(List.of(path));
            } else {
                setPaths(null);
            }
        }

        public void setSelectedElement(NamedElement element) {
            this.selectedElement = element;
            this.allPaths.clear(); // Ensure path-mode is off
            this.nodePositions.clear();
            repaint();
        }

        private Set<NamedElement> getUniqueNodes() {
            return allPaths.stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            if (allPaths.isEmpty() && selectedElement == null) {
                return;
            }

            if (!allPaths.isEmpty()) {
                drawPathNetwork(g2);
            } else if (selectedElement != null) {
                drawSingleElementFocus(g2);
            }
        }

        private void drawSingleElementFocus(Graphics2D g2) {
             // Get neighbors of selected element
            Set<NamedElement> relevantNodes = getNeighbors(selectedElement);
            relevantNodes.add(selectedElement);
            int w = getWidth(), h = getHeight();
            int cx = w/2, cy = h/2;

            // Position nodes: selected node in center, neighbors in a circle
            Map<NamedElement, Point> pos = new HashMap<>();
            
            // Place selected node in center
            pos.put(selectedElement, new Point(cx, cy));

            // Place neighbors in a circle around the selected node
            int radius = Math.min(w, h)/4;
            List<NamedElement> neighborsList = new ArrayList<>(relevantNodes);
            neighborsList.remove(selectedElement);
            for (int i = 0; i < neighborsList.size(); i++) {
                double angle = 2 * Math.PI * i / neighborsList.size();
                int x = cx + (int)(radius * Math.cos(angle));
                int y = cy + (int)(radius * Math.sin(angle));
                pos.put(neighborsList.get(i), new Point(x, y));
            }
            nodePositions = pos;

            Set<NodePair> edges = new HashSet<>();
            for(NamedElement neighbor : relevantNodes) {
                if(neighbor != selectedElement) {
                    edges.add(new NodePair(selectedElement, neighbor));
                }
            }
            drawNetwork(g2, relevantNodes, Set.of(selectedElement), Set.of(), edges);
        }
        
        private Set<NamedElement> getNeighbors(NamedElement element) {
            Set<NamedElement> neighbors = new HashSet<>();
            
            if (element == null) return neighbors;

            // Add neighbors from explicit relationships
            for (Relationship r : rels) {
                List<Element> ends = new ArrayList<>(r.getRelatedElement());
                if (ends.contains(element)) {
                    for (Element end : ends) {
                        if (end instanceof NamedElement && end != element) {
                            neighbors.add((NamedElement) end);
                        }
                    }
                }
            }

            // Add neighbors from ownership relationships
            Element owner = ownershipRels.get(element);
            if (owner instanceof NamedElement) {
                neighbors.add((NamedElement) owner);
            }
            for (Map.Entry<Element, Element> entry : ownershipRels.entrySet()) {
                if (entry.getValue() == element && entry.getKey() instanceof NamedElement) {
                    neighbors.add((NamedElement) entry.getKey());
                }
            }

            // Add neighbors from classifier behaviors
            if (element instanceof BehavioredClassifier) {
                Behavior behavior = classifierBehaviorRels.get(element);
                if (behavior != null) {
                    neighbors.add(behavior);
                }
            }
            for (Map.Entry<BehavioredClassifier, Behavior> entry : classifierBehaviorRels.entrySet()) {
                if (entry.getValue() == element) {
                    neighbors.add(entry.getKey());
                }
            }

            // Add neighbors from typed attributes
            for (Property prop : typedAttributeRels) {
                if (prop.getOwner() == element && prop.getType() instanceof NamedElement) {
                    neighbors.add((NamedElement) prop.getType());
                }
                if (prop.getType() == element && prop.getOwner() instanceof NamedElement) {
                    neighbors.add((NamedElement) prop.getOwner());
                }
            }

            // Add neighbors from stereotypes
            if (stereotypeRels.containsKey(element)) {
                for (Stereotype stereotype : stereotypeRels.get(element)) {
                    if (stereotype instanceof NamedElement) {
                        neighbors.add((NamedElement) stereotype);
                    }
                }
            }
            // Add elements that have this stereotype applied
            for (Map.Entry<Element, List<Stereotype>> entry : stereotypeRels.entrySet()) {
                if (entry.getKey() instanceof NamedElement && entry.getValue().contains(element)) {
                    neighbors.add((NamedElement) entry.getKey());
                }
            }

            return neighbors;
        }

        private void drawPathNetwork(Graphics2D g2) {
            Set<NamedElement> uniqueNodes = getUniqueNodes();

            // Initial layout if positions are not set
            if (nodePositions.isEmpty() && getWidth() > 0 && getHeight() > 0) {
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int radius = Math.min(cx, cy) * 2 / 3;
                List<NamedElement> nodeList = new ArrayList<>(uniqueNodes);
                for (int i = 0; i < nodeList.size(); i++) {
                    double angle = 2 * Math.PI * i / nodeList.size();
                    int x = cx + (int) (radius * Math.cos(angle));
                    int y = cy + (int) (radius * Math.sin(angle));
                    nodePositions.put(nodeList.get(i), new Point(x, y));
                }
            }
            
            Set<NamedElement> highlightedNodes = (highlightedPath != null) ? new HashSet<>(highlightedPath) : new HashSet<>();
            Set<NodePair> highlightedEdges = new HashSet<>();
            if (highlightedPath != null) {
                for (int i = 0; i < highlightedPath.size() - 1; i++) {
                    highlightedEdges.add(new NodePair(highlightedPath.get(i), highlightedPath.get(i + 1)));
                }
            }

            Set<NodePair> allEdges = new HashSet<>();
            for (List<NamedElement> path : allPaths) {
                for (int i = 0; i < path.size() - 1; i++) {
                    allEdges.add(new NodePair(path.get(i), path.get(i + 1)));
                }
            }

            drawNetwork(g2, uniqueNodes, highlightedNodes, highlightedEdges, allEdges);
        }

        private void drawNetwork(Graphics2D g2, Set<NamedElement> nodesToDraw, Set<NamedElement> highlightedNodes, Set<NodePair> highlightedEdges, Set<NodePair> allEdges) {
            // Draw all edges
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
            FontMetrics fm = g2.getFontMetrics();
            for (NodePair edge : allEdges) {
                Point p1 = nodePositions.get(edge.n1());
                Point p2 = nodePositions.get(edge.n2());
                if (p1 != null && p2 != null) {
                    boolean isHighlighted = highlightedEdges.contains(edge);
                    Stroke defaultStroke = g2.getStroke();
                    if (isHighlighted) {
                        g2.setStroke(new BasicStroke(3.0f));
                    }

                    drawRelationshipLine(g2, edge.n1(), edge.n2(), p1, p2, isHighlighted);
                    
                    g2.setStroke(defaultStroke);
                }
            }

            // Draw all nodes
            int d = 40;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            fm = g2.getFontMetrics();
            for (NamedElement ne : nodesToDraw) {
                Point p = nodePositions.get(ne);
                if (p == null) continue;

                int x = p.x - d / 2, y = p.y - d / 2;

                if (highlightedNodes.contains(ne)) {
                    g2.setColor(new Color(255, 220, 220)); // Highlight fill
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.fillOval(x, y, d, d);

                if (highlightedNodes.contains(ne)) {
                     g2.setColor(Color.RED); // Highlight border
                     g2.setStroke(new BasicStroke(2.0f));
                } else {
                    g2.setColor(Color.BLACK);
                    g2.setStroke(new BasicStroke(1.0f));
                }
                g2.drawOval(x, y, d, d);
                g2.setStroke(new BasicStroke(1.0f));


                String lbl = getFormattedElementName(ne);
                int tw = fm.stringWidth(lbl);
                g2.setColor(Color.BLACK);
                g2.drawString(lbl, p.x - tw / 2, y - 5);
            }
        }

        private void drawRelationshipLine(Graphics2D g2, NamedElement node1, NamedElement node2, Point p1, Point p2, boolean isHighlighted) {
             String description = "related";
             boolean foundRel = false;
             double nodeRadius = 20;

            // 1. Explicit Relationship
            for (Relationship r : rels) {
                if (r.getRelatedElement().contains(node1) && r.getRelatedElement().contains(node2)) {
                    description = r.getHumanType();
                    g2.setColor(isHighlighted ? Color.RED : Color.GRAY);

                    Point from = p1, to = p2;
                    boolean directed = false;
                    if (r instanceof DirectedRelationship) {
                        DirectedRelationship dr = (DirectedRelationship) r;
                        if (dr.getSource().contains(node1) && dr.getTarget().contains(node2)) {
                            from = p1; to = p2; directed = true;
                        } else if (dr.getSource().contains(node2) && dr.getTarget().contains(node1)) {
                            from = p2; to = p1; directed = true;
                        }
                    }
                    
                    if (directed) {
                        if (from.distance(to) > nodeRadius) {
                            Point adjustedTo = adjustPoint(to, from, nodeRadius);
                            drawArrowLine(g2, from.x, from.y, adjustedTo.x, adjustedTo.y);
                        }
                    } else {
                        g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                    foundRel = true; break;
                }
            }

            // 2. Ownership
            if (!foundRel && (ownershipRels.get(node1) == node2 || ownershipRels.get(node2) == node1)) {
                g2.setColor(isHighlighted ? Color.RED : Color.BLUE);
                description = "owns";
                Point from, to;
                if (ownershipRels.get(node2) == node1) { // node1 owns node2
                    from = p1; to = p2;
                } else { // node2 owns node1
                    from = p2; to = p1;
                }
                if (from.distance(to) > nodeRadius) {
                    Point adjustedTo = adjustPoint(to, from, nodeRadius);
                    drawDashedLine(g2, from.x, from.y, adjustedTo.x, adjustedTo.y);
                }
                foundRel = true;
            }

            // 3. Classifier Behavior
            if (!foundRel) {
               if ((classifierBehaviorRels.containsKey(node1) && classifierBehaviorRels.get(node1) == node2)) {
                    g2.setColor(isHighlighted ? Color.RED : Color.GREEN);
                    if (p1.distance(p2) > nodeRadius) {
                        Point adjustedTo = adjustPoint(p2, p1, nodeRadius);
                        drawArrowLine(g2, p1.x, p1.y, adjustedTo.x, adjustedTo.y);
                    }
                    description = "has behavior";
                    foundRel = true;
                } else if ((classifierBehaviorRels.containsKey(node2) && classifierBehaviorRels.get(node2) == node1)) {
                     g2.setColor(isHighlighted ? Color.RED : Color.GREEN);
                    if (p2.distance(p1) > nodeRadius) {
                        Point adjustedTo = adjustPoint(p1, p2, nodeRadius);
                        drawArrowLine(g2, p2.x, p2.y, adjustedTo.x, adjustedTo.y);
                    }
                    description = "has behavior";
                    foundRel = true;
                }
            }

            // 4. Typed Attribute
            if (!foundRel) {
                for (Property prop : typedAttributeRels) {
                    Point from = null, to = null;
                    if (prop.getOwner() == node1 && prop.getType() == node2) {
                        from = p1; to = p2;
                    } else if (prop.getOwner() == node2 && prop.getType() == node1) {
                        from = p2; to = p1;
                    }

                    if (from != null) {
                        g2.setColor(isHighlighted ? Color.RED : Color.RED);
                        if (from.distance(to) > nodeRadius) {
                            Point adjustedTo = adjustPoint(to, from, nodeRadius);
                            drawDottedLine(g2, from.x, from.y, adjustedTo.x, adjustedTo.y);
                        }
                        description = "is typed by";
                        foundRel = true;
                        break;
                    }
                }
            }

            // 5. Stereotype
            if (!foundRel) {
                Point from = null, to = null;
                 if (stereotypeRels.containsKey(node1) && stereotypeRels.get(node1).contains(node2)) { // node1 has stereotype node2
                    from = p1; to = p2;
                } else if (stereotypeRels.containsKey(node2) && stereotypeRels.get(node2).contains(node1)) { // node2 has stereotype node1
                    from = p2; to = p1;
                }

                if(from != null){
                    g2.setColor(isHighlighted ? Color.RED : new Color(148, 0, 211));
                    if (from.distance(to) > nodeRadius) {
                        Point adjustedTo = adjustPoint(to, from, nodeRadius);
                        drawZigzagLine(g2, from.x, from.y, adjustedTo.x, adjustedTo.y);
                    }
                    description = "applies stereotype";
                    foundRel = true;
                }
            }
            if (!foundRel) { // Default if no specific relationship found
                 g2.setColor(isHighlighted ? Color.RED : Color.GRAY);
                 g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
            
            // Draw description
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.BLACK);
            int stringWidth = fm.stringWidth(description);
            g2.drawString(description, (p1.x + p2.x - stringWidth)/2, (p1.y + p2.y)/2 - 5);
        }

        private void drawArrowHead(Graphics2D g2, int x1, int y1, int x2, int y2) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int arrowSize = 10;
            Path2D.Double arrowHead = new Path2D.Double();
            arrowHead.moveTo(x2, y2);
            arrowHead.lineTo(x2 - arrowSize * Math.cos(angle - Math.PI / 6), y2 - arrowSize * Math.sin(angle - Math.PI / 6));
            arrowHead.lineTo(x2 - arrowSize * Math.cos(angle + Math.PI / 6), y2 - arrowSize * Math.sin(angle + Math.PI / 6));
            arrowHead.closePath();
            
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1f));
            g2.fill(arrowHead);
            g2.setStroke(oldStroke);
        }

        private void drawDashedLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
            Stroke oldStroke = g2.getStroke();
            float width = 1.0f;
            if (oldStroke instanceof BasicStroke) {
                width = ((BasicStroke) oldStroke).getLineWidth();
            }
            float[] dash = {5.0f};
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(oldStroke);
            drawArrowHead(g2, x1, y1, x2, y2);
        }

        private void drawDottedLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
            Stroke oldStroke = g2.getStroke();
            float width = 1.0f;
            if (oldStroke instanceof BasicStroke) {
                width = ((BasicStroke) oldStroke).getLineWidth();
            }
            float[] dash = {2.0f, 2.0f};
            g2.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(oldStroke);
            drawArrowHead(g2, x1, y1, x2, y2);
        }

        private void drawArrowLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
            g2.drawLine(x1, y1, x2, y2);
            drawArrowHead(g2, x1, y1, x2, y2);
        }

        private void drawZigzagLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
            // Calculate the number of zigzag segments
            int segments = 4;
            
            // Calculate the direction vector
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            
            // Calculate perpendicular vector for zigzag
            double perpX = -dy / len * 10;  // 10 is the zigzag amplitude
            double perpY = dx / len * 10;
            
            // Create path for zigzag
            Path2D path = new Path2D.Double();
            path.moveTo(x1, y1);
            
            double prevX = x1;
            double prevY = y1;

            for (int i = 1; i < segments; i++) {
                double t = (double) i / segments;
                double x = x1 + dx * t;
                double y = y1 + dy * t;
                
                if (i % 2 == 1) {
                    x += perpX;
                    y += perpY;
                } else {
                    x -= perpX;
                    y -= perpY;
                }
                
                path.lineTo(x, y);
                prevX = x;
                prevY = y;
            }
            
            path.lineTo(x2, y2);
            
            // Draw the zigzag path
            Stroke oldStroke = g2.getStroke();
            g2.draw(path);
            g2.setStroke(oldStroke);
            drawArrowHead(g2, (int)prevX, (int)prevY, x2, y2);
        }

        private Point adjustPoint(Point pointToAdjust, Point referencePoint, double amount) {
            double dx = pointToAdjust.x - referencePoint.x;
            double dy = pointToAdjust.y - referencePoint.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist == 0) return pointToAdjust;

            double newX = pointToAdjust.x - (dx / dist * amount);
            double newY = pointToAdjust.y - (dy / dist * amount);
            return new Point((int) newX, (int) newY);
        }
    }
}

