# Flow Documentation

This is the comprehensive documentation for the Flow project, built with [Starlight](https://starlight.astro.build/) and [Astro](https://astro.build/).

## Project Overview

Flow is a comprehensive development platform consisting of:

- **FlowLang**: A lightweight, embeddable scripting language for Kotlin/JVM applications
- **Flow Frontend**: A modern Flutter application with dual workspace architecture

## Documentation Structure

### FlowLang Documentation
- [Introduction](/flowlang/introduction) - Overview of FlowLang scripting language
- [Tutorial](/flowlang/tutorial) - Step-by-step learning guide
- [Language Reference](/flowlang/language-reference) - Complete syntax documentation
- [API Reference](/flowlang/api-reference) - Integration and API details
- [Developer Guide](/flowlang/developer-guide) - Extending and customizing FlowLang
- [Examples](/flowlang/examples) - Real-world usage patterns
- [Changelog](/flowlang/changelog) - Version history and changes

### Flow Frontend Documentation
- [Introduction](/frontend/introduction) - Overview of the Flutter application
- [Features](/frontend/features) - Detailed feature overview
- [Architecture](/frontend/architecture) - Technical architecture and design patterns
- [Getting Started](/frontend/getting-started) - Setup and installation guide
- [Development](/frontend/development) - Development guidelines and best practices

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm, yarn, or pnpm

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd FlowDocumentation
   ```

2. Install dependencies:
   ```bash
   npm install
   # or
   yarn install
   # or
   pnpm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   # or
   yarn dev
   # or
   pnpm dev
   ```

4. Open your browser and navigate to `http://localhost:4321`

### Building for Production

```bash
npm run build
# or
yarn build
# or
pnpm build
```

The built site will be in the `dist/` directory.

## Development

### Project Structure

```
FlowDocumentation/
├── docs/                    # Documentation content
│   ├── flowlang/           # FlowLang documentation
│   └── frontend/           # Flow Frontend documentation
├── src/                    # Source files
│   └── assets/             # Static assets
├── astro.config.mjs        # Astro configuration
├── package.json            # Dependencies and scripts
├── tsconfig.json           # TypeScript configuration
└── README.md               # This file
```

### Adding Content

1. **FlowLang Documentation**: Add new pages to `docs/flowlang/`
2. **Frontend Documentation**: Add new pages to `docs/frontend/`
3. **Update Navigation**: Modify `astro.config.mjs` to update the sidebar

### Styling

The documentation uses Starlight's built-in styling system. You can customize:

- Colors and themes in `astro.config.mjs`
- Custom CSS in `src/styles/`
- Component overrides in `src/components/`

## Features

- **Modern Design**: Clean, responsive design with dark/light theme support
- **Search**: Full-text search across all documentation
- **Navigation**: Intuitive sidebar navigation with collapsible sections
- **Code Highlighting**: Syntax highlighting for multiple languages
- **Mobile Friendly**: Responsive design that works on all devices
- **Fast Loading**: Optimized for performance with Astro's static site generation

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Commit your changes: `git commit -m "Add your feature"`
5. Push to the branch: `git push origin feature/your-feature-name`
6. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For questions and support:

1. Check the documentation first
2. Search existing issues
3. Create a new issue if needed
4. Join our community discussions

## Acknowledgments

- Built with [Astro](https://astro.build/)
- Documentation powered by [Starlight](https://starlight.astro.build/)
- Icons from [Heroicons](https://heroicons.com/)
- Syntax highlighting by [Shiki](https://shiki.matsu.io/)
