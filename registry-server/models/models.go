package models

import "time"

// Package represents a Flow package
type Package struct {
	ID          int       `json:"id"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	Author      string    `json:"author"`
	License     string    `json:"license"`
	Repository  string    `json:"repository"`
	Homepage    string    `json:"homepage"`
	CreatedAt   time.Time `json:"created_at"`
}

// PackageVersion represents a specific version of a package
type PackageVersion struct {
	ID          int       `json:"id"`
	PackageID   int       `json:"package_id"`
	Version     string    `json:"version"`
	DownloadURL string    `json:"download_url"`
	Checksum    string    `json:"checksum"`
	PublishedAt time.Time `json:"published_at"`
	Downloads   int       `json:"downloads"`
}

// Dependency represents a package dependency
type Dependency struct {
	ID                 int    `json:"id"`
	PackageVersionID   int    `json:"package_version_id"`
	DependencyName     string `json:"dependency_name"`
	VersionConstraint  string `json:"version_constraint"`
}

// User represents a registry user
type User struct {
	ID           int       `json:"id"`
	Username     string    `json:"username"`
	Email        string    `json:"email"`
	PasswordHash string    `json:"-"`
	APIToken     string    `json:"-"`
	CreatedAt    time.Time `json:"created_at"`
}

// PackageInfo represents the full package information returned by the API
type PackageInfo struct {
	Name         string   `json:"name"`
	Version      string   `json:"version"`
	Description  string   `json:"description"`
	Author       string   `json:"author"`
	License      string   `json:"license"`
	DownloadURL  string   `json:"download_url"`
	Checksum     string   `json:"checksum"`
	Dependencies []string `json:"dependencies"`
	Repository   string   `json:"repository"`
	Homepage     string   `json:"homepage"`
	Keywords     []string `json:"keywords"`
	PublishedAt  string   `json:"published_at"`
	Downloads    int      `json:"downloads,omitempty"`
}

// SearchResult represents a package search result
type SearchResult struct {
	Name        string   `json:"name"`
	Version     string   `json:"version"`
	Description string   `json:"description"`
	Downloads   int      `json:"downloads"`
	Keywords    []string `json:"keywords,omitempty"`
	License     string   `json:"license,omitempty"`
	UpdatedAt   string   `json:"updated_at,omitempty"`
}

// SearchResponse represents the search API response
type SearchResponse struct {
	Results []SearchResult `json:"results"`
	Total   int            `json:"total"`
}

// VersionsResponse represents the versions list API response
type VersionsResponse struct {
	Versions []string `json:"versions"`
}

// PublishRequest represents a package publish request
type PublishRequest struct {
	Name         string   `json:"name" binding:"required"`
	Version      string   `json:"version" binding:"required"`
	Description  string   `json:"description"`
	Author       string   `json:"author" binding:"required"`
	License      string   `json:"license"`
	Repository   string   `json:"repository"`
	Homepage     string   `json:"homepage"`
	Keywords     []string `json:"keywords"`
	Dependencies []string `json:"dependencies"`
	Checksum     string   `json:"checksum" binding:"required"`
}

// PublishResponse represents the publish API response
type PublishResponse struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
	URL     string `json:"url,omitempty"`
}

// RegisterRequest represents a user registration request
type RegisterRequest struct {
	Username string `json:"username" binding:"required"`
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required,min=8"`
}

// LoginRequest represents a user login request
type LoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required"`
}

// AuthResponse represents an authentication response
type AuthResponse struct {
	Token    string `json:"token"`
	Username string `json:"username"`
	Email    string `json:"email"`
}

