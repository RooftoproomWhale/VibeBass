// Run with: node --test webApp/src/webMain/tests/practice-media.test.cjs
// This exercises the real browser bridge with controlled DOM/PDF/network timing.
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../resources/practice-media.js'), 'utf8');
const deferred = () => {
    let resolve;
    let reject;
    const promise = new Promise((done, fail) => { resolve = done; reject = fail; });
    return { promise, resolve, reject };
};
const flush = () => new Promise(done => setImmediate(done));

function browser() {
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
        scrollTo(options) { this.lastScroll = options; this.scrollTop = options.top; }
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
    const searches = [];
    const intervals = new Map();
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
        setInterval: callback => { const id = ++nextInterval; intervals.set(id, callback); return id; },
        clearInterval: id => intervals.delete(id),
        requestAnimationFrame: callback => { callback(); return 1; },
        cancelAnimationFrame() {},
        addEventListener() {},
        removeEventListener() {},
        matchMedia: () => ({ matches: false }),
        fetch: (url, options) => {
            const response = deferred();
            searches.push({ url, options, ...response });
            // Intentionally ignore abort: stale responses still need generation guards.
            return response.promise;
        },
        pdfjsLib: {
            GlobalWorkerOptions: {},
            getDocument: ({ url }) => {
                const request = deferred();
                const task = { ...request, destroy: async () => { task.destroyed = true; } };
                documentRequests.set(url, task);
                return task;
            },
        },
    };
    context.window = context;
    vm.runInNewContext(source, context, { filename: 'practice-media.js' });
    return {
        window: context,
        documents: documentRequests,
        searches,
        intervals,
        element: id => context.document.getElementById(id),
        canvases: () => context.document.getElementById('pdf-pages')?.querySelectorAll('canvas') ?? [],
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
const pdf = pages => ({ numPages: pages.length, getPage: number => Promise.resolve(pages[number - 1]), destroy: async () => {} });

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
    app.documents.get('blob:previous').resolve(pdf([page('previous', 'Previous song')]));
    await loaded;
    await flush();
    assert.equal(app.searches.length, 1);
    let selected;
    app.window.onPdfFileSelected = (name, url) => {
        selected = { name, url };
        app.searches[0].resolve({ ok: true, json: async () => ({ videoId: 'previous-video' }) });
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
    app.searches[1].resolve({ ok: true, json: async () => ({ videoId: 'latest-video' }) });
    await flush();
    app.searches[0].resolve({ ok: true, json: async () => ({ videoId: 'stale-video' }) });
    await Promise.all([firstSearch, secondSearch]);
    await flush();
    assert.deepEqual(videos, ['latest-video']);
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
