import os
from openai import OpenAI
from typing import List, Dict
import json
import logging

logger = logging.getLogger(__name__)

# Initialize OpenAI client
client = OpenAI(api_key=os.getenv('OPENAI_API_KEY'))

def get_llm_recommendation(query: str, search_results: List[Dict]) -> Dict:
    """Get LLM recommendation for the best matching system type."""
    
    # Prepare context for the LLM
    context = "\n\n".join([
        f"System Type ID: {result['id']}\n"
        f"System Type: {result['label']}\n"
        f"Definition: {result['definition']}\n"
        f"Match Score: {result['score']}"
        for result in search_results
    ])
    
    # Prepare the prompt
    prompt = f"""Given a user's element description and a set of potential system types, recommend the most appropriate system type.

User's Element Description:
{query}

Available System Types and their definitions:
{context}

Please analyze the element description and the available system types, then provide:
1. The ID of the recommended system type (must be one of the provided System Type IDs)
2. A brief explanation of why this type is the best match
3. A confidence score between 0 and 1

Format your response as a JSON object with these exact keys: "recommended_id", "explanation", "confidence"

IMPORTANT: The recommended_id MUST be one of the System Type IDs provided above.
"""

    try:
        response = client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "You are a helpful assistant that recommends system types based on element descriptions. Always respond with valid JSON and ensure the recommended_id matches one of the provided System Type IDs exactly."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.3
        )
        
        # Parse the response
        try:
            recommendation = json.loads(response.choices[0].message.content)
            
            rec_id = recommendation.get('recommended_id')
            
            # Validate that the recommended ID exists in the search results
            valid_ids = {result['id'] for result in search_results}
            if rec_id not in valid_ids:
                logger.warning(f"LLM returned invalid ID '{rec_id}'")
                recommendation['recommended_id'] = None
                recommendation['explanation'] = f"Error: LLM returned invalid system type ID: '{rec_id}'"
                recommendation['confidence'] = 0

            return recommendation
        except json.JSONDecodeError as e:
            logger.error(f"Error parsing LLM response as JSON: {str(e)}")
            return {
                "recommended_id": None,
                "explanation": "Failed to parse LLM response",
                "confidence": 0
            }
    except Exception as e:
        logger.error(f"Error getting LLM recommendation: {str(e)}")
        return {
            "recommended_id": None,
            "explanation": "Failed to get LLM recommendation",
            "confidence": 0
        } 