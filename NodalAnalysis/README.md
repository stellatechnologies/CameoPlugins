# Nodal Analysis Plugin for Cameo Enterprise Architect

A powerful plugin for analyzing and visualizing relationships between model elements in your Cameo projects. This plugin helps system engineers and architects understand complex relationships and dependencies within their models through interactive visualization and path finding capabilities.

## Features

### Path Finder
The Path Finder tool helps you discover and visualize relationships between any two elements in your model.

- **Select and Find**: Choose any two named elements in your model's containment tree to find paths between them
- **Customizable Search**: 
  - Adjust minimum path length
  - Toggle stereotype relationship inclusion
  - Interactive path exploration
- **Visual Results**:
  - Interactive graph visualization
  - Draggable nodes for better layout
  - Different line styles for various relationship types
  - Path highlighting on selection

### Relationship Explorer
The Relationship Explorer provides a comprehensive view of all relationships in your selected model elements.

- **Relationship Types Supported**:
  - Direct relationships (associations, dependencies, etc.)
  - Ownership relationships (parent-child)
  - Typed attributes
  - Applied stereotypes
  - Classifier behaviors
- **Interactive Visualization**:
  - Zoomable graph view
  - Node dragging for custom layouts
  - Element selection and focus
  - Relationship filtering

## Usage

### Path Finder

1. Open your model in Cameo Enterprise Architect
2. In the Containment Tree, select two elements between which you want to find relationships
3. Click on "Stella > Find Paths Between Elements"
4. In the Path Finder dialog:
   - Use the spinner to adjust minimum path length
   - Toggle "Ignore Stereotypes" checkbox if needed
   - Click "Find Paths" to search
   - Select paths from the list to highlight them in the graph
   - Drag nodes to adjust the visualization

### Relationship Explorer

1. Select elements in your model's containment tree
2. Click on "Stella > Relationship Explorer"
3. In the Relationship Explorer dialog:
   - Click elements to focus on their relationships
   - Drag nodes to customize the layout
   - Use the visualization options to explore different relationship types
   - Hover over relationships for additional details

## Visualization Guide

The plugin uses different line styles to represent various relationship types:

- **Solid Line**: Direct relationships (associations, generalizations, etc.)
- **Dashed Line**: Dependency relationships
- **Dotted Line**: Stereotype applications
- **Zigzag Line**: Classifier behavior relationships
- **Arrow**: Direction of the relationship
- **Line Color**:
  - Black: Standard relationships
  - Blue: Selected/highlighted paths
  - Gray: Background relationships when focusing on specific elements

## Technical Details

### Requirements
- Cameo Enterprise Architect or Cameo Systems Modeler
- Java Runtime Environment (JRE) 8 or higher

### Supported Relationship Types
1. **Direct Relationships**
   - Associations
   - Generalizations
   - Dependencies
   - Realizations
   - Any other UML relationships

2. **Structural Relationships**
   - Element ownership
   - Package containment
   - Typed attributes
   - Port definitions

3. **Behavioral Relationships**
   - Classifier behaviors
   - State machines
   - Activity diagrams

4. **Profile Applications**
   - Stereotypes
   - Tagged values
   - Profile relationships

## Troubleshooting

### Common Issues

1. **Plugin Not Appearing in Menu**
   - Verify proper installation directory
   - Check Cameo version compatibility
   - Ensure proper restart after installation

2. **Long Path Finding Times**
   - Reduce minimum path length
   - Select more specific elements
   - Consider using the relationship explorer for large models

3. **Graph Visualization Issues**
   - Adjust window size for better visibility
   - Drag nodes to reduce overlap
   - Focus on specific elements to reduce complexity

### Error Messages

- "No open project": Ensure a model is open in Cameo
- "Please select two elements": Path Finder requires exactly two elements selected
- "Please select elements in the containment tree": Ensure selections are made in the containment tree view

## Support

For issues, feature requests, or contributions:
1. Check the troubleshooting section above
2. Submit an issue on the GitHub repository
3. Include relevant details:
   - Cameo version
   - Plugin version
   - Error messages
   - Steps to reproduce

## License

This plugin is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details. 