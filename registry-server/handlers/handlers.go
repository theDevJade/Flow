package handlers

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/flowlang/registry-server/config"
	"github.com/flowlang/registry-server/models"
	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

type Handler struct {
	db  *sql.DB
	cfg *config.Config
}

func NewHandler(db *sql.DB, cfg *config.Config) *Handler {
	if err := os.MkdirAll(cfg.StoragePath, 0700); err != nil {
		panic(fmt.Sprintf("Failed to create storage directory: %v", err))
	}

	return &Handler{
		db:  db,
		cfg: cfg,
	}
}

// GetPackageInfo handles GET /api/v1/packages/:name
func (h *Handler) GetPackageInfo(c *gin.Context) {
	name := c.Param("name")

	// Get package
	var pkg models.Package
	err := h.db.QueryRow(`
		SELECT id, name, description, author, license, repository, homepage, created_at
		FROM packages WHERE name = $1
	`, name).Scan(&pkg.ID, &pkg.Name, &pkg.Description, &pkg.Author, &pkg.License, &pkg.Repository, &pkg.Homepage, &pkg.CreatedAt)

	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": fmt.Sprintf("Package '%s' not found", name)})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	// Get latest version
	var version models.PackageVersion
	err = h.db.QueryRow(`
		SELECT id, version, download_url, checksum, published_at, downloads
		FROM package_versions
		WHERE package_id = $1
		ORDER BY published_at DESC
		LIMIT 1
	`, pkg.ID).Scan(&version.ID, &version.Version, &version.DownloadURL, &version.Checksum, &version.PublishedAt, &version.Downloads)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "No versions found"})
		return
	}

	// Get dependencies
	rows, err := h.db.Query(`
		SELECT dependency_name FROM dependencies WHERE package_version_id = $1
	`, version.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}
	defer rows.Close()

	var dependencies []string
	for rows.Next() {
		var dep string
		if err := rows.Scan(&dep); err != nil {
			continue
		}
		dependencies = append(dependencies, dep)
	}
	if dependencies == nil {
		dependencies = []string{}
	}

	// Get keywords
	keywordRows, err := h.db.Query(`
		SELECT keyword FROM package_keywords WHERE package_id = $1
	`, pkg.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}
	defer keywordRows.Close()

	var keywords []string
	for keywordRows.Next() {
		var keyword string
		if err := keywordRows.Scan(&keyword); err != nil {
			continue
		}
		keywords = append(keywords, keyword)
	}
	if keywords == nil {
		keywords = []string{}
	}

	// Build response
	info := models.PackageInfo{
		Name:         pkg.Name,
		Version:      version.Version,
		Description:  pkg.Description,
		Author:       pkg.Author,
		License:      pkg.License,
		DownloadURL:  version.DownloadURL,
		Checksum:     version.Checksum,
		Dependencies: dependencies,
		Repository:   pkg.Repository,
		Homepage:     pkg.Homepage,
		Keywords:     keywords,
		PublishedAt:  version.PublishedAt.Format(time.RFC3339),
		Downloads:    version.Downloads,
	}

	c.JSON(http.StatusOK, info)
}

// ListVersions handles GET /api/v1/packages/:name/versions
func (h *Handler) ListVersions(c *gin.Context) {
	name := c.Param("name")

	// Get package ID
	var packageID int
	err := h.db.QueryRow("SELECT id FROM packages WHERE name = $1", name).Scan(&packageID)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": fmt.Sprintf("Package '%s' not found", name)})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	// Get all versions
	rows, err := h.db.Query(`
		SELECT version FROM package_versions
		WHERE package_id = $1
		ORDER BY published_at DESC
	`, packageID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}
	defer rows.Close()

	var versions []string
	for rows.Next() {
		var version string
		if err := rows.Scan(&version); err != nil {
			continue
		}
		versions = append(versions, version)
	}

	if versions == nil {
		versions = []string{}
	}

	c.JSON(http.StatusOK, models.VersionsResponse{Versions: versions})
}

