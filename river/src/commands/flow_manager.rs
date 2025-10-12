use std::fs;
use std::path::PathBuf;
use std::process::Command;
use std::error::Error;
use colored::*;

const FLOW_REPO: &str = "https://github.com/thedevjade/flow.git";

pub fn install_flow(version: String) -> Result<(), Box<dyn Error>> {
    println!("\n{}", "Installing Flow compiler...".cyan().bold());
    println!("  Version: {}", version.green());
    
    let flow_dir = get_flow_dir()?;
    let version_dir = flow_dir.join("versions").join(&version);
    
    if version_dir.exists() {
        return Err(format!("Flow {} is already installed", version).into());
    }
    
    fs::create_dir_all(&version_dir)
        .map_err(|e| format!("Failed to create version directory: {}", e))?;
    
    println!("\n  {} Downloading Flow {}...", "→".cyan(), version);
    
    // Clone or download Flow repository
    let clone_dir = flow_dir.join("tmp").join(&version);
    fs::create_dir_all(&clone_dir.parent().unwrap())
        .map_err(|e| format!("Failed to create temp directory: {}", e))?;
    
    if version == "latest" || version == "main" {
        // Clone main branch
        let status = Command::new("git")
            .args(&["clone", "--depth=1", FLOW_REPO, clone_dir.to_str().unwrap()])
            .status()
            .map_err(|e| format!("Failed to clone repository: {}", e))?;
        
        if !status.success() {
            return Err("Failed to clone Flow repository".into());
        }
    } else {
        // Clone and checkout specific version/commit
        let status = Command::new("git")
            .args(&["clone", FLOW_REPO, clone_dir.to_str().unwrap()])
            .status()
            .map_err(|e| format!("Failed to clone repository: {}", e))?;
        
        if !status.success() {
            return Err("Failed to clone Flow repository".into());
        }
        
        let status = Command::new("git")
            .args(&["checkout", &version])
            .current_dir(&clone_dir)
            .status()
            .map_err(|e| format!("Failed to checkout version: {}", e))?;
        
        if !status.success() {
            return Err(format!("Version {} not found", version).into());
        }
    }
    
    println!("  {} Building Flow compiler...", "→".cyan());
    
    // Build Flow compiler
    let flowbase_dir = clone_dir.join("flowbase");
    let build_dir = flowbase_dir.join("build");
    
    fs::create_dir_all(&build_dir)
        .map_err(|e| format!("Failed to create build directory: {}", e))?;
    
    let status = Command::new("cmake")
        .arg("..")
        .current_dir(&build_dir)
        .status()
        .map_err(|e| format!("Failed to run cmake: {}", e))?;
    
    if !status.success() {
        return Err("CMake configuration failed".into());
    }
    
    let status = Command::new("make")
        .arg("-j")
        .current_dir(&build_dir)
        .status()
        .map_err(|e| format!("Failed to run make: {}", e))?;
    
    if !status.success() {
        return Err("Build failed".into());
    }
    
    println!("  {} Installing binaries...", "→".cyan());
    
    // Copy binaries to version directory
    let bin_dir = version_dir.join("bin");
    fs::create_dir_all(&bin_dir)
        .map_err(|e| format!("Failed to create bin directory: {}", e))?;
    
    fs::copy(build_dir.join("flowbase"), bin_dir.join("flow"))
        .map_err(|e| format!("Failed to copy flow binary: {}", e))?;
    
    fs::copy(build_dir.join("flow-lsp"), bin_dir.join("flow-lsp"))
        .map_err(|e| format!("Failed to copy flow-lsp binary: {}", e))?;
    
    // Clean up
    fs::remove_dir_all(&clone_dir)
        .map_err(|e| format!("Failed to clean up: {}", e))?;
    
    println!("\n{} Flow {} installed successfully!", "✓".green().bold(), version);
    println!("  Run 'river use-flow {}' to activate", version);
    
    Ok(())
}

pub fn list_flows() -> Result<(), Box<dyn Error>> {
    println!("\n{}", "Installed Flow versions:".cyan().bold());
    
    let flow_dir = get_flow_dir()?;
    let versions_dir = flow_dir.join("versions");
    
    if !versions_dir.exists() {
        println!("  No Flow versions installed");
        return Ok(());
    }
    
    let current = get_current_version()?;
    
    let entries = fs::read_dir(&versions_dir)
        .map_err(|e| format!("Failed to read versions directory: {}", e))?;
    
    for entry in entries {
        let entry = entry.map_err(|e| format!("Failed to read entry: {}", e))?;
        let version = entry.file_name().to_string_lossy().to_string();
        
        if Some(&version) == current.as_ref() {
            println!("  {} {} (active)", "→".green(), version.green().bold());
        } else {
            println!("    {}", version);
        }
    }
    
    Ok(())
}

pub fn use_flow(version: String) -> Result<(), Box<dyn Error>> {
    let flow_dir = get_flow_dir()?;
    let version_dir = flow_dir.join("versions").join(&version);
    
    if !version_dir.exists() {
        return Err(format!("Flow {} is not installed. Run 'river install-flow {}'", version, version).into());
    }
    
    let current_link = flow_dir.join("current");
    
    // Remove existing symlink if it exists
    if current_link.exists() {
        fs::remove_file(&current_link)
            .map_err(|e| format!("Failed to remove current link: {}", e))?;
    }
    
    // Create new symlink
    #[cfg(unix)]
    std::os::unix::fs::symlink(&version_dir, &current_link)
        .map_err(|e| format!("Failed to create symlink: {}", e))?;
    
    #[cfg(windows)]
    std::os::windows::fs::symlink_dir(&version_dir, &current_link)
        .map_err(|e| format!("Failed to create symlink: {}", e))?;
    
    println!("\n{} Now using Flow {}", "✓".green().bold(), version.green());
    println!("  Compiler: {}/bin/flow", version_dir.display());
    
    Ok(())
}

pub fn update_flow() -> Result<(), Box<dyn Error>> {
    println!("\n{}", "Updating Flow to latest version...".cyan().bold());
    
    // Install latest version
    install_flow("latest".to_string())?;
    
    // Switch to it
    use_flow("latest".to_string())?;
    
    Ok(())
}

fn get_flow_dir() -> Result<PathBuf, Box<dyn Error>> {
    let home = std::env::var("HOME")
        .map_err(|_| "HOME environment variable not set")?;
    
    let flow_dir = PathBuf::from(home).join(".flow");
    fs::create_dir_all(&flow_dir)
        .map_err(|e| format!("Failed to create Flow directory: {}", e))?;
    
    Ok(flow_dir)
}

fn get_current_version() -> Result<Option<String>, Box<dyn Error>> {
    let flow_dir = get_flow_dir()?;
    let current_link = flow_dir.join("current");
    
    if !current_link.exists() {
        return Ok(None);
    }
    
    let target = fs::read_link(&current_link)
        .map_err(|e| format!("Failed to read current link: {}", e))?;
    
    let version = target.file_name()
        .and_then(|s| s.to_str())
        .map(|s| s.to_string());
    
    Ok(version)
}
