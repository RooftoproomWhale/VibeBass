// Run with: node --test webApp/src/webMain/tests/practice-api.test.cjs
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../resources/practice-api.js'), 'utf8');
const bridge = fs.readFileSync(path.join(__dirname,
    '../../../../shared/src/wasmJsMain/kotlin/com/woong/vibebass/sync/SyncDataManager.wasmJs.kt'), 'utf8');
const samples = require('./pdf-samples.json');
const song = { id: 1, title: '곡', artist: null, youtubeVideoId: 'dQw4w9WgXcQ', anchorPoints: [] };
const ok = data => ({ ok: true, json: async () => data });
function browser(fetch) {
    const context = { fetch };
    context.window = context;
    vm.runInNewContext(source, context);
    return context;
}

test('real Wasm save bridge preserves PDF names, quotes, slashes and control characters on the web origin', async () => {
    const js = bridge.slice(bridge.indexOf('private fun saveSyncDataJs(')).match(/js\("""([\s\S]*?)"""\)/)[1];
    for (const name of [...samples, '제목 "따옴표" \\ 경로\r\n줄바꿈\t탭\u0000🎸']) {
        let payload;
        const context = browser(async (url, options) => {
            assert.equal(url, '/api/songs');
            assert.equal(options.method, 'POST');
            assert.equal(options.headers['Content-Type'], 'application/json');
            payload = JSON.parse(options.body);
            return ok(song);
        });
        const result = new Promise((resolve, reject) => Object.assign(context, {
            title: name, artist: '가수 \\ "이름"\n\t', youtubeVideoId: song.youtubeVideoId,
            anchorPointsJson: '[{"timeSec":10,"scrollPixel":1024,"pagePosition":1.25}]',
            onSuccess: resolve, onFailure: reject
        }));
        vm.runInNewContext(js, context);
        await result;
        assert.equal(payload.title, name);
        assert.equal(payload.artist, context.artist);
        assert.equal(payload.anchorPoints[0].pagePosition, 1.25);
    }
});

test('HTTP failures, network failures and malformed JSON are safe and retryable', async () => {
    for (const [status, expected] of [[400, /입력/], [404, /찾을 수/], [502, /검색에 실패/], [503, /비활성화/], [500, /서버 요청/]]) {
        const api = browser(async () => ({ ok: false, status, text: async () => 'SECRET proxy stack', json: async () => ({ detail: 'SECRET' }) })).vibeBassApi;
        await assert.rejects(api.loadSongs(), error => expected.test(error.message) && !error.message.includes('SECRET'));
    }
    let response = () => { throw new Error('SECRET network details'); };
    const api = browser(async () => response()).vibeBassApi;
    await assert.rejects(api.loadSongs(), /서버에 연결/);
    response = () => ({ ok: true, json: async () => { throw new Error('SECRET invalid JSON'); } });
    await assert.rejects(api.loadSongs(), /응답을 읽을 수/);
    response = () => ok([song]);
    assert.equal((await api.loadSongs())[0].id, 1);
});

test('invalid numeric anchors never reach fetch and invalid lists never partially load', async () => {
    let calls = 0;
    const api = browser(async () => { calls++; return ok(song); }).vibeBassApi;
    for (const anchors of ['[{"timeSec":NaN}]', '[{"timeSec":0,"scrollPixel":-1}]',
        '[{"timeSec":0,"scrollPixel":null}]', '[{"timeSec":0,"scrollPixel":1,"pagePosition":1e100}]']) {
        await assert.rejects(api.saveSong('곡', '', song.youtubeVideoId, anchors), /입력/);
    }
    assert.equal(calls, 0);
    for (const data of [{}, [song, { ...song, id: 2, anchorPoints: [null] }], [{ ...song, id: 2 ** 53 }]]) {
        await assert.rejects(browser(async () => ok(data)).vibeBassApi.loadSongs(), /응답 형식/);
    }
});

test('search query encoding, invalid video IDs and cancellation keep their contracts', async () => {
    const query = '한글 &key=other + #?';
    const signal = {};
    const api = browser(async (url, options) => {
        assert.equal(url, '/api/youtube/search?query=' + encodeURIComponent(query));
        assert.equal(options.signal, signal);
        return ok({ videoId: song.youtubeVideoId });
    }).vibeBassApi;
    assert.equal((await api.searchYoutube(query, signal)).videoId, song.youtubeVideoId);
    await assert.rejects(browser(async () => ok({ videoId: 'bad' })).vibeBassApi.searchYoutube('곡'), /검색 결과/);
    const aborted = Object.assign(new Error('cancelled'), { name: 'AbortError' });
    await assert.rejects(browser(async () => { throw aborted; }).vibeBassApi.searchYoutube('곡'), error => error === aborted);
});
