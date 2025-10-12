use clap::{Parser, Subcommand};

#[derive(Parser)]
#[command(name = "river")]
#[command(about = "Package manager for the Flow programming language", long_about = None)]
#[command(version)]
pub struct Cli {
    #[command(subcommand)]
    pub command: Commands,
}

#[derive(Subcommand)]
pub enum Commands {
    /// Initialize a new Flow package
    Init {
        /// Package name
        #[arg(default_value = ".")]
        name: String,
        
        /// Package kind: bin (executable) or lib (library)
        #[arg(short, long, default_value = "bin")]
        kind: String,
    },
    
    /// Build the current package
    Build,
    
    /// Install a package from the registry
    Install {
        /// Package name to install
        package: String,
    },
    
    /// Add a dependency to the current package
    Add {
        /// Package name
        package: String,
        
        /// Version constraint (default: latest)
        #[arg(short, long)]
        version: Option<String>,
    },
    
    /// Remove a dependency from the current package
    Remove {
        /// Package name
        package: String,
    },
    
    /// Publish the current package to the registry
    Publish,
    
    /// Search for packages in the registry
    Search {
        /// Search query
        query: String,
    },
    
    /// List installed packages
    List,
    
    /// Update all dependencies
    Update,
    
    /// Clean build artifacts
    Clean,
}

