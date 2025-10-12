<script lang="ts">
  import Router from 'svelte-spa-router';
  import Landing from './pages/Landing.svelte';
  import Registry from './pages/Registry.svelte';
  import Install from './pages/Install.svelte';
  import Desktop from './pages/Desktop.svelte';
  import Header from './lib/Header.svelte';
  
  let desktopMode = $state(false);
  
  const routes = {
    '/': Landing,
    '/registry': Registry,
    '/install': Install,
  };
  
  function toggleDesktop() {
    desktopMode = !desktopMode;
  }
</script>

{#if desktopMode}
  <Desktop onClose={() => desktopMode = false} />
{:else}
  <Header />
  <Router {routes} />
  
  <!-- Classic Mac Button -->
  <button class="mac-button" onclick={toggleDesktop} title="Open Classic Mac Desktop">
    <span class="mac-icon">🖥️</span>
    <span class="mac-label">Classic View</span>
  </button>
{/if}

<style>
  .mac-button {
    position: fixed;
    bottom: 20px;
    left: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    background: linear-gradient(145deg, #e6e6e6, #c0c0c0);
    border: 2px solid;
    border-top-color: #fff;
    border-left-color: #fff;
    border-right-color: #808080;
    border-bottom-color: #808080;
    padding: 12px 16px;
    cursor: pointer;
    font-family: 'Chicago', 'MS Sans Serif', Arial, sans-serif;
    box-shadow: 
      2px 2px 4px rgba(0, 0, 0, 0.3),
      inset -1px -1px 0 rgba(0, 0, 0, 0.1);
    transition: all 0.1s ease;
    z-index: 9999;
  }
  
  .mac-button:hover {
    background: linear-gradient(145deg, #f0f0f0, #d0d0d0);
  }
  
  .mac-button:active {
    border-style: solid;
    border-top-color: #808080;
    border-left-color: #808080;
    border-right-color: #fff;
    border-bottom-color: #fff;
    box-shadow: inset 1px 1px 2px rgba(0, 0, 0, 0.3);
    transform: translateY(1px);
  }
  
  .mac-icon {
    font-size: 24px;
    filter: grayscale(0.2);
  }
  
  .mac-label {
    font-size: 10px;
    font-weight: bold;
    color: #000;
    text-align: center;
    white-space: nowrap;
  }
</style>
