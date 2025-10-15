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
        
        /// Package kind: bin (executable), lib (library), or native (C library)
        #[arg(short, long, default_value = "bin")]
        kind: String,
    },
    
    /// Build the current package
    Build,
    
    /// Build and run the current package
    Run {
        /// Arguments to pass to the program
        #[arg(trailing_var_arg = true)]
        args: Vec<String>,
    },
    
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
    
    /// Login to the registry
    Login,
    
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
    
    /// Install a specific version of the Flow compiler
    InstallFlow {
        /// Version to install (e.g., "0.1.0", "latest", or git commit)
        version: String,
    },
    
    /// List installed Flow compiler versions
    ListFlows,
    
    /// Switch to a specific Flow compiler version
    UseFlow {
        /// Version to use
        version: String,
    },
    
    /// Update Flow compiler to the latest version
    UpdateFlow,
}

