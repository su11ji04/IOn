import os


def map_speaker_segments(segments, diarization):
    speaker_turns = []
    for turn, _, speaker in diarization.itertracks(yield_label=True):
        matched_segments = [
            s["text"] for s in segments if not (s["end"] < turn.start or turn.end < s["start"])
        ]
        if matched_segments:
            text = "".join(matched_segments).strip()
            speaker_turns.append((speaker, text))
    return speaker_turns