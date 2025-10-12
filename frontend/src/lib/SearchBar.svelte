<script lang="ts">
  let { 
    value = $bindable(''), 
    placeholder = 'Search packages...',
    onSearch 
  } = $props<{ 
    value?: string, 
    placeholder?: string,
    onSearch?: (query: string) => void 
  }>();
  
  function handleSubmit(e: Event) {
    e.preventDefault();
    onSearch?.(value);
  }
  
  function handleInput(e: Event) {
    const target = e.target as HTMLInputElement;
    value = target.value;
  }
</script>

<form class="search-bar" onsubmit={handleSubmit}>
  <div class="search-input-wrapper">
    <svg class="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <circle cx="11" cy="11" r="8"></circle>
      <path d="m21 21-4.35-4.35"></path>
    </svg>
    <input 
      type="text" 
      class="search-input" 
      {placeholder}
      value={value}
      oninput={handleInput}
    />
    {#if value}
      <button 
        type="button" 
        class="clear-btn"
        aria-label="Clear search"
        onclick={() => value = ''}
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    {/if}
  </div>
  <button type="submit" class="search-btn">
    Search
  </button>
</form>

<style>
  .search-bar {
    display: flex;
    gap: 0.75rem;
    width: 100%;
    max-width: 700px;
  }
  
  .search-input-wrapper {
    position: relative;
    flex: 1;
  }
  
  .search-icon {
    position: absolute;
    left: 1rem;
    top: 50%;
    transform: translateY(-50%);
    color: var(--text-muted);
    pointer-events: none;
  }
  
  .search-input {
    width: 100%;
    padding: 0.875rem 3rem 0.875rem 3rem;
    background: var(--bg-secondary);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    color: var(--text-primary);
    font-size: 1rem;
    transition: all 0.3s ease;
  }
  
  .search-input:focus {
    border-color: var(--accent-primary);
    box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.1);
  }
  
  .clear-btn {
    position: absolute;
    right: 1rem;
    top: 50%;
    transform: translateY(-50%);
    background: none;
    border: none;
    padding: 0.25rem;
    color: var(--text-muted);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: color 0.2s ease;
  }
  
  .clear-btn:hover {
    color: var(--text-primary);
  }
  
  .search-btn {
    padding: 0.875rem 2rem;
    white-space: nowrap;
  }
  
  @media (max-width: 640px) {
    .search-bar {
      flex-direction: column;
    }
    
    .search-btn {
      width: 100%;
    }
  }
</style>

