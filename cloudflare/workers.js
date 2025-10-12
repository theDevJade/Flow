export default {
    async fetch(request) {
        const url = new URL(request.url);
        const path = url.pathname.toLowerCase().replace(/\/+$/, '');
        const userAgent = request.headers.get('user-agent') || '';

        const isPowerShell =
            path === '/init.ps1' ||
            path.endsWith('.ps1') ||
            /powershell|windows/i.test(userAgent);

        const targetUrl = isPowerShell
            ? 'https://raw.githubusercontent.com/theDevJade/Flow/main/lake/lake-init.ps1'
            : 'https://raw.githubusercontent.com/theDevJade/Flow/main/lake/lake-init';

        const response = await fetch(targetUrl);

        return new Response(await response.text(), {
            status: response.status,
            headers: {
                'Content-Type': 'text/plain; charset=utf-8',
                'Cache-Control': 'no-store',
            }
        });
    }
};
