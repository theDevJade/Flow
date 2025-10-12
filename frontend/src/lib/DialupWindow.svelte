<script lang="ts">
  import { onMount } from 'svelte';
  
  let status = $state('Initializing...');
  let phoneNumber = $state('1-800-FLOW-COM');
  let connectionSpeed = $state(0);
  let elapsed = $state(0);
  
  const statuses = [
    'Initializing modem...',
    'Dialing: 1-800-FLOW-COM',
    '*BEEP* *BEEP* *BEEP*',
    'Carrier detected',
    'Handshaking...',
    '*SCREEEECH* *WARBLE*',
    'Negotiating protocols...',
    'Verifying username and password...',
    'Establishing connection...',
    'Connected at 56000 bps!',
    'Loading Flow compiler...',
    'Ready!'
  ];
  
  let statusIndex = $state(0);
  
  onMount(() => {
    const statusInterval = setInterval(() => {
      if (statusIndex < statuses.length - 1) {
        statusIndex++;
        status = statuses[statusIndex];
        if (statusIndex >= 9) {
          connectionSpeed = 56000;
        }
      }
    }, 1000);
    
    const timeInterval = setInterval(() => {
      elapsed++;
    }, 1000);
    
    return () => {
      clearInterval(statusInterval);
      clearInterval(timeInterval);
    };
  });
  
  function formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
</script>

<div class="dialup-window">
  <div class="window-section">
    <div class="section-header">Status</div>
    <div class="status-box">
      <div class="status-row">
        <span class="label">Status:</span>
        <span class="value">{status}</span>
      </div>
      <div class="status-row">
        <span class="label">Dialing:</span>
        <span class="value">{phoneNumber}</span>
      </div>
      <div class="status-row">
        <span class="label">Device:</span>
        <span class="value">Flow Modem 56K</span>
      </div>
      <div class="status-row">
        <span class="label">Speed:</span>
        <span class="value">{connectionSpeed > 0 ? `${connectionSpeed} bps` : 'Negotiating...'}</span>
      </div>
      <div class="status-row">
        <span class="label">Duration:</span>
        <span class="value">{formatTime(elapsed)}</span>
      </div>
    </div>
  </div>
  
  <div class="modem-lights">
    <div class="light-row">
      <div class="light" class:on={statusIndex >= 1}>
        <span class="light-label">CD</span>
      </div>
      <div class="light" class:on={statusIndex >= 3}>
        <span class="light-label">RX</span>
      </div>
      <div class="light" class:on={statusIndex >= 5}>
        <span class="light-label">TX</span>
      </div>
      <div class="light" class:on={statusIndex >= 9}>
        <span class="light-label">OH</span>
      </div>
    </div>
  </div>
  
  <div class="button-row">
    <button class="dialog-button" disabled={statusIndex < 9}>Details >></button>
    <button class="dialog-button">Cancel</button>
  </div>
</div>

<style>
  .dialup-window {
    padding: 20px;
    background: #c0c0c0;
    font-family: 'MS Sans Serif', Arial, sans-serif;
    font-size: 11px;
  }
  
  .window-section {
    margin-bottom: 15px;
  }
  
  .section-header {
    font-weight: bold;
    margin-bottom: 8px;
    color: #000;
  }
  
  .status-box {
    background: #fff;
    border: 2px inset #808080;
    padding: 12px;
  }
  
  .status-row {
    display: flex;
    margin: 6px 0;
    font-size: 11px;
  }
  
  .label {
    width: 80px;
    font-weight: bold;
    color: #000;
  }
  
  .value {
    color: #000;
    flex: 1;
  }
  
  .modem-lights {
    background: #fff;
    border: 2px inset #808080;
    padding: 15px;
    margin-bottom: 15px;
  }
  
  .light-row {
    display: flex;
    gap: 20px;
    justify-content: center;
  }
  
  .light {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
  }
  
  .light::before {
    content: '';
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #808080;
    border: 1px solid #000;
    box-shadow: inset -1px -1px 2px rgba(0,0,0,0.5);
  }
  
  .light.on::before {
    background: #00ff00;
    box-shadow: 
      0 0 8px #00ff00,
      inset -1px -1px 2px rgba(0,0,0,0.3);
  }
  
  .light-label {
    font-size: 9px;
    font-weight: bold;
    color: #000;
  }
  
  .button-row {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
  }
  
  .dialog-button {
    min-width: 80px;
    padding: 4px 12px;
    background: #c0c0c0;
    border: 2px outset #fff;
    border-top-color: #fff;
    border-left-color: #fff;
    border-right-color: #808080;
    border-bottom-color: #808080;
    color: #000;
    font-family: 'MS Sans Serif', Arial, sans-serif;
    font-size: 11px;
    cursor: pointer;
  }
  
  .dialog-button:active:not(:disabled) {
    border-style: inset;
    border-top-color: #808080;
    border-left-color: #808080;
    border-right-color: #fff;
    border-bottom-color: #fff;
  }
  
  .dialog-button:disabled {
    color: #808080;
    cursor: default;
  }
</style>

