(function () {
    'use strict';

    const messages = {
        400: '입력 내용을 확인해 주세요. 제목, 영상 ID와 싱크 위치가 올바르지 않습니다.',
        404: '요청한 곡을 찾을 수 없습니다. 보관함을 새로고침해 주세요.',
        502: '자동 검색에 실패했습니다. YouTube 링크를 직접 입력해 주세요.',
        503: '자동 검색이 비활성화되어 있습니다. YouTube 링크를 직접 입력해 주세요.'
    };
    const failure = message => Object.assign(new Error(message), { userMessage: message });
    const validPosition = value => Number.isFinite(value) && value >= 0 && value <= 3.4028234663852886e38;
    const validAnchors = anchors => Array.isArray(anchors) && anchors.every(anchor => anchor &&
        validPosition(anchor.timeSec) && validPosition(anchor.scrollPixel) &&
        (anchor.pagePosition == null || validPosition(anchor.pagePosition)));

    async function request(path, options) {
        let response;
        try { response = await fetch('/api/' + path, options); }
        catch (error) {
            if (error.name === 'AbortError') throw error;
            throw failure('서버에 연결할 수 없습니다. 연결을 확인하고 다시 시도해 주세요.');
        }
        // Proxy HTML and upstream details are not user-facing error messages.
        if (!response.ok) throw failure(messages[response.status] || '서버 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.');
        try { return await response.json(); }
        catch (_) { throw failure('서버 응답을 읽을 수 없습니다. 다시 시도해 주세요.'); }
    }

    window.vibeBassApi = {
        async saveSong(title, artist, youtubeVideoId, anchorPointsJson) {
            let anchorPoints;
            try { anchorPoints = JSON.parse(anchorPointsJson); }
            catch (_) { throw failure(messages[400]); }
            if (!validAnchors(anchorPoints)) throw failure(messages[400]);
            return request('songs', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ title, artist, youtubeVideoId, anchorPoints })
            });
        },
        async loadSongs() {
            const songs = await request('songs');
            if (!Array.isArray(songs) || !songs.every(song => song && Number.isSafeInteger(song.id) && song.id > 0 &&
                typeof song.title === 'string' && (song.artist == null || typeof song.artist === 'string') &&
                typeof song.youtubeVideoId === 'string' && validAnchors(song.anchorPoints))) {
                throw failure('보관함 응답 형식이 올바르지 않습니다. 다시 시도해 주세요.');
            }
            return songs;
        },
        async searchYoutube(query, signal) {
            const data = await request('youtube/search?query=' + encodeURIComponent(query), { signal });
            if (!data || typeof data.videoId !== 'string' || !/^[A-Za-z0-9_-]{11}$/.test(data.videoId)) {
                throw failure('검색 결과를 읽을 수 없습니다. YouTube 링크를 직접 입력해 주세요.');
            }
            return data;
        }
    };
})();
