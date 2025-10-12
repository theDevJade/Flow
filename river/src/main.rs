mod cli;
mod commands;
mod config;
mod manifest;
mod package;
mod registry;
mod resolver;
mod builder;
mod utils;

use clap::Parser;
use colored::*;

fn main() {
    let cli = cli::Cli::parse();
    
    let result = match cli.command {
        cli::Commands::Init { name, kind } => commands::init::execute(name, kind),
        cli::Commands::Build => commands::build::execute(),
        cli::Commands::Install { package } => commands::install::execute(package),
        cli::Commands::Add { package, version } => commands::add::execute(package, version),
        cli::Commands::Remove { package } => commands::remove::execute(package),
        cli::Commands::Publish => commands::publish::execute(),
        cli::Commands::Search { query } => commands::search::execute(query),
        cli::Commands::List => commands::list::execute(),
        cli::Commands::Update => commands::update::execute(),
        cli::Commands::Clean => commands::clean::execute(),
    };
    
    match result {
        Ok(()) => {},
        Err(e) => {
            eprintln!("{} {}", "error:".red().bold(), e);
            std::process::exit(1);
        }
    }
}

