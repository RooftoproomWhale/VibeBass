// Run with: node webApp/src/webMain/tests/pdf-browser-check.cjs
// Serves only the listed local PDFs and test resources on loopback. API records exist in memory only.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const samples = require('./pdf-samples.json');
const directory = process.env.VIBEBASS_PDF_SAMPLE_DIR || path.join(os.homedir(), 'Downloads');
const files = new Map(samples.map((name, index) => ['/sample/' + index, path.join(directory, name)]));
for (const file of files.values()) fs.accessSync(file, fs.constants.R_OK);
for (const name of ['styles.css', 'practice-api.js', 'practice-media.js']) files.set('/' + name, path.join(__dirname, '../resources', name));
let songs = [];

async function runPdfChecks() {
    const results = [];
    const names = await (await fetch('/samples')).json();
    for (const [index, name] of names.entries()) {
        const blob = await (await fetch('/sample/' + index)).blob();
        const width = Math.min(innerWidth - 32, 740);
        const height = innerHeight - 80;
        updatePdfPosition(16, 64, width, height);
        let selection;
        const loaded = new Promise((resolve, reject) => {
            onPdfFileSelected = (selectedName, url) => {
                selection = selectedName;
                initPdfViewer(url, false).then(resolve, reject);
            };
        });
        // Exercise the production file-change handler without opening an OS chooser.
        const click = HTMLInputElement.prototype.click;
        try { HTMLInputElement.prototype.click = function () {}; triggerPdfUpload(); }
        finally { HTMLInputElement.prototype.click = click; }
        const input = document.querySelector('input[type=file]');
        const transfer = new DataTransfer();
        transfer.items.add(new File([blob], name, { type: 'application/pdf' }));
        input.files = transfer.files;
        input.dispatchEvent(new Event('change'));
        await loaded;
        const pages = document.getElementById('pdf-pages');
        const canvases = [...pages.querySelectorAll('canvas')];
        if (selection !== name || !canvases.length || !canvases.every(canvas => {
            const pixels = canvas.getContext('2d').getImageData(0, 0, canvas.width, canvas.height).data;
            return pixels.some((value, offset) => offset % 4 !== 3 && value < 240);
        })) throw new Error('PDF render failed: ' + name);
        let position;
        onPdfScroll = (pixel, pagePosition) => { position = { pixel, pagePosition }; };
        scrollToPdfPixel(0, canvases.length - 0.25);
        const before = pages.scrollTop;
        updatePdfPosition(16, 64, Math.floor(width / 2), height);
        updatePdfPosition(16, 64, width, height);
        if (Math.abs(pages.scrollTop - before) > 1 || Math.abs(position.pagePosition - (canvases.length - 0.25)) > 0.003) {
            throw new Error('PDF resize position failed: ' + name);
        }
        const title = name + ' "연습"\\\n🎸';
        await vibeBassApi.saveSong(title, '가수\t이름', 'dQw4w9WgXcQ', JSON.stringify([
            { timeSec: 10, scrollPixel: position.pixel, pagePosition: position.pagePosition }
        ]));
        const saved = (await vibeBassApi.loadSongs()).at(-1);
        if (saved.title !== title || saved.anchorPoints[0].pagePosition !== position.pagePosition) throw new Error('JSON round trip failed');
        results.push({ name, pages: canvases.length, bytes: blob.size, pagePosition: position.pagePosition, passed: true });
    }
    document.getElementById('result').textContent = JSON.stringify(results, null, 2);
    return results;
}

const server = http.createServer(async (request, response) => {
    const pathname = new URL(request.url, 'http://127.0.0.1').pathname;
    const json = (value, status = 200) => { response.writeHead(status, { 'Content-Type': 'application/json' }); response.end(JSON.stringify(value)); };
    if (request.headers.origin && request.headers.origin !== 'http://' + request.headers.host) return json({}, 403);
    if (pathname === '/api/songs' && request.method === 'POST') {
        const chunks = [];
        let size = 0;
        for await (const chunk of request) { size += chunk.length; if (size > 262144) return json({}, 413); chunks.push(chunk); }
        try { const song = { ...JSON.parse(Buffer.concat(chunks).toString()), id: songs.length + 1 }; songs.push(song); return json(song, 201); }
        catch (_) { return json({}, 400); }
    }
    if (pathname === '/api/songs') return json(songs);
    if (pathname === '/api/youtube/search') return json({ code: 'YOUTUBE_SEARCH_DISABLED' }, 503);
    if (pathname === '/samples') return json(samples);
    if (pathname === '/') {
        response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        return response.end('<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">' +
            '<title>VibeBass PDF bridge check</title><link rel="stylesheet" href="/styles.css">' +
            '<script src="/practice-api.js"></script><script src="/practice-media.js"></script>' +
            '<button onclick="runPdfChecks().catch(error => document.getElementById(\'result\').textContent = error.message)">PDF 9개 검사</button>' +
            '<pre id="result">PDF 브리지·임시 API 검사. Compose·Spring·DB 통합 검증은 별도입니다.</pre>' +
            '<script>' + runPdfChecks.toString() + '</script></html>');
    }
    if (files.has(pathname)) {
        response.writeHead(200, { 'Content-Type': pathname.endsWith('.js') ? 'text/javascript' : pathname.endsWith('.css') ? 'text/css' : 'application/pdf' });
        return fs.createReadStream(files.get(pathname)).pipe(response);
    }
    json({}, 404);
});
server.listen(0, '127.0.0.1', () => console.log('PDF check: http://127.0.0.1:' + server.address().port));
