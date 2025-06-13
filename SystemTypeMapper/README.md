# System Type Mapper

A Cameo plugin that helps users find and map appropriate system types for their elements using semantic search and AI-powered recommendations.

## Features

- Semantic search for finding similar system types based on element names and descriptions
- AI-powered recommendations using OpenAI's GPT model
- Interactive UI for viewing and applying system type recommendations
- Confidence scores and explanations for recommended types

## Setup

### Prerequisites

- Python 3.8 or higher
- Java Development Kit (JDK) 8 or higher
- Cameo Systems Modeler

### Flask Backend Setup

1. Navigate to the `flask_backend` directory:
   ```bash
   cd flask_backend
   ```

2. Create a virtual environment and activate it:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Set up your OpenAI API key:
   - Copy `.env.example` to `.env`:
     ```bash
     cp .env.example .env
     ```
   - Edit `.env` and replace `your_api_key_here` with your actual OpenAI API key

5. Start the Flask server:
   ```bash
   python app.py
   ```

### Cameo Plugin Setup

1. In Cameo Systems Modeler, go to Help > Resource/Plugin Manager
2. Click "Import" and select the `CameoPlugin` directory
3. Restart Cameo Systems Modeler

## Usage

1. Select one or more elements in your model
2. Click the "Search Similar Elements" button in the toolbar
3. The plugin will:
   - Search for semantically similar system types
   - Use AI to analyze and recommend the best matching type
   - Display results with confidence scores and explanations
4. Click "Apply" on any result to set the system type for your element

## Development

### Backend Development

The Flask backend uses:
- sentence-transformers for semantic search
- FAISS for efficient similarity search
- OpenAI's GPT model for intelligent recommendations

### Plugin Development

The Cameo plugin is written in Java and uses:
- Swing for the UI
- JSON for communication with the backend
- Custom styling for a modern look and feel

## License

This project is licensed under the MIT License - see the LICENSE file for details. 