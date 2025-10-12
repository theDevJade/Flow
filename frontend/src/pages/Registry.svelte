<script lang="ts">
  import { onMount } from 'svelte';
  import SearchBar from '../lib/SearchBar.svelte';
  import PackageCard from '../lib/PackageCard.svelte';
  import { mockApi, type SearchResult } from '../lib/mockApi';
  
  let searchQuery = $state('');
  let packages = $state<SearchResult[]>([]);
  let filteredPackages = $state<SearchResult[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let sortBy = $state<'downloads' | 'name' | 'recent'>('downloads');
  
  onMount(async () => {
    await loadPackages();
  });
  
  async function loadPackages() {
    try {
      loading = true;
      error = null;
      packages = await mockApi.getAllPackages();
      filteredPackages = packages;
      console.log('Loaded packages:', packages.length, packages);
      sortPackages();
    } catch (e) {
      console.error('Error loading packages:', e);
      error = e instanceof Error ? e.message : 'Failed to load packages';
    } finally {
      loading = false;
    }
  }
  
  async function handleSearch(query: string) {
    searchQuery = query;
    
    if (!query.trim()) {
      filteredPackages = packages;
      sortPackages();
      return;
    }
    
    // Don't show loading for search, just filter
    const lowerQuery = query.toLowerCase();
    filteredPackages = packages.filter(pkg => 
      pkg.name.toLowerCase().includes(lowerQuery) ||
      pkg.description.toLowerCase().includes(lowerQuery) ||
      (pkg.keywords && pkg.keywords.some(k => k.toLowerCase().includes(lowerQuery)))
    );
    sortPackages();
  }
  
  function sortPackages() {
    filteredPackages = [...filteredPackages].sort((a, b) => {
      switch (sortBy) {
        case 'downloads':
          return b.downloads - a.downloads;
        case 'name':
          return a.name.localeCompare(b.name);
        case 'recent':
          return 0; // In real app, would sort by published_at
        default:
          return 0;
      }
    });
  }
  
  // Watch sortBy changes without causing infinite loop
  let lastSortBy = 'downloads';
  $effect(() => {
    if (sortBy !== lastSortBy) {
      lastSortBy = sortBy;
      sortPackages();
    }
  });
</script>

<div class="registry">
  
  <!-- Hero Section -->
  <section class="registry-hero">
    <div class="container">
      <div class="hero-content">
        <h1 class="hero-title">
          Package <span class="gradient-text">Registry</span>
        </h1>
        <p class="hero-subtitle">
          Discover and install packages to supercharge your Flow applications.
          All packages are verified and ready to use.
        </p>
        
        <div class="search-section">
          <SearchBar 
            bind:value={searchQuery}
            onSearch={handleSearch}
            placeholder="Search packages..."
          />
        </div>
        
        <div class="stats">
          <div class="stat-item glass animate-scale-in" style="animation-delay: 0.2s">
            <div class="stat-value">{packages.length}</div>
            <div class="stat-label">Total Packages</div>
          </div>
          <div class="stat-item glass animate-scale-in" style="animation-delay: 0.3s">
            <div class="stat-value">{Math.floor(packages.reduce((sum, p) => sum + p.downloads, 0) / 1000)}K</div>
            <div class="stat-label">Total Downloads</div>
          </div>
          <div class="stat-item glass animate-scale-in" style="animation-delay: 0.4s">
            <div class="stat-value">100%</div>
            <div class="stat-label">Verified</div>
          </div>
        </div>
      </div>
    </div>
  </section>
  
  <!-- Packages Section -->
  <section class="packages-section">
    <div class="container">
      <div class="packages-header">
        <h2>
          {#if searchQuery}
            Search Results for "{searchQuery}"
          {:else}
            All Packages
          {/if}
        </h2>
        
        <div class="controls">
          <label for="sort">Sort by:</label>
          <select id="sort" bind:value={sortBy} class="sort-select">
            <option value="downloads">Most Downloaded</option>
            <option value="name">Name</option>
            <option value="recent">Recently Updated</option>
          </select>
        </div>
      </div>
      
      {#if loading}
        <div class="loading">
          <div class="spinner"></div>
          <p>Loading packages...</p>
        </div>
      {:else if error}
        <div class="error glass">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <h3>Error</h3>
          <p>{error}</p>
          <button onclick={loadPackages}>Try Again</button>
        </div>
      {:else if filteredPackages.length === 0}
        <div class="empty glass">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="m21 21-4.35-4.35"></path>
          </svg>
          <h3>No packages found</h3>
          <p>Try a different search term or browse all packages</p>
          <button onclick={() => { searchQuery = ''; handleSearch(''); }}>
            View All Packages
          </button>
        </div>
      {:else}
        <div class="packages-grid">
          {#each filteredPackages as pkg, i (pkg.name)}
            <div class="package-wrapper" style="animation-delay: {i * 0.05}s">
              <PackageCard {pkg} />
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </section>
  
  <!-- Installation Guide -->
  <section class="install-guide">
    <div class="container">
      <div class="guide-content glass">
        <h2>How to Install Packages</h2>
        <div class="guide-steps">
          <div class="step">
            <div class="step-number">1</div>
            <div class="step-content">
              <h3>Install River</h3>
              <p>First, make sure you have the Flow package manager installed</p>
              <code>curl -sSf https://flowlang.org/install.sh | sh</code>
            </div>
          </div>
          <div class="step">
            <div class="step-number">2</div>
            <div class="step-content">
              <h3>Install Package</h3>
              <p>Use river to install any package from the registry</p>
              <code>river install &lt;package-name&gt;</code>
            </div>
          </div>
          <div class="step">
            <div class="step-number">3</div>
            <div class="step-content">
              <h3>Import and Use</h3>
              <p>Import the package in your Flow code</p>
              <code>import http from "http"</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</div>

<style>
  .registry {
    width: 100%;
    min-height: 100vh;
    background: var(--bg-primary);
  }
  
  /* Hero Section */
  .registry-hero {
    padding: 4rem 0 6rem;
    position: relative;
  }
  
  .hero-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2rem;
    text-align: center;
  }
  
  .hero-title {
    font-size: 3.5rem;
    margin: 0;
  }
  
  .gradient-text {
    background: linear-gradient(135deg, var(--ocean-cyan) 0%, var(--ocean-foam) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .hero-subtitle {
    font-size: 1.2rem;
    color: var(--text-secondary);
    max-width: 700px;
    margin: 0;
  }
  
  .search-section {
    width: 100%;
    display: flex;
    justify-content: center;
    margin: 2rem 0;
  }
  
  .stats {
    display: flex;
    gap: 2rem;
    margin-top: 2rem;
  }
  
  .stat-item {
    padding: 1.5rem 2.5rem;
    text-align: center;
    min-width: 150px;
    transition: transform 0.3s ease;
  }
  
  .stat-item:hover {
    transform: translateY(-5px);
  }
  
  .stat-value {
    font-size: 2.5rem;
    font-weight: 700;
    color: var(--accent-primary);
    margin-bottom: 0.5rem;
    animation: countUp 1s ease-out;
  }
  
  @keyframes countUp {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  .stat-label {
    font-size: 0.9rem;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  
  /* Packages Section */
  .packages-section {
    padding: 3rem 0;
  }
  
  .packages-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
    flex-wrap: wrap;
    gap: 1rem;
  }
  
  .packages-header h2 {
    margin: 0;
    font-size: 2rem;
  }
  
  .controls {
    display: flex;
    align-items: center;
    gap: 1rem;
  }
  
  .controls label {
    color: var(--text-secondary);
    font-size: 0.95rem;
  }
  
  .sort-select {
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 0.5rem 1rem;
    color: var(--text-primary);
    font-family: inherit;
    cursor: pointer;
    transition: all 0.3s ease;
  }
  
  .sort-select:focus {
    outline: none;
    border-color: var(--accent-primary);
  }
  
  .packages-grid {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  
  .package-wrapper {
    opacity: 0;
    animation: fadeInUp 0.5s ease-out forwards;
  }
  
  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translateY(20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
  
  /* Loading State */
  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 2rem;
    gap: 1rem;
  }
  
  .spinner {
    width: 50px;
    height: 50px;
    border: 3px solid var(--border-color);
    border-top-color: var(--accent-primary);
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
  
  .loading p {
    color: var(--text-muted);
  }
  
  /* Error State */
  .error {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4rem 2rem;
    gap: 1rem;
    text-align: center;
  }
  
  .error svg {
    color: #ef4444;
  }
  
  .error h3 {
    margin: 0;
    color: var(--text-primary);
  }
  
  .error p {
    margin: 0;
    color: var(--text-secondary);
  }
  
  /* Empty State */
  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 4rem 2rem;
    gap: 1rem;
    text-align: center;
  }
  
  .empty svg {
    color: var(--text-muted);
  }
  
  .empty h3 {
    margin: 0;
    color: var(--text-primary);
  }
  
  .empty p {
    margin: 0;
    color: var(--text-secondary);
  }
  
  /* Installation Guide */
  .install-guide {
    padding: 4rem 0;
  }
  
  .guide-content {
    padding: 3rem;
  }
  
  .guide-content h2 {
    text-align: center;
    margin: 0 0 3rem 0;
    font-size: 2.5rem;
  }
  
  .guide-steps {
    display: flex;
    flex-direction: column;
    gap: 2rem;
  }
  
  .step {
    display: flex;
    gap: 2rem;
    align-items: flex-start;
  }
  
  .step-number {
    flex-shrink: 0;
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--ocean-blue) 0%, var(--ocean-cyan) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--text-primary);
  }
  
  .step-content {
    flex: 1;
  }
  
  .step-content h3 {
    margin: 0 0 0.5rem 0;
    font-size: 1.5rem;
  }
  
  .step-content p {
    margin: 0 0 1rem 0;
    color: var(--text-secondary);
  }
  
  .step-content code {
    display: block;
    padding: 1rem;
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    font-size: 1rem;
  }
  
  @media (max-width: 768px) {
    .hero-title {
      font-size: 2.5rem;
    }
    
    .stats {
      flex-direction: column;
      width: 100%;
    }
    
    .stat-item {
      width: 100%;
    }
    
    .packages-header {
      flex-direction: column;
      align-items: flex-start;
    }
    
    .step {
      flex-direction: column;
      gap: 1rem;
    }
  }
</style>

