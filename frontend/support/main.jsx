import { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AgeIllustration, StatusIllustration, HousingIllustration, IncomeIllustration } from './visuals.jsx';
import { REGIONS, getDistricts, RegionMap } from './RegionMap.jsx';
import { districtAtLocation } from './korea-regions.js';

const FIELDS = ['sido', 'sigungu', 'status', 'housing', 'householdIncomeLevel'];
const STATUS = ['대학생', '구직자', '재직자', '기타'];
const HOUSING = ['월세', '전세', '자가', '가족과 거주', '기숙사·기타'];
const INCOME = ['중위소득 50% 이하', '중위소득 100% 이하', '중위소득 150% 이하', '중위소득 150% 초과', '미확인'];
const EMPTY = { age: '', sido: '', sigungu: '', status: '', housing: '', householdIncomeLevel: '' };
const FIELD_ORDER = Object.keys(EMPTY);
const STEP_FIELDS = [['age'], ['sido', 'sigungu'], ['status'], ['housing'], ['householdIncomeLevel']];
const isFilled = (key, value) => key === 'age'
    ? value !== '' && Number.isInteger(Number(value)) && Number(value) >= 0 && Number(value) <= 120
    : Boolean(value?.trim());
const AGE_STAGES = {
    minor: ['미성년자', '0–18세'], youth: ['청년', '19–34세'],
    middle: ['중년', '35–64세'], senior: ['노년', '65세 이상']
};
const array = value => Array.isArray(value) ? value : [];
const stageFor = age => {
    const value = Number(age);
    if (age === '' || !Number.isInteger(value) || value < 0 || value > 120) return null;
    return value < 19 ? 'minor' : value < 35 ? 'youth' : value < 65 ? 'middle' : 'senior';
};

function loadProfile() {
    try {
        const stored = window.Easyton.getUserProfile();
        if (!stored || typeof stored !== 'object') return { ...EMPTY };
        const result = { ...EMPTY };
        for (const key of Object.keys(EMPTY)) result[key] = stored[key] == null ? '' : String(stored[key]);
        result.sido = { '강원도': '강원특별자치도', '전라북도': '전북특별자치도', '제주도': '제주특별자치도' }[result.sido] || result.sido;
        return result;
    } catch { return { ...EMPTY }; }
}

function loadMatches() {
    try { return array(window.Easyton.getSupportMatches()); } catch { return []; }
}

function storedDeadlines() {
    try { return new Set(array(window.Easyton.getCalendarEvents()).map(event => event.id)); } catch { return new Set(); }
}

function Step({ number, title, description, visual, caption, detail, children, isMap = false }) {
    return <section className={`support-step${isMap ? ' support-step--map' : ''}`} aria-labelledby={`step-${number}`}>
        <div className="support-visual-panel">
            <div className="support-visual-art">{visual}</div>
            <div className="support-visual-caption" aria-live="polite" aria-atomic="true">
                <strong>{caption}</strong>{detail && <span>{detail}</span>}
            </div>
        </div>
        <div className="support-step-control">
            <span className="support-step-number">{String(number).padStart(2, '0')} <span>/ {String(STEP_FIELDS.length).padStart(2, '0')}</span></span>
            <h2 id={`step-${number}`}>{title}</h2>
            <p className="support-step-description">{description}</p>
            {children}
        </div>
    </section>;
}

function SelectField({ id, label, value, options, onChange, placeholder = '선택해 주세요', disabled = false }) {
    const names = options.map(option => typeof option === 'string' ? option : option.name);
    return <div className="support-field">
        <label htmlFor={id}>{label}</label>
        <select id={id} value={value} onChange={event => onChange(event.target.value)} disabled={disabled}>
            <option value="">{placeholder}</option>
            {value && !names.includes(value) && <option value={value}>{value}</option>}
            {names.map(name => <option key={name} value={name}>{name}</option>)}
        </select>
    </div>;
}

