<script lang="ts">
  import type { SearchResult } from './mockApi';
  
  let { pkg } = $props<{ pkg: SearchResult }>();
  
  function formatDownloads(downloads: number): string {
    if (downloads >= 1000000) {
      return `${(downloads / 1000000).toFixed(1)}M`;
    }
    if (downloads >= 1000) {
      return `${(downloads / 1000).toFixed(1)}K`;
    }
    return downloads.toString();
  }
  
  function formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) return 'today';
    if (diffDays === 1) return 'yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`;
    if (diffDays < 365) return `${Math.floor(diffDays / 30)} months ago`;
    return `${Math.floor(diffDays / 365)} years ago`;
  }
</script>

<div class="package-card glass">
  <div class="package-main">
    <div class="package-info">
      <div class="package-title-row">
        <h3 class="package-name">{pkg.name}</h3>
        <span class="package-version">{pkg.version}</span>
      </div>
      <p class="package-description">{pkg.description}</p>
      {#if pkg.keywords && pkg.keywords.length > 0}
        <div class="package-keywords">
          {#each pkg.keywords.slice(0, 5) as keyword}
            <span class="keyword">{keyword}</span>
          {/each}
        </div>
      {/if}
    </div>
    
    <div class="package-meta">
      <div class="meta-item">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
          <polyline points="7 10 12 15 17 10"></polyline>
          <line x1="12" y1="15" x2="12" y2="3"></line>
        </svg>
        <span>{formatDownloads(pkg.downloads)}</span>
      </div>
      {#if pkg.updated_at}
        <div class="meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
          <span>{formatDate(pkg.updated_at)}</span>
        </div>
      {/if}
      {#if pkg.license}
        <div class="meta-item">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
            <polyline points="14 2 14 8 20 8"></polyline>
          </svg>
          <span>{pkg.license}</span>
        </div>
      {/if}
    </div>
  </div>
  
  <div class="package-actions">
    <button class="install-btn" title="Install via River" onclick={() => {
      window.dispatchEvent(new CustomEvent('openInstallWindow'));
    }}>
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
        <polyline points="7 10 12 15 17 10"></polyline>
        <line x1="12" y1="15" x2="12" y2="3"></line>
      </svg>
      Install
    </button>
  </div>
</div>

<style>
  .package-card {
    padding: 1.5rem;
    transition: all 0.3s ease;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 2rem;
  }
  
  .package-card:hover {
    transform: translateX(4px);
    box-shadow: 0 4px 16px rgba(6, 182, 212, 0.2);
    border-color: var(--accent-primary);
  }
  
  .package-main {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 1rem;
    min-width: 0;
  }
  
  .package-info {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
  
  .package-title-row {
    display: flex;
    align-items: baseline;
    gap: 1rem;
    flex-wrap: wrap;
  }
  
  .package-name {
    margin: 0;
    font-size: 1.35rem;
    color: var(--text-primary);
    font-weight: 600;
    font-family: 'Fira Code', monospace;
  }
  
  .package-version {
    padding: 0.15rem 0.6rem;
    background: rgba(6, 182, 212, 0.15);
    border: 1px solid var(--border-color);
    border-radius: 6px;
    font-size: 0.8rem;
    color: var(--ocean-foam);
    font-family: 'Fira Code', monospace;
    font-weight: 500;
  }
  
  .package-description {
    margin: 0;
    color: var(--text-secondary);
    font-size: 0.95rem;
    line-height: 1.6;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  
  .package-keywords {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }
  
  .keyword {
    padding: 0.2rem 0.6rem;
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    font-size: 0.75rem;
    color: var(--text-muted);
    font-weight: 500;
  }
  
  .package-meta {
    display: flex;
    gap: 1.5rem;
    flex-wrap: wrap;
  }
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    color: var(--text-muted);
    font-size: 0.85rem;
  }
  
  .meta-item svg {
    opacity: 0.6;
    flex-shrink: 0;
  }
  
  .package-actions {
    display: flex;
    align-items: center;
  }
  
  .install-btn {
    padding: 0.6rem 1.25rem;
    font-size: 0.9rem;
    background: transparent;
    border: 1px solid var(--accent-primary);
    color: var(--accent-primary);
    box-shadow: none;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    white-space: nowrap;
  }
  
  .install-btn svg {
    width: 16px;
    height: 16px;
  }
  
  .install-btn:hover {
    background: var(--accent-primary);
    color: var(--text-primary);
    transform: translateY(-2px);
  }
  
  @media (max-width: 768px) {
    .package-card {
      flex-direction: column;
      align-items: stretch;
      gap: 1rem;
    }
    
    .package-actions {
      width: 100%;
    }
    
    .install-btn {
      width: 100%;
      justify-content: center;
    }
    
    .package-meta {
      gap: 1rem;
    }
  }
</style>

