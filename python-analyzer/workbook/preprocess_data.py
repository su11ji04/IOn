# workbook/preprocess_data.py
import pandas as pd
import tiktoken

def load_and_clean_text(filepath):
    df = pd.read_csv(filepath)
    raw_text = " ".join(df.iloc[:, 1].astype(str))  # 두 번째 컬럼 본문 가정
    cleaned_text = raw_text.replace("\n", " ").replace("\xa0", " ").strip()
    return cleaned_text

def split_text(text, max_tokens=1200):
    enc = tiktoken.encoding_for_model("gpt-4")
    tokens = enc.encode(text)
    chunks = []
    while tokens:
        chunk = tokens[:max_tokens]
        tokens = tokens[max_tokens:]
        chunks.append(enc.decode(chunk))
    return chunks
