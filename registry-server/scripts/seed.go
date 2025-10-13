package main

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	"github.com/joho/godotenv"
	_ "github.com/lib/pq"
	"github.com/flowlang/registry-server/config"
)

type PackageData struct {
	Name         string
	Version      string
	Description  string
	Author       string
	License      string
	Repository   string
	Homepage     string
	Keywords     []string
	Dependencies []string
	Checksum     string
	Downloads    int
}

var mockPackages = []PackageData{
	{
		Name:         "http",
		Version:      "2.0.0",
		Description:  "HTTP client library for Flow with support for modern web protocols",
		Author:       "Flow Team",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/http",
		Homepage:     "https://flowc.dev/packages/http",
		Keywords:     []string{"http", "client", "rest", "api"},
		Dependencies: []string{"json", "tls"},
		Checksum:     "a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2",
		Downloads:    15420,
	},
	{
		Name:         "json",
		Version:      "1.8.2",
		Description:  "Fast and reliable JSON parser and serializer for Flow",
		Author:       "Flow Core",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/json",
		Homepage:     "https://flowc.dev/packages/json",
		Keywords:     []string{"json", "parser", "serializer"},
		Dependencies: []string{},
		Checksum:     "b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6",
		Downloads:    28500,
	},
	{
		Name:         "tls",
		Version:      "1.5.0",
		Description:  "TLS/SSL implementation for secure network communications",
		Author:       "Security Team",
		License:      "Apache-2.0",
		Repository:   "https://github.com/flowlang/tls",
		Homepage:     "https://flowc.dev/packages/tls",
		Keywords:     []string{"tls", "ssl", "security", "encryption"},
		Dependencies: []string{"crypto"},
		Checksum:     "c9e2f3b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8",
		Downloads:    12300,
	},
	{
		Name:         "database",
		Version:      "3.1.4",
		Description:  "Unified database interface supporting PostgreSQL, MySQL, and SQLite",
		Author:       "Flow Database Team",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/database",
		Homepage:     "https://flowc.dev/packages/database",
		Keywords:     []string{"database", "sql", "postgresql", "mysql", "sqlite"},
		Dependencies: []string{"async", "pool"},
		Checksum:     "d1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3",
		Downloads:    9850,
	},
	{
		Name:         "async",
		Version:      "2.3.1",
		Description:  "Async runtime and utilities for concurrent programming",
		Author:       "Flow Core",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/async",
		Homepage:     "https://flowc.dev/packages/async",
		Keywords:     []string{"async", "concurrent", "runtime", "futures"},
		Dependencies: []string{},
		Checksum:     "e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2",
		Downloads:    21400,
	},
	{
		Name:         "web",
		Version:      "1.0.0",
		Description:  "Modern web framework for building fast and scalable applications",
		Author:       "Flow Web Team",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/web",
		Homepage:     "https://flowc.dev/packages/web",
		Keywords:     []string{"web", "framework", "server", "router"},
		Dependencies: []string{"http", "router", "templates"},
		Checksum:     "f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5",
		Downloads:    5200,
	},
	{
		Name:         "crypto",
		Version:      "2.1.0",
		Description:  "Cryptographic primitives and utilities",
		Author:       "Security Team",
		License:      "Apache-2.0",
		Repository:   "https://github.com/flowlang/crypto",
		Homepage:     "https://flowc.dev/packages/crypto",
		Keywords:     []string{"crypto", "encryption", "hash", "security"},
		Dependencies: []string{},
		Checksum:     "a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8",
		Downloads:    18700,
	},
	{
		Name:         "testing",
		Version:      "1.4.2",
		Description:  "Comprehensive testing framework with assertions and mocking",
		Author:       "Flow Core",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/testing",
		Homepage:     "https://flowc.dev/packages/testing",
		Keywords:     []string{"testing", "test", "assertions", "mock"},
		Dependencies: []string{},
		Checksum:     "b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3",
		Downloads:    14200,
	},
	{
		Name:         "cli",
		Version:      "0.9.5",
		Description:  "Build beautiful command-line interfaces with ease",
		Author:       "Flow CLI Team",
		License:      "MIT",
		Repository:   "https://github.com/flowlang/cli",
		Homepage:     "https://flowc.dev/packages/cli",
		Keywords:     []string{"cli", "command-line", "terminal", "args"},
		Dependencies: []string{"colors", "parser"},
		Checksum:     "c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8",
		Downloads:    8900,
	},
}

