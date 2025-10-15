use crate::manifest::Manifest;
use crate::config::Config;
use crate::registry::Registry;
use colored::*;

pub fn execute() -> Result<(), Box<dyn std::error::Error>> {
    println!("{} Updating dependencies...", "→".cyan().bold());


    let manifest = Manifest::load("River.toml")?;

    if manifest.dependencies.is_empty() {
        println!("{} No dependencies to update", "!".yellow());
        return Ok(());
    }


    let config = Config::load()?;
    let registry = Registry::new(config.registry.url.clone());

    for (name, _) in manifest.dependencies.iter() {
        println!("  {} Checking {}...", "→".cyan(), name.yellow());

        match registry.get_package_info(name) {
            Ok(info) => {
                println!("    {} Latest version: {}", "✓".green(), info.version.cyan());

            }
            Err(e) => {
                println!("    {} Error: {}", "✗".red(), e);
            }
        }
    }

    println!("{} Update check complete", "✓".green().bold());

    Ok(())
}

