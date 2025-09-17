# Flow GitHub Workflows

This directory contains three comprehensive GitHub Actions workflows for building and deploying the Flow project across all platforms.

## 🚀 Workflows Overview

### 1. Manual Build (`manual-build.yml`)
**Trigger:** Manual dispatch via GitHub Actions tab
**Purpose:** Build specific components on-demand with granular control

**Features:**
- ✅ Selective building (choose what to build)
- ✅ All platforms: macOS, Windows, Linux, Web
- ✅ Backend JARs (WebSocket server, Core engine)
- ✅ Minecraft plugin
- ✅ Interactive checkboxes for each component
- ✅ 30-day artifact retention

**Usage:**
1. Go to GitHub Actions tab
2. Select "Manual Build - All Platforms"
3. Click "Run workflow"
4. Choose which components to build
5. Download artifacts when complete

### 2. Automated Build (`automated-build.yml`)
**Trigger:** Push/PR to main/develop branches
**Purpose:** Continuous integration with smart change detection

**Features:**
- 🎯 **Smart Change Detection**: Only builds changed components
- 🧪 **Full Testing Suite**: Tests, linting, formatting, security scans
- 🏗️ **Conditional Platform Builds**: Full platform builds only on main branch
- 📊 **Coverage Reports**: Uploaded to Codecov
- 🔐 **Security Scanning**: Trivy vulnerability scanner
- 📈 **Build Summary**: Detailed status reporting

**Behavior:**
- **PRs/Feature branches**: Test and validate only
- **Main branch pushes**: Test + build all platforms + create artifacts

### 3. Release Workflow (`release.yml`)
**Trigger:** Git tags (v*) or manual dispatch
**Purpose:** Create GitHub releases with all production artifacts

**Features:**
- 🏷️ **Auto Release Creation**: From git tags or manual trigger
- 📦 **Complete Artifact Set**: All platforms + backend + plugin
- 📝 **Auto-generated Release Notes**: From commit history
- 🎯 **Production-ready**: Optimized release builds
- 📱 **Cross-platform**: macOS app, Windows exe, Linux AppImage, Web

**Artifacts Created:**
- `Flow-macOS.zip` - macOS application bundle
- `Flow-Windows.zip` - Windows executable and dependencies  
- `Flow-Linux.tar.gz` - Linux application bundle
- `Flow-Web.tar.gz` - Web application build
- `Flow-WebSocketServer.jar` - Backend WebSocket server
- `Flow-CoreEngine.jar` - Core processing engine
- `Flow-MinecraftPlugin.jar` - PaperMC plugin

## 🛠️ Technical Details

### Build Requirements
- **Flutter**: 3.24.0
- **Java**: OpenJDK 23 (Corretto distribution)
- **Gradle**: Uses wrapper (included in repo)
- **Node.js**: Not required (Flutter handles web builds)

### Platform-specific Dependencies
- **macOS**: Xcode command line tools (pre-installed on runner)
- **Windows**: Visual Studio Build Tools (pre-installed on runner)  
- **Linux**: GTK3, CMake, Ninja, Clang (auto-installed in workflow)

### Caching Strategy
- **Gradle**: Dependencies and build cache
- **Flutter**: SDK and pub cache
- **Platform builds**: Incremental builds where possible

### Artifact Management
- **Development builds**: 7-day retention
- **Release builds**: 30-day retention
- **Test results**: 7-day retention
- **Release assets**: Permanent (attached to GitHub release)

## 📋 Usage Examples

### Creating a Release
```bash
# Option 1: Git tag (automatic)
git tag v1.0.0
git push origin v1.0.0

# Option 2: Manual dispatch
# Go to GitHub Actions → "Create Release" → Run workflow
# Enter version: v1.0.0
# Choose draft/prerelease options
```

### Development Workflow
```bash
# Regular development - triggers automated build
git push origin feature/new-feature  # Tests only

# Main branch - triggers full build
git push origin main  # Tests + all platform builds
```

### Manual Testing
```bash
# Use manual build for testing specific components
# GitHub Actions → "Manual Build" → Select components
# Useful for:
# - Testing platform-specific builds
# - Validating before release  
# - Debug builds
```

## 🔧 Configuration

### Environment Variables
```yaml
env:
  FLUTTER_VERSION: '3.24.0'  # Update as needed
  JAVA_VERSION: '23'         # Java version for backend
```

### Path Filters (Automated Build)
The automated build only runs when relevant files change:
```yaml
paths:
  - 'flow_frontend/**'    # Flutter frontend
  - 'webserver/**'        # WebSocket server
  - 'flow/**'            # Core engine
  - 'common/**'          # Shared modules
  - 'plugin/**'          # Minecraft plugin
  - '*.gradle*'          # Build configuration
  - 'gradle/**'          # Gradle wrapper/config
```

### Secrets Required
- `GITHUB_TOKEN` - Auto-provided by GitHub Actions
- `CODECOV_TOKEN` - Optional, for coverage reports

## 🚨 Troubleshooting

### Common Issues

**Build fails on specific platform:**
- Check platform dependencies are correctly installed
- Verify Flutter version compatibility
- Review build logs for missing tools

**Gradle build fails:**
- Check Java version (must be 23+)
- Verify Gradle wrapper is executable
- Clear Gradle cache if needed

**Flutter build fails:**
- Ensure Flutter version matches project requirements
- Check pubspec.yaml for dependency conflicts
- Verify platform is enabled (`flutter config`)

**Artifact upload fails:**
- Check artifact paths are correct
- Verify build actually produces expected files
- Ensure artifact names are unique

### Debug Steps
1. **Check Workflow Logs**: Detailed output in GitHub Actions
2. **Manual Build**: Use manual workflow to test specific components
3. **Local Testing**: Reproduce builds locally first
4. **Platform Runners**: Test on same OS as GitHub runners

## 📚 Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Flutter CI/CD Guide](https://docs.flutter.dev/deployment/cd)
- [Gradle Build Scans](https://gradle.com/build-scans/)

---

**Note**: These workflows are designed for the Flow project's multi-platform architecture (Flutter frontend + Kotlin backend). Adapt paths and commands as needed for your specific setup.