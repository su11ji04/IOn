from pyannote.audio import Pipeline
import os

def load_Huggingface_token_from_file(path=None):
    if path is None:
        # 현재 파일 위치 기준으로 keys 폴더 찾기
        base_dir = os.path.dirname(os.path.abspath(__file__))
        path = os.path.join(base_dir, "keys", "Huggingface_token.txt")
    with open(path, "r", encoding="utf-8") as f:
        return f.read().strip()


def run_diarization(audio_path):
    token = load_Huggingface_token_from_file()
    pipeline = Pipeline.from_pretrained("pyannote/speaker-diarization", use_auth_token=token)
    diarization = pipeline(audio_path, num_speakers=2)
    return diarization