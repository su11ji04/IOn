import os
import json
import logging
from typing import Any, Dict, List, Optional
import requests

log = logging.getLogger("uvicorn.error")

_SYSTEM_JSON_ONLY = (
    "You are a JSON-only generator. Return a single valid JSON object and nothing else. "
    "No markdown, no code fences, no commentary."
)

# VOICEREPORT 기본값
def _default_report() -> Dict[str, Any]:
    return {
        "subTitle": None,
        "day": None,
        "conversationSummary": None,
        "overallFeedback": None,
        "expression": {
            "parentExpression": None,
            "kidExpression": None,
            "parentConditions": None,
            "kidConditions": None,
            "expressionFeedback": None,
        },
        "changeProposal": [],
        "emotion": {
            "timeline": [],
            "emotionFeedback": None,
        },
        "kidAttitude": None,
        "frequency": {
            "parentFrequency": None,
            "kidFrequency": None,
            "frequencyFeedback": None,
        },
        "strength": None,
    }

# 프롬프트 구성
def build_prompt(dialogue: str, user_info: dict) -> str:
    schema = r"""
반드시 아래 스키마의 단일 JSON 객체만 출력하세요. 설명/마크다운/코드펜스 금지.

{
  "subTitle": string | null,
  "day": string | null,
  "conversationSummary": string | null,
  "overallFeedback": string | null,
  "expression": {
    "parentExpression": string | null,
    "kidExpression": string | null,
    "parentConditions": string | null,
    "kidConditions": string | null,
    "expressionFeedback": string | null
  } | null,
  "changeProposal": [
    { "existingExpression": string | null, "proposalExpression": string | null }
  ] | [] | null,
  "emotion": {
    "timeline": [
      { "time": "MM:SS", "momentEmotion": string | null }
    ] | [] | null,
    "emotionFeedback": string | null
  } | null,
  "kidAttitude": string | null,
  "frequency": {
    "parentFrequency": integer | null,
    "kidFrequency": integer | null,
    "frequencyFeedback": string | null
  } | null,
  "strength": string | null
}
""".strip()

    age = user_info.get("child_age")
    style = user_info.get("parenting_style")
    goal = user_info.get("parenting_goal")
    traits = user_info.get("child_traits")
    tone = user_info.get("preferred_tone")
    health = user_info.get("health_issues")

    age_hint = ""
    try:
        if isinstance(age, int):
            if 0 <= age <= 2:
                age_hint = ("[연령 가이드: 0-2세] 애착·정서 안정 중심. 즉각적 반응, 신체적 접촉(안아주기·눈맞춤), "
                            "간단한 감정 언어 반복(예: '기뻐', '슬퍼'). 훈육보다 안전·일관성 확보가 핵심.")
            elif 3 <= age <= 6:
                age_hint = ("[연령 가이드: 3-6세] 감정 명명, 선택권 2개 제시, 짧고 단순한 지시문, "
                            "시각적 큐(그림·차트) 사용, 긍정적 강화. 타임아웃 대신 '쿨다운 코너(분=나이)'.")
            elif age == 7:
                age_hint = ("[연령 가이드: 7세] 규칙 사전 합의+간단 시각 차트, 자연적 결과 활용, "
                            "문제 해결 단계(정의→아이디어→선택→실행) 짧게 코칭.")
    except Exception:
        pass

    style_hint = f"[양육스타일 참고: {style}]" if style else ""
    goal_hint = f"[양육목표: {goal}]" if goal else ""
    traits_hint = f"[아이 성향: {traits}]" if traits else ""
    tone_hint = f"[권장 톤: {tone}]" if tone else ""
    health_hint = f"[건강 이슈 주의: {health}]" if health else ""

    personalization = " / ".join(
        [h for h in [age_hint, style_hint, goal_hint, traits_hint, tone_hint, health_hint] if h]
    )

    return (
        "당신은 초보 부모를 돕는 감정·대화 분석 어시스턴트입니다.\n"
        "반드시 **위 스키마에 정확히 맞는 JSON 하나**만 출력하세요. 다른 텍스트 금지.\n"
        "- 자연어는 한국어, 모르면 null/[]\n"
        "- 수치값: frequency.*는 0~100 정수 또는 null (소수점 금지)\n"
        "- 시간 포맷: 'MM:SS' (불가하면 emotion.timeline은 빈 배열)\n"
        "- 길이(length)는 초 단위 정수. 추정 불가하면 null\n"
        "- **개인화**: 사용자 정보(연령/양육목표/스타일/아이 성향/건강/톤)를 적극 반영하고, 일반론/모호한 조언 금지\n"
        "- **구체성/측정가능성** 준수\n"
        "\n"
        "필드별 작성 규칙(스키마 유지):\n"
        "• conversationSummary: 2~3문장, 핵심 흐름 + 짧은 직접 인용 1~2개 포함\n"
        "• overallFeedback: 2~3문장. 목표/연령/성향 맞춤 + 측정 기준 1개 포함\n"
        "• frequency: 추정 불가 시 null, feedback은 목표 제시\n"
        "• expression: 대표 표현/트리거/대체 화법 제안\n"
        "• emotion.timeline: **부모 감정 흐름**만 3~5개, 근사 MM:SS 가능\n"
        "• emotion.emotionFeedback: 감정 조절 팁 + 목표\n"
        "• kidAttitude: 1문장 요약\n"
        "• changeProposal: 3~5쌍(기존/대체 문장)\n"
        "• strength: 1문장 강화 포인트\n"
        "\n"
        f"{personalization}\n\n"
        f"[사용자 정보]\n{json.dumps(user_info, ensure_ascii=False)}\n\n"
        f"[대화]\n{dialogue}\n\n"
        f"[출력 스키마]\n{schema}\n"
        "※ 오직 JSON만 출력하세요."
    )

