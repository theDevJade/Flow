<script lang="ts">
  import { link } from 'svelte-spa-router';
  
  let { transparent = false } = $props();
</script>

<header class="header" class:transparent>
  <div class="container">
    <div class="header-content">
      <a href="/" use:link class="logo">
        <div class="logo-icon">~</div>
        <span class="logo-text">Flow</span>
      </a>
      
      <nav class="nav">
        <a href="/" use:link class="nav-link">Home</a>
        <a href="/registry" use:link class="nav-link">Registry</a>
        <a href="/install" use:link class="nav-link">Install</a>
        <a href="https://github.com/theDevJade/flow" target="_blank" rel="noopener" class="nav-link">Docs</a>
      </nav>
      
      <div class="header-actions">
        <a href="https://github.com/theDevJade/flow" target="_blank" rel="noopener" class="btn-secondary">
          GitHub
        </a>
        <a href="/install" use:link class="btn-primary">
          Get Started
        </a>
      </div>
    </div>
  </div>
</header>

<style>
  .header {
    position: sticky;
    top: 0;
    z-index: 100;
    backdrop-filter: blur(10px);
    background: rgba(10, 22, 40, 0.8);
    border-bottom: 1px solid var(--border-color);
    transition: all 0.3s ease;
  }
  
  .header.transparent {
    background: transparent;
    border-bottom: 1px solid transparent;
  }
  
  .header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 1rem 0;
    gap: 2rem;
  }
  
  .logo {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    text-decoration: none;
    transition: transform 0.3s ease;
  }
  
  .logo:hover {
    transform: scale(1.05);
  }
  
  .logo-icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
    background: linear-gradient(135deg, var(--ocean-cyan) 0%, var(--ocean-teal) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.5rem;
    font-weight: bold;
    color: var(--text-primary);
    box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
  }
  
  .logo-text {
    font-size: 1.5rem;
    font-weight: 700;
    background: linear-gradient(135deg, var(--ocean-cyan) 0%, var(--ocean-foam) 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .nav {
    display: flex;
    gap: 2rem;
    flex: 1;
    justify-content: center;
  }
  
  .nav-link {
    color: var(--text-secondary);
    text-decoration: none;
    font-weight: 500;
    transition: color 0.3s ease;
    position: relative;
  }
  
  .nav-link:hover {
    color: var(--accent-primary);
  }
  
  .nav-link::after {
    content: '';
    position: absolute;
    bottom: -4px;
    left: 0;
    width: 0;
    height: 2px;
    background: var(--accent-primary);
    transition: width 0.3s ease;
  }
  
  .nav-link:hover::after {
    width: 100%;
  }
  
  .header-actions {
    display: flex;
    gap: 1rem;
    align-items: center;
  }
  
  .btn-secondary, .btn-primary {
    padding: 0.5rem 1.25rem;
    border-radius: 8px;
    font-weight: 500;
    text-decoration: none;
    transition: all 0.3s ease;
    font-size: 0.95rem;
  }
  
  .btn-secondary {
    background: var(--bg-elevated);
    color: var(--text-primary);
    border: 1px solid var(--border-color);
  }
  
  .btn-secondary:hover {
    background: var(--ocean-medium);
    transform: translateY(-2px);
  }
  
  .btn-primary {
    background: linear-gradient(135deg, var(--ocean-blue) 0%, var(--ocean-cyan) 100%);
    color: var(--text-primary);
    box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
  }
  
  .btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(6, 182, 212, 0.5);
  }
  
  @media (max-width: 768px) {
    .nav {
      display: none;
    }
    
    .header-actions {
      gap: 0.5rem;
    }
    
    .btn-secondary, .btn-primary {
      padding: 0.4rem 1rem;
      font-size: 0.9rem;
    }
  }
</style>

