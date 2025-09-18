import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://thedevjade.github.io',
  base: '/Flow',
  integrations: [
    starlight({
      title: 'Flow Documentation',
      description: 'Documentation for FlowLang scripting language and Flow frontend/backend application',
      defaultLocale: 'root',
      social: [
        {
          label: 'GitHub',
          href: 'https://github.com/theDevJade/Flow',
          icon: 'github',
        },
      ],
      sidebar: [
        {
          label: 'FlowLang',
          items: [
            { label: 'Introduction', link: '/flowlang-introduction' },
            { label: 'Tutorial', link: '/flowlang-tutorial' },
            { label: 'Language Reference', link: '/flowlang-language-reference' },
            { label: 'API Reference', link: '/flowlang-api-reference' },
            { label: 'Developer Guide', link: '/flowlang-developer-guide' },
            { label: 'Examples', link: '/flowlang-examples' },
            { label: 'Changelog', link: '/flowlang-changelog' },
          ],
        },
        {
          label: 'Flow Frontend',
          items: [
            { label: 'Introduction', link: '/frontend-introduction' },
            { label: 'Features', link: '/frontend-features' },
            { label: 'Architecture', link: '/frontend-architecture' },
            { label: 'Getting Started', link: '/frontend-getting-started' },
            { label: 'Development', link: '/frontend-development' },
          ],
        },
        {
          label: 'Backend WebServer',
          items: [
            { label: 'Backend WebServer', link: '/backend-webserver' },
            { label: 'WebSocket API Reference', link: '/websocket-api-reference' },
            { label: 'Developer Integration Guide', link: '/developer-integration-guide' },
          ],
        },
        {
          label: 'Flow Extension System',
          items: [
            { label: 'Introduction', link: '/extension-system-introduction' },
            { label: 'Quick Start', link: '/extension-system-quick-start' },
            { label: 'Developer Guide', link: '/extension-system-developer-guide' },
            { label: 'API Reference', link: '/extension-system-api-reference' },
            { label: 'Documentation Index', link: '/extension-system-documentation-index' },
          ],
        },
        {
          label: 'Legal',
          items: [
            { label: 'License', link: '/license' },
          ],
        },
      ],
    }),
  ],
});
