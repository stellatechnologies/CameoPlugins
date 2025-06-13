from flask import Flask, request, jsonify
from flask_cors import CORS
from semantic_search import SemanticSearchEngine
import os
from clean_json import clean_json_file
from dotenv import load_dotenv
import logging

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

# Load environment variables
load_dotenv()

# Ensure OpenAI API key is set
if not os.getenv('OPENAI_API_KEY'):
    logging.warning("OPENAI_API_KEY not set. LLM recommendations will not be available.")
else:
    logging.info("OPENAI_API_KEY is set. LLM recommendations enabled.")

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Clean the JSON file first
json_file_path = os.path.join(os.path.dirname(__file__), 'artifact_objects.json')
clean_json_path = os.path.join(os.path.dirname(__file__), 'artifact_objects_clean.json')
clean_json_file(json_file_path, clean_json_path)

# Initialize the semantic search engine with cleaned JSON
db_path = os.path.join(os.path.dirname(__file__), 'embeddings.db')
search_engine = SemanticSearchEngine(clean_json_path, db_path)

@app.route('/capitalize', methods=['POST'])
def capitalize_names():
    data = request.get_json()
    
    if not data or 'elements' not in data:
        return jsonify({'error': 'Invalid input. Expected {"elements": [...]'}), 400
    
    capitalized_elements = [
        {
            'name': element['name'].upper(),
            'comments': [comment.upper() for comment in element.get('comments', [])]
        }
        for element in data['elements']
    ]
    
    return jsonify({'elements': capitalized_elements})

@app.route('/search', methods=['POST'])
def semantic_search():
    data = request.get_json()
    if not data or 'query' not in data:
        return jsonify({'error': 'Invalid input. Expected {"query": "search text"}'}), 400
    
    query = data['query']
    num_results = data.get('num_results', 3)  # Default to 3 results if not specified
    
    try:
        # Get semantic search results
        results = search_engine.search(query, k=num_results)
        
        # Get LLM recommendation if OpenAI API key is set
        recommendation = None
        if os.getenv('OPENAI_API_KEY'):
            try:
                from openai_helper import get_llm_recommendation
                recommendation = get_llm_recommendation(query, results)
            except Exception as e:
                logging.error(f"Error getting LLM recommendation: {str(e)}")
                recommendation = {
                    "recommended_id": None,
                    "explanation": "Failed to get LLM recommendation",
                    "confidence": 0
                }
        else:
            recommendation = {
                "recommended_id": None,
                "explanation": "OpenAI API key not set",
                "confidence": 0
            }
        
        return jsonify({
            'results': results,
            'recommendation': recommendation
        })
    except Exception as e:
        logging.error(f'Search failed: {str(e)}')
        return jsonify({'error': f'Search failed: {str(e)}'}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000) 