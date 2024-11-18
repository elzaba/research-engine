# semantic_search.py

# Dependencies
# pip install fastapi uvicorn sentence-transformers faiss-cpu

from fastapi import FastAPI, HTTPException
from sentence_transformers import SentenceTransformer
import faiss
import numpy as np
import json
from typing import List
from threading import Lock
import os
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = FastAPI()

# Load a pre-trained model to generate embeddings for text data
logger.info("Loading SentenceTransformer model...")
model = SentenceTransformer('paraphrase-MiniLM-L6-v2')
embedding_dim = model.get_sentence_embedding_dimension()  # Dimension of the embeddings
logger.info(f"Loaded SentenceTransformer model with embedding dimension: {embedding_dim}")

# Initialize FAISS IndexHNSWFlat with on-disk option
logger.info("Initializing FAISS IndexHNSWFlat...")
index = faiss.IndexHNSWFlat(embedding_dim, 32)  # 32 is the default number of neighbors (M parameter)
index.hnsw.efConstruction = 200  # Higher values improve recall at the cost of indexing time
index.hnsw.efSearch = 50  # Trade-off between search speed and accuracy
logger.info("FAISS IndexHNSWFlat initialized with efConstruction=200 and efSearch=50.")

document_ids = []  # Keeps track of the document IDs associated with each embedding
lock = Lock()  # Ensures thread-safe operations

# Define file paths for saving the FAISS index and document IDs
INDEX_FILE = "faiss_hnsw_index.bin"
IDS_FILE = "document_ids.json"

@app.on_event("startup")
def load_faiss_index():
    """
    Load the FAISS HNSW index and document IDs from disk at startup.
    """
    global index, document_ids
    logger.info("Attempting to load FAISS HNSW index and document IDs...")
    if os.path.exists(INDEX_FILE):
        index = faiss.read_index(INDEX_FILE)  # Load index
        logger.info(f"FAISS HNSW index loaded from file: {INDEX_FILE}")
    else:
        logger.warning("No existing FAISS HNSW index found. A new index will be created.")

    if os.path.exists(IDS_FILE):
        with open(IDS_FILE, "r") as f:
            document_ids = json.load(f)  # Load document IDs
        logger.info(f"Document IDs loaded from file: {IDS_FILE}")
    else:
        logger.warning("No existing document IDs file found. Starting with an empty list.")

@app.on_event("shutdown")
def save_faiss_index():
    """
    Save the FAISS HNSW index and document IDs to disk on shutdown.
    """
    save_incremental()

def save_incremental():
    """
    Save the FAISS index and document IDs after each batch or on shutdown.
    """
    with lock:
        faiss.write_index(index, INDEX_FILE)
        with open(IDS_FILE, "w") as f:
            json.dump(document_ids, f)
        logger.info("Incremental save completed.")

@app.post("/index_documents/")
async def index_documents(documents: List[dict], batch_size: int = 100):
    """
    Index documents in batches to optimize memory usage and prevent crashes.
    """
    total_docs = len(documents)
    logger.info(f"Starting to index {total_docs} documents in batches of {batch_size}.")
    
    for i in range(0, total_docs, batch_size):
        batch = documents[i:i + batch_size]
        embeddings = [model.encode(doc["text"]) for doc in batch]
        ids = [doc["id"] for doc in batch]
        
        with lock:
            index.add(np.array(embeddings, dtype="float32"))
            document_ids.extend(ids)
        
        logger.info(f"Indexed batch {i // batch_size + 1}/{(total_docs + batch_size - 1) // batch_size}.")
        save_incremental()  # Save index and IDs after every batch
    
    return {"message": f"{total_docs} documents indexed in FAISS HNSW successfully."}

@app.get("/search/")
async def search(query: str, top_k: int = 5):
    """
    Search the FAISS HNSW index for the most similar documents to the query.
    """
    logger.info(f"Received search query: '{query}' with top_k={top_k}")
    if len(document_ids) == 0:
        logger.warning("Search attempted, but no documents are indexed.")
        raise HTTPException(status_code=404, detail="No documents indexed")

    # Generate the embedding for the query text
    query_embedding = model.encode(query)
    logger.info(f"Generated embedding for search query.")

    distances, indices = index.search(np.array([query_embedding], dtype="float32"), top_k)
    logger.info(f"FAISS search complete. Distances: {distances[0]}, Indices: {indices[0]}")

    results = []
    with lock:
        for dist, i in zip(distances[0], indices[0]):
            if i < len(document_ids):  # Check index bounds
                results.append({"id": document_ids[i], "score": float(dist)})
                logger.info(f"Match found - ID: {document_ids[i]}, Distance: {dist}")
    
    return results if results else {"message": "No matching documents found."}
