(function () {
    'use strict';

    const overlays = new Map();
    let player = null;
    let playerReady = false;
    let wantedVideoId = '';
    let youtubeApiPromise = null;
    let youtubeTimer = null;
    let pdfUrl = '';
    let pdfGeneration = 0;
    let pdfLoadingTask = null;
    let pdfDocument = null;
    let pdfRenderTask = null;
    let searchController = null;
    let noticeTimer = null;
    const uploadUrls = new Set();

    function overlay(id) {
        if (!overlays.has(id)) {
            const element = document.createElement('div');
            element.id = id;
            element.className = 'media-overlay';
            element.hidden = true;
            document.body.appendChild(element);
            overlays.set(id, { element, bounds: null, mounted: false, active: false });
        }
        return overlays.get(id);
    }

    function applyBounds(state) {
        const b = state.bounds;
        const valid = b && b.every(Number.isFinite) && b[2] > 0 && b[3] > 0;
        if (!valid) { state.element.hidden = true; return; }
        const [left, top, width, height, clipLeft, clipTop, clipRight, clipBottom] = b;
        const visibleLeft = Math.max(left, clipLeft, 0);
        const visibleTop = Math.max(top, clipTop, 0);
        const visibleRight = Math.min(left + width, clipRight, window.innerWidth);
        const visibleBottom = Math.min(top + height, clipBottom, window.innerHeight);
        state.element.hidden = !state.mounted || !state.active || visibleRight <= visibleLeft || visibleBottom <= visibleTop;
        Object.assign(state.element.style, {
            left: left + 'px', top: top + 'px', width: width + 'px', height: height + 'px',
            clipPath: 'inset(' + Math.max(0, visibleTop - top) + 'px ' +
                Math.max(0, left + width - visibleRight) + 'px ' +
                Math.max(0, top + height - visibleBottom) + 'px ' + Math.max(0, visibleLeft - left) + 'px)'
        });
    }

    function position(id, left, top, width, height, clipLeft = left, clipTop = top,
                      clipRight = left + width, clipBottom = top + height) {
        const state = overlay(id);
        state.bounds = [left, top, width, height, clipLeft, clipTop, clipRight, clipBottom];
        state.mounted = true;
        applyBounds(state);
        const loading = document.getElementById('app-loading');
        if (loading) loading.hidden = true;
    }

    function status(parent, message, error = false) {
        const text = document.createElement('p');
        text.className = 'media-status';
        text.setAttribute('role', error ? 'alert' : 'status');
        text.textContent = message;
        parent.replaceChildren(text);
    }

    function notice(message) {
        let element = document.getElementById('media-notice');
        if (!element) {
            element = document.createElement('div');
            element.id = 'media-notice';
            element.className = 'media-notice';
            element.setAttribute('role', 'alert');
            document.body.appendChild(element);
        }
        element.textContent = message;
        element.hidden = false;
        clearTimeout(noticeTimer);
        noticeTimer = setTimeout(() => { element.hidden = true; }, 8000);
    }

    window.updateYoutubePosition = (...bounds) => position('youtube-player-container', ...bounds);
    window.updatePdfPosition = (...bounds) => position('pdf-viewer-container', ...bounds);
    window.hideYoutubePlayer = function () {
        const state = overlay('youtube-player-container');
        state.mounted = false;
        applyBounds(state);
    };
    window.hidePdfViewer = function () {
        const state = overlay('pdf-viewer-container');
        state.mounted = false;
        applyBounds(state);
    };

    function loadYoutubeApi() {
        if (window.YT && window.YT.Player) return Promise.resolve();
        if (youtubeApiPromise) return youtubeApiPromise;
        youtubeApiPromise = new Promise((resolve, reject) => {
            const timer = setTimeout(() => reject(new Error('YouTube API timeout')), 15000);
            window.onYouTubeIframeAPIReady = () => { clearTimeout(timer); resolve(); };
            const script = document.createElement('script');
            script.src = 'https://www.youtube.com/iframe_api';
            script.onerror = () => { clearTimeout(timer); reject(new Error('YouTube API unavailable')); };
            document.head.appendChild(script);
        }).catch(error => { youtubeApiPromise = null; throw error; });
        return youtubeApiPromise;
    }

    window.initYoutubePlayer = async function (videoId) {
        wantedVideoId = videoId || '';
        const state = overlay('youtube-player-container');
        state.active = !!wantedVideoId;
        applyBounds(state);
        if (!wantedVideoId) {
            if (playerReady && player.stopVideo) player.stopVideo();
            window.onYoutubeTimeUpdate?.(0);
            window.onYoutubeStateChange?.(false);
            return;
        }
        if (player) {
            if (playerReady && player.getVideoData?.().video_id !== wantedVideoId) player.cueVideoById(wantedVideoId);
            return;
        }
        status(state.element, '영상을 불러오고 있어요.');
        try {
            await loadYoutubeApi();
            if (!wantedVideoId || player) return;
            const target = document.createElement('div');
            target.id = 'yt-player';
            state.element.replaceChildren(target);
            player = new window.YT.Player('yt-player', {
                width: '100%', height: '100%', videoId: wantedVideoId,
                playerVars: { playsinline: 1, rel: 0 },
                events: {
                    onReady: event => {
                        playerReady = true;
                        event.target.getIframe?.().setAttribute('title', '연습 영상 YouTube 플레이어');
                        if (!wantedVideoId) event.target.stopVideo();
                        else if (event.target.getVideoData?.().video_id !== wantedVideoId) event.target.cueVideoById(wantedVideoId);
                        clearInterval(youtubeTimer);
                        youtubeTimer = setInterval(() => {
                            if (playerReady && player.getCurrentTime) window.onYoutubeTimeUpdate?.(player.getCurrentTime());
                        }, 100);
                    },
                    onStateChange: event => window.onYoutubeStateChange?.(event.data === 1),
                    onError: () => notice('이 영상을 재생할 수 없어요. 다른 YouTube 링크를 입력해 주세요.')
                }
            });
        } catch (_) {
            if (wantedVideoId) status(state.element, '영상을 연결하지 못했어요. 링크를 다시 적용해 주세요.', true);
        }
    };

    function pdfPages() {
        const state = overlay('pdf-viewer-container');
        let pages = document.getElementById('pdf-pages');
        if (!pages) {
            pages = document.createElement('div');
            pages.id = 'pdf-pages';
            pages.tabIndex = 0;
            pages.setAttribute('role', 'region');
            pages.setAttribute('aria-label', 'PDF 악보. 방향키로 스크롤할 수 있습니다.');
            pages.addEventListener('scroll', () => window.onPdfScroll?.(pages.scrollTop), { passive: true });
            state.element.appendChild(pages);
        }
        return pages;
    }

    function releasePdf() {
        searchController?.abort();
        searchController = null;
        pdfRenderTask?.cancel();
        pdfRenderTask = null;
        const owner = pdfLoadingTask || pdfDocument;
        if (owner) Promise.resolve(owner.destroy()).catch(() => {});
        pdfLoadingTask = null;
        pdfDocument = null;
    }

    async function searchFromPage(page, generation) {
        try {
            const content = await page.getTextContent();
            if (generation !== pdfGeneration || wantedVideoId) return;
            const items = content.items.filter(item => typeof item.str === 'string');
            let title = '';
            let fontSize = 0;
            for (const item of items) {
                const size = Math.abs(item.transform?.[0] || 0);
                const value = item.str.trim();
                if (size > fontSize && value.length > 1 && !/akbobada|^com$|^co\.kr$/i.test(value)) {
                    title = value;
                    fontSize = size;
                }
            }
            if (!title) return;
            const artist = items.map(item => item.str).join(' ').match(/([가-힣a-zA-Z0-9]+)\s+(?:노래|작사|작곡)/)?.[1];
            const query = artist && artist !== title ? artist + ' ' + title : title;
            searchController = new AbortController();
            const response = await fetch('http://localhost:8082/api/youtube/search?query=' + encodeURIComponent(query), {
                signal: searchController.signal
            });
            if (!response.ok) throw new Error('Search unavailable');
            const data = await response.json();
            if (generation === pdfGeneration && !wantedVideoId && typeof data.videoId === 'string') {
                window.onYoutubeVideoIdFound?.(data.videoId);
            }
        } catch (error) {
            if (generation === pdfGeneration && error.name !== 'AbortError' && !wantedVideoId) {
                notice('자동으로 영상을 찾지 못했어요. YouTube 링크를 직접 입력해 주세요.');
            }
        }
    }

    window.initPdfViewer = async function (url, searchYoutube = true) {
        const state = overlay('pdf-viewer-container');
        state.active = !!url;
        applyBounds(state);
        if (url && url === pdfUrl) return;
        pdfUrl = url || '';
        const generation = ++pdfGeneration;
        releasePdf();
        for (const objectUrl of uploadUrls) {
            if (objectUrl !== pdfUrl) { URL.revokeObjectURL(objectUrl); uploadUrls.delete(objectUrl); }
        }
        const pages = pdfPages();
        pages.replaceChildren();
        pages.scrollTop = 0;
        window.onPdfScroll?.(0);
        if (!pdfUrl) return;
        status(pages, '악보를 불러오고 있어요.');
        try {
            if (!window.pdfjsLib) throw new Error('PDF.js unavailable');
            window.pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
            const task = window.pdfjsLib.getDocument({
                url: pdfUrl,
                cMapUrl: 'https://cdn.jsdelivr.net/npm/pdfjs-dist@3.11.174/cmaps/',
                cMapPacked: true,
                isEvalSupported: false
            });
            pdfLoadingTask = task;
            const doc = await task.promise;
            if (generation !== pdfGeneration) return;
            pdfDocument = doc;
            pages.replaceChildren();
            // ponytail: all pages stay resident; add page virtualization for large scores.
            for (let number = 1; number <= doc.numPages; number++) {
                const page = await doc.getPage(number);
                if (generation !== pdfGeneration) return;
                if (number === 1 && searchYoutube && !wantedVideoId) void searchFromPage(page, generation);
                const viewport = page.getViewport({ scale: 1.5 });
                const canvas = document.createElement('canvas');
                canvas.width = Math.ceil(viewport.width);
                canvas.height = Math.ceil(viewport.height);
                canvas.setAttribute('role', 'img');
                canvas.setAttribute('aria-label', '악보 ' + number + ' / ' + doc.numPages + '페이지');
                pages.appendChild(canvas);
                const render = page.render({ canvasContext: canvas.getContext('2d'), viewport });
                pdfRenderTask = render;
                await render.promise;
                if (generation !== pdfGeneration) return;
                pdfRenderTask = null;
            }
        } catch (_) {
            if (generation === pdfGeneration) {
                pdfUrl = '';
                releasePdf();
                status(pages, '악보를 열 수 없어요. 암호가 없는 PDF 파일을 다시 선택해 주세요.', true);
            }
        }
    };

    window.scrollToPdfPixel = function (pixel) {
        const pages = document.getElementById('pdf-pages');
        if (pages && Number.isFinite(pixel)) pages.scrollTo({ top: Math.max(0, pixel), behavior: 'auto' });
    };

    window.triggerPdfUpload = function () {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'application/pdf,.pdf';
        input.hidden = true;
        input.addEventListener('cancel', () => input.remove(), { once: true });
        input.addEventListener('change', async () => {
            const file = input.files?.[0];
            input.remove();
            if (!file) return;
            try {
                if ((!/\.pdf$/i.test(file.name) && file.type !== 'application/pdf') ||
                    !(await file.slice(0, 1024).text()).includes('%PDF-')) {
                    notice('PDF 형식의 악보 파일을 선택해 주세요.');
                    return;
                }
                const url = URL.createObjectURL(file);
                uploadUrls.add(url);
                if (typeof window.onPdfFileSelected === 'function') {
                    // Selection precedes Compose's next effect; invalidate stale callbacks now.
                    ++pdfGeneration;
                    searchController?.abort();
                    window.onPdfFileSelected(file.name, url);
                } else { URL.revokeObjectURL(url); uploadUrls.delete(url); }
            } catch (_) {
                notice('파일을 읽지 못했어요. 다른 PDF 파일을 선택해 주세요.');
            }
        }, { once: true });
        document.body.appendChild(input);
        input.click();
    };

    window.adjustOverlayLayers = function () {
        overlays.forEach(applyBounds);
    };
    window.addEventListener('resize', () => overlays.forEach(applyBounds));
    window.addEventListener('pagehide', event => {
        if (event.persisted) return;
        ++pdfGeneration;
        releasePdf();
        clearInterval(youtubeTimer);
        clearTimeout(noticeTimer);
        player?.destroy();
        uploadUrls.forEach(url => URL.revokeObjectURL(url));
    });
})();
