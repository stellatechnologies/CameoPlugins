import json

def is_d3f_node(node_id):
    """Check if a node ID is in the d3f namespace"""
    return isinstance(node_id, str) and node_id.startswith('d3f:')

def build_flat_hierarchy(graph_data, node_id, objects_dict=None, processed_nodes=None):
    if objects_dict is None:
        objects_dict = {}
    if processed_nodes is None:
        processed_nodes = set()
        
    # Skip if we've seen this node before or if it's not a d3f node
    if node_id in processed_nodes or not is_d3f_node(node_id):
        return
    
    processed_nodes.add(node_id)
    
    # Find the current node
    node = next((item for item in graph_data if item.get('@id') == node_id), None)
    if not node:
        return
        
    # Create a slim version of the node with only the fields we want
    node_data = {
        'id': node.get('@id'),
        'label': node.get('rdfs:label'),
        'definition': node.get('d3f:definition'),
        'subClassOf': node.get('rdfs:subClassOf'),
        'children': []  # Will store just the IDs of children
    }
    
    # Add this node to our dictionary
    objects_dict[node_id] = node_data
    
    # Find all nodes that reference this one as their superclass
    for potential_child in graph_data:
        child_id = potential_child.get('@id')
        if not is_d3f_node(child_id):
            continue
            
        subclass_refs = potential_child.get('rdfs:subClassOf', [])
        if not isinstance(subclass_refs, list):
            subclass_refs = [subclass_refs]
            
        # Check if any of the references point to our current node
        is_child = any(
            (isinstance(ref, dict) and ref.get('@id') == node_id) or
            (isinstance(ref, str) and ref == node_id)
            for ref in subclass_refs
        )
        
        if is_child:
            node_data['children'].append(child_id)
            build_flat_hierarchy(graph_data, child_id, objects_dict, processed_nodes)
    
    return objects_dict

def print_hierarchy_summary(objects_dict, node_id, indent=0, processed=None):
    """Helper function to print a summary of the hierarchy"""
    if processed is None:
        processed = set()
    
    if node_id in processed or not is_d3f_node(node_id):
        return
    
    processed.add(node_id)
    
    node = objects_dict.get(node_id)
    if not node:
        return
        
    label = node.get('label', node.get('id'))
    print('  ' * indent + f"- {label}")
    
    for child_id in node.get('children', []):
        print_hierarchy_summary(objects_dict, child_id, indent + 1, processed)

# Read the JSON file
with open('d3fend.json', 'r', encoding='utf-8') as file:
    data = json.load(file)

graph_data = data['@graph']

# Build the flat hierarchy starting from Artifact
artifact_objects = build_flat_hierarchy(graph_data, 'd3f:Artifact')

# Filter out non-d3f namespace subclass references from each object
for obj_id, obj_data in artifact_objects.items():
    if 'subClassOf' in obj_data:
        # Convert to list if not already
        subclass_refs = obj_data['subClassOf']
        if not isinstance(subclass_refs, list):
            subclass_refs = [subclass_refs]
            
        # Keep only d3f namespace references
        filtered_refs = [ref for ref in subclass_refs if 
                        isinstance(ref, dict) and 
                        '@id' in ref and
                        is_d3f_node(ref['@id'])]
        
        # Update the object with filtered refs
        if filtered_refs:
            obj_data['subClassOf'] = filtered_refs
        else:
            # If no valid refs remain, set to single parent
            obj_data['subClassOf'] = {'@id': 'd3f:D3FENDCore'}


# Print a summary of what we found
print("\nHierarchy summary:")
print_hierarchy_summary(artifact_objects, 'd3f:Artifact')

# Save the objects to a file
with open('artifact_objects.json', 'w', encoding='utf-8') as file:
    json.dump(artifact_objects, file, indent=2, ensure_ascii=False)

print("\nComplete object dictionary has been saved to 'artifact_objects.json'")
    


    