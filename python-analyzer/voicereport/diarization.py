# voicereport/diarization.py
from typing import List, Dict, Optional
import os
import logging
import os.path as _p

log = logging.getLogger("uvicorn.error")

HF_TOKEN_ENV = "HUGGINGFACE_TOKEN"
HF_TOKEN_FILENAME = "huggingface_token.txt"
NUM_SPEAKERS_ENV = "NUM_SPEAKERS"


# HUGGINGFACE TOKEN LOAD
def _load_hf_token_from_file(default_path: Optional[str] = None) -> Optional[str]:
    # 1) ENV
    env = os.getenv(HF_TOKEN_ENV)
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
        base_dir = _p.dirname(_p.abspath(__file__))
        path_pkg = default_path or _p.join(base_dir, "keys", HF_TOKEN_FILENAME)
        if _p.exists(path_pkg):
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
        project_root = _p.dirname(_p.dirname(_p.abspath(__file__)))
        path_root = _p.join(project_root, "keys", HF_TOKEN_FILENAME)
        if _p.exists(path_root):
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


# PipeLine Load
def _load_pipeline(token: str):
    from pyannote.audio import Pipeline

    last_err = None
    model_ids = [
        "pyannote/speaker-diarization-3.1",
        "pyannote/speaker-diarization",
    ]

    for mid in model_ids:
        try:
            log.info("[DIAR] loading model: %s", mid)
            try:
                pl = Pipeline.from_pretrained(mid, use_auth_token=token)
            except TypeError:
                pl = Pipeline.from_pretrained(mid, token=token)

            if callable(pl):
                log.info("[DIAR] model loaded ok: %s", mid)
                return pl
            log.error("[DIAR] non-callable pipeline for %s: %r", mid, type(pl))
        except Exception as e:
            last_err = e
            log.error("[DIAR] load failed for %s: %s", mid, e, exc_info=True)

    raise RuntimeError(f"Failed to load pyannote pipeline. Last error: {last_err}")


# 화자 분리
def run_diarization(audio_path: str) -> List[Dict]:
    # AUDIO FILE ERROR CHECK
    if not _p.exists(audio_path):
        raise FileNotFoundError(f"Audio file not found: {audio_path}")
    if not _p.isfile(audio_path):
        raise RuntimeError(f"Audio path is not a file: {audio_path}")

    token = _load_hf_token_from_file()
    if not token:
        raise RuntimeError(
            f"HuggingFace token not found. Set {HF_TOKEN_ENV} or provide keys/{HF_TOKEN_FILENAME}"
        )

    # Pipeline Load
    try:
        pipeline = _load_pipeline(token)
    except Exception as e:
        log.error("[DIAR] pipeline load failed: %s", e, exc_info=True)
        raise
    num_speakers_env = os.getenv(NUM_SPEAKERS_ENV)
    try:
        num_speakers: Optional[int] = None
        if num_speakers_env:
            try:
                num_speakers = int(num_speakers_env.strip())
                if num_speakers <= 0:
                    num_speakers = None
            except ValueError:
                num_speakers = None

        if num_speakers is not None:
            diar = pipeline(audio_path, num_speakers=num_speakers)
        else:
            diar = pipeline(audio_path)
    except Exception as e:
        log.error(
            "[DIAR] pipeline inference failed: %s (audio=%s, num_speakers_env=%r)",
            e, audio_path, num_speakers_env, exc_info=True
        )
        raise

    results: List[Dict] = []

    # FALLBACK
    if hasattr(diar, "itertracks"):
        for segment, _, label in diar.itertracks(yield_label=True):
            lab = (str(label).strip() if label is not None else "") or "UNKNOWN"
            results.append({
                "start": float(segment.start),
                "end": float(segment.end),
                "speaker": lab,
            })
    elif hasattr(diar, "iter_segments"):
        for segment, label in diar.iter_segments(label=True):
            lab = (str(label).strip() if label is not None else "") or "UNKNOWN"
            results.append({
                "start": float(segment.start),
                "end": float(segment.end),
                "speaker": lab,
            })
    else:
        timeline = getattr(diar, "get_timeline", lambda: [])()
        for seg in timeline:
            results.append({
                "start": float(getattr(seg, "start", 0.0)),
                "end": float(getattr(seg, "end", 0.0)),
                "speaker": "UNKNOWN",
            })

    # 이상치 보정
    for r in results:
        if r["end"] < r["start"]:
            r["start"], r["end"] = r["end"], r["start"]

    # SPEAKER 라벨 정규화
    speaker_map: Dict[str, str] = {}
    next_id = 0
    for r in results:
        spk = r.get("speaker") or "UNKNOWN"
        if spk not in speaker_map:
            speaker_map[spk] = f"SPEAKER_{next_id:02d}"
            next_id += 1
        r["speaker"] = speaker_map[spk]

    # 정렬
    results.sort(key=lambda x: (x.get("start", 0.0), x.get("end", 0.0)))
    return results


__all__ = ["run_diarization"]
