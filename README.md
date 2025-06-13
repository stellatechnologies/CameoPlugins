# Cameo Enterprise Architect Plugins

This repository contains a collection of plugins for Cameo Enterprise Architect (formerly MagicDraw) and Cameo Systems Modeler. These plugins enhance the functionality of Cameo with additional features for system modeling, configuration management, and data export.

## Available Plugins

### 1. System Type Mapper
A powerful plugin that helps users find and map appropriate system types for their elements using semantic search and AI-powered recommendations.

**Key Features:**
- Semantic search for finding similar system types
- AI-powered recommendations using OpenAI's GPT model
- Interactive UI for viewing and applying system type recommendations
- Confidence scores and explanations for recommended types
- Customizable containment diagram generation
- Multiple output formats
- Hierarchical view options
- Print and export capabilities

**Requirements:**
- Python 3.8+ (for backend)
- OpenAI API key
- Java Development Kit (JDK) 8+

[Detailed Documentation](SystemTypeMapper/README.md)

### 2. Configuration Plugin
A plugin for managing and applying configurations across your Cameo models.

**Key Features:**
- Configuration management
- Model customization
- Settings persistence
- Configuration templates

[Detailed Documentation](Configuration/README.md)

### 3. Generic Table Export
A flexible plugin for exporting model data to various table formats.

**Key Features:**
- Customizable table exports
- Multiple format support (CSV, Excel, HTML)
- Configurable export templates
- Advanced filtering options

[Detailed Documentation](GenericTableExport/README.md)

## Installation

### Prerequisites
- Cameo Enterprise Architect or Cameo Systems Modeler
- Java Development Kit (JDK) 8 or higher
- Python 3.8+ (for plugins with Python backends)

### Plugin Installation Steps

1. **Locate Cameo Plugin Directory**
   - Windows: `C:\Program Files\No Magic\Cameo Systems Modeler\plugins`
   - macOS: `/Applications/Cameo Systems Modeler.app/Contents/plugins`
   - Linux: `/opt/NoMagic/Cameo Systems Modeler/plugins`

2. **Install Plugins**
   - Copy the plugin directories from the `plugins` folder to your Cameo plugins directory:
     ```
     plugins/
     ├── com.stellatechnologies.configuration/
     ├── com.stellatechnologies.generictableexport/
     └── com.stellatechnologies.systemtypemapper/
     ```
   - For System Type Mapper, follow the additional setup instructions in its README

3. **Restart Cameo**
   - Close and reopen Cameo Enterprise Architect/Systems Modeler
   - The new plugins should appear in the appropriate menus

## Usage

### System Type Mapper
1. Select elements in your model
2. Click "Search Similar Elements" in the toolbar
3. Review and apply recommended system types
4. Use the containment diagram features to visualize relationships

### Configuration Plugin
1. Access through the Configuration menu
2. Select or create configurations
3. Apply configurations to your model

### Generic Table Export
1. Select the data you want to export
2. Choose export format and template
3. Configure export settings
4. Generate the export

## Development

### Project Structure
```
.
├── SystemTypeMapper/          # System Type Mapper plugin
│   ├── CameoPlugin/          # Java plugin code
│   └── flask_backend/        # Python backend
├── Configuration/            # Configuration plugin
├── GenericTableExport/       # Table export plugin
├── plugins/                  # Compiled plugin distributions
│   ├── com.stellatechnologies.configuration/
│   ├── com.stellatechnologies.generictableexport/
│   └── com.stellatechnologies.systemtypemapper/
└── LICENSE                   # Project license
```

### Building Plugins
1. Navigate to the plugin directory
2. Run the build script:
   ```bash
   ant build
   ```
3. The compiled plugin will be in the `dist` directory
4. Copy the compiled plugin to the `plugins` directory with the appropriate package name

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For issues and feature requests, please use the GitHub issue tracker. 