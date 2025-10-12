<script lang="ts">
  import Window from '../lib/Window.svelte';
  import Landing from '../pages/Landing.svelte';
  import Registry from '../pages/Registry.svelte';
  import DialupWindow from '../lib/DialupWindow.svelte';
  import InstallWindow from '../lib/InstallWindow.svelte';
  import EasterEggWindow from '../lib/EasterEggWindow.svelte';
  import CreditsWindow from '../lib/CreditsWindow.svelte';
  
  interface AppWindow {
    id: string;
    title: string;
    component?: any;
    content?: string;
    icon: string;
    isOpen: boolean;
    inDock: boolean;
    zIndex: number;
  }
  
  interface Props {
    onClose?: () => void;
  }
  
  let { onClose }: Props = $props();
  
  let maxZIndex = $state(100);
  
  let windows = $state<AppWindow[]>([
    { id: 'about', title: 'About Flow', component: Landing, icon: '~', isOpen: true, inDock: true, zIndex: 100 },
    { id: 'registry', title: 'Package Registry', component: Registry, icon: '📦', isOpen: false, inDock: true, zIndex: 100 },
    { id: 'dialup', title: 'Connecting...', icon: '📞', isOpen: false, inDock: false, zIndex: 100 },
    { id: 'install', title: 'Installation', icon: '💾', isOpen: false, inDock: false, zIndex: 100 },
    { id: 'credits', title: 'Credits', icon: '⭐', isOpen: false, inDock: false, zIndex: 100 },
  ]);
  
  let currentTime = $state(new Date().toLocaleTimeString('en-US', { 
    hour: '2-digit', 
    minute: '2-digit' 
  }));
  
  setInterval(() => {
    currentTime = new Date().toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }, 1000);
  
  // Listen for install window event
  if (typeof window !== 'undefined') {
    window.addEventListener('openInstallWindow', () => {
      openWindow('install');
    });
  }
  
  function openWindow(id: string) {
    const window = windows.find(w => w.id === id);
    if (window) {
      if (!window.isOpen) {
        window.isOpen = true;
      }
      bringToFront(id);
    }
  }
  
  function closeWindow(id: string) {
    const window = windows.find(w => w.id === id);
    if (window) {
      window.isOpen = false;
    }
  }
  
  function bringToFront(id: string) {
    maxZIndex++;
    const window = windows.find(w => w.id === id);
    if (window) {
      window.zIndex = maxZIndex;
    }
  }
  
  let showAboutMenu = $state(false);
  let showFileMenu = $state(false);
  let easterEggClicks = $state(0);
  
  function closeAllMenus() {
    showAboutMenu = false;
    showFileMenu = false;
  }
  
  function toggleMenu(menu: 'about' | 'file') {
    if (menu === 'about') {
      showFileMenu = false;
      showAboutMenu = !showAboutMenu;
    } else {
      showAboutMenu = false;
      showFileMenu = !showFileMenu;
    }
  }
  
  function handleAboutClick() {
    openWindow('credits');
    closeAllMenus();
  }
  
  function handleLogoClick() {
    easterEggClicks++;
    if (easterEggClicks >= 5) {
      // Easter egg unlocked!
      const easterEgg = windows.find(w => w.id === 'easteregg');
      if (!easterEgg) {
        windows = [...windows, { 
          id: 'easteregg', 
          title: 'Secret Message', 
          icon: '🎉', 
          isOpen: true, 
          inDock: false, 
          zIndex: maxZIndex + 1 
        }];
        maxZIndex++;
      } else {
        openWindow('easteregg');
      }
      easterEggClicks = 0;
    }
  }
</script>

