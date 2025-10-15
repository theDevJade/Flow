use crate::config::Config;
use crate::registry::Registry;
use colored::*;
use std::io::{self, Write};

pub fn execute() -> Result<(), Box<dyn std::error::Error>> {
    println!("{} Logging into Flow Package Registry", "→".cyan().bold());
    println!();
    
    // Get email and password from user
    print!("Email: ");
    io::stdout().flush()?;
    let mut email = String::new();
    io::stdin().read_line(&mut email)?;
    email = email.trim().to_string();
    
    print!("Password: ");
    io::stdout().flush()?;
    let mut password = String::new();
    io::stdin().read_line(&mut password)?;
    password = password.trim().to_string();
    
    if email.is_empty() || password.is_empty() {
        return Err("Email and password are required".into());
    }
    
    // Load config
    let mut config = Config::load()?;
    
    // Create registry client
    let registry = Registry::new(config.registry.url.clone());
    
    // Authenticate with registry
    println!("  {} Authenticating...", "→".cyan());
    
    let client = reqwest::blocking::Client::new();
    let response = client
        .post(&format!("{}/api/v1/auth/login", config.registry.url))
        .json(&serde_json::json!({
            "email": email,
            "password": password
        }))
        .send()?;
    
    if !response.status().is_success() {
        return Err("Authentication failed. Please check your credentials.".into());
    }
    
    #[derive(serde::Deserialize)]
    struct AuthResponse {
        token: String,
        username: String,
        email: String,
    }
    
    let auth_response: AuthResponse = response.json()?;
    
    // Save token to config
    config.registry.token = Some(auth_response.token);
    config.save()?;
    
    println!("  {} Welcome back, {}!", "✓".green(), auth_response.username.yellow());
    println!("  {} Token saved to config", "✓".green());
    
    Ok(())
}
