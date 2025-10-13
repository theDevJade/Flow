// API Client for Flow Package Registry

export interface PackageInfo {
    name: string;
    version: string;
    description: string;
    author: string;
    license: string;
    download_url: string;
    checksum: string;
    dependencies: string[];
    repository: string;
    homepage: string;
    keywords: string[];
    published_at: string;
    downloads?: number;
}

export interface SearchResult {
    name: string;
    version: string;
    description: string;
    downloads: number;
    keywords?: string[];
    license?: string;
    updated_at?: string;
}

export interface SearchResponse {
    results: SearchResult[];
    total: number;
}

// Fallback data (used if API is unavailable)
const fallbackPackages: PackageInfo[] = [
    {
        name: "http",
        version: "2.0.0",
        description: "HTTP client library for Flow with support for modern web protocols",
        author: "Flow Team",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/http-2.0.0.tar.gz",
        checksum: "a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2",
        dependencies: ["json", "tls"],
        repository: "https://github.com/flowlang/http",
        homepage: "https://flowlang.org/packages/http",
        keywords: ["http", "client", "rest", "api"],
        published_at: "2025-10-11T12:00:00Z",
        downloads: 15420
    },
    {
        name: "json",
        version: "1.8.2",
        description: "Fast and reliable JSON parser and serializer for Flow",
        author: "Flow Core",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/json-1.8.2.tar.gz",
        checksum: "b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6",
        dependencies: [],
        repository: "https://github.com/flowlang/json",
        homepage: "https://flowlang.org/packages/json",
        keywords: ["json", "parser", "serializer"],
        published_at: "2025-09-28T14:30:00Z",
        downloads: 28500
    },
    {
        name: "tls",
        version: "1.5.0",
        description: "TLS/SSL implementation for secure network communications",
        author: "Security Team",
        license: "Apache-2.0",
        download_url: "https://registry.flowlang.org/packages/tls-1.5.0.tar.gz",
        checksum: "c9e2f3b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8",
        dependencies: ["crypto"],
        repository: "https://github.com/flowlang/tls",
        homepage: "https://flowlang.org/packages/tls",
        keywords: ["tls", "ssl", "security", "encryption"],
        published_at: "2025-10-01T10:15:00Z",
        downloads: 12300
    },
    {
        name: "database",
        version: "3.1.4",
        description: "Unified database interface supporting PostgreSQL, MySQL, and SQLite",
        author: "Flow Database Team",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/database-3.1.4.tar.gz",
        checksum: "d1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3",
        dependencies: ["async", "pool"],
        repository: "https://github.com/flowlang/database",
        homepage: "https://flowlang.org/packages/database",
        keywords: ["database", "sql", "postgresql", "mysql", "sqlite"],
        published_at: "2025-10-08T16:45:00Z",
        downloads: 9850
    },
    {
        name: "async",
        version: "2.3.1",
        description: "Async runtime and utilities for concurrent programming",
        author: "Flow Core",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/async-2.3.1.tar.gz",
        checksum: "e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2",
        dependencies: [],
        repository: "https://github.com/flowlang/async",
        homepage: "https://flowlang.org/packages/async",
        keywords: ["async", "concurrent", "runtime", "futures"],
        published_at: "2025-09-15T11:20:00Z",
        downloads: 21400
    },
    {
        name: "web",
        version: "1.0.0",
        description: "Modern web framework for building fast and scalable applications",
        author: "Flow Web Team",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/web-1.0.0.tar.gz",
        checksum: "f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5",
        dependencies: ["http", "router", "templates"],
        repository: "https://github.com/flowlang/web",
        homepage: "https://flowlang.org/packages/web",
        keywords: ["web", "framework", "server", "router"],
        published_at: "2025-10-10T09:00:00Z",
        downloads: 5200
    },
    {
        name: "crypto",
        version: "2.1.0",
        description: "Cryptographic primitives and utilities",
        author: "Security Team",
        license: "Apache-2.0",
        download_url: "https://registry.flowlang.org/packages/crypto-2.1.0.tar.gz",
        checksum: "a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8",
        dependencies: [],
        repository: "https://github.com/flowlang/crypto",
        homepage: "https://flowlang.org/packages/crypto",
        keywords: ["crypto", "encryption", "hash", "security"],
        published_at: "2025-09-20T13:30:00Z",
        downloads: 18700
    },
    {
        name: "testing",
        version: "1.4.2",
        description: "Comprehensive testing framework with assertions and mocking",
        author: "Flow Core",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/testing-1.4.2.tar.gz",
        checksum: "b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3",
        dependencies: [],
        repository: "https://github.com/flowlang/testing",
        homepage: "https://flowlang.org/packages/testing",
        keywords: ["testing", "test", "assertions", "mock"],
        published_at: "2025-10-05T15:10:00Z",
        downloads: 14200
    },
    {
        name: "cli",
        version: "0.9.5",
        description: "Build beautiful command-line interfaces with ease",
        author: "Flow CLI Team",
        license: "MIT",
        download_url: "https://registry.flowlang.org/packages/cli-0.9.5.tar.gz",
        checksum: "c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8d1e4f6a9c2d5e8b1f4a7c3e9f2b8",
        dependencies: ["colors", "parser"],
        repository: "https://github.com/flowlang/cli",
        homepage: "https://flowlang.org/packages/cli",
        keywords: ["cli", "command-line", "terminal", "args"],
        published_at: "2025-10-02T08:25:00Z",
        downloads: 8900
    }
];

// API Configuration
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';

// API client
export const api = {
    async getPackageInfo(name: string): Promise<PackageInfo> {
        const response = await fetch(`${API_BASE_URL}/packages/${name}`);
        if (!response.ok) {
            throw new Error(`Package '${name}' not found`);
        }
        return await response.json();
    },

    async searchPackages(query: string, limit: number = 10): Promise<SearchResponse> {
        const params = new URLSearchParams({ q: query, limit: limit.toString() });
        const response = await fetch(`${API_BASE_URL}/search?${params}`);
        if (!response.ok) {
            throw new Error('Search failed');
        }
        return await response.json();
    },

    async listVersions(name: string): Promise<string[]> {
        const response = await fetch(`${API_BASE_URL}/packages/${name}/versions`);
        if (!response.ok) {
            throw new Error(`Package '${name}' not found`);
        }
        const data = await response.json();
        return data.versions;
    },

    async getAllPackages(): Promise<SearchResult[]> {
        try {
            const response = await fetch(`${API_BASE_URL}/packages`);
            if (!response.ok) {
                throw new Error('Failed to fetch packages');
            }
            return await response.json();
        } catch (error) {
            console.warn('API unavailable, using fallback data');
            return fallbackPackages.map(pkg => ({
                name: pkg.name,
                version: pkg.version,
                description: pkg.description,
                downloads: pkg.downloads || 0,
                keywords: pkg.keywords,
                license: pkg.license,
                updated_at: pkg.published_at
            }));
        }
    }
};

