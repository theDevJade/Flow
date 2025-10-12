# GitHub Actions Workflow Badges

Add these badges to your main README.md to show workflow status:

## CI Status
```markdown
[![CI - Build and Test](https://github.com/USERNAME/Flow/actions/workflows/ci.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/ci.yml)
```

## GitHub Pages Deployment
```markdown
[![Deploy to GitHub Pages](https://github.com/USERNAME/Flow/actions/workflows/pages.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/pages.yml)
```

## PR Checks
```markdown
[![PR Checks](https://github.com/USERNAME/Flow/actions/workflows/pr-check.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/pr-check.yml)
```

## CodeQL Analysis
```markdown
[![CodeQL](https://github.com/USERNAME/Flow/actions/workflows/codeql.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/codeql.yml)
```

## All Badges Together

```markdown
[![CI - Build and Test](https://github.com/USERNAME/Flow/actions/workflows/ci.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/ci.yml)
[![Deploy to GitHub Pages](https://github.com/USERNAME/Flow/actions/workflows/pages.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/pages.yml)
[![CodeQL](https://github.com/USERNAME/Flow/actions/workflows/codeql.yml/badge.svg)](https://github.com/USERNAME/Flow/actions/workflows/codeql.yml)
```

**Note:** Replace `USERNAME` with your actual GitHub username or organization name.

## Branch-Specific Badges

To show status for a specific branch:

```markdown
[![CI](https://github.com/USERNAME/Flow/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/USERNAME/Flow/actions/workflows/ci.yml)
```

## Custom Badge Styles

GitHub Actions badges support different styles:

```markdown
<!-- Flat style (default) -->
![CI](https://img.shields.io/github/actions/workflow/status/USERNAME/Flow/ci.yml?style=flat&label=CI)

<!-- Flat Square -->
![CI](https://img.shields.io/github/actions/workflow/status/USERNAME/Flow/ci.yml?style=flat-square&label=CI)

<!-- For the Badge -->
![CI](https://img.shields.io/github/actions/workflow/status/USERNAME/Flow/ci.yml?style=for-the-badge&label=CI)
```

