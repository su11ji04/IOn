# python-analyzer/claude_request.py
import os
import json
import re
import logging
from typing import Any, Dict, List
import requests

log = logging.getLogger("uvicorn.error")

# ──────────────────────────────────────────────────────────────────────────────
# Prompt builders
# ──────────────────────────────────────────────────────────────────────────────

def build_prompt(dialogue: str, user_info: dict) -> str:
    # JSON 스키마를 명시하고 "오직 JSON만" 강하게 요구
    schema = r"""
반드시 아래 스키마의 단일 JSON 객체만 출력하세요. 설명/마크다운/코드펜스 금지.

{
  "subTitle": string | null,
  "day": string | null,
  "conversationSummary": string | null,
  "length": integer | null,
  "overallFeedback": string | null,
  "frequency": {
    "parentFrequency": integer | null,
    "kidFrequency": integer | null,
    "frequencyFeedback": string | null
  } | null,
  "expression": {
    "parentExpression": string | null,
    "kidExpression": string | null,
    "parentConditions": string | null,
    "kidConditions": string | null,
    "expressionFeedback": string | null
  } | null,
  "emotion": {
    "timeline": [
      { "time": "MM:SS", "momentEmotion": string | null }
    ] | [] | null,
    "emotionFeedback": string | null
  } | null,
  "kidAttitude": string | null,
  "changeProposal": [
    { "existingExpression": string | null, "proposalExpression": string | null }
  ] | [] | null,
  "pattern": string | null,
  "strength": string | null
}
""".strip()

    return (
        "당신은 초보 부모를 돕는 감정·대화 분석 어시스턴트입니다.\n"
        "주어진 대화를 분석하여 위 **스키마에 정확히 맞는 JSON 하나만** 출력하세요.\n"
        "- 수치값: frequency.*는 0~100 정수 또는 null\n"
        "- 시간 포맷: 'MM:SS' (어려우면 timeline은 빈 배열)\n"
        "- 자연어는 한국어, 모르면 null/[]\n\n"
        f"[사용자 정보]\n{json.dumps(user_info, ensure_ascii=False)}\n\n"
        f"[대화]\n{dialogue}\n\n"
        f"[출력 스키마]\n{schema}\n"
        "※ 오직 JSON만 출력하세요."
    )

_SYSTEM_JSON_ONLY = (
    "You are a JSON-only generator. Return a single valid JSON object and nothing else. "
    "No markdown, no code fences, no commentary."
)

# ──────────────────────────────────────────────────────────────────────────────
# Key loading
# ──────────────────────────────────────────────────────────────────────────────

def load_claude_key_from_file(default_path=None):
    # 1) ENV
    env = os.getenv("ANTHROPIC_API_KEY")
    if env and env.strip():
        try: log.info("[CLAUDE] key loaded from ENV (****%s)", env[-6:])
        except: pass
        return env.strip()

    # 2) 패키지(local) keys
    try:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path_pkg = default_path or os.path.join(base_dir, "keys", "claude_key.txt")
        if os.path.exists(path_pkg):
            with open(path_pkg, "r", encoding="utf-8") as f:
                key = f.read().strip()
            if key:
                try: log.info("[CLAUDE] key loaded from %s (****%s)", path_pkg, key[-6:])
                except: pass
                return key
    except Exception as e:
        log.warning("[CLAUDE] package key read failed: %s", e)

    # 3) 프로젝트 루트 /keys
    try:
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path_root = os.path.join(project_root, "keys", "claude_key.txt")
        if os.path.exists(path_root):
            with open(path_root, "r", encoding="utf-8") as f:
                key = f.read().strip()
            if key:
                try: log.info("[CLAUDE] key loaded from %s (****%s)", path_root, key[-6:])
                except: pass
                return key
    except Exception as e:
        log.warning("[CLAUDE] root key read failed: %s", e)

    return None

# ──────────────────────────────────────────────────────────────────────────────
# JSON helpers
# ──────────────────────────────────────────────────────────────────────────────

