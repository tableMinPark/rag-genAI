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

class Context(BaseModel):
    id: int
    content: str

class ConvertContext(BaseModel):
    id: int
    content: str
    vector: list[float]

@app.post("/embed", response_model=list[ConvertContext])
def embed(contexts: list[Context]):

    embeddings = model.encode(
        [context.content for context in contexts],
        normalize_embeddings=True
    )

    convertContexts = []
    vectors = embeddings.tolist()

    for i, context in enumerate(contexts):
        convertContexts.append(ConvertContext(id=context.id, content=context.content, vector=vectors[i]))

    return convertContexts