// DownloadPackage handles GET /api/v1/packages/:name/:version/download
func (h *Handler) DownloadPackage(c *gin.Context) {
	name := c.Param("name")
	version := c.Param("version")

	// Get package version info
	var versionID int
	var checksum string
	err := h.db.QueryRow(`
		SELECT pv.id, pv.checksum
		FROM package_versions pv
		JOIN packages p ON pv.package_id = p.id
		WHERE p.name = $1 AND pv.version = $2
	`, name, version).Scan(&versionID, &checksum)

	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Package version not found"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	// Increment download counter
	_, _ = h.db.Exec(`UPDATE package_versions SET downloads = downloads + 1 WHERE id = $1`, versionID)

	// Get file path
	filename := fmt.Sprintf("%s-%s.tar.gz", name, version)
	filepath := filepath.Join(h.cfg.StoragePath, filename)

	// Check if file exists
	if _, err := os.Stat(filepath); os.IsNotExist(err) {
		c.JSON(http.StatusNotFound, gin.H{"error": "Package file not found"})
		return
	}

	// Set headers
	c.Header("Content-Type", "application/gzip")
	c.Header("X-Checksum", checksum)
	c.Header("Content-Disposition", fmt.Sprintf("attachment; filename=%s", filename))

	// Serve file
	c.File(filepath)
}

// SearchPackages handles GET /api/v1/search?q={query}&limit={limit}
func (h *Handler) SearchPackages(c *gin.Context) {
	query := c.Query("q")
	limitStr := c.DefaultQuery("limit", "10")
	limit, _ := strconv.Atoi(limitStr)

	if limit <= 0 || limit > 100 {
		limit = 10
	}

	var results []models.SearchResult

	if query == "" {
		// Return all packages if no query
		rows, err := h.db.Query(`
			SELECT p.name, pv.version, p.description, pv.downloads, pv.published_at, p.license
			FROM packages p
			JOIN package_versions pv ON p.id = pv.package_id
			WHERE pv.id IN (
				SELECT MAX(id) FROM package_versions GROUP BY package_id
			)
			ORDER BY pv.downloads DESC
			LIMIT $1
		`, limit)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
			return
		}
		defer rows.Close()

		for rows.Next() {
			var result models.SearchResult
			var publishedAt time.Time
			if err := rows.Scan(&result.Name, &result.Version, &result.Description, &result.Downloads, &publishedAt, &result.License); err != nil {
				continue
			}
			result.UpdatedAt = publishedAt.Format(time.RFC3339)

			// Get keywords
			keywords, _ := h.getKeywords(result.Name)
			result.Keywords = keywords

			results = append(results, result)
		}
	} else {
		// Search by name, description, or keywords
		searchQuery := "%" + strings.ToLower(query) + "%"

		rows, err := h.db.Query(`
			SELECT DISTINCT p.name, pv.version, p.description, pv.downloads, pv.published_at, p.license
			FROM packages p
			JOIN package_versions pv ON p.id = pv.package_id
			LEFT JOIN package_keywords pk ON p.id = pk.package_id
			WHERE pv.id IN (
				SELECT MAX(id) FROM package_versions GROUP BY package_id
			)
			AND (
				LOWER(p.name) LIKE $1 OR
				LOWER(p.description) LIKE $1 OR
				LOWER(pk.keyword) LIKE $1
			)
			ORDER BY pv.downloads DESC
			LIMIT $2
		`, searchQuery, limit)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
			return
		}
		defer rows.Close()

		for rows.Next() {
			var result models.SearchResult
			var publishedAt time.Time
			if err := rows.Scan(&result.Name, &result.Version, &result.Description, &result.Downloads, &publishedAt, &result.License); err != nil {
				continue
			}
			result.UpdatedAt = publishedAt.Format(time.RFC3339)

			// Get keywords
			keywords, _ := h.getKeywords(result.Name)
			result.Keywords = keywords

			results = append(results, result)
		}
	}

	if results == nil {
		results = []models.SearchResult{}
	}

	c.JSON(http.StatusOK, models.SearchResponse{
		Results: results,
		Total:   len(results),
	})
}

