package middleware

import (
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

type RateLimiter struct {
	requests map[string][]time.Time
	mu       sync.RWMutex
	limit    int
	window   time.Duration
}

func NewRateLimiter(limit int, window time.Duration) *RateLimiter {
	rl := &RateLimiter{
		requests: make(map[string][]time.Time),
		limit:    limit,
		window:   window,
	}

	go rl.cleanup()

	return rl
}

func (rl *RateLimiter) cleanup() {
	ticker := time.NewTicker(time.Minute)
	for range ticker.C {
		rl.mu.Lock()
		now := time.Now()
		for ip, times := range rl.requests {
			filtered := []time.Time{}
			for _, t := range times {
				if now.Sub(t) < rl.window {
					filtered = append(filtered, t)
				}
			}
			if len(filtered) == 0 {
				delete(rl.requests, ip)
			} else {
				rl.requests[ip] = filtered
			}
		}
		rl.mu.Unlock()
	}
}

func (rl *RateLimiter) Middleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		ip := c.ClientIP()

		rl.mu.Lock()
		now := time.Now()
		
		requests, exists := rl.requests[ip]
		if !exists {
			rl.requests[ip] = []time.Time{now}
			rl.mu.Unlock()
			c.Next()
			return
		}

		filtered := []time.Time{}
		for _, t := range requests {
			if now.Sub(t) < rl.window {
				filtered = append(filtered, t)
			}
		}

		if len(filtered) >= rl.limit {
			rl.mu.Unlock()
			c.Header("X-RateLimit-Limit", string(rune(rl.limit)))
			c.Header("X-RateLimit-Remaining", "0")
			c.Header("Retry-After", "60")
			c.JSON(http.StatusTooManyRequests, gin.H{
				"error": "Rate limit exceeded. Please try again later.",
			})
			c.Abort()
			return
		}

		filtered = append(filtered, now)
		rl.requests[ip] = filtered
		rl.mu.Unlock()

		c.Header("X-RateLimit-Limit", string(rune(rl.limit)))
		c.Header("X-RateLimit-Remaining", string(rune(rl.limit-len(filtered))))
		c.Next()
	}
}

