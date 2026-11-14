(() => {
    const recommendation = document.querySelector('#ai-recommendation');
    const file = document.querySelector('#resume-file');
    const feedback = document.querySelector('#resume-feedback');
    const retry = document.querySelector('#ai-retry');
    const links = document.querySelector('#ai-links');
    let timer;
    let active;
    let selection;
    const answers = new Map();

    async function ask(path, options) {
        const response = await fetch('/api/supports/ai/' + path, { method: 'POST', ...options });
        const result = await response.json().catch(() => ({}));
        if (!response.ok || !result.message) throw new Error(result.message || '요청에 실패했습니다. 다시 시도해 주세요.');
        return result;
    }

    function showLinks(items) {
        links.replaceChildren(...(items || []).filter(item => /^https?:\/\//i.test(item.url)).map(item => {
            const anchor = document.createElement('a');
            anchor.className = 'btn small primary';
            anchor.href = item.url;
            anchor.target = '_blank';
            anchor.rel = 'noopener noreferrer';
            anchor.textContent = item.name + (item.sample ? ' 신청 페이지 (예시 링크) ↗' : ' 신청 페이지 바로가기 ↗');
            return anchor;
        }));
        links.hidden = links.children.length === 0;
    }

    function recommend(detail) {
        clearTimeout(timer);
        active?.abort();
        selection = detail;
        retry.hidden = true;
        showLinks([]);
        recommendation.textContent = detail ? '선택한 조건으로 AI 추천을 준비하고 있어요…' : '카테고리나 지역을 선택하면 추천을 확인할 수 있어요.';
        if (!detail) return;
        const key = JSON.stringify(detail);
        if (answers.has(key)) {
            recommendation.textContent = answers.get(key).message;
            showLinks(answers.get(key).links);
            return;
        }
        const controller = new AbortController();
        active = controller;
        timer = setTimeout(async () => {
            try {
                const answer = await ask('recommend', {
                    signal: controller.signal, headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(detail)
                });
                if (!controller.signal.aborted) {
                    answers.set(key, answer);
                    recommendation.textContent = answer.message;
                    showLinks(answer.links);
                }
            } catch (error) {
                if (controller.signal.aborted) return;
                recommendation.textContent = error.message;
                retry.hidden = false;
            }
        }, 500);
    }
    window.addEventListener('support-selection', event => recommend(event.detail));
    retry.addEventListener('click', () => recommend(selection));
    document.querySelector('#resume-form').addEventListener('submit', async event => {
        event.preventDefault();
        const upload = file.files[0];
        if (!upload) return;
        if (upload.size > 5 * 1024 * 1024) {
            feedback.textContent = '5MB 이하의 파일을 선택해 주세요.';
            return;
        }
        const button = event.currentTarget.querySelector('button');
        const data = new FormData();
        data.append('file', upload);
        button.disabled = file.disabled = true;
        feedback.textContent = '이력서를 분석하고 있어요…';
        try {
            feedback.textContent = (await ask('resume', { body: data })).message;
        } catch (error) {
            feedback.textContent = error.message;
        } finally {
            button.disabled = file.disabled = false;
        }
    });
    file.addEventListener('change', () => { feedback.textContent = ''; });
})();
