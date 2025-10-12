use crate::manifest::Manifest;
use colored::*;

pub fn execute(package: String) -> Result<(), Box<dyn std::error::Error>> {
    println!("{} Removing dependency '{}'...", "→".cyan().bold(), package.yellow());
    
    // Load manifest
    let mut manifest = Manifest::load("River.toml")?;
    
    // Remove dependency
    if manifest.remove_dependency(&package) {
        manifest.save("River.toml")?;
        println!("{} Removed {}", "✓".green().bold(), package.yellow());
    } else {
        println!("{} Package '{}' not found in dependencies", "!".yellow(), package);
    }
    
    Ok(())
}