# CLAUDE KEY 받아오기
def load_claude_key_from_file(default_path: Optional[str] = None) -> Optional[str]:
    env = os.getenv("ANTHROPIC_API_KEY")
    if env and env.strip():
        try:
            log.info("[CLAUDE] key loaded from ENV (****%s)", env[-6:])
        except Exception:
            pass
        return env.strip()

    try:
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path_pkg = default_path or os.path.join(base_dir, "keys", "claude_key.txt")
        if os.path.exists(path_pkg):
            with open(path_pkg, "r", encoding="utf-8") as f:
                key = f.read().strip()
            if key:
                try:
                    log.info("[CLAUDE] key loaded from %s (****%s)", path_pkg, key[-6:])
                except Exception:
                    pass
                return key
    except Exception as e:
        log.warning("[CLAUDE] package key read failed: %s", e)

    try:
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        path_root = os.path.join(project_root, "keys", "claude_key.txt")
        if os.path.exists(path_root):
            with open(path_root, "r", encoding="utf-8") as f:
                key = f.read().strip()
            if key:
                try:
                    log.info("[CLAUDE] key loaded from %s (****%s)", path_root, key[-6:])
                except Exception:
                    pass
                return key
    except Exception as e:
        log.warning("[CLAUDE] root key read failed: %s", e)

    return None

# JSON RETURN 값 정리
def _extract_json_loose(text: str) -> Dict[str, Any]:
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
                blob = text[start : i + 1]
                return json.loads(blob)
    raise ValueError("unterminated JSON")

# JSON RETURN 값 정규화
def _coerce_report(r: Any) -> Dict[str, Any]:
    base = _default_report()
    if not isinstance(r, dict):
        return base

    # base 키 업데이트
    for k in list(base.keys()):
        if k in r:
            base[k] = r[k] if r[k] is not None else base[k]

    # frequency
    freq = base.get("frequency") or {}
    for k in ("parentFrequency", "kidFrequency"):
        v = (freq or {}).get(k)
        if isinstance(v, (int, float)):
            v = int(max(0, min(100, v)))
        else:
            v = None if v is not None else None
        freq[k] = v
    if "frequencyFeedback" in (freq or {}):
        if not (isinstance(freq.get("frequencyFeedback"), str) or freq.get("frequencyFeedback") is None):
            freq["frequencyFeedback"] = None
    base["frequency"] = freq

    # emotion.timeline
    emo = base.get("emotion") or {}
    tl = emo.get("timeline")
    if tl is None:
        emo["timeline"] = []
    elif not isinstance(tl, list):
        emo["timeline"] = []
    base["emotion"] = emo

    # changeProposal
    cp = base.get("changeProposal")
    if cp is None:
        base["changeProposal"] = []
    elif not isinstance(cp, list):
        base["changeProposal"] = [cp] if cp is not None else []

    return base

# CLAUDE 분석 실행
def request_claude(prompt: str) -> dict:
    api_key = os.getenv("ANTHROPIC_API_KEY") or load_claude_key_from_file()
    if not api_key:
        d = _default_report()
        d["overallFeedback"] = "Claude API key not found. Set ANTHROPIC_API_KEY or provide keys/claude_key.txt"
        log.error("[CLAUDE] missing key")
        return d

    model = os.getenv("CLAUDE_MODEL", "claude-3-5-sonnet-20240620")
    api_url = "https://api.anthropic.com/v1/messages"
    headers = {
        "x-api-key": api_key,
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    payload = {
        "model": model,
        "max_tokens": 2000,
        "temperature": 0.2,
        "system": _SYSTEM_JSON_ONLY,
        "messages": [{"role": "user", "content": prompt}],
    }

    try:
        r = requests.post(api_url, headers=headers, json=payload, timeout=180)
        status = r.status_code
        # 요청 실패
        if status // 100 != 2:
            preview = (r.text or "")[:300].replace("\n", " ")
            log.error("[CLAUDE] HTTP %s: %s", status, preview)
            d = _default_report()
            d["overallFeedback"] = f"LLM HTTP {status}: {preview}"
            return d
        # 요청 성공
        obj = r.json()
        blocks: List[Dict[str, Any]] = obj.get("content", []) if isinstance(obj, dict) else []
        texts: List[str] = []
        for b in blocks:
            if isinstance(b, dict):
                t = b.get("text")
                if isinstance(t, str) and t.strip():
                    texts.append(t)
        raw = "\n".join(texts).strip()
        log.info("[CLAUDE] raw.len=%d, model=%s", len(raw), model)

        # LLM -> JSON
        try:
            result = json.loads(raw)
            return _coerce_report(result)
        except Exception:
            pass
        cleaned = raw.strip().strip("`").strip()
        try:
            result = json.loads(cleaned)
            return _coerce_report(result)
        except Exception:
            pass
        try:
            result = _extract_json_loose(raw)
            return _coerce_report(result)
        except Exception as e:
            log.warning("[CLAUDE] JSON extract failed: %s; raw preview=%s",
                        e, raw[:300].replace("\n", " "))
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
