use crate::config::Config;
use colored::*;
use walkdir::WalkDir;

pub fn execute() -> Result<(), Box<dyn std::error::Error>> {
    println!("{} Installed packages:", "→".cyan().bold());


    let config = Config::load()?;

    let packages_dir = &config.paths.packages;

    if !packages_dir.exists() {
        println!("{} No packages installed", "!".yellow());
        return Ok(());
    }

    let mut count = 0;

    for entry in WalkDir::new(packages_dir)
        .max_depth(1)
        .into_iter()
        .filter_map(|e| e.ok())
    {
        if entry.path() == packages_dir {
            continue;
        }

        if entry.file_type().is_dir() {
            if let Some(name) = entry.file_name().to_str() {
                // Parse name-version format
                if let Some((pkg_name, version)) = name.rsplit_once('-') {
                    println!("  {} {} @ {}",
                        "→".cyan(),
                        pkg_name.yellow(),
                        version.cyan());
                    count += 1;
                }
            }
        }
    }

    if count == 0 {
        println!("{} No packages installed", "!".yellow());
    } else {
        println!("\nTotal: {}", count.to_string().green().bold());
    }

    Ok(())
}

