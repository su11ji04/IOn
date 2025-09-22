from typing import List, Dict, Any, Optional

def _overlap(a_start: float, a_end: float, b_start: float, b_end: float) -> float:
    s = max(a_start, b_start)
    e = min(a_end, b_end)
    return max(0.0, e - s)

def _pick_speaker_for_segment(stt_seg: Dict[str, Any], diar: List[Dict[str, Any]]) -> str:
    s_start = float(stt_seg.get("start", 0.0))
    s_end = float(stt_seg.get("end", 0.0))
    mid = (s_start + s_end) / 2.0

    for d in diar:
        if float(d["start"]) <= mid <= float(d["end"]):
            return d.get("speaker", "UNKNOWN") or "UNKNOWN"

    best_spk = "UNKNOWN"
    best_ov = 0.0

    for d in diar:
        ov = _overlap(s_start, s_end, float(d["start"]), float(d["end"]))
        if ov > best_ov:
            best_ov = ov
            best_spk = d.get("speaker", "UNKNOWN") or "UNKNOWN"

    return best_spk if best_ov > 0.0 else "UNKNOWN"


def map_speaker_segments(stt_segments: List[Dict[str, Any]],
                         diar_segments: Optional[List[Dict[str, Any]]]) -> List[Dict[str, Any]]:
    diar = diar_segments or []

    # 시간순 정렬
    stt_sorted = sorted(
        [
            {
                "start": float(s.get("start", 0.0)),
                "end": float(s.get("end", 0.0)),
                "text": (s.get("text") or "").strip(),
            }
            for s in (stt_segments or [])
            if s is not None
        ],
        key=lambda x: (x["start"], x["end"])
    )

    diar_sorted = sorted(
        [
            {
                "start": float(d.get("start", 0.0)),
                "end": float(d.get("end", 0.0)),
                "speaker": d.get("speaker", "UNKNOWN") or "UNKNOWN",
            }
            for d in diar
            if d is not None
        ],
        key=lambda x: (x["start"], x["end"])
    )

    turns: List[Dict[str, Any]] = []
    
    # RETURN 객체 준비
    for s in stt_sorted:
        if not s["text"]:
            continue
        speaker = _pick_speaker_for_segment(s, diar_sorted) if diar_sorted else "UNKNOWN"
        turns.append({
            "speaker": speaker,
            "start": s["start"],
            "end": s["end"],
            "text": s["text"],
        })

    return turns
