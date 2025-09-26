# workbook/preprocess_data.py
import pandas as pd
import tiktoken

def load_and_clean_text(filepath):
    df = pd.read_csv(filepath)
    # 두 번째 컬럼 본문 가정. 컬럼 수 체크
    col_idx = 1 if df.shape[1] > 1 else 0
    raw_text = " ".join(df.iloc[:, col_idx].astype(str))
    cleaned_text = raw_text.replace("\n", " ").replace("\xa0", " ").strip()
    return cleaned_text

def split_text(text, max_tokens=1200):
    enc = tiktoken.encoding_for_model("gpt-4")
    tokens = enc.encode(text)
    chunks = []
    i = 0
    while i < len(tokens):
        chunk = tokens[i:i+max_tokens]
        i += max_tokens
        chunks.append(enc.decode(chunk))
    return chunks
