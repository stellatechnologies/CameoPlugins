# Generic Table Export Plugin

A flexible Cameo plugin for exporting model data to various table formats. This plugin allows users to customize their exports and create reusable export templates.

## Features

- Export model data to multiple formats (CSV, Excel, HTML)
- Customizable export templates
- Configurable column selection
- Support for nested data structures
- Batch export capabilities
- Export filtering options

## Installation

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Cameo Enterprise Architect or Cameo Systems Modeler

### Setup

1. **Build the Plugin**
   ```bash
   cd GenericTableExport
   ant build
   ```

2. **Install the Plugin**
   - Copy the contents of the `dist` directory to your Cameo plugins folder:
     - Windows: `C:\Program Files\No Magic\Cameo Systems Modeler\plugins\GenericTableExport`
     - macOS: `/Applications/Cameo Systems Modeler.app/Contents/plugins\GenericTableExport`
     - Linux: `/opt/NoMagic/Cameo Systems Modeler/plugins/GenericTableExport`

3. **Restart Cameo**
   - Close and reopen Cameo Enterprise Architect/Systems Modeler
   - The Generic Table Export plugin should appear in the appropriate menu

## Usage

### Basic Export

1. Select the elements you want to export
2. Click "Export to Table" in the toolbar
3. Choose your export format
4. Select columns to include
5. Click "Export"

### Advanced Features

#### Custom Templates

1. **Create Template**
   - Configure export settings
   - Click "Save as Template"
   - Enter template name and description

2. **Use Template**
   - Click "Load Template"
   - Select your template
   - Customize if needed
   - Export

#### Column Configuration

1. **Add/Remove Columns**
   - Use the column selector
   - Drag to reorder
   - Set column properties

2. **Custom Columns**
   - Click "Add Custom Column"
   - Enter column name
   - Define value expression
   - Save

#### Export Filters

1. **Apply Filters**
   - Click "Add Filter"
   - Select filter type
   - Set filter criteria
   - Apply

2. **Save Filter Set**
   - Configure filters
   - Click "Save Filter Set"
   - Name your filter set

## Development

### Project Structure
```
GenericTableExport/
├── src/                    # Source code
├── test/                   # Test files
├── lib/                    # Dependencies
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

### Dependencies

The plugin uses the following libraries:
- Apache POI (for Excel export)
- OpenCSV (for CSV export)
- Jsoup (for HTML export)

## Troubleshooting

### Common Issues

1. **Export Fails**
   - Check file permissions
   - Verify disk space
   - Check log files

2. **Missing Data**
   - Verify element selection
   - Check filter settings
   - Review column configuration

3. **Format Issues**
   - Check template settings
   - Verify column mappings
   - Review data types

### Getting Help

For issues and feature requests, please use the GitHub issue tracker.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 