# voicereport/mapping.py
from typing import List, Dict, Any, Optional

def _overlap(a_start: float, a_end: float, b_start: float, b_end: float) -> float:
    """구간 겹친 길이(초). 음수면 0."""
    s = max(a_start, b_start)
    e = min(a_end, b_end)
    return max(0.0, e - s)

def _pick_speaker_for_segment(stt_seg: Dict[str, Any], diar: List[Dict[str, Any]]) -> str:
    """
    규칙:
      1) STT 세그먼트의 중앙값이 포함되는 diar 세그먼트가 있으면 그 스피커.
      2) 없으면 겹친 길이가 가장 큰 diar 세그먼트의 스피커.
      3) 그래도 없으면 'UNKNOWN'
    """
    s_start = float(stt_seg.get("start", 0.0))
    s_end = float(stt_seg.get("end", 0.0))
    mid = (s_start + s_end) / 2.0

    # 1) 중앙값 포함 규칙
    for d in diar:
        if float(d["start"]) <= mid <= float(d["end"]):
            return d.get("speaker", "UNKNOWN") or "UNKNOWN"

    # 2) 최댓값 겹침 규칙
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
    """
    입력:
      - stt_segments: [{"start": float, "end": float, "text": str}, ...]
      - diar_segments: [{"start": float, "end": float, "speaker": "SPEAKER_00"}, ...]  (없을 수 있음)
    출력:
      - turns: [{"speaker": str, "start": float, "end": float, "text": str}, ...]
    """
    diar = diar_segments or []

    # 시간순 정렬(방어적)
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

    # (옵션) 바로 인접하고 같은 화자인 연속 텍스트를 합치고 싶다면 아래 주석 해제
    # merged: List[Dict[str, Any]] = []
    # for t in turns:
    #     if merged and merged[-1]["speaker"] == t["speaker"] and t["start"] - merged[-1]["end"] <= 0.3:
    #         merged[-1]["end"] = t["end"]
    #         merged[-1]["text"] = (merged[-1]["text"] + " " + t["text"]).strip()
    #     else:
    #         merged.append(t)
    # return merged

    return turns
