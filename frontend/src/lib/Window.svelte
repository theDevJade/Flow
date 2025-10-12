<script lang="ts">
  let {
    title = 'Window',
    initialX = 100,
    initialY = 100,
    width = 800,
    height = 600,
    zIndex = 100,
    onClose,
    onClick,
    children
  } = $props<{
    title?: string;
    initialX?: number;
    initialY?: number;
    width?: number;
    height?: number;
    zIndex?: number;
    onClose?: () => void;
    onClick?: () => void;
    children?: any;
  }>();
  
  let x = $state(initialX);
  let y = $state(initialY);
  let isDragging = $state(false);
  let isMaximized = $state(false);
  let dragStartX = 0;
  let dragStartY = 0;
  let windowEl: HTMLDivElement;
  
  let savedPosition = { x: initialX, y: initialY, width, height };
  
  function handleMouseDown(e: MouseEvent) {
    if ((e.target as HTMLElement).closest('.window-controls')) return;
    
    isDragging = true;
    dragStartX = e.clientX - x;
    dragStartY = e.clientY - y;
    
    // Bring window to front
    if (windowEl) {
      windowEl.style.zIndex = '1000';
    }
  }
  
  function handleMouseMove(e: MouseEvent) {
    if (!isDragging) return;
    
    x = e.clientX - dragStartX;
    y = e.clientY - dragStartY;
    
    // Keep window in bounds
    x = Math.max(0, Math.min(x, window.innerWidth - 200));
    y = Math.max(0, Math.min(y, window.innerHeight - 100));
  }
  
  function handleMouseUp() {
    isDragging = false;
  }
  
  function handleMinimize() {
    // In classic Mac style, minimize just closes
    onClose?.();
  }
  
  function handleMaximize() {
    if (!isMaximized) {
      savedPosition = { x, y, width, height };
      x = 0;
      y = 40; // Below menu bar
      width = window.innerWidth;
      height = window.innerHeight - 40 - 70; // Menu bar + dock
      isMaximized = true;
    } else {
      x = savedPosition.x;
      y = savedPosition.y;
      width = savedPosition.width;
      height = savedPosition.height;
      isMaximized = false;
    }
  }
  
  function handleClose() {
    onClose?.();
  }
  
  $effect(() => {
    const cleanup = () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };
    
    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
    }
    
    return cleanup;
  });
</script>

<svelte:window />

<div 
  bind:this={windowEl}
  class="window"
  class:dragging={isDragging}
  style="left: {x}px; top: {y}px; width: {width}px; height: {height}px; z-index: {zIndex}"
  onmousedown={onClick}
>
  <div class="window-titlebar" role="button" tabindex="0" onmousedown={handleMouseDown} onkeydown={(e) => e.key === 'Enter' && handleMouseDown(e as any)}>
    <div class="window-controls">
      <button class="window-btn close" onclick={handleClose} title="Close"></button>
      <button class="window-btn minimize" onclick={handleMinimize} title="Minimize"></button>
      <button class="window-btn maximize" onclick={handleMaximize} title="Maximize"></button>
    </div>
    <div class="window-title">{title}</div>
  </div>
  
  <div class="window-content" style="background: var(--bg-primary); color: var(--text-primary);">
    {@render children?.()}
  </div>
</div>

<style>
  .window {
    position: fixed;
    background: #c0c0c0;
    border-radius: 0;
    box-shadow: 
      0 0 0 2px #000,
      4px 4px 0 rgba(0, 0, 0, 0.6);
    overflow: hidden;
    transition: box-shadow 0.1s ease;
    z-index: 100;
    image-rendering: pixelated;
  }
  
  .window.dragging {
    box-shadow: 
      0 0 0 2px #000,
      6px 6px 0 rgba(0, 0, 0, 0.7);
    cursor: grabbing;
  }
  
  .window-titlebar {
    height: 28px;
    background: linear-gradient(180deg, #ffffff 0%, #e0e0e0 50%, #c0c0c0 100%);
    border-bottom: 2px solid #808080;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
    display: flex;
    align-items: center;
    padding: 0 8px;
    cursor: grab;
    user-select: none;
    position: relative;
  }
  
  .window-titlebar:active {
    cursor: grabbing;
  }
  
  /* Striped pattern for classic Mac titlebar */
  .window-titlebar::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: repeating-linear-gradient(
      45deg,
      transparent,
      transparent 2px,
      rgba(0, 0, 0, 0.03) 2px,
      rgba(0, 0, 0, 0.03) 4px
    );
    pointer-events: none;
  }
  
  .window-controls {
    display: flex;
    gap: 4px;
    margin-right: 10px;
    position: relative;
    z-index: 1;
  }
  
  .window-btn {
    width: 14px;
    height: 14px;
    border-radius: 0;
    border: 1px solid #000;
    cursor: pointer;
    transition: none;
    position: relative;
    padding: 0;
    flex-shrink: 0;
    background: #fff;
    box-shadow: 
      inset 1px 1px 0 rgba(255, 255, 255, 0.8),
      inset -1px -1px 0 rgba(0, 0, 0, 0.3);
  }
  
  .window-btn:active {
    box-shadow: 
      inset -1px -1px 0 rgba(255, 255, 255, 0.8),
      inset 1px 1px 0 rgba(0, 0, 0, 0.3);
  }
  
  .window-btn.close::after {
    content: '×';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: #000;
    font-size: 12px;
    font-weight: bold;
    line-height: 1;
  }
  
  .window-title {
    flex: 1;
    text-align: center;
    font-size: 12px;
    font-weight: bold;
    color: #000;
    font-family: 'Chicago', 'Geneva', 'Courier New', monospace;
    text-shadow: 1px 1px 0 rgba(255, 255, 255, 0.8);
    position: relative;
    z-index: 1;
  }
  
  .window-content {
    height: calc(100% - 28px);
    overflow: auto;
    background: #fff;
    position: relative;
  }
  
  /* Classic Mac scrollbar styling */
  .window-content::-webkit-scrollbar {
    width: 16px;
  }
  
  .window-content::-webkit-scrollbar-track {
    background: #e0e0e0;
    border-left: 1px solid #808080;
  }
  
  .window-content::-webkit-scrollbar-thumb {
    background: linear-gradient(90deg, #f0f0f0 0%, #d0d0d0 50%, #b0b0b0 100%);
    border: 1px solid #808080;
    box-shadow: inset 1px 1px 0 rgba(255, 255, 255, 0.5);
  }
  
  .window-content::-webkit-scrollbar-thumb:hover {
    background: linear-gradient(90deg, #e0e0e0 0%, #c0c0c0 50%, #a0a0a0 100%);
  }
  
  .window-content::-webkit-scrollbar-button {
    height: 16px;
    background: linear-gradient(90deg, #f0f0f0 0%, #d0d0d0 50%, #b0b0b0 100%);
    border: 1px solid #808080;
  }
</style>