func main() {
	log.Println("🌱 Flow Registry Seed Script")
	log.Println("============================")

	// Load environment
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using environment variables")
	}

	cfg := config.Load()

	// Connect to database
	connStr := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		cfg.DBHost,
		cfg.DBPort,
		cfg.DBUser,
		cfg.DBPassword,
		cfg.DBName,
		cfg.DBSSLMode,
	)

	db, err := sql.Open("postgres", connStr)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	if err := db.Ping(); err != nil {
		log.Fatalf("Failed to ping database: %v", err)
	}

	log.Println("✓ Connected to database")

	// Seed packages
	for i, pkg := range mockPackages {
		log.Printf("\n[%d/%d] Seeding package: %s v%s", i+1, len(mockPackages), pkg.Name, pkg.Version)

		// Insert or update package
		var packageID int
		err := db.QueryRow(`
			INSERT INTO packages (name, description, author, license, repository, homepage)
			VALUES ($1, $2, $3, $4, $5, $6)
			ON CONFLICT (name) DO UPDATE SET
				description = EXCLUDED.description,
				author = EXCLUDED.author,
				license = EXCLUDED.license,
				repository = EXCLUDED.repository,
				homepage = EXCLUDED.homepage
			RETURNING id
		`, pkg.Name, pkg.Description, pkg.Author, pkg.License, pkg.Repository, pkg.Homepage).Scan(&packageID)

		if err != nil {
			log.Printf("  ✗ Failed to insert package: %v", err)
			continue
		}

		log.Printf("  ✓ Package inserted (ID: %d)", packageID)

		// Insert version
		downloadURL := fmt.Sprintf("https://registry.flowc.dev/packages/%s/%s/download", pkg.Name, pkg.Version)
		var versionID int

		// Check if version exists
		err = db.QueryRow(`
			SELECT id FROM package_versions WHERE package_id = $1 AND version = $2
		`, packageID, pkg.Version).Scan(&versionID)

		if err == sql.ErrNoRows {
			// Insert new version
			err = db.QueryRow(`
				INSERT INTO package_versions (package_id, version, download_url, checksum, downloads, published_at)
				VALUES ($1, $2, $3, $4, $5, $6)
				RETURNING id
			`, packageID, pkg.Version, downloadURL, pkg.Checksum, pkg.Downloads, time.Now().Add(-time.Hour*24*7)).Scan(&versionID)

			if err != nil {
				log.Printf("  ✗ Failed to insert version: %v", err)
				continue
			}

			log.Printf("  ✓ Version inserted (ID: %d)", versionID)
		} else if err != nil {
			log.Printf("  ✗ Failed to check version: %v", err)
			continue
		} else {
			log.Printf("  ℹ Version already exists (ID: %d)", versionID)
		}

		// Insert dependencies
		for _, dep := range pkg.Dependencies {
			_, err := db.Exec(`
				INSERT INTO dependencies (package_version_id, dependency_name)
				VALUES ($1, $2)
				ON CONFLICT DO NOTHING
			`, versionID, dep)

			if err != nil {
				log.Printf("  ⚠ Failed to insert dependency %s: %v", dep, err)
			} else {
				log.Printf("  ✓ Dependency added: %s", dep)
			}
		}

		// Insert keywords
		for _, keyword := range pkg.Keywords {
			_, err := db.Exec(`
				INSERT INTO package_keywords (package_id, keyword)
				VALUES ($1, $2)
				ON CONFLICT DO NOTHING
			`, packageID, keyword)

			if err != nil {
				log.Printf("  ⚠ Failed to insert keyword %s: %v", keyword, err)
			} else {
				log.Printf("  ✓ Keyword added: %s", keyword)
			}
		}
	}

	log.Println("\n============================")
	log.Printf("✓ Successfully seeded %d packages!", len(mockPackages))
	log.Println("\nYou can now query packages at:")
	log.Println("  GET http://localhost:8080/api/v1/packages")
	log.Println("  GET http://localhost:8080/api/v1/packages/http")
	log.Println("  GET http://localhost:8080/api/v1/search?q=web")
}

