# voicereport/diarization.py
from typing import List, Dict, Optional
import os
import logging

log = logging.getLogger("uvicorn.error")


def _load_hf_token_from_file(default_path: Optional[str] = None) -> Optional[str]:
    """
    1순위: 환경변수 HUGGINGFACE_TOKEN
    2순위: voicereport/keys/Huggingface_token.txt
    3순위: 프로젝트 루트(keys/Huggingface_token.txt)
    """
    # 1) ENV
    env = os.getenv("HUGGINGFACE_TOKEN")
    if env:
        tok = env.strip()
        if tok:
            try:
                log.info("[DIAR] token: loaded from ENV (****%s)", tok[-6:])
            except Exception:
                pass
            return tok

    # 2) 패키지 경로
    try:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path_pkg = default_path or os.path.join(base_dir, "keys", "Huggingface_token.txt")
        if os.path.exists(path_pkg):
            with open(path_pkg, "r", encoding="utf-8") as f:
                tok = f.read().strip()
            if tok:
                try:
                    log.info("[DIAR] token: loaded from %s (****%s)", path_pkg, tok[-6:])
                except Exception:
                    pass
                return tok
    except Exception as e:
        try:
            log.warning("[DIAR] read package token failed: %s", e)
        except Exception:
            pass

    # 3) 프로젝트 루트 경로
    try:
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path_root = os.path.join(project_root, "keys", "Huggingface_token.txt")
        if os.path.exists(path_root):
            with open(path_root, "r", encoding="utf-8") as f:
                tok = f.read().strip()
            if tok:
                try:
                    log.info("[DIAR] token: loaded from %s (****%s)", path_root, tok[-6:])
                except Exception:
                    pass
                return tok
        else:
            try:
                log.info("[DIAR] token file not found at %s", path_root)
            except Exception:
                pass
    except Exception as e:
        try:
            log.warning("[DIAR] read root token failed: %s", e)
        except Exception:
            pass

    return None


def _load_pipeline(token: str):
    """pyannote Pipeline 로드(3.1 우선) + 호출 가능성 가드 + 폴백."""
    from pyannote.audio import Pipeline

    last_err = None
    model_ids = [
        "pyannote/speaker-diarization-3.1",  # 권장
        "pyannote/speaker-diarization",      # 폴백
    ]
    for mid in model_ids:
        try:
            log.info("[DIAR] loading model: %s", mid)
            pl = Pipeline.from_pretrained(mid, use_auth_token=token)
            if callable(pl):
                log.info("[DIAR] model loaded ok: %s", mid)
                return pl
            else:
                log.error("[DIAR] non-callable pipeline for %s: %r", mid, type(pl))
        except Exception as e:
            last_err = e
            log.error("[DIAR] load failed for %s: %s", mid, e, exc_info=True)

    raise RuntimeError(f"Failed to load pyannote pipeline. Last error: {last_err}")


def run_diarization(audio_path: str) -> List[Dict]:
    """
    Returns: [{"start": float, "end": float, "speaker": "SPEAKER_00"}, ...]
    """
    # 무거운 의존성은 함수 내부 임포트 (모듈 임포트 시 실패 방지)
    token = _load_hf_token_from_file()
    if not token:
        raise RuntimeError("HuggingFace token not found. Set HUGGINGFACE_TOKEN or provide keys/Huggingface_token.txt")

    pipeline = _load_pipeline(token)

    # 스피커 수 고정 옵션 (없으면 자동 추정)
    num_speakers_env = os.getenv("NUM_SPEAKERS")
    try:
        if num_speakers_env and num_speakers_env.isdigit():
            diar = pipeline(audio_path, num_speakers=int(num_speakers_env))
        else:
            diar = pipeline(audio_path)
    except Exception as e:
        log.error("[DIAR] pipeline call failed: %s", e, exc_info=True)
        raise

    results: List[Dict] = []

    # 버전에 따라 메서드가 달라서 순차 폴백
    if hasattr(diar, "itertracks"):
        for segment, _, label in diar.itertracks(yield_label=True):
            results.append({
                "start": float(segment.start),
                "end": float(segment.end),
                "speaker": str(label)
            })
    elif hasattr(diar, "iter_segments"):
        for segment, label in diar.iter_segments(label=True):
            results.append({
                "start": float(segment.start),
                "end": float(segment.end),
                "speaker": str(label)
            })
    else:
        timeline = getattr(diar, "get_timeline", lambda: [])()
        for seg in timeline:
            results.append({
                "start": float(getattr(seg, "start", 0.0)),
                "end": float(getattr(seg, "end", 0.0)),
                "speaker": "UNKNOWN"
            })

    # 스피커명 정규화
    speaker_map: Dict[str, str] = {}
    next_id = 0
    for r in results:
        spk = r["speaker"]
        if spk not in speaker_map:
            speaker_map[spk] = f"SPEAKER_{next_id:02d}"
            next_id += 1
        r["speaker"] = speaker_map[spk]

    results.sort(key=lambda x: (x.get("start", 0.0), x.get("end", 0.0)))
    return results


__all__ = ["run_diarization"]