// GetAllPackages handles GET /api/v1/packages
func (h *Handler) GetAllPackages(c *gin.Context) {
	rows, err := h.db.Query(`
		SELECT p.name, pv.version, p.description, pv.downloads, pv.published_at, p.license
		FROM packages p
		JOIN package_versions pv ON p.id = pv.package_id
		WHERE pv.id IN (
			SELECT MAX(id) FROM package_versions GROUP BY package_id
		)
		ORDER BY pv.downloads DESC
	`)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}
	defer rows.Close()

	var results []models.SearchResult
	for rows.Next() {
		var result models.SearchResult
		var publishedAt time.Time
		if err := rows.Scan(&result.Name, &result.Version, &result.Description, &result.Downloads, &publishedAt, &result.License); err != nil {
			continue
		}
		result.UpdatedAt = publishedAt.Format(time.RFC3339)

		// Get keywords
		keywords, _ := h.getKeywords(result.Name)
		result.Keywords = keywords

		results = append(results, result)
	}

	if results == nil {
		results = []models.SearchResult{}
	}

	c.JSON(http.StatusOK, results)
}

// PublishPackage handles POST /api/v1/publish (authenticated)
func (h *Handler) PublishPackage(c *gin.Context) {
	// Get user from context (set by auth middleware)
	username, _ := c.Get("username")

	// Parse multipart form
	if err := c.Request.ParseMultipartForm(100 << 20); err != nil { // 100 MB limit
		c.JSON(http.StatusBadRequest, gin.H{"error": "Failed to parse form"})
		return
	}

	// Get form fields
	name := c.PostForm("name")
	version := c.PostForm("version")
	description := c.PostForm("description")
	author := c.PostForm("author")
	license := c.PostForm("license")
	repository := c.PostForm("repository")
	homepage := c.PostForm("homepage")
	checksum := c.PostForm("checksum")
	dependencies := c.PostFormArray("dependencies[]")
	keywords := c.PostFormArray("keywords[]")

	if name == "" || version == "" || author == "" || checksum == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing required fields"})
		return
	}

	// Get uploaded file
	file, _, err := c.Request.FormFile("package")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Package file required"})
		return
	}
	defer file.Close()

	// Verify checksum
	hasher := sha256.New()
	if _, err := io.Copy(hasher, file); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read file"})
		return
	}
	computedChecksum := fmt.Sprintf("%x", hasher.Sum(nil))
	if computedChecksum != checksum {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Checksum mismatch"})
		return
	}

	// Reset file pointer
	file.Seek(0, 0)

	// Save file
	filename := fmt.Sprintf("%s-%s.tar.gz", name, version)
	destPath := filepath.Join(h.cfg.StoragePath, filename)
	dest, err := os.Create(destPath)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save file"})
		return
	}
	defer dest.Close()

	if _, err := io.Copy(dest, file); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save file"})
		return
	}

	// Begin transaction
	tx, err := h.db.Begin()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}
	defer tx.Rollback()

	// Insert or get package
	var packageID int
	err = tx.QueryRow(`
		INSERT INTO packages (name, description, author, license, repository, homepage)
		VALUES ($1, $2, $3, $4, $5, $6)
		ON CONFLICT (name) DO UPDATE SET
			description = EXCLUDED.description,
			author = EXCLUDED.author,
			license = EXCLUDED.license,
			repository = EXCLUDED.repository,
			homepage = EXCLUDED.homepage
		RETURNING id
	`, name, description, author, license, repository, homepage).Scan(&packageID)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create package"})
		return
	}

	// Check if version already exists
	var existingVersion string
	err = tx.QueryRow(`
		SELECT version FROM package_versions WHERE package_id = $1 AND version = $2
	`, packageID, version).Scan(&existingVersion)

	if err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "Version already exists"})
		return
	}

	// Insert package version
	downloadURL := fmt.Sprintf("https://registry.flowc.dev/api/v1/packages/%s/%s/download", name, version)
	var versionID int
	err = tx.QueryRow(`
		INSERT INTO package_versions (package_id, version, download_url, checksum)
		VALUES ($1, $2, $3, $4)
		RETURNING id
	`, packageID, version, downloadURL, checksum).Scan(&versionID)

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create version"})
		return
	}

	// Insert dependencies
	for _, dep := range dependencies {
		if dep != "" {
			_, err = tx.Exec(`
				INSERT INTO dependencies (package_version_id, dependency_name)
				VALUES ($1, $2)
			`, versionID, dep)
			if err != nil {
				c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to add dependencies"})
				return
			}
		}
	}

	// Insert keywords
	for _, keyword := range keywords {
		if keyword != "" {
			_, err = tx.Exec(`
				INSERT INTO package_keywords (package_id, keyword)
				VALUES ($1, $2)
				ON CONFLICT DO NOTHING
			`, packageID, keyword)
			if err != nil {
				continue // Ignore errors for keywords
			}
		}
	}

	// Commit transaction
	if err := tx.Commit(); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to commit transaction"})
		return
	}

	c.JSON(http.StatusOK, models.PublishResponse{
		Success: true,
		Message: fmt.Sprintf("Package published successfully by %v", username),
		URL:     fmt.Sprintf("https://registry.flowc.dev/packages/%s", name),
	})
}

// DeletePackage handles DELETE /api/v1/packages/:name/:version (authenticated)
func (h *Handler) DeletePackage(c *gin.Context) {
	name := c.Param("name")
	version := c.Param("version")

	// Get package version ID
	var versionID int
	err := h.db.QueryRow(`
		SELECT pv.id
		FROM package_versions pv
		JOIN packages p ON pv.package_id = p.id
		WHERE p.name = $1 AND pv.version = $2
	`, name, version).Scan(&versionID)

	if err == sql.ErrNoRows {
		c.JSON(http.StatusNotFound, gin.H{"error": "Package version not found"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	// Delete from database (cascade will handle dependencies)
	_, err = h.db.Exec(`DELETE FROM package_versions WHERE id = $1`, versionID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to delete package"})
		return
	}

	// Delete file
	filename := fmt.Sprintf("%s-%s.tar.gz", name, version)
	filepath := filepath.Join(h.cfg.StoragePath, filename)
	os.Remove(filepath) // Ignore error if file doesn't exist

	c.JSON(http.StatusOK, gin.H{"message": "Package deleted successfully"})
}

// Register handles POST /api/v1/auth/register
func (h *Handler) Register(c *gin.Context) {
	var req models.RegisterRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Hash password
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to hash password"})
		return
	}

	// Generate API token
	apiToken := uuid.New().String()

	// Insert user
	var userID int
	err = h.db.QueryRow(`
		INSERT INTO users (username, email, password_hash, api_token)
		VALUES ($1, $2, $3, $4)
		RETURNING id
	`, req.Username, req.Email, string(hashedPassword), apiToken).Scan(&userID)

	if err != nil {
		if strings.Contains(err.Error(), "duplicate") {
			c.JSON(http.StatusConflict, gin.H{"error": "Username or email already exists"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create user"})
		return
	}

	// Generate JWT token
	token, err := h.generateJWT(req.Username, req.Email)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	c.JSON(http.StatusCreated, models.AuthResponse{
		Token:    token,
		Username: req.Username,
		Email:    req.Email,
	})
}

// Login handles POST /api/v1/auth/login
func (h *Handler) Login(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// Get user
	var user models.User
	err := h.db.QueryRow(`
		SELECT id, username, email, password_hash
		FROM users WHERE email = $1
	`, req.Email).Scan(&user.ID, &user.Username, &user.Email, &user.PasswordHash)

	if err == sql.ErrNoRows {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid credentials"})
		return
	}
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	// Verify password
	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid credentials"})
		return
	}

	// Generate JWT token
	token, err := h.generateJWT(user.Username, user.Email)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to generate token"})
		return
	}

	c.JSON(http.StatusOK, models.AuthResponse{
		Token:    token,
		Username: user.Username,
		Email:    user.Email,
	})
}

// Helper functions

func (h *Handler) getKeywords(packageName string) ([]string, error) {
	rows, err := h.db.Query(`
		SELECT pk.keyword
		FROM package_keywords pk
		JOIN packages p ON pk.package_id = p.id
		WHERE p.name = $1
	`, packageName)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var keywords []string
	for rows.Next() {
		var keyword string
		if err := rows.Scan(&keyword); err != nil {
			continue
		}
		keywords = append(keywords, keyword)
	}

	if keywords == nil {
		keywords = []string{}
	}

	return keywords, nil
}

func (h *Handler) generateJWT(username, email string) (string, error) {
	claims := jwt.MapClaims{
		"username": username,
		"email":    email,
		"exp":      time.Now().Add(time.Hour * 24 * 30).Unix(), // 30 days
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(h.cfg.JWTSecret))
}