<div class="desktop-container">
<!-- Menu Bar -->
<div class="menu-bar" onclick={closeAllMenus}>
  <div class="menu-left">
    <div class="menu-item apple-logo" onclick={(e) => { e.stopPropagation(); handleLogoClick(); }}>
      <span title="Click me 5 times... 🤫">~</span>
    </div>
    <div class="menu-item" onclick={(e) => { e.stopPropagation(); toggleMenu('about'); }}>
      Flow
      {#if showAboutMenu}
        <div class="dropdown-menu">
          <div class="menu-option" onclick={handleAboutClick}>About Flow...</div>
          <div class="menu-divider"></div>
          <div class="menu-option" onclick={() => { openWindow('install'); closeAllMenus(); }}>Installation</div>
          <div class="menu-option" onclick={() => { openWindow('dialup'); closeAllMenus(); }}>How It Works</div>
          <div class="menu-divider"></div>
          <div class="menu-option" onclick={() => { onClose?.(); closeAllMenus(); }}>Exit to Modern View</div>
        </div>
      {/if}
    </div>
    <div class="menu-item" onclick={(e) => { e.stopPropagation(); toggleMenu('file'); }}>
      File
      {#if showFileMenu}
        <div class="dropdown-menu">
          <div class="menu-option" onclick={() => { openWindow('registry'); closeAllMenus(); }}>Open Registry</div>
          <div class="menu-divider"></div>
          <div class="menu-option disabled">Save (⌘S)</div>
          <div class="menu-option disabled">Print...</div>
        </div>
      {/if}
    </div>
    <div class="menu-item disabled">Edit</div>
    <div class="menu-item disabled">View</div>
    <div class="menu-item disabled">Window</div>
    <div class="menu-item" onclick={(e) => { e.stopPropagation(); openWindow('credits'); }}>Help</div>
  </div>
  <div class="menu-right">
    <div class="menu-item">{currentTime}</div>
  </div>
</div>

<!-- Desktop -->
<div class="desktop">
  <!-- Desktop Icons -->
  <div class="desktop-icons">
    <!-- Organized main icons -->
    {#each windows.filter(w => w.inDock) as window, i}
      <button 
        class="desktop-icon"
        style="left: 20px; top: {20 + i * 100}px"
        ondblclick={() => openWindow(window.id)}
      >
        <div class="icon">{window.icon}</div>
        <div class="icon-label">{window.title}</div>
      </button>
    {/each}
    
    <!-- Messy scattered icons -->
    {#each windows.filter(w => !w.inDock && w.id !== 'easteregg') as window, i}
      <button 
        class="desktop-icon messy"
        style="left: {150 + i * 120 + (i * 30)}px; top: {80 + (i % 2) * 150 + (i * 20)}px"
        ondblclick={() => openWindow(window.id)}
      >
        <div class="icon">{window.icon}</div>
        <div class="icon-label">{window.title}</div>
      </button>
    {/each}
  </div>
  
  <!-- Windows -->
  {#each windows as window, i}
    {#if window.isOpen}
        <Window 
          title={window.title}
          initialX={100 + i * 40}
          initialY={70 + i * 40}
          width={window.id === 'registry' ? 900 : window.id === 'easteregg' ? 1000 : 800}
          height={window.id === 'dialup' || window.id === 'install' ? 500 : window.id === 'easteregg' ? 700 : 600}
          zIndex={window.zIndex}
          onClose={() => closeWindow(window.id)}
          onClick={() => bringToFront(window.id)}
        >
        {#if window.id === 'about'}
          <Landing />
        {:else if window.id === 'registry'}
          <Registry />
        {:else if window.id === 'dialup'}
          <DialupWindow />
        {:else if window.id === 'install'}
          <InstallWindow />
        {:else if window.id === 'credits'}
          <CreditsWindow />
        {:else if window.id === 'easteregg'}
          <EasterEggWindow />
        {/if}
      </Window>
    {/if}
  {/each}
</div>

<!-- Dock -->
<div class="dock-container">
  <div class="dock">
    {#each windows.filter(w => w.inDock) as window}
      <button 
        class="dock-icon"
        class:active={window.isOpen}
        onclick={() => openWindow(window.id)}
        title={window.title}
      >
        <div class="dock-icon-inner">{window.icon}</div>
      </button>
    {/each}
  </div>
</div>
</div>

<style>
  /* Menu Bar - Classic Mac Style */
  .menu-bar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    height: 32px;
    background: linear-gradient(180deg, #d0d0d0 0%, #a0a0a0 100%);
    border-bottom: 2px solid #606060;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 10px;
    z-index: 10000;
    font-size: 12px;
    font-family: 'Chicago', 'Geneva', 'Courier New', monospace;
    image-rendering: pixelated;
  }
  
  .menu-left, .menu-right {
    display: flex;
    align-items: center;
    gap: 20px;
  }
  
  .menu-item {
    color: #000;
    cursor: pointer;
    padding: 2px 8px;
    border-radius: 0;
    user-select: none;
    font-weight: bold;
    text-shadow: 1px 1px 0 rgba(255, 255, 255, 0.5);
    position: relative;
  }
  
  .menu-item:hover:not(.disabled) {
    background: #000;
    color: #fff;
    text-shadow: none;
  }
  
  .menu-item.disabled {
    opacity: 0.5;
    cursor: default;
  }
  
  .apple-logo {
    font-size: 18px;
    font-weight: bold;
  }
  
  .dropdown-menu {
    position: absolute;
    top: 100%;
    left: 0;
    background: #d0d0d0;
    border: 2px solid #000;
    box-shadow: 4px 4px 0 rgba(0, 0, 0, 0.5);
    min-width: 180px;
    z-index: 10001;
    margin-top: 2px;
  }
  
  .menu-option {
    padding: 6px 20px;
    cursor: pointer;
    color: #000;
    font-weight: normal;
  }
  
  .menu-option:hover:not(.disabled) {
    background: #000;
    color: #fff;
  }
  
  .menu-option.disabled {
    opacity: 0.5;
    cursor: default;
  }
  
  .menu-divider {
    height: 1px;
    background: #808080;
    margin: 2px 0;
  }
  
  /* Desktop */
  .desktop {
    position: fixed;
    top: 32px;
    left: 0;
    right: 0;
    bottom: 60px;
    background: 
      /* Ocean waves pattern */
      repeating-linear-gradient(
        0deg,
        rgba(6, 182, 212, 0.05) 0px,
        rgba(6, 182, 212, 0.1) 2px,
        transparent 2px,
        transparent 20px
      ),
      repeating-linear-gradient(
        45deg,
        rgba(20, 184, 166, 0.03) 0px,
        rgba(20, 184, 166, 0.06) 1px,
        transparent 1px,
        transparent 15px
      ),
      /* Ocean gradient */
      linear-gradient(
        180deg,
        #1a4d6d 0%,
        #0d3a52 30%,
        #084566 50%,
        #053a5c 70%,
        #022e4d 100%
      );
    overflow: hidden;
    image-rendering: pixelated;
  }
  
  /* Animated water effect */
  .desktop::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: 
      radial-gradient(ellipse at 30% 40%, rgba(100, 200, 255, 0.1) 0%, transparent 50%),
      radial-gradient(ellipse at 70% 60%, rgba(50, 150, 200, 0.08) 0%, transparent 50%);
    animation: waterShimmer 10s ease-in-out infinite;
    pointer-events: none;
  }
  
  @keyframes waterShimmer {
    0%, 100% { opacity: 0.3; }
    50% { opacity: 0.6; }
  }
  
  .desktop-icons {
    position: relative;
    width: 100%;
    height: 100%;
  }
  
  .desktop-icon {
    position: absolute;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    background: none;
    border: none;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    width: 100px;
  }
  
  .desktop-icon:hover {
    background: rgba(255, 255, 255, 0.1);
  }
  
  .desktop-icon.messy {
    transform: rotate(-2deg);
  }
  
  .desktop-icon.messy:nth-child(even) {
    transform: rotate(3deg);
  }
  
  .desktop-icon .icon {
    font-size: 42px;
    filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.5));
    image-rendering: pixelated;
  }
  
  .icon-label {
    color: #fff;
    font-size: 11px;
    text-align: center;
    text-shadow: 
      -1px -1px 0 #000,
      1px -1px 0 #000,
      -1px 1px 0 #000,
      1px 1px 0 #000;
    font-family: 'Chicago', 'Geneva', 'Courier New', monospace;
    font-weight: bold;
  }
  
  /* Dock - Classic Mac Style */
  .dock-container {
    position: fixed;
    bottom: 5px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 9999;
  }
  
  .dock {
    background: linear-gradient(180deg, #d0d0d0 0%, #a0a0a0 100%);
    border: 2px solid #606060;
    box-shadow: 
      inset 0 1px 0 rgba(255, 255, 255, 0.5),
      0 4px 12px rgba(0, 0, 0, 0.6);
    border-radius: 4px;
    padding: 4px;
    display: flex;
    gap: 4px;
  }
  
  .dock-icon {
    width: 48px;
    height: 48px;
    background: none;
    border: none;
    border-radius: 0;
    cursor: pointer;
    transition: all 0.1s ease;
    position: relative;
  }
  
  .dock-icon:hover {
    filter: brightness(1.2);
  }
  
  .dock-icon:active {
    filter: brightness(0.8);
  }
  
  .dock-icon.active::before {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 8px;
    height: 2px;
    background: #000;
  }
  
  .dock-icon-inner {
    width: 100%;
    height: 100%;
    background: linear-gradient(145deg, #e0e0e0 0%, #b0b0b0 50%, #909090 100%);
    border: 2px solid #000;
    box-shadow: 
      inset 1px 1px 2px rgba(255, 255, 255, 0.6),
      inset -1px -1px 2px rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    image-rendering: pixelated;
  }
  
  .dock-icon:hover .dock-icon-inner {
    background: linear-gradient(145deg, #f0f0f0 0%, #c0c0c0 50%, #a0a0a0 100%);
  }
</style>