def _default_report() -> Dict[str, Any]:
    return {
        "subTitle": None, "day": None, "conversationSummary": None, "length": None,
        "overallFeedback": None,
        "frequency": {"parentFrequency": None, "kidFrequency": None, "frequencyFeedback": None},
        "expression": {"parentExpression": None, "kidExpression": None,
                       "parentConditions": None, "kidConditions": None, "expressionFeedback": None},
        "emotion": {"timeline": [], "emotionFeedback": None},
        "kidAttitude": None,
        "changeProposal": [],
        "pattern": None, "strength": None
    }

def _extract_json_loose(text: str) -> Dict[str, Any]:
    """가장 바깥 JSON 객체 하나를 괄호 카운팅으로 추출."""
    start = text.find("{")
    if start == -1:
        raise ValueError("no '{' found")
    depth = 0
    for i in range(start, len(text)):
        c = text[i]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                blob = text[start:i+1]
                return json.loads(blob)
    raise ValueError("unterminated JSON")

# ──────────────────────────────────────────────────────────────────────────────
# Anthropic call
# ──────────────────────────────────────────────────────────────────────────────

def request_claude(prompt: str) -> dict:
    # MOCK 빠른 테스트용
    if os.getenv("MOCK_CLAUDE") == "1":
        d = _default_report()
        d.update({
            "subTitle": "모의 분석 리포트",
            "conversationSummary": "테스트 요약입니다.",
            "length": 60,
            "overallFeedback": "모의 결과"
        })
        log.info("[CLAUDE] MOCK enabled")
        return d

    api_key = os.getenv("ANTHROPIC_API_KEY") or load_claude_key_from_file()
    if not api_key:
        raise RuntimeError("Claude API key not found. Set ANTHROPIC_API_KEY or provide keys/claude_key.txt")

    model = os.getenv("CLAUDE_MODEL", "claude-3-haiku-20240307")  # 필요시 환경변수로 교체 가능
    api_url = "https://api.anthropic.com/v1/messages"
    headers = {
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    payload = {
        "model": model,
        "max_tokens": 1200,
        "temperature": 0.2,
        "system": _SYSTEM_JSON_ONLY,
        "messages": [{"role": "user", "content": prompt}],
    }

    try:
        r = requests.post(api_url, headers=headers, json=payload, timeout=120)
        status = r.status_code
        if status // 100 != 2:
            # 에러 본문 남기고 기본 스키마 반환
            preview = (r.text or "")[:300].replace("\n", " ")
            log.error("[CLAUDE] HTTP %s: %s", status, preview)
            d = _default_report()
            d["overallFeedback"] = f"LLM HTTP {status}: {preview}"
            return d

        obj = r.json()
        blocks: List[Dict[str, Any]] = obj.get("content", []) if isinstance(obj, dict) else []
        # Anthropic는 [{"type":"text","text":"..."}] 형태가 일반적
        texts = []
        for b in blocks:
            if isinstance(b, dict):
                t = b.get("text")
                if isinstance(t, str) and t.strip():
                    texts.append(t)
        raw = "\n".join(texts).strip()
        log.info("[CLAUDE] raw.len=%d, model=%s", len(raw), model)

        # 1차: 바로 json.loads
        try:
            return json.loads(raw)
        except Exception:
            pass

        # 2차: 코드펜스/잡텍스트 제거 후 재시도
        cleaned = raw.strip().strip("`").strip()
        try:
            return json.loads(cleaned)
        except Exception:
            pass

        # 3차: 괄호 카운팅으로 JSON 블록 추출
        try:
            return _extract_json_loose(raw)
        except Exception as e:
            log.warning("[CLAUDE] JSON extract failed: %s; raw preview=%s",
                        e, raw[:300].replace("\n", " "))

        # 마지막: 기본 스키마 반환(리포트가 비지 않도록)
        d = _default_report()
        d["overallFeedback"] = "LLM 응답을 JSON으로 파싱하지 못했습니다."
        return d

    except requests.Timeout:
        d = _default_report()
        d["overallFeedback"] = "LLM 요청이 타임아웃되었습니다."
        log.error("[CLAUDE] timeout")
        return d
    except Exception as e:
        d = _default_report()
        d["overallFeedback"] = f"LLM 호출 오류: {e}"
        log.error("[CLAUDE] call failed: %s", e, exc_info=True)
        return d