function SupportApp() {
    const [profile, setProfile] = useState(loadProfile);
    const [matches, setMatches] = useState(loadMatches);
    const [added, setAdded] = useState(storedDeadlines);
    const [message, setMessage] = useState('');
    const [busy, setBusy] = useState(false);
    const [showNationalMap, setShowNationalMap] = useState(false);
    const [mapPreview, setMapPreview] = useState(null);
    const [locationMessage, setLocationMessage] = useState('');
    const [locating, setLocating] = useState(false);
    const locationRequest = useRef(0);
    const resultsRef = useRef(null);
    const ageRef = useRef(null);
    const submitting = useRef(false);
    const profileRef = useRef(profile);
    const advanceTimer = useRef(null);
    const regionFocusFrame = useRef(null);
    const stage = stageFor(profile.age);
    const districts = getDistricts(profile.sido);
    const regionKnown = REGIONS.some(region => region.name === profile.sido || region.aliases?.includes(profile.sido));
    const filled = STEP_FIELDS.filter(fields => fields.every(key => isFilled(key, profile[key]))).length;

    function cancelAdvance() {
        clearTimeout(advanceTimer.current);
        advanceTimer.current = null;
        cancelAnimationFrame(regionFocusFrame.current);
        regionFocusFrame.current = null;
    }

    function advanceFrom(key, next, delay = 450) {
        cancelAdvance();
        if (!isFilled(key, next[key])) return;
        advanceTimer.current = setTimeout(() => {
            advanceTimer.current = null;
            const index = FIELD_ORDER.indexOf(key);
            const remaining = [...FIELD_ORDER.slice(index + 1), ...FIELD_ORDER.slice(0, index)];
            const targetKey = remaining.find(field => !isFilled(field, profileRef.current[field]));
            const target = document.getElementById(targetKey || 'submit');
            if (!target || target.disabled) return;
            const section = target.closest('.support-step, .support-submit-panel');
            if (!section) return;
            const header = document.querySelector('.site-header')?.getBoundingClientRect().height || 0;
            target.focus({ preventScroll: true });
            window.scrollTo({
                top: Math.max(0, window.scrollY + section.getBoundingClientRect().top - header - 20,
                    window.scrollY + target.getBoundingClientRect().bottom - window.innerHeight + 24),
                behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth'
            });
        }, delay);
    }

    const update = (key, value, { advance = false, delay } = {}) => {
        cancelAdvance();
        const current = profileRef.current;
        const next = { ...current, [key]: value, ...(key === 'sido' && value !== current.sido ? { sigungu: '' } : {}) };
        profileRef.current = next;
        setProfile(next);
        setMessage('');
        if (advance) advanceFrom(key, next, delay);
    };
    const select = (key, value) => update(key, value, { advance: true });

    function selectDistrict(value) {
        setMapPreview(null);
        locationRequest.current++;
        setLocating(false);
        setLocationMessage('');
        setShowNationalMap(false);
        update('sigungu', value);
    }

    function findCurrentLocation() {
        setMapPreview(null);
        if (!navigator.geolocation) {
            setLocationMessage('이 브라우저에서 위치를 확인할 수 없어요. 거주 지역을 직접 골라 주세요.');
            return;
        }
        cancelAdvance();
        const request = ++locationRequest.current;
        setLocating(true);
        setLocationMessage('현재 위치를 확인하는 중이에요…');
        navigator.geolocation.getCurrentPosition(({ coords }) => {
            if (request !== locationRequest.current) return;
            const district = districtAtLocation(coords.longitude, coords.latitude);
            const region = REGIONS.find(item => item.code === district?.sidoCode);
            setLocating(false);
            if (!region) {
                setLocationMessage('현재 위치의 시·군·구를 찾지 못했어요. 거주 지역을 직접 골라 주세요.');
                return;
            }
            const next = { ...profileRef.current, sido: region.name, sigungu: district.name };
            profileRef.current = next;
            setProfile(next);
            setMessage('');
            setShowNationalMap(false);
            setLocationMessage('현재 위치 주변을 선택했어요. 실제 거주지와 다르면 바꿔 주세요.');
        }, () => {
            if (request !== locationRequest.current) return;
            setLocating(false);
            setLocationMessage('위치를 확인하지 못했어요. 브라우저 위치 권한을 확인하거나 거주 지역을 직접 골라 주세요.');
        }, { timeout: 10000 });
    }

    function selectRegion(value) {
        setMapPreview(null);
        locationRequest.current++;
        setLocating(false);
        setLocationMessage('');
        setShowNationalMap(false);
        update('sido', value);
        if (!value) return;
        regionFocusFrame.current = requestAnimationFrame(() => {
            regionFocusFrame.current = null;
            const target = document.getElementById('sigungu');
            if (!target || target.disabled) return;
            target.focus({ preventScroll: true });
            const rect = target.getBoundingClientRect();
            const header = document.querySelector('.site-header')?.getBoundingClientRect().height || 0;
            const offset = rect.bottom > window.innerHeight - 24 ? rect.bottom - window.innerHeight + 24
                : rect.top < header + 20 ? rect.top - header - 20 : 0;
            if (offset) window.scrollBy({ top: offset,
                behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
        });
    }

    useEffect(() => {
        const cancelOnEscape = event => { if (event.key === 'Escape') cancelAdvance(); };
        window.addEventListener('wheel', cancelAdvance, { passive: true });
        window.addEventListener('touchstart', cancelAdvance, { passive: true });
        window.addEventListener('keydown', cancelOnEscape);
        return () => {
            cancelAdvance();
            window.removeEventListener('wheel', cancelAdvance);
            window.removeEventListener('touchstart', cancelAdvance);
            window.removeEventListener('keydown', cancelOnEscape);
        };
    }, []);

    function stepAge(direction) {
        const current = profile.age === '' || !Number.isFinite(Number(profile.age)) ? 0 : Math.round(Number(profile.age));
        ageRef.current?.focus();
        update('age', String(Math.max(0, Math.min(120, current + direction))), { advance: true, delay: 1100 });
    }

    async function recommend(event) {
        event.preventDefault();
        cancelAdvance();
        if (submitting.current) return;
        const age = profile.age === '' ? null : Number(profile.age);
        if (age !== null && (!Number.isInteger(age) || age < 0 || age > 120)) {
            setMessage('만 나이는 0세부터 120세까지 정수로 입력해 주세요.');
            ageRef.current?.focus();
            return;
        }
        const next = { age };
        for (const key of FIELDS) next[key] = profile[key].trim() || null;
        submitting.current = true;
        setBusy(true);
        setMessage('');
        try {
            window.Easyton.saveUserProfile(next);
            const response = await window.Easyton.postJson('/api/supports/match', { profile: next });
            if (!Array.isArray(response)) throw new Error('Invalid response');
            window.Easyton.saveSupportMatches(response);
            setMatches(response);
            requestAnimationFrame(() => {
                const results = resultsRef.current;
                if (!results) return;
                const header = document.querySelector('.site-header')?.getBoundingClientRect().height || 0;
                window.scrollTo({
                    top: Math.max(0, window.scrollY + results.getBoundingClientRect().top - header - 20),
                    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth'
                });
            });
        } catch (error) {
            setMessage(window.EasytonUI.errorText(error));
        } finally {
            submitting.current = false;
            setBusy(false);
        }
    }

    function addDeadline(match) {
        try {
            const id = 'support-' + match.id;
            const events = array(window.Easyton.getCalendarEvents()).filter(event => event.id !== id);
            events.push({ id, title: match.name + ' 신청 마감', type: 'SUPPORT_DEADLINE', date: match.applyEnd,
                startTime: null, endTime: null, description: match.organization, sourceLink: '/support.html' });
            window.Easyton.saveCalendarEvents(events);
            setAdded(current => new Set([...current, id]));
        } catch { setMessage('일정을 저장하지 못했어요. 브라우저 저장 공간을 확인해 주세요.'); }
    }

    return <>
        <div className="support-intro">
            <div><p className="support-eyebrow">나에게 맞는 기회를 찾는 첫걸음</p>
                <h1>지원사업 추천</h1><p className="lead">내 이야기를 하나씩 알려주세요.<br className="mobile-break" /> 조건에 맞는 지원사업을 함께 찾아볼게요.</p>
                <p className="support-flow-hint">선택을 마치면 다음 빈 항목으로 부드럽게 이동해요.</p></div>
            <div className="support-progress"><span className="support-progress-dot" />입력한 항목 <strong>{filled}<span> / {STEP_FIELDS.length}</span></strong></div>
        </div>
        <form id="profile-form" className="support-form" onSubmit={recommend} onFocusCapture={cancelAdvance} onPointerDownCapture={cancelAdvance} aria-busy={busy}>
            <Step number={1} title="만 나이" description="지금의 나이를 알려주세요. 위·아래 화살표로도 바꿀 수 있어요."
                visual={<div className="visual-swap" key={stage || 'empty'}><AgeIllustration stage={stage} /></div>}
                caption={stage ? AGE_STAGES[stage][0] : '당신의 첫걸음은 어디쯤인가요?'} detail={stage ? AGE_STAGES[stage][1] : '나이에 맞춰 그림이 바뀌어요'}>
                <label htmlFor="age">만 나이</label>
                <div className="support-age-input"><span aria-hidden="true">만</span>
                    <input ref={ageRef} id="age" type="number" min="0" max="120" step="1" inputMode="numeric" aria-describedby="age-hint" placeholder="나이" value={profile.age}
                        onChange={event => update('age', event.target.value, { advance: true, delay: 1100 })}
                        onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); advanceFrom('age', profileRef.current, 0); } }} />
                    <span aria-hidden="true">세</span><div className="support-age-arrows">
                        <button type="button" aria-label="나이 1세 늘리기" disabled={Number(profile.age) >= 120} onClick={() => stepAge(1)}>⌃</button>
                        <button type="button" aria-label="나이 1세 줄이기" disabled={profile.age !== '' && Number(profile.age) <= 0} onClick={() => stepAge(-1)}>⌄</button>
                    </div>
                </div>
                <p className="support-input-hint" id="age-hint">0~120세 · 입력 후 잠시 기다리거나 Enter를 누르면 다음 빈 항목으로 이동해요.</p>
            </Step>
            <Step number={2} title="거주 지역" description="현재 위치를 찾거나 시·도와 시·군·구를 직접 골라 주세요." isMap
                visual={<div className="support-region-map">
                    {regionKnown && <button type="button" className="btn small support-map-back" onClick={() => {
                        cancelAdvance(); setMapPreview(null); setShowNationalMap(current => !current);
                    }}>{showNationalMap ? '선택 지역 확대' : '전국 지도 보기'}</button>}
                    <RegionMap sido={profile.sido} district={profile.sigungu} zoomed={regionKnown && !showNationalMap}
                        onSelectRegion={selectRegion} onSelectDistrict={selectDistrict} onPreview={setMapPreview} />
                </div>}
                caption={mapPreview?.district || mapPreview?.sido || profile.sigungu || profile.sido || '대한민국'} detail={mapPreview ? [mapPreview.sido, mapPreview.district].filter(Boolean).join(' · ') : profile.sido ? `${profile.sido}${profile.sigungu ? ' · ' + profile.sigungu : ''}` : '지도나 오른쪽 선택창에서 거주 지역을 선택해 주세요'}>
                <button type="button" className="btn small" onClick={findCurrentLocation} disabled={locating}>현재 위치로 찾기</button>
                <p className="support-input-hint" role="status">{locationMessage}</p>
                <div className="support-region-fields">
                <SelectField id="sido" label="거주 시·도" value={profile.sido} options={REGIONS} onChange={selectRegion} />
                {districts.length || !profile.sido ? <SelectField id="sigungu" label="시·군·구" value={profile.sigungu} options={districts}
                    disabled={!profile.sido} placeholder={profile.sido ? '선택해 주세요' : '거주 시·도를 먼저 선택해 주세요'} onChange={selectDistrict} />
                    : <div className="support-field"><label htmlFor="sigungu">시·군·구</label><input id="sigungu" value={profile.sigungu} placeholder={profile.sido ? '시·군·구를 입력해 주세요' : '거주 시·도를 먼저 선택해 주세요'} disabled={!profile.sido}
                        onChange={event => update('sigungu', event.target.value, { advance: true, delay: 1100 })}
                        onKeyDown={event => { if (event.key === 'Enter') { event.preventDefault(); advanceFrom('sigungu', profileRef.current, 0); } }} /></div>}
                </div>
                {regionKnown && <p className="support-input-hint">지역을 선택하면 지도에서 위치를 확인할 수 있어요.</p>}
            </Step>
            <Step number={3} title="현재 상태" description="요즘 어떤 하루를 보내고 계신가요? 현재 상황과 가장 가까운 항목을 골라주세요."
                visual={<div className="visual-swap" key={profile.status}><StatusIllustration value={profile.status} /></div>}
                caption={profile.status || '지금의 나를 알려주세요'} detail="나의 상황에 맞는 지원을 찾아요">
                <SelectField id="status" label="현재 상태" value={profile.status} options={STATUS} onChange={value => select('status', value)} />
            </Step>
            <Step number={4} title="거주 형태" description="지금 머무는 공간은 어떤 곳인가요? 내 생활에 가까운 항목을 선택해 주세요."
                visual={<div className="visual-swap" key={profile.housing}><HousingIllustration value={profile.housing} /></div>}
                caption={profile.housing || '나의 일상을 담은 공간'} detail="주거 조건에 맞는 기회를 살펴봐요">
                <SelectField id="housing" label="거주 형태" value={profile.housing} options={HOUSING} onChange={value => select('housing', value)} />
            </Step>
            <Step number={5} title="가구 소득" description="가구 소득 구간을 알고 있다면 선택해 주세요. 잘 모르겠다면 ‘미확인’을 선택해도 괜찮아요."
                visual={<div className="visual-swap"><IncomeIllustration value={profile.householdIncomeLevel} /></div>}
                caption={profile.householdIncomeLevel || '생활에 필요한 지원까지'} detail="선택한 소득 구간을 함께 살펴봐요">
                <aside className="support-income-guide" aria-label="기준 중위소득 설명">
                    <strong>기준 중위소득이란?</strong>
                    <p>가구 소득의 중간값을 바탕으로 정부가 정하는 복지 지원 기준이에요. 가구원 수와 연도에 따라 기준 금액이 달라져요.</p>
                    <p>100%는 기준 금액, 50%는 그 절반, 150%는 1.5배를 뜻해요.</p>
                    <a href="https://www.mohw.go.kr/menu.es?mid=a10708010500" target="_blank" rel="noopener noreferrer">보건복지부 기준 안내 ↗</a>
                </aside>
                <SelectField id="householdIncomeLevel" label="가구 소득" value={profile.householdIncomeLevel} options={INCOME} onChange={value => select('householdIncomeLevel', value)} />
            </Step>
            <div className="support-submit-panel">
                <div><strong>나에게 맞는 지원사업, 만나볼까요?</strong><p>입력한 조건으로 추천 결과를 확인하세요.</p></div>
                <button type="submit" className="btn primary" id="submit" disabled={busy}>{busy ? '추천 찾는 중…' : '추천 받기'}<span aria-hidden="true">→</span></button>
            </div>
        </form>
        <div id="message" className="notice error support-message" role="alert" hidden={!message}>{message}</div>
        <section id="results" className="support-results" ref={resultsRef} aria-live="polite" aria-label="지원사업 추천 결과">
            {matches.length > 0 && <><div className="support-results-heading"><h2 id="results-title">추천 결과 <span>{matches.length}건</span></h2><p>내 조건을 바탕으로 확인한 지원사업이에요.</p></div>
                <div className="support-result-grid">{matches.map(match => {
                    const isAdded = added.has('support-' + match.id);
                    const url = /^https?:\/\//i.test(match.applyUrl || '') ? match.applyUrl : null;
                    return <article className="card stack" key={match.id}>
                        <span className={'badge ' + match.verdict}>{window.EasytonUI.VERDICT_LABELS[match.verdict] || match.verdict}</span>
                        <h3>{match.name}</h3><p>{match.reason}</p>
                        <dl className="meta"><dt>기관</dt><dd>{match.organization || '-'}</dd>
                            <dt>신청 기간</dt><dd>{window.EasytonUI.dateLabel(match.applyStart)} ~ {window.EasytonUI.dateLabel(match.applyEnd)}</dd>
                            <dt>필요 서류</dt><dd>{array(match.requiredDocuments).join(', ') || '-'}</dd></dl>
                        {match.verdict !== 'NOT_ELIGIBLE' && <div className="row support-result-actions">
                            {match.applyEnd && <button type="button" className="btn small" disabled={isAdded} onClick={() => addDeadline(match)}>{isAdded ? '캘린더에 추가됨' : '마감일을 캘린더에 추가'}</button>}
                            {url && <a className="btn small" href={url} target="_blank" rel="noopener noreferrer">신청 페이지 (예시 링크)</a>}
                        </div>}
                    </article>;
                })}</div></>}
        </section>
    </>;
}

createRoot(document.getElementById('support-root')).render(<SupportApp />);
