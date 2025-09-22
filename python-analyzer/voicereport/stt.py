import whisper

def run_stt(audio_path):
    model = whisper.load_model("small")
    result = model.transcribe(audio_path, language="ko")
    return result.get("segments", [])