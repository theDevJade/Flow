package middleware

import (
	"regexp"
	"strings"

	"github.com/gin-gonic/gin"
)

var (
	packageNameRegex = regexp.MustCompile(`^[a-z0-9_-]+$`)
	versionRegex     = regexp.MustCompile(`^\d+\.\d+\.\d+(-[a-z0-9.-]+)?$`)
)

func SecurityHeaders() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("X-Content-Type-Options", "nosniff")
		c.Header("X-Frame-Options", "DENY")
		c.Header("X-XSS-Protection", "1; mode=block")
		c.Header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
		c.Header("Content-Security-Policy", "default-src 'self'")
		c.Header("Referrer-Policy", "strict-origin-when-cross-origin")
		c.Header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
		
		c.Next()
	}
}

func ValidatePackageName(name string) bool {
	if len(name) < 2 || len(name) > 100 {
		return false
	}
	return packageNameRegex.MatchString(name)
}

func ValidateVersion(version string) bool {
	if len(version) > 50 {
		return false
	}
	return versionRegex.MatchString(version)
}

func SanitizeString(s string, maxLength int) string {
	s = strings.TrimSpace(s)
	
	if len(s) > maxLength {
		s = s[:maxLength]
	}
	
	dangerous := []string{"<script", "</script", "javascript:", "onerror=", "onload="}
	for _, d := range dangerous {
		s = strings.ReplaceAll(strings.ToLower(s), d, "")
	}
	
	return s
}

func ValidateEmail(email string) bool {
	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	return emailRegex.MatchString(email) && len(email) < 255
}

func ValidateUsername(username string) bool {
	usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
	return usernameRegex.MatchString(username) && len(username) >= 3 && len(username) <= 50
}

