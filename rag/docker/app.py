import os
from fastapi import FastAPI
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

MODEL_NAME = os.getenv(
    "MODEL_NAME",
    "BAAI/bge-base-en-v1.5"
)

app = FastAPI(title="Embedding API")

model = SentenceTransformer(MODEL_NAME)

class EmbedRequest(BaseModel):
    texts: list[str]

class EmbedResponse(BaseModel):
    dimension: int
    vectors: list[list[float]]

@app.post("/embed", response_model=EmbedResponse)
def embed(req: EmbedRequest):
    embeddings = model.encode(
        req.texts,
        normalize_embeddings=True
    )

    return {
        "dimension": embeddings.shape[1],
        "vectors": embeddings.tolist()
    }
