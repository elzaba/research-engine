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

# Configure logging to output to the console
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = FastAPI()

# Load a pre-trained model to generate embeddings for text data
model = SentenceTransformer('paraphrase-MiniLM-L6-v2')
embedding_dim = model.get_sentence_embedding_dimension()  # Dimension of the embeddings

# Initialize FAISS index, a library used for efficient similarity search
index = faiss.IndexFlatL2(embedding_dim)
document_ids = []  # Keeps track of the document IDs associated with each embedding
lock = Lock()  # Ensures that indexing operations are thread-safe

# Define file paths for saving the FAISS index and document IDs to disk
INDEX_FILE = "faiss_index.bin"
IDS_FILE = "document_ids.json"

@app.on_event("startup")
def load_faiss_index():
    """
    Loads the FAISS index and document IDs from disk when the app starts.
    If no files are found, starts with an empty index and ID list.
    """
    global index, document_ids
    if os.path.exists(INDEX_FILE):
        index = faiss.read_index(INDEX_FILE)  # Loads the FAISS index from file
        logger.info("FAISS index loaded successfully.")
    else:
        logger.info("No existing FAISS index found. Initializing a new index.")

    if os.path.exists(IDS_FILE):
        with open(IDS_FILE, "r") as f:
            document_ids = json.load(f)  # Loads document IDs from file
        logger.info("Document IDs loaded successfully.")
    else:
        logger.info("No existing document IDs found. Starting with an empty list.")

@app.on_event("shutdown")
def save_faiss_index():
    """
    Saves the FAISS index and document IDs to disk when the app shuts down.
    """
    faiss.write_index(index, INDEX_FILE)
    logger.info("FAISS index saved successfully.")

    with open(IDS_FILE, "w") as f:
        json.dump(document_ids, f)
    logger.info("Document IDs saved successfully.")

@app.post("/index_documents/")
async def index_documents(documents: List[dict]):
    """
    Indexes a list of documents with FAISS. Each document must have an 'id' and 'text' field.
    """
    # Generate embeddings for each document's text
    embeddings = [model.encode(doc["text"]) for doc in documents]
    ids = [doc["id"] for doc in documents]
    
    # Log each embedding generated for observability
    for i, embedding in enumerate(embeddings):
        logger.info(f"Generated embedding for document ID {ids[i]}: {embedding[:5]}...")  # Shows the first few values

    with lock:  # Ensure thread-safe access to FAISS index
        index.add(np.array(embeddings, dtype="float32"))  # Add embeddings to FAISS
        document_ids.extend(ids)  # Add document IDs to the list

    logger.info("Documents indexed successfully in FAISS.")
    return {"message": "Documents indexed in FAISS successfully"}

@app.get("/search/")
async def search(query: str, top_k: int = 5):
    """
    Searches the FAISS index for the most similar documents to the query.
    """
    # Check if any documents are indexed
    if len(document_ids) == 0:
        logger.warning("Search attempted but no documents are indexed.")
        raise HTTPException(status_code=404, detail="No documents indexed")

    # Generate the embedding for the query text
    query_embedding = model.encode(query)
    logger.info(f"Generated embedding for search query: {query_embedding[:5]}...")  # Shows the first few values

    # Perform similarity search on the FAISS index
    distances, indices = index.search(np.array([query_embedding], dtype="float32"), top_k)
    
    results = []
    with lock:
        for dist, i in zip(distances[0], indices[0]):
            if i < len(document_ids):  # Ensure index is within bounds
                results.append({"id": document_ids[i], "score": float(dist)})
                logger.info(f"Match found - ID: {document_ids[i]}, Distance: {dist}")

    if not results:
        logger.info("No matching documents found for the query.")

    return results


