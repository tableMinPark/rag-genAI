from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import os
import torch
from sentence_transformers import CrossEncoder

MODEL_NAME = os.getenv(
    "MODEL_NAME", 
    "BAAI/bge-reranker-base"
)

app = FastAPI(title="Reranker API")
model = CrossEncoder(MODEL_NAME)

class Document(BaseModel):
    id: str
    content: str

class RerankRequest(BaseModel):
    query: str
    documents: List[Document]
    top_k: int = 5

class RerankResult(BaseModel):
    id: str
    content: str
    score: float

class RerankResponse(BaseModel):
    documents: List[RerankResult]

@app.post("/rerank", response_model=RerankResponse)
def rerank(req: RerankRequest):
    pairs = [(req.query, document.content) for document in req.documents]

    with torch.no_grad():
        scores = model.predict(pairs)

    ranked = sorted(
        zip(req.documents, scores),
        key=lambda x: x[1],
        reverse=True
    )

    documents = [
        RerankResult(id=document.id, content=document.content, score=float(score))
        for document, score in ranked[: req.top_k]
    ]

    return RerankResponse(documents=documents)
