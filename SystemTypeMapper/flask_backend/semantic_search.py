import json
from sentence_transformers import SentenceTransformer
import faiss
import numpy as np
import sqlite3
import pickle
import os
import logging
import base64

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class SemanticSearchEngine:
    def __init__(self, json_file_path, db_path='embeddings.db'):
        self.db_path = db_path
        self.json_file_path = json_file_path
        
        # Initialize the database if it doesn't exist
        self._init_db()
        
        # Check if we need to generate embeddings
        if not self._embeddings_exist():
            logger.info("Generating new embeddings...")
            self._generate_embeddings()
        else:
            logger.info("Loading existing embeddings from database...")
            self._load_embeddings()

    def _init_db(self):
        with sqlite3.connect(self.db_path) as conn:
            conn.execute('''CREATE TABLE IF NOT EXISTS embeddings
                          (id TEXT PRIMARY KEY,
                           label TEXT,
                           definition TEXT,
                           embedding TEXT)''')
            conn.execute('''CREATE TABLE IF NOT EXISTS metadata
                          (key TEXT PRIMARY KEY,
                           value TEXT)''')

    def _embeddings_exist(self):
        if not os.path.exists(self.db_path):
            return False
            
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute("SELECT COUNT(*) FROM embeddings")
            count = cursor.fetchone()[0]
            return count > 0

    def _generate_embeddings(self):
        # Load the model
        model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
        
        # Load and process the JSON data
        with open(self.json_file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # Store embeddings in database
        with sqlite3.connect(self.db_path) as conn:
            for key, value in data.items():
                if isinstance(value, dict):
                    content = f"{value.get('label', '')} - {value.get('definition', '')}"
                    # Ensure we get a numpy array and convert it to the right format
                    embedding = model.encode([content])[0].astype(np.float32)
                    
                    # Convert embedding to base64 string
                    embedding_bytes = embedding.tobytes()
                    embedding_b64 = base64.b64encode(embedding_bytes).decode('utf-8')
                    
                    try:
                        conn.execute(
                            "INSERT OR REPLACE INTO embeddings (id, label, definition, embedding) VALUES (?, ?, ?, ?)",
                            (str(value.get('id', '')),
                             str(value.get('label', '')),
                             str(value.get('definition', '')),
                             embedding_b64)
                        )
                    except Exception as e:
                        logger.error(f"Error inserting data for key {key}: {str(e)}")
                        raise
            
            # Store the embedding dimension
            conn.execute(
                "INSERT OR REPLACE INTO metadata (key, value) VALUES (?, ?)",
                ('dimension', str(embedding.shape[0]))
            )
        
        self._load_embeddings()

    def _load_embeddings(self):
        with sqlite3.connect(self.db_path) as conn:
            # Get dimension
            cursor = conn.cursor()
            cursor.execute("SELECT value FROM metadata WHERE key = 'dimension'")
            self.dimension = int(cursor.fetchone()[0])
            
            # Load all embeddings
            cursor.execute("SELECT id, label, definition, embedding FROM embeddings")
            rows = cursor.fetchall()
            
            # Prepare data structures
            self.documents = []
            embeddings_list = []
            
            for row in rows:
                doc = {
                    'id': row[0],
                    'label': row[1],
                    'definition': row[2]
                }
                self.documents.append(doc)
                
                # Convert base64 string back to numpy array
                embedding_bytes = base64.b64decode(row[3])
                embedding = np.frombuffer(embedding_bytes, dtype=np.float32)
                embeddings_list.append(embedding)
            
            # Create FAISS index
            if not embeddings_list:
                self.embeddings = np.array([])
                self.index = None
                return

            self.embeddings = np.vstack(embeddings_list)
            self.index = faiss.IndexFlatL2(self.dimension)
            self.index.add(self.embeddings.astype('float32'))
    
    def search(self, query, k=3):
        if self.index is None:
            return []

        # Load the model for query encoding
        model = SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')
        
        # Generate query embedding
        query_embedding = model.encode([query])
        
        # Search the index
        distances, indices = self.index.search(query_embedding.astype('float32'), k)
        
        # Format results
        results = []
        for i, (distance, idx) in enumerate(zip(distances[0], indices[0])):
            if idx < len(self.documents):
                doc = self.documents[idx]
                results.append({
                    'id': doc['id'],
                    'label': doc['label'],
                    'definition': doc['definition'],
                    'score': float(1 / (1 + distance))
                })
        
        return results 