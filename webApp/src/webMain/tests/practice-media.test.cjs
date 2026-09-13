// Run with: node --experimental-vm-modules --test webApp/src/webMain/tests/practice-media.test.cjs
// This exercises the real browser bridge with controlled DOM/PDF/network timing.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../resources/practice-media.js'), 'utf8');
const apiSource = fs.readFileSync(path.join(__dirname, '../resources/practice-api.js'), 'utf8');
const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
    return { promise, resolve, reject };
};
const flush = () => new Promise(done => setImmediate(done));

function browser({ moduleReady = Promise.resolve(), roundScroll = false } = {}) {
    class Element {
        constructor(tagName) {
            this.tagName = tagName.toUpperCase();
            this.style = {};
            this.dataset = {};
            this.attributes = {};
            this.children = [];
            this.listeners = {};
            this.scrollTop = 0;
            this.clientWidth = 800;
            this.clientHeight = 600;
            this.classList = { add() {}, remove() {}, toggle() {} };
        }
        appendChild(child) {
            child.remove();
            child.parentNode = this;
            this.children.push(child);
            return child;
        }
        append(...children) { children.forEach(child => this.appendChild(child)); }
        replaceChildren(...children) {
            this.children.forEach(child => { child.parentNode = null; });
            this.children = [];
            this.append(...children);
        }
        remove() {
            if (this.parentNode) {
                this.parentNode.children = this.parentNode.children.filter(child => child !== this);
                this.parentNode = null;
            }
        }
        setAttribute(name, value) { this.attributes[name] = String(value); }
        getAttribute(name) { return this.attributes[name] ?? null; }
        addEventListener(name, callback) { this.listeners[name] = callback; }
        removeEventListener(name) { delete this.listeners[name]; }
        dispatchEvent(event) { return this.listeners[event.type]?.(event); }
        click() {}
        getContext() { return { canvas: this }; }
        get offsetHeight() {
            return this.tagName === 'CANVAS' && this.width
                ? Math.max(0, this.parentNode.clientWidth - 2 * (this.parentNode.padding || 0)) * this.height / this.width
                : this.clientHeight;
        }
        get offsetTop() {
            if (this.tagName !== 'CANVAS') return 0;
            const siblings = this.parentNode.children.filter(child => child.tagName === 'CANVAS');
            return (this.parentNode.padding || 0) + siblings.slice(0, siblings.indexOf(this))
                .reduce((top, child) => top + child.offsetHeight + (this.parentNode.gap || 0), 0);
        }
        scrollTo(options) { this.lastScroll = options; this.scrollTop = roundScroll ? Math.round(options.top) : options.top; }
        getBoundingClientRect() { return { left: 0, top: 0, width: this.clientWidth, height: this.clientHeight }; }
        querySelectorAll(selector) { return descendants(this).filter(node => node.tagName === selector.toUpperCase()); }
        set textContent(value) { this.replaceChildren(); this.text = String(value); }
        get textContent() { return this.text ?? ''; }
        set innerHTML(value) { this.replaceChildren(); this.text = String(value); }
        get isConnected() { return this === body || Boolean(this.parentNode?.isConnected); }
    }
    const descendants = element => element.children.flatMap(child => [child, ...descendants(child)]);
    const body = new Element('body');
    const head = new Element('head');
    const documentRequests = new Map();
    const imports = [];
    const searches = [];
    const intervals = new Map();
    const resizeCallbacks = [];
    let nextInterval = 0;
    const context = {
        console,
        Promise,
        URL: { createObjectURL: file => 'blob:' + file.name, revokeObjectURL() {} },
        AbortController,
        devicePixelRatio: 2,
        innerWidth: 1440,
        innerHeight: 900,
        location: { hostname: 'localhost', origin: 'http://localhost:8082', protocol: 'http:' },
        document: {
            body,
            head,
            createElement: tag => new Element(tag),
            getElementById: id => [body, head, ...descendants(body), ...descendants(head)].find(node => node.id === id) ?? null,
        },
        setTimeout,
        clearTimeout,
        getComputedStyle: element => ({ paddingTop: String(element.padding || 0) }),
        ResizeObserver: class {
            constructor(callback) { resizeCallbacks.push(callback); }
            observe() {}
            disconnect() {}
        },
        setInterval: callback => { const id = ++nextInterval; intervals.set(id, callback); return id; },
        clearInterval: id => intervals.delete(id),
        requestAnimationFrame: callback => { callback(); return 1; },
        cancelAnimationFrame() {},
        events: {},
        addEventListener(name, callback) { this.events[name] = callback; },
        removeEventListener(name) { delete this.events[name]; },
        matchMedia: () => ({ matches: false }),
        fetch: (url, options) => {
            const response = deferred();
            searches.push({ url, options, ...response });
            // Intentionally ignore abort: stale responses still need generation guards.
            return response.promise;
        },
        pdfjsLib: {
            GlobalWorkerOptions: {},
            getDocument: options => {
                const request = deferred();
                const task = { ...request, options, destroy: async () => { task.destroyed = true; } };
                documentRequests.set(options.url, task);
                return task;
            },
        },
    };
    context.window = context;
    vm.runInNewContext(apiSource, context, { filename: 'practice-api.js' });
    const pdfModule = new vm.SyntheticModule(['getDocument', 'GlobalWorkerOptions'], function () {
        this.setExport('getDocument', context.pdfjsLib.getDocument);
        this.setExport('GlobalWorkerOptions', context.pdfjsLib.GlobalWorkerOptions);
    });
    vm.runInNewContext(source, context, {
        filename: 'practice-media.js',
        importModuleDynamically: async specifier => {
            imports.push(specifier);
            await moduleReady;
            if (pdfModule.status === 'unlinked') await pdfModule.link(() => {});
            await pdfModule.evaluate();
            return pdfModule;
        },
    });
    return {
        window: context,
        documents: documentRequests,
        imports,
        searches,
        intervals,
        element: id => context.document.getElementById(id),
        canvases: () => context.document.getElementById('pdf-pages')?.querySelectorAll('canvas') ?? [],
        layoutPdf: ({ width, height = 160, padding = 0, gap = 0 }) => {
            const pages = context.document.getElementById('pdf-pages');
            Object.assign(pages, { clientWidth: width, clientHeight: height, padding, gap });
            resizeCallbacks.forEach(callback => callback());
        },
    };
}

function page(label, title = '') {
    return {
        getViewport: ({ scale }) => ({ width: 600 * scale, height: 800 * scale }),
        getTextContent: async () => ({ items: title ? [{ str: title, transform: [24, 0, 0, 24, 0, 0] }] : [] }),
        render: ({ canvasContext }) => {
            canvasContext.canvas.pageLabel = label;
            return { promise: Promise.resolve(), cancel() {} };
        },
    };
}
const pdf = pages => ({ numPages: pages.length, getPage: number => Promise.resolve(pages[number - 1]) });

test('PDF keeps page order when later page data arrives first', async () => {
    const app = browser();
    const first = deferred();
    const second = deferred();
    const loaded = app.window.initPdfViewer('blob:ordered', false);
    await flush();
    app.documents.get('blob:ordered').resolve(pdf([first.promise, second.promise]));
    second.resolve(page(2));
    await flush();
    first.resolve(page(1));
    await loaded;
    assert.deepEqual(app.canvases().map(canvas => canvas.pageLabel), [1, 2]);
});

test('selecting another PDF invalidates old video results before the next viewer effect', async () => {
    const app = browser();
    const videos = [];
    app.window.onYoutubeVideoIdFound = value => videos.push(value);
    const loaded = app.window.initPdfViewer('blob:previous');
    await flush();
    app.documents.get('blob:previous').resolve(pdf([page('previous', 'Previous song')]));
    await loaded;
    await flush();
    assert.equal(app.searches.length, 1);
    let selected;
    app.window.onPdfFileSelected = (name, url) => {
        selected = { name, url };
        app.searches[0].resolve({ ok: true, json: async () => ({ videoId: 'previous001' }) });
    };
    app.window.triggerPdfUpload();
    const input = app.window.document.body.querySelectorAll('input')[0];
    input.files = [{ name: 'next.pdf', type: 'application/pdf', slice: () => ({ text: async () => '%PDF-1.7' }) }];
    await input.dispatchEvent({ type: 'change' });
    await flush();
    assert.deepEqual(selected, { name: 'next.pdf', url: 'blob:next.pdf' });
    assert.equal(app.documents.has(selected.url), false, 'The new Compose viewer effect has not run yet');
    assert.deepEqual(videos, []);
});

test('YouTube uses the latest pending video, preserves playback while hidden, and stops when cleared', async () => {
    const app = browser();
    const players = [];
    const times = [];
    const states = [];
    app.window.onYoutubeTimeUpdate = value => times.push(value);
    app.window.onYoutubeStateChange = value => states.push(value);
    const first = app.window.initYoutubePlayer('first-video');
    const latest = app.window.initYoutubePlayer('latest-video');
    app.window.YT = {
        Player: class {
            constructor(_, options) {
                this.videoId = options.videoId;
                this.events = options.events;
                this.cues = [];
                this.stops = 0;
                players.push(this);
            }
            getVideoData() { return { video_id: this.videoId }; }
            cueVideoById(value) { this.cues.push(value); this.videoId = value; }
            stopVideo() { this.stops++; }
            getIframe() { return app.window.document.createElement('iframe'); }
            getCurrentTime() { return 17.25; }
        },
    };
    app.window.onYouTubeIframeAPIReady();
    await Promise.all([first, latest]);
    assert.equal(players.length, 1);
    const player = players[0];
    assert.equal(player.videoId, 'latest-video');
    player.events.onReady({ target: player });
    player.events.onStateChange({ data: 1 });
    await app.window.initYoutubePlayer('latest-video');
    assert.deepEqual(player.cues, []);
    app.window.updateYoutubePosition(20, 80, 320, 180);
    app.window.hideYoutubePlayer();
    assert.equal(app.element('youtube-player-container').hidden, true);
    assert.equal(app.intervals.size, 1);
    app.intervals.forEach(callback => callback());
    assert.deepEqual(times, [17.25]);
    assert.deepEqual(states, [true]);
    assert.equal(player.stops, 0);
    await app.window.initYoutubePlayer('');
    assert.equal(player.stops, 1);
    assert.equal(times.at(-1), 0);
    assert.equal(states.at(-1), false);
});

test('switching PDF rejects stale document loads, pages, and video results', async () => {
    const app = browser();
    const videos = [];
    app.window.onYoutubeVideoIdFound = value => videos.push(value);

    const delayedLoad = app.window.initPdfViewer('blob:delayed', false);
    await flush();
    const oldPage = deferred();
    const oldLoad = app.window.initPdfViewer('blob:old', false);
    await flush();
    app.documents.get('blob:old').resolve(pdf([oldPage.promise]));
    await flush();
    const currentLoad = app.window.initPdfViewer('blob:current', false);
    await flush();
    assert.equal(app.documents.get('blob:delayed').destroyed, true);
    assert.equal(app.documents.get('blob:old').destroyed, true);
    app.documents.get('blob:current').resolve(pdf([page('current')]));
    await currentLoad;
    app.documents.get('blob:delayed').resolve(pdf([page('delayed')]));
    oldPage.resolve(page('old'));
    await Promise.all([delayedLoad, oldLoad]);
    assert.deepEqual(app.canvases().map(canvas => canvas.pageLabel), ['current']);

    const firstSearch = app.window.initPdfViewer('blob:first-search');
    await flush();
    app.documents.get('blob:first-search').resolve(pdf([page('first-search', 'First song')]));
    await flush();
    const secondSearch = app.window.initPdfViewer('blob:second-search');
    await flush();
    app.documents.get('blob:second-search').resolve(pdf([page('second-search', 'Second song')]));
    await flush();
    assert.equal(app.searches.length, 2);
    app.searches[1].resolve({ ok: true, json: async () => ({ videoId: 'latest00001' }) });
    await flush();
    app.searches[0].resolve({ ok: true, json: async () => ({ videoId: 'stale000001' }) });
    await Promise.all([firstSearch, secondSearch]);
    await flush();
    assert.deepEqual(videos, ['latest00001']);
    assert.deepEqual(app.canvases().map(canvas => canvas.pageLabel), ['second-search']);
});

test('PDF scroll uses raw coordinates and overlays use supplied CSS bounds at DPR 2', async () => {
    const app = browser();
    const loaded = app.window.initPdfViewer('blob:coordinates', false);
    await flush();
    app.documents.get('blob:coordinates').resolve(pdf([page(1)]));
    await loaded;
    app.window.updatePdfPosition(400, 100, 800, 600);
    const host = app.element('pdf-viewer-container');
    assert.equal(host.style.left, '400px');
    assert.equal(host.style.top, '100px');
    assert.equal(host.style.width, '800px');
    assert.equal(host.style.height, '600px');
    app.window.updatePdfPosition(400, 100, 800, 600, 420, 120, 1150, 650);
    assert.equal(host.style.clipPath, 'inset(20px 50px 50px 20px)');
    assert.equal(host.hidden, false);
    app.window.hidePdfViewer();
    assert.equal(host.hidden, true);
    app.window.updatePdfPosition(400, 100, 800, 600);
    assert.equal(host.hidden, false);
    assert.deepEqual(app.canvases().map(canvas => canvas.pageLabel), [1]);
    app.window.scrollToPdfPixel(500);
    assert.equal(app.element('pdf-pages').scrollTop, 500);
    assert.equal(app.element('pdf-pages').lastScroll.behavior, 'auto');
    app.window.scrollToPdfPixel(-20);
    assert.equal(app.element('pdf-pages').scrollTop, 0);
    app.window.scrollToPdfPixel(NaN);
    assert.equal(app.element('pdf-pages').scrollTop, 0);
    const positions = [];
    app.window.onPdfScroll = value => positions.push(value);
    app.element('pdf-pages').scrollTop = 725;
    app.element('pdf-pages').dispatchEvent({ type: 'scroll' });
    assert.deepEqual(positions, [725]);
});

test('PDF engine and assets use one patched version and disable legacy eval', async () => {
    const app = browser();
    const loaded = app.window.initPdfViewer('blob:secure', false);
    await flush();
    const base = 'https://cdn.jsdelivr.net/npm/pdfjs-dist@6.3.289/';
    assert.deepEqual(app.imports, [base + 'build/pdf.min.mjs']);
    assert.equal(app.window.pdfjsLib.GlobalWorkerOptions.workerSrc, base + 'build/pdf.worker.min.mjs');
    const task = app.documents.get('blob:secure');
    assert.equal(task.options.isEvalSupported, false);
    assert.equal(task.options.cMapPacked, true);
    assert.equal(task.options.cMapUrl, base + 'cmaps/');
    assert.equal(task.options.iccUrl, base + 'iccs/');
    assert.equal(task.options.standardFontDataUrl, base + 'standard_fonts/');
    assert.equal(task.options.wasmUrl, base + 'wasm/');
    task.resolve(pdf([page(1)]));
    await loaded;
});

test('switching or clearing a PDF during module loading never opens the stale file', async () => {
    const ready = deferred();
    const app = browser({ moduleReady: ready.promise });
    const old = app.window.initPdfViewer('blob:old', false);
    const latest = app.window.initPdfViewer('blob:latest', false);
    ready.resolve();
    await flush();
    assert.equal(app.imports.length, 1);
    assert.deepEqual([...app.documents.keys()], ['blob:latest']);
    app.documents.get('blob:latest').resolve(pdf([page('latest')]));
    await Promise.all([old, latest]);
    assert.deepEqual(app.canvases().map(canvas => canvas.pageLabel), ['latest']);

    const pending = deferred();
    const cleared = browser({ moduleReady: pending.promise });
    const loading = cleared.window.initPdfViewer('blob:cleared', false);
    await cleared.window.initPdfViewer('');
    pending.resolve();
    await loading;
    assert.equal(cleared.documents.size, 0);
});

test('module loading failure shows an error and allows another import attempt', async () => {
    const ready = deferred();
    const app = browser({ moduleReady: ready.promise });
    const loaded = app.window.initPdfViewer('blob:failed', false);
    ready.reject(new Error('CDN unavailable'));
    await loaded;
    const message = app.element('pdf-pages').children[0];
    assert.equal(message.getAttribute('role'), 'alert');
    assert.equal(app.documents.size, 0);
    await app.window.initPdfViewer('blob:failed', false);
    assert.equal(app.imports.length, 2);
});

test('recorded page position survives resize, hidden panes and playback at another width', async () => {
    const app = browser({ roundScroll: true });
    const loaded = app.window.initPdfViewer('blob:responsive', false);
    app.layoutPdf({ width: 648, padding: 24, gap: 24 });
    await flush();
    app.documents.get('blob:responsive').resolve(pdf([page(1), page(2)]));
    await loaded;
    let recorded;
    app.window.onPdfScroll = (pixel, pagePosition) => { recorded = { pixel, pagePosition }; };
    const pages = app.element('pdf-pages');
    pages.scrollTop = 1024; // Second page, 25% from its top at the viewer's content inset.
    pages.dispatchEvent({ type: 'scroll' });
    assert.deepEqual(recorded, { pixel: 1024, pagePosition: 1.25 });
    app.layoutPdf({ width: 324, padding: 12, gap: 12 });
    assert.equal(pages.scrollTop, 512);
    app.layoutPdf({ width: 0, height: 0 });
    app.layoutPdf({ width: 648, padding: 24, gap: 24 });
    assert.equal(pages.scrollTop, 1024);
    for (let count = 0; count < 5; count++) {
        app.layoutPdf({ width: 325, padding: 12, gap: 12 });
        app.layoutPdf({ width: 648, padding: 24, gap: 24 });
    }
    assert.equal(pages.scrollTop, 1024, 'Rounding must not accumulate across repeated resizes');
    app.window.scrollToPdfPixel(recorded.pixel, recorded.pagePosition);
    app.layoutPdf({ width: 324, padding: 12, gap: 12 });
    assert.equal(pages.scrollTop, 512);
});

test('paused playback target waits for its page and can be replaced or cancelled', async () => {
    const app = browser();
    const second = deferred();
    const loaded = app.window.initPdfViewer('blob:pending-position', false);
    app.layoutPdf({ width: 648, padding: 24, gap: 24 });
    app.window.scrollToPdfPixel(1024, 1.25);
    await flush();
    app.documents.get('blob:pending-position').resolve(pdf([page(1), second.promise]));
    await flush();
    assert.equal(app.canvases().length, 1);
    assert.equal(app.element('pdf-pages').scrollTop, 0, 'Do not apply a target to an incomplete page stack');
    app.window.scrollToPdfPixel(1224, 1.5); // Edited anchors while the video remains paused.
    second.resolve(page(2));
    await loaded;
    assert.equal(app.element('pdf-pages').scrollTop, 1224);
    app.window.scrollToPdfPixel(null); // Sync edit mode / auto-follow off.
    app.element('pdf-pages').scrollTop = 200;
    app.element('pdf-pages').dispatchEvent({ type: 'scroll' });
    app.layoutPdf({ width: 324, padding: 12, gap: 12 });
    assert.equal(app.element('pdf-pages').scrollTop, 100);
});

test('saved song bridge carries new page coordinates and accepts legacy pixel-only data', async () => {
    const bridge = fs.readFileSync(path.join(__dirname,
        '../../../../shared/src/wasmJsMain/kotlin/com/woong/vibebass/sync/SyncDataManager.wasmJs.kt'), 'utf8');
    const js = bridge.slice(bridge.indexOf('private fun loadSongsJs(')).match(/js\("""([\s\S]*?)"""\)/)[1];
    const rows = [];
    const complete = deferred();
    const context = {
        fetch: async url => { assert.equal(url, '/api/songs'); return { ok: true, json: async () => [{
            id: 1, title: 'Score', artist: null, youtubeVideoId: 'video',
            anchorPoints: [{ timeSec: 0, scrollPixel: 0 }, { timeSec: 10, scrollPixel: 1024, pagePosition: 1.25 }]
        }] }; },
        onSongItem: (...row) => rows.push(row),
        onComplete: complete.resolve,
        onFailure: complete.reject
    };
    context.window = context;
    vm.runInNewContext(apiSource, context);
    vm.runInNewContext(js, context);
    await complete.promise;
    assert.equal(rows[0][4], '0:0:,10:1024:1.25');
});

test('automatic search stays on the web origin and explains missing-key status', async () => {
    const app = browser();
    const loaded = app.window.initPdfViewer('blob:disabled-search');
    await flush();
    app.documents.get('blob:disabled-search').resolve(pdf([page(1, '한글 & 제목')]));
    await loaded;
    await flush();
    assert.equal(app.searches[0].url, '/api/youtube/search?query=' + encodeURIComponent('한글 & 제목'));
    app.searches[0].resolve({ ok: false, status: 503 });
    await flush();
    assert.match(app.element('media-notice').textContent, /비활성화/);
    app.window.events.pagehide({ persisted: false });
});

test('Space in the PDF records the current position once and leaves other shortcuts alone', async () => {
    const app = browser();
    const loaded = app.window.initPdfViewer('blob:keyboard', false);
    app.layoutPdf({ width: 648, padding: 24, gap: 24 });
    await flush();
    app.documents.get('blob:keyboard').resolve(pdf([page(1), page(2)]));
    await loaded;
    const pages = app.element('pdf-pages');
    let position;
    const recorded = [];
    app.window.onPdfScroll = (pixel, pagePosition) => { position = { pixel, pagePosition }; };
    app.window.onPdfRecordShortcut = repeat => { if (!repeat) recorded.push(position); return true; };
    pages.scrollTop = 1024; // The scroll event has not fired yet.
    let prevented = 0;
    const key = { type: 'keydown', code: 'Space', repeat: false, preventDefault: () => prevented++ };
    pages.dispatchEvent(key);
    pages.dispatchEvent({ ...key, repeat: true });
    pages.dispatchEvent({ ...key, shiftKey: true });
    assert.deepEqual(recorded, [{ pixel: 1024, pagePosition: 1.25 }]);
    assert.equal(prevented, 2);
    app.window.onPdfRecordShortcut = () => false; // Practice mode.
    pages.dispatchEvent(key);
    assert.equal(prevented, 2);
});
