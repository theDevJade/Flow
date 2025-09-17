# GitHub Pages Deployment

This repository is configured to automatically deploy the Flow Documentation to GitHub Pages using GitHub Actions.

## Deployment Process

### Automatic Deployment
- **Trigger**: Every push to the `main` branch
- **Manual Trigger**: Available from the Actions tab in GitHub
- **Build Process**: Uses Node.js 20 and npm
- **Output**: Static site deployed to GitHub Pages

### Workflow Steps
1. **Checkout**: Clone the repository
2. **Setup Node.js**: Install Node.js 20 with npm caching
3. **Install Dependencies**: Run `npm ci` for clean install
4. **Build Site**: Run `npm run build` to generate static files
5. **Upload Artifacts**: Upload build artifacts to GitHub
6. **Deploy**: Deploy to GitHub Pages

## Configuration Files

### GitHub Workflow
- **File**: `.github/workflows/deploy.yml`
- **Purpose**: Defines the CI/CD pipeline for deployment
- **Triggers**: Push to main branch, manual workflow dispatch

### Static Site Configuration
- **File**: `.nojekyll`
- **Purpose**: Tells GitHub Pages to serve static files without Jekyll processing
- **Required**: Yes, for Astro static sites

## Setup Instructions

### 1. Enable GitHub Pages
1. Go to your repository settings
2. Navigate to "Pages" section
3. Set source to "GitHub Actions"

### 2. Configure Repository Permissions
The workflow requires the following permissions:
- `contents: read` - Read repository contents
- `pages: write` - Deploy to GitHub Pages
- `id-token: write` - Generate deployment tokens

### 3. Environment Setup
The workflow uses:
- **Node.js**: Version 20
- **Package Manager**: npm
- **Build Command**: `npm run build`
- **Output Directory**: `./dist`

## Local Development

### Prerequisites
- Node.js 20 or later
- npm (comes with Node.js)

### Development Commands
```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Development Server
- **URL**: http://localhost:4321
- **Hot Reload**: Enabled for development
- **File Watching**: Automatic rebuild on changes

## Build Configuration

### Astro Configuration
The site is configured in `astro.config.mjs`:
- **Framework**: Astro with Starlight
- **Output**: Static site generation
- **Base URL**: Configured for GitHub Pages
- **Site URL**: Set to your GitHub Pages domain

### Content Structure
- **Source**: `src/content/docs/`
- **Output**: `dist/`
- **Static Assets**: `src/assets/`

## Troubleshooting

### Common Issues

#### Build Failures
- Check Node.js version (requires 20+)
- Verify all dependencies are installed
- Check for TypeScript errors
- Ensure all content files are valid Markdown

#### Deployment Issues
- Verify GitHub Pages is enabled
- Check repository permissions
- Ensure workflow has required permissions
- Check Actions tab for error details

#### Content Not Updating
- Clear browser cache
- Check if build completed successfully
- Verify content files are in correct location
- Check for syntax errors in Markdown

### Debug Commands
```bash
# Check build locally
npm run build

# Preview built site
npm run preview

# Check for TypeScript errors
npx astro check

# Validate content
npx astro sync
```

## Custom Domain (Optional)

To use a custom domain:
1. Add `CNAME` file to repository root
2. Configure DNS settings
3. Update `site` URL in `astro.config.mjs`

## Performance Optimization

### Build Optimizations
- **Static Generation**: All pages pre-rendered
- **Asset Optimization**: Images and CSS optimized
- **Code Splitting**: JavaScript split by page
- **Tree Shaking**: Unused code removed

### GitHub Pages Optimizations
- **CDN**: Global content delivery
- **Compression**: Automatic gzip compression
- **Caching**: Browser and CDN caching
- **HTTPS**: Automatic SSL certificates

## Monitoring

### Build Status
- Check Actions tab for build status
- Monitor deployment logs
- Set up notifications for failures

### Site Analytics
- GitHub Pages provides basic analytics
- Consider adding Google Analytics
- Monitor page load times
- Track user engagement

## Security

### Repository Security
- Keep dependencies updated
- Use `npm audit` to check vulnerabilities
- Enable Dependabot for automatic updates
- Review workflow permissions regularly

### Content Security
- Validate all user-generated content
- Sanitize Markdown input
- Use HTTPS for all resources
- Implement CSP headers if needed

## Support

For deployment issues:
1. Check the Actions tab for error logs
2. Verify all configuration files are correct
3. Test build locally with `npm run build`
4. Check GitHub Pages documentation
5. Review Astro documentation for build issues

## License

This deployment configuration is part of the Flow project and is licensed under the Flow Non-Commercial Copyleft License (FNCL). See the [LICENSE](/LICENSE) file for details.
