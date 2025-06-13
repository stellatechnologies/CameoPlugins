# Configuration Plugin

A Cameo plugin for managing and applying configurations across your Cameo models. This plugin provides a flexible way to customize and persist model settings.

## Features

- Configuration management for Cameo models
- Customizable model settings
- Settings persistence across sessions
- Easy configuration switching
- Configuration templates

## Installation

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Cameo Enterprise Architect or Cameo Systems Modeler

### Setup

1. **Build the Plugin**
   ```bash
   cd Configuration
   ant build
   ```

2. **Install the Plugin**
   - Copy the contents of the `dist` directory to your Cameo plugins folder:
     - Windows: `C:\Program Files\No Magic\Cameo Systems Modeler\plugins\Configuration`
     - macOS: `/Applications/Cameo Systems Modeler.app/Contents/plugins/Configuration`
     - Linux: `/opt/NoMagic/Cameo Systems Modeler/plugins/Configuration`

3. **Restart Cameo**
   - Close and reopen Cameo Enterprise Architect/Systems Modeler
   - The Configuration plugin should appear in the appropriate menu

## Usage

### Accessing the Plugin

1. Open your Cameo model
2. Navigate to the Configuration menu
3. Select "Configuration Manager"

### Managing Configurations

1. **Create a New Configuration**
   - Click "New Configuration"
   - Enter configuration name and description
   - Set desired parameters
   - Save the configuration

2. **Apply a Configuration**
   - Select a configuration from the list
   - Click "Apply"
   - The settings will be applied to your current model

3. **Edit Existing Configuration**
   - Select a configuration
   - Click "Edit"
   - Modify settings as needed
   - Save changes

4. **Delete Configuration**
   - Select a configuration
   - Click "Delete"
   - Confirm deletion

### Configuration Templates

1. **Create Template**
   - Set up a configuration as desired
   - Click "Save as Template"
   - Enter template name and description

2. **Use Template**
   - Click "New from Template"
   - Select a template
   - Customize as needed
   - Save as new configuration

## Development

### Project Structure
```
Configuration/
├── src/                    # Source code
├── test/                   # Test files
├── build.xml              # Build configuration
├── plugin.xml             # Plugin manifest
└── manifest.mf            # Manifest file
```

### Building from Source

1. Ensure you have JDK 8+ installed
2. Run the build script:
   ```bash
   ant build
   ```
3. The compiled plugin will be in the `dist` directory

## Troubleshooting

### Common Issues

1. **Plugin Not Appearing**
   - Verify installation path
   - Check plugin.xml is present
   - Restart Cameo

2. **Configuration Not Saving**
   - Check write permissions
   - Verify disk space
   - Check log files

### Getting Help

For issues and feature requests, please use the GitHub issue tracker.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 