import json

def clean_json_file(input_path, output_path):
    # Read the file in binary mode first
    with open(input_path, 'rb') as f:
        content = f.read()
    
    # Decode using utf-8 and handle errors
    content_str = content.decode('utf-8', errors='ignore')
    
    # Parse and rewrite the JSON to ensure it's clean
    data = json.loads(content_str)
    
    # Write back with proper encoding
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=True, indent=2)

if __name__ == '__main__':
    input_file = 'artifact_objects.json'
    output_file = 'artifact_objects_clean.json'
    clean_json_file(input_file, output_file)
    print(f"Cleaned JSON file saved to {output_file}") 