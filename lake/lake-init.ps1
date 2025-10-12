# Lake - The Flow toolchain installer (PowerShell version)
# Run with: iwr -useb https://lake.flow-lang.org/init.ps1 | iex

param(
    [string]$FlowHome = "$env:USERPROFILE\.flow"
)

$ErrorActionPreference = 'Stop'

# Constants
$LakeVersion = "0.1.0"
$LakeBin = "$FlowHome\bin"
$LakeDistServer = if ($env:LAKE_DIST_SERVER) { $env:LAKE_DIST_SERVER } else { "https://github.com/theDevJade/flow/releases" }

# Colors for output
function Write-Info {
    param([string]$Message)
    Write-Host "info: " -ForegroundColor Cyan -NoNewline
    Write-Host $Message
}

function Write-Success {
    param([string]$Message)
    Write-Host "success: " -ForegroundColor Green -NoNewline
    Write-Host $Message
}

function Write-Warning {
    param([string]$Message)
    Write-Host "warning: " -ForegroundColor Yellow -NoNewline
    Write-Host $Message
}

function Write-Error {
    param([string]$Message)
    Write-Host "error: " -ForegroundColor Red -NoNewline
    Write-Host $Message
}

# Banner
function Show-Banner {
    Write-Host @"
    ___     ______  ____        ___  ____        __
   / __)   (  ___ \(___ \      / __)(___ \      /  \
  | |__ _   )___) ) ___) )    | |__  ___) )    / /\ \
  |  __)   (  ___/ (___ (     |__  )(__ (    ( (_  ) )
  | |      | |     ___) )    ___| |___) )    \ \/ /
  |_|      |_|    (____/    (______)(____/     \__/

  Lake - The Flow Toolchain Installer
"@
}

# Detect architecture
function Get-Architecture {
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
    switch ($arch) {
        'X64' { return 'x86_64' }
        'Arm64' { return 'aarch64' }
        default {
            Write-Error "Unsupported architecture: $arch"
            exit 1
        }
    }
}

# Get OS triple
function Get-OSTriple {
    $arch = Get-Architecture
    return "$arch-pc-windows-msvc"
}

# Download prebuilt binaries
function Get-PrebuiltBinaries {
    param([string]$Triple)
    
    $version = "latest"
    $baseUrl = "$LakeDistServer/download/$version"
    $flowUrl = "$baseUrl/flow-$Triple.zip"
    $riverUrl = "$baseUrl/river-$Triple.zip"
    
    $downloadDir = "$FlowHome\downloads"
    New-Item -ItemType Directory -Force -Path $downloadDir | Out-Null
    
    try {
        Write-Info "Downloading Flow compiler..."
        Invoke-WebRequest -Uri $flowUrl -OutFile "$downloadDir\flow.zip" -UseBasicParsing
        Expand-Archive -Path "$downloadDir\flow.zip" -DestinationPath $LakeBin -Force
        Remove-Item "$downloadDir\flow.zip"
        
        Write-Info "Downloading River package manager..."
        Invoke-WebRequest -Uri $riverUrl -OutFile "$downloadDir\river.zip" -UseBasicParsing
        Expand-Archive -Path "$downloadDir\river.zip" -DestinationPath $LakeBin -Force
        Remove-Item "$downloadDir\river.zip"
        
        return $true
    } catch {
        return $false
    }
}

# Build from source
function Build-FromSource {
    Write-Info "Checking build dependencies..."
    
    # Check for required tools
    $requiredTools = @('git', 'cmake', 'cargo')
    foreach ($tool in $requiredTools) {
        if (!(Get-Command $tool -ErrorAction SilentlyContinue)) {
            Write-Error "Required tool not found: $tool"
            Write-Host ""
            Write-Host "Please install the following:"
            Write-Host "  - Git"
            Write-Host "  - CMake (3.15+)"
            Write-Host "  - Visual Studio or Build Tools with C++ support"
            Write-Host "  - Rust (rustup.rs)"
            Write-Host "  - LLVM (12+)"
            Write-Host ""
            Write-Host "Installation guides:"
            Write-Host "  Git:   https://git-scm.com/download/win"
            Write-Host "  CMake: https://cmake.org/download/"
            Write-Host "  Rust:  https://rustup.rs/"
            Write-Host "  LLVM:  https://releases.llvm.org/"
            exit 1
        }
    }
    
    $srcDir = "$FlowHome\src"
    
    if (Test-Path $srcDir) {
        Write-Info "Updating source..."
        Push-Location $srcDir
        git pull origin main
        Pop-Location
    } else {
        Write-Info "Cloning repository..."
        git clone --depth 1 https://github.com/theDevJade/flow.git $srcDir
    }
    
    # Build Flow compiler
    Write-Info "Building Flow compiler (this may take a few minutes)..."
    Push-Location "$srcDir\flowbase"
    
    if (!(Test-Path "build")) {
        New-Item -ItemType Directory -Path "build" | Out-Null
    }
    
    Push-Location "build"
    cmake .. 2>&1 | Out-Null
    cmake --build . --config Release 2>&1 | Out-Null
    
    Copy-Item "Release\flowbase.exe" "$LakeBin\flow.exe" -Force
    Copy-Item "Release\flow-lsp.exe" "$LakeBin\flow-lsp.exe" -Force
    
    Pop-Location
    Pop-Location
    
    # Build River package manager
    Write-Info "Building River package manager..."
    Push-Location "$srcDir\river"
    cargo build --release 2>&1 | Out-Null
    Copy-Item "target\release\river.exe" "$LakeBin\river.exe" -Force
    Pop-Location
    
    Write-Success "Built from source"
}

# Setup environment
function Set-Environment {
    # Add to PATH in user environment
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    
    if ($userPath -notlike "*$LakeBin*") {
        Write-Info "Adding to PATH..."
        [Environment]::SetEnvironmentVariable(
            'Path',
            "$LakeBin;$userPath",
            'User'
        )
    }
    
    # Set FLOW_HOME
    [Environment]::SetEnvironmentVariable('FLOW_HOME', $FlowHome, 'User')
    
    # Update current session
    $env:Path = "$LakeBin;$env:Path"
    $env:FLOW_HOME = $FlowHome
}

# Main installation function
function Install-Flow {
    Show-Banner
    Write-Host ""
    Write-Info "Installing Flow toolchain"
    Write-Host ""
    
    # Detect platform
    $triple = Get-OSTriple
    Write-Info "Platform: $triple"
    Write-Host ""
    
    # Check for existing installation
    if (Test-Path $FlowHome) {
        Write-Warning "Flow is already installed at $FlowHome"
        $response = Read-Host "Would you like to update it? (y/N)"
        if ($response -ne 'y' -and $response -ne 'Y') {
            Write-Info "Installation cancelled"
            exit 0
        }
        Write-Host ""
    }
    
    # Create directories
    New-Item -ItemType Directory -Force -Path $LakeBin | Out-Null
    New-Item -ItemType Directory -Force -Path "$FlowHome\toolchains" | Out-Null
    New-Item -ItemType Directory -Force -Path "$FlowHome\downloads" | Out-Null
    
    Write-Info "Checking for prebuilt binaries..."
    
    # Try to download prebuilt binaries
    if (Get-PrebuiltBinaries $triple) {
        Write-Success "Downloaded prebuilt toolchain"
    } else {
        Write-Warning "Prebuilt binaries not available"
        Write-Info "Building from source..."
        Build-FromSource
    }
    
    # Setup environment
    Set-Environment
    
    Write-Host ""
    Write-Host "✓ Flow toolchain installed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "To get started, restart your terminal and run:"
    Write-Host ""
    Write-Host "  flow --version" -ForegroundColor Cyan
    Write-Host "  river --version" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Note: You may need to restart your terminal for PATH changes to take effect."
    Write-Host ""
}

# Run installation
try {
    Install-Flow
} catch {
    Write-Host ""
    Write-Error "Installation failed: $_"
    exit 1
}

