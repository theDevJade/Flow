use crate::config::Config;
use crate::registry::Registry;
use colored::*;

pub fn execute(query: String) -> Result<(), Box<dyn std::error::Error>> {
    println!("{} Searching for '{}'...", "→".cyan().bold(), query.yellow());
    
    // Load config
    let config = Config::load()?;
    
    // Connect to registry
    let registry = Registry::new(config.registry.url.clone());
    
    // Search packages
    let results = registry.search(&query)?;
    
    if results.is_empty() {
        println!("{} No packages found", "!".yellow());
        return Ok(());
    }
    
    println!("\nFound {} packages:\n", results.len());
    
    for result in results {
        println!("  {} {} @ {}", 
            "→".cyan(),
            result.name.yellow().bold(),
            result.version.cyan());
        
        if let Some(desc) = result.description {
            println!("    {}", desc.dimmed());
        }
        println!();
    }
    
    Ok(())
}

