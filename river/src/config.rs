use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    pub registry: RegistryConfig,
    pub paths: PathsConfig,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RegistryConfig {
    pub url: String,
    #[serde(default)]
    pub token: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PathsConfig {
    pub packages: PathBuf,
    pub cache: PathBuf,
}

impl Default for Config {
    fn default() -> Self {
        let home = dirs::home_dir().unwrap_or_else(|| PathBuf::from("."));
        let river_home = home.join(".river");


        let registry_url = std::env::var("RIVER_REGISTRY_URL")
            .unwrap_or_else(|_| "https://registry.flowc.dev".to_string());

        Config {
            registry: RegistryConfig {
                url: registry_url,
                token: None,
            },
            paths: PathsConfig {
                packages: river_home.join("packages"),
                cache: river_home.join("cache"),
            },
        }
    }
}

impl Config {
    /// Load config from file or create default
    pub fn load() -> Result<Self, Box<dyn std::error::Error>> {
        let config_path = Self::config_path();

        if config_path.exists() {
            let content = fs::read_to_string(config_path)?;
            let config: Config = toml::from_str(&content)?;
            Ok(config)
        } else {
            let config = Config::default();
            config.save()?;
            Ok(config)
        }
    }

    /// Save config to file
    pub fn save(&self) -> Result<(), Box<dyn std::error::Error>> {
        let config_path = Self::config_path();

        if let Some(parent) = config_path.parent() {
            fs::create_dir_all(parent)?;
        }

        let content = toml::to_string_pretty(self)?;
        fs::write(config_path, content)?;

        Ok(())
    }

    /// Get the config file path
    fn config_path() -> PathBuf {
        let home = dirs::home_dir().unwrap_or_else(|| PathBuf::from("."));
        home.join(".river").join("config.toml")
    }

    /// Ensure all directories exist
    pub fn ensure_directories(&self) -> Result<(), Box<dyn std::error::Error>> {
        fs::create_dir_all(&self.paths.packages)?;
        fs::create_dir_all(&self.paths.cache)?;
        Ok(())
    }
}

