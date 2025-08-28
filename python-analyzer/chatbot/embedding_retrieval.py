import os
import pandas as pd
import numpy as np
from sklearn.preprocessing import normalize
from sentence_transformers import SentenceTransformer
import faiss

# CSV 기반 유사 문단 검색
def retrieve_relevant_context(question: str, csv_path: str = "data/ocr_paragraphs.csv", k: int = 2):

    # CSV 파일에서 문단 불러오기
    df = pd.read_csv(csv_path)
    paragraphs = df["content"].tolist()

    # SBERT 모델 로딩 및 임베딩
    model = SentenceTransformer('snunlp/KR-SBERT-V40K-klueNLI-augSTS')
    corpus_embeddings = model.encode(paragraphs)
    corpus_embeddings = normalize(corpus_embeddings, norm='l2')

    question_embedding = model.encode([question])
    question_embedding = normalize(question_embedding, norm='l2')

    # FAISS 인덱스 구성 및 검색
    index = faiss.IndexFlatIP(corpus_embeddings.shape[1])
    index.add(corpus_embeddings)

    _, I = index.search(question_embedding, k)
    top_paragraphs = [paragraphs[i] for i in I[0]]

    final_doc = "\n".join(top_paragraphs)

    for i, p in enumerate(top_paragraphs):
        print(f"\n--- 문단 {i+1} ---\n{p}")


    return final_doc