package main

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	"github.com/flowlang/registry-server/config"
	"github.com/flowlang/registry-server/handlers"
	"github.com/flowlang/registry-server/middleware"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	_ "github.com/lib/pq"
)

func main() {
	// Load environment variables
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, using environment variables")
	}

	cfg := config.Load()
	
	if cfg.Environment == "production" {
		if cfg.JWTSecret == "your-super-secret-jwt-key-change-in-production" {
			log.Fatal("FATAL: Default JWT secret detected in production! Set JWT_SECRET environment variable.")
		}
		if len(cfg.JWTSecret) < 32 {
			log.Fatal("FATAL: JWT secret too short! Must be at least 32 characters.")
		}
	}

	db, err := initDB(cfg)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
	defer db.Close()

	if cfg.Environment == "production" {
		gin.SetMode(gin.ReleaseMode)
	}
	
	router := gin.Default()

	router.Use(middleware.SecurityHeaders())

	corsConfig := cors.Config{
		AllowOrigins: []string{
			"https://flowc.dev",
			"https://registry.flowc.dev",
			"https://www.flowc.dev",
			"http://localhost:3000",
			"http://localhost:5173",
		},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Authorization"},
		ExposeHeaders:    []string{"Content-Length"},
		AllowCredentials: true,
		MaxAge:           12 * time.Hour,
	}
	
	if cfg.Environment == "development" {
		corsConfig.AllowOrigins = append(corsConfig.AllowOrigins, "http://localhost:8080")
	}
	
	router.Use(cors.New(corsConfig))

	h := handlers.NewHandler(db, cfg)

	authLimiter := middleware.NewRateLimiter(5, time.Minute)
	apiLimiter := middleware.NewRateLimiter(60, time.Minute)
	downloadLimiter := middleware.NewRateLimiter(30, time.Minute)

	api := router.Group("/api/v1")
	api.Use(apiLimiter.Middleware())
	{
		api.GET("/packages/:name", h.GetPackageInfo)
		api.GET("/packages/:name/versions", h.ListVersions)
		
		downloadGroup := api.Group("/packages")
		downloadGroup.Use(downloadLimiter.Middleware())
		{
			downloadGroup.GET("/:name/:version/download", h.DownloadPackage)
		}
		
		api.GET("/search", h.SearchPackages)
		api.GET("/packages", h.GetAllPackages)
		
		authGroup := api.Group("/auth")
		authGroup.Use(authLimiter.Middleware())
		{
			authGroup.POST("/register", h.Register)
			authGroup.POST("/login", h.Login)
		}
	}

	protected := api.Group("")
	protected.Use(middleware.AuthMiddleware(cfg.JWTSecret))
	protected.Use(authLimiter.Middleware())
	{
		protected.POST("/publish", h.PublishPackage)
		protected.DELETE("/packages/:name/:version", h.DeletePackage)
	}

	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok", "version": "1.0.0"})
	})

	if cfg.Environment == "production" {
		router.Use(func(c *gin.Context) {
			if c.Request.Header.Get("X-Forwarded-Proto") != "https" {
				c.Redirect(301, "https://"+c.Request.Host+c.Request.RequestURI)
				c.Abort()
				return
			}
			c.Next()
		})
	}

	// Start server
	addr := fmt.Sprintf(":%s", cfg.Port)
	log.Printf("Starting Flow Registry Server on %s", addr)
	log.Printf("Environment: %s", cfg.Environment)
	log.Printf("Database: Connected")
	log.Printf("Storage: %s", cfg.StoragePath)
	log.Printf("Security: Rate limiting enabled")
	
	if err := router.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}

func initDB(cfg *config.Config) (*sql.DB, error) {
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
		return nil, err
	}

	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(5)
	db.SetConnMaxLifetime(5 * time.Minute)

	if err := db.Ping(); err != nil {
		return nil, err
	}

	// Run migrations
	if err := runMigrations(db); err != nil {
		return nil, fmt.Errorf("failed to run migrations: %w", err)
	}

	return db, nil
}

func runMigrations(db *sql.DB) error {
	log.Println("Running database migrations...")

	migrations := []string{
		// Create packages table
		`CREATE TABLE IF NOT EXISTS packages (
			id SERIAL PRIMARY KEY,
			name VARCHAR(255) UNIQUE NOT NULL,
			description TEXT,
			author VARCHAR(255) NOT NULL,
			license VARCHAR(50),
			repository TEXT,
			homepage TEXT,
			created_at TIMESTAMP DEFAULT NOW()
		)`,

		// Create package_versions table
		`CREATE TABLE IF NOT EXISTS package_versions (
			id SERIAL PRIMARY KEY,
			package_id INTEGER REFERENCES packages(id) ON DELETE CASCADE,
			version VARCHAR(50) NOT NULL,
			download_url TEXT NOT NULL,
			checksum VARCHAR(64) NOT NULL,
			published_at TIMESTAMP DEFAULT NOW(),
			downloads INTEGER DEFAULT 0,
			UNIQUE(package_id, version)
		)`,

		// Create dependencies table
		`CREATE TABLE IF NOT EXISTS dependencies (
			id SERIAL PRIMARY KEY,
			package_version_id INTEGER REFERENCES package_versions(id) ON DELETE CASCADE,
			dependency_name VARCHAR(255) NOT NULL,
			version_constraint VARCHAR(50)
		)`,

		`CREATE TABLE IF NOT EXISTS users (
			id SERIAL PRIMARY KEY,
			username VARCHAR(255) UNIQUE NOT NULL,
			email VARCHAR(255) UNIQUE NOT NULL,
			password_hash VARCHAR(255) NOT NULL,
			api_token VARCHAR(64) UNIQUE,
			email_verified BOOLEAN DEFAULT FALSE,
			failed_login_attempts INTEGER DEFAULT 0,
			locked_until TIMESTAMP,
			created_at TIMESTAMP DEFAULT NOW(),
			last_login TIMESTAMP,
			CONSTRAINT username_format CHECK (username ~ '^[a-zA-Z0-9_-]+$'),
			CONSTRAINT username_length CHECK (length(username) >= 3 AND length(username) <= 50)
		)`,

		// Create package_keywords table
		`CREATE TABLE IF NOT EXISTS package_keywords (
			id SERIAL PRIMARY KEY,
			package_id INTEGER REFERENCES packages(id) ON DELETE CASCADE,
			keyword VARCHAR(100) NOT NULL,
			UNIQUE(package_id, keyword)
		)`,


		`CREATE TABLE IF NOT EXISTS audit_log (
			id SERIAL PRIMARY KEY,
			user_id INTEGER REFERENCES users(id),
			action VARCHAR(50) NOT NULL,
			resource_type VARCHAR(50),
			resource_id VARCHAR(255),
			ip_address VARCHAR(50),
			user_agent TEXT,
			created_at TIMESTAMP DEFAULT NOW()
		)`,

		`CREATE INDEX IF NOT EXISTS idx_packages_name ON packages(name)`,
		`CREATE INDEX IF NOT EXISTS idx_package_versions_package_id ON package_versions(package_id)`,
		`CREATE INDEX IF NOT EXISTS idx_package_keywords_package_id ON package_keywords(package_id)`,
		`CREATE INDEX IF NOT EXISTS idx_package_keywords_keyword ON package_keywords(keyword)`,
		`CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)`,
		`CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)`,
		`CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id)`,
		`CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at)`,
	}

	for _, migration := range migrations {
		if _, err := db.Exec(migration); err != nil {
			return fmt.Errorf("migration failed: %w", err)
		}
	}

	log.Println("Migrations completed successfully")
	return nil
}

