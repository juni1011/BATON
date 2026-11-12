import { useEffect, useId, useMemo, useState } from "react";
import { DISTRICTS, MAP_VIEWBOX, REGIONS } from "./korea-regions.js";
export { REGIONS, getDistricts } from "./korea-regions.js";

const validBounds = bounds => Array.isArray(bounds) && bounds.length === 4
    && bounds.every(Number.isFinite) && bounds[2] > bounds[0] && bounds[3] > bounds[1];
const validPoint = point => Array.isArray(point) && point.length === 2 && point.every(Number.isFinite);
const allBounds = REGIONS.filter(item => validBounds(item.bounds)).map(item => item.bounds);
const nationalBounds = allBounds.length ? [
    Math.min(...allBounds.map(bounds => bounds[0])), Math.min(...allBounds.map(bounds => bounds[1])),
    Math.max(...allBounds.map(bounds => bounds[2])), Math.max(...allBounds.map(bounds => bounds[3]))
] : [MAP_VIEWBOX[0], MAP_VIEWBOX[1], MAP_VIEWBOX[0] + MAP_VIEWBOX[2], MAP_VIEWBOX[1] + MAP_VIEWBOX[3]];
const mapPadding = 24;
const viewBox = [0, 0, nationalBounds[2] - nationalBounds[0] + mapPadding * 2, nationalBounds[3] - nationalBounds[1] + mapPadding * 2];
const nationalTransform = { scale: 1, x: mapPadding - nationalBounds[0], y: mapPadding - nationalBounds[1] };

function regionTransform(region, focused) {
    if (!focused || !validBounds(region?.bounds)) return nationalTransform;
    const [left, top, width, height] = viewBox;
    const [x0, y0, x1, y1] = region.bounds;
    const padding = Math.min(width, height) * 0.08;
    const scale = Math.min((width - padding * 2) / (x1 - x0), (height - padding * 2) / (y1 - y0));
    return {
        scale,
        x: left + width / 2 - (x0 + x1) / 2 * scale,
        y: top + height / 2 - (y0 + y1) / 2 * scale
    };
}

export default function RegionMap({ sido = "", district = "", zoomed = false, onSelectRegion, onSelectDistrict }) {
    const titleId = useId();
    const descriptionId = useId();
    const [readyToZoom, setReadyToZoom] = useState(false);
    const region = useMemo(() => REGIONS.find(item => item.name === sido || item.code === sido || item.aliases?.includes(sido)), [sido]);
    const districts = useMemo(() => region ? DISTRICTS.filter(item => item.sidoCode === region.code) : [], [region]);
    const selectedDistrict = useMemo(
        () => districts.find(item => item.name.replace(/\s/g, "") === String(district ?? "").replace(/\s/g, "") || item.code === district),
        [districts, district]
    );

    // First paint keeps the national view, including when an existing profile opens already zoomed.
    useEffect(() => {
        if (!zoomed || !region) {
            setReadyToZoom(false);
            return undefined;
        }
        let secondFrame;
        const firstFrame = requestAnimationFrame(() => {
            secondFrame = requestAnimationFrame(() => setReadyToZoom(true));
        });
        return () => {
            cancelAnimationFrame(firstFrame);
            if (secondFrame !== undefined) cancelAnimationFrame(secondFrame);
        };
    }, [zoomed, region?.code]);

    const focused = Boolean(zoomed && readyToZoom && region);
    const canSelectRegion = !focused && typeof onSelectRegion === "function";
    const canSelectDistrict = focused && typeof onSelectDistrict === "function";
    const transform = useMemo(() => regionTransform(region, focused), [region, focused]);
    const marker = validPoint(selectedDistrict?.center) ? {
        x: selectedDistrict.center[0] * transform.scale + transform.x,
        y: selectedDistrict.center[1] * transform.scale + transform.y
    } : null;
    const regionLabel = region ? [region.name, selectedDistrict?.name].filter(Boolean).join(" · ") : "대한민국";
    const description = !region
        ? "지도에서 시·도를 선택하면 해당 지역의 실제 행정 경계를 표시합니다. Tab 키로 이동하고 Enter 또는 스페이스 키로 선택할 수도 있습니다."
        : selectedDistrict
            ? `${region.name}와 ${selectedDistrict.name}의 행정 경계를 강조하고 선택한 시·군·구 안에 위치 표시를 보여 줍니다.`
            : `${region.name}의 행정 경계를 강조합니다.${focused ? " 선택한 시도의 시·군·구 경계도 표시합니다." : ""}`;

    function selectWithKeyboard(event, canSelect, onSelect, name) {
        if (!canSelect || (event.key !== "Enter" && event.key !== " ")) return;
        event.preventDefault();
        event.stopPropagation();
        if (event.repeat) return;
        onSelect(name);
    }

    function selectWithPointer(event, canSelect, onSelect, name) {
        if (!canSelect) return;
        event.stopPropagation();
        onSelect(name);
    }

    return (
        <figure className={`region-map${focused ? " region-map--focused" : ""}`}>
            <div className="region-map__viewport">
                <svg
                    className="region-map__svg"
                    viewBox={viewBox.join(" ")}
                    role="group"
                    aria-labelledby={titleId}
                    aria-describedby={descriptionId}
                    preserveAspectRatio="xMidYMid meet"
                >
                    <title id={titleId}>{`${regionLabel} 지도`}</title>
                    <desc id={descriptionId}>{description}</desc>
                    <g
                        className="region-map__world"
                        style={{ transform: `matrix(${transform.scale}, 0, 0, ${transform.scale}, ${transform.x}, ${transform.y})` }}
                    >
                        {REGIONS.map(item => (
                            <path
                                key={item.code}
                                d={item.path}
                                className={`region-map__province${item.code === region?.code ? " is-selected" : ""}${canSelectRegion ? " is-interactive" : ""}`}
                                fillRule="evenodd"
                                vectorEffect="non-scaling-stroke"
                                data-region={item.name}
                                data-region-code={item.code}
                                data-center-x={item.center[0]}
                                data-center-y={item.center[1]}
                                role="button"
                                tabIndex={canSelectRegion ? 0 : -1}
                                aria-label={item.name}
                                aria-pressed={item.code === region?.code}
                                aria-disabled={!canSelectRegion}
                                aria-hidden={focused || undefined}
                                onClick={event => selectWithPointer(event, canSelectRegion, onSelectRegion, item.name)}
                                onKeyDown={event => selectWithKeyboard(event, canSelectRegion, onSelectRegion, item.name)}
                            >
                                <title>{item.name}</title>
                            </path>
                        ))}
                        {focused && districts.map(item => (
                            <path
                                key={item.code}
                                d={item.path}
                                className={`region-map__district${item.code === selectedDistrict?.code ? " is-selected" : ""}${canSelectDistrict ? " is-interactive" : ""}`}
                                fillRule="evenodd"
                                vectorEffect="non-scaling-stroke"
                                data-district={item.name}
                                data-district-code={item.code}
                                data-center-x={item.center[0]}
                                data-center-y={item.center[1]}
                                role="button"
                                tabIndex={canSelectDistrict ? 0 : -1}
                                aria-label={item.name}
                                aria-pressed={item.code === selectedDistrict?.code}
                                aria-disabled={!canSelectDistrict}
                                onClick={event => selectWithPointer(event, canSelectDistrict, onSelectDistrict, item.name)}
                                onKeyDown={event => selectWithKeyboard(event, canSelectDistrict, onSelectDistrict, item.name)}
                            >
                                <title>{`${region.name} ${item.name}`}</title>
                            </path>
                        ))}
                        {region && <path d={region.path} className="region-map__outline" vectorEffect="non-scaling-stroke" />}
                    </g>
                    {marker && (
                        <g className="region-map__marker" aria-hidden="true" style={{ transform: `translate(${marker.x}px, ${marker.y}px)` }}>
                            <g transform="scale(.7)">
                                <path d="M0 0C-7-11-25-28-25-44a25 25 0 0 1 50 0C25-28 7-11 0 0Z" />
                                <circle cx="0" cy="-44" r="8" />
                            </g>
                        </g>
                    )}
                </svg>
            </div>
        </figure>
    );
}

export { RegionMap };
