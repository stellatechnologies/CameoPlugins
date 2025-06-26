import uuid
import json

# Read in artifact_objects_clean.json
with open('artifact_objects_clean.json', 'r') as f:
    data = json.load(f)
    
    
new_data = []

for key, value in data.items():
    new_data.append({
        "UUID": str(uuid.uuid4()),
        "Name": value["label"],
        "Description": value["definition"],
        "D3FEND_ID": value["id"]
    })
    
# Write to new_artifact_objects.json
with open('new_artifact_objects.json', 'w') as f:
    json.dump(new_data, f, indent=4)