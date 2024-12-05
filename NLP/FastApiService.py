# semantic_search.py

# Dependencies
# pip install fastapi uvicorn sentence-transformers faiss-cpu

from fastapi import FastAPI, HTTPException, BackgroundTasks
from sentence_transformers import SentenceTransformer
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.decomposition import TruncatedSVD, IncrementalPCA
from sklearn.cluster import MiniBatchKMeans
from sklearn.metrics.pairwise import cosine_similarity
import matplotlib.pyplot as plt
from io import BytesIO
import base64
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

def build_co_occurrence_matrix(documents: List[str], window_size: int = 5):
    vectorizer = CountVectorizer(stop_words="english")
    word_matrix = vectorizer.fit_transform(documents)
    vocab = vectorizer.get_feature_names_out()  # Array of words
    vocab_size = len(vocab)
    
    co_occurrence_matrix = np.zeros((vocab_size, vocab_size), dtype=np.float32)
    
    # For each document
    for doc in documents:
        words = doc.split()
        for i, word in enumerate(words):
            # Find the index of the word in vocab using np.where
            word_idx = np.where(vocab == word)[0]
            if word_idx.size > 0:  # Ensure the word is in the vocabulary
                word_idx = word_idx[0]
                # Create co-occurrence with the surrounding words
                for j in range(max(0, i - window_size), min(len(words), i + window_size + 1)):
                    if i != j:
                        neighbor_word_idx = np.where(vocab == words[j])[0]
                        if neighbor_word_idx.size > 0:
                            co_occurrence_matrix[word_idx, neighbor_word_idx[0]] += 1
    return co_occurrence_matrix, vocab

def apply_svd(co_occurrence_matrix, n_components=100):
    svd = TruncatedSVD(n_components=n_components)
    reduced_matrix = svd.fit_transform(co_occurrence_matrix)
    return reduced_matrix    

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
    Index documents in batches, using both Sentence-BERT embeddings and co-occurrence matrix + SVD for indexing.
    """
    total_docs = len(documents)
    logger.info(f"Starting to index {total_docs} documents in batches of {batch_size}.")
    
    new_documents = []  # To store documents without duplicate IDs
    new_ids = set()     # To track IDs being added in this batch
    
    with lock:
        existing_ids = set(document_ids)  # Convert document_ids to a set for efficient lookup
    
    for doc in documents:
        doc_id = doc["id"]
        if doc_id not in existing_ids and doc_id not in new_ids:  # Check for duplicates
            new_documents.append(doc)
            new_ids.add(doc_id)

    # Generate embeddings and Co-occurrence matrix
    all_embeddings = []
    all_co_occurrences = []

    for doc in new_documents:
        text = doc["text"]
        embeddings = model.encode(text)
        co_occurrence_matrix, vocab = build_co_occurrence_matrix([text])  # Co-occurrence for each document
        reduced_matrix = apply_svd(co_occurrence_matrix)
        
        all_embeddings.append(embeddings)
        all_co_occurrences.append(reduced_matrix.flatten())  # Flatten SVD result

    # Convert to numpy arrays and add to the FAISS index
    with lock:
        index.add(np.array(all_embeddings, dtype="float32"))
        document_ids.extend([doc["id"] for doc in new_documents])
    
    logger.info(f"Indexed {len(new_documents)} new documents in FAISS HNSW successfully.")
    save_incremental()  # Save after batch indexing
    return {"message": "Documents indexed with embeddings and co-occurrence + SVD successfully."}

@app.get("/search/")
async def search(query: str, top_k: int = 5, use_svd: bool = False):
    """
    Search the FAISS HNSW index for the most similar documents to the query.
    Optionally, use SVD-based document representation.
    """
    logger.info(f"Received search query: '{query}' with top_k={top_k}")

    if len(document_ids) == 0:
        logger.warning("Search attempted, but no documents are indexed.")
        raise HTTPException(status_code=404, detail="No documents indexed")

    query_embedding = model.encode(query)
    logger.info(f"Generated embedding for search query: {query_embedding}")

    # Perform search using FAISS
    distances, indices = index.search(np.array([query_embedding], dtype="float32"), top_k)
    logger.info(f"FAISS search complete. Distances: {distances[0]}, Indices: {indices[0]}")

    results = []
    with lock:
        for dist, i in zip(distances[0], indices[0]):
            if i < len(document_ids):  # Check index bounds
                results.append({"id": document_ids[i], "score": float(dist)})
                logger.info(f"Match found - ID: {document_ids[i]}, Distance: {dist}")

    if use_svd:
        logger.info("Using SVD for query-document similarity.")
        
        # Transform the query embedding using the same SVD
        query_vector = np.array([query_embedding], dtype="float32")
        reduced_query = svd.transform(query_vector)
        
        # Retrieve SVD-reduced document embeddings
        with lock:
            num_docs = index.ntotal
            embeddings = np.zeros((num_docs, index.d), dtype=np.float32)
            index.reconstruct_n(0, num_docs, embeddings)
        
        reduced_embeddings = svd.transform(embeddings)
        
        # Compute cosine similarity between query and documents
        similarity_scores = np.dot(reduced_embeddings, reduced_query.T).flatten()
        similarity_indices = np.argsort(-similarity_scores)  # Sort in descending order
        
        # Retrieve top-k results
        results = []
        for idx in similarity_indices[:top_k]:
            if idx < len(document_ids):  # Check bounds
                results.append({"id": document_ids[idx], "score": float(similarity_scores[idx])})
                logger.info(f"SVD Match - ID: {document_ids[idx]}, Similarity: {similarity_scores[idx]}")
        
        return results if results else {"message": "No matching documents found."}

@app.post("/cluster/")
async def cluster_documents(n_clusters: int, background_tasks: BackgroundTasks):
    background_tasks.add_task(run_clustering, n_clusters)
    return {"message": "Clustering started in the background. Check logs for updates."}

def run_clustering(n_clusters: int):
    """
    Cluster the indexed documents into 'n_clusters' groups.
    """
    logger.info(f"Clustering {len(document_ids)} documents into {n_clusters} clusters.")

    # Prepare the cluster file path
    cluster_file = "document_clusters.json"
    # Parameters for batch processing
    batch_size = 100
    reduced_dim = 128
    
    try:
        # Check if documents exist
        with lock:
            if len(document_ids) == 0:
                raise HTTPException(status_code=404, detail="No documents indexed to cluster.")

        # Initialize PCA for dimensionality reduction
        ipca = IncrementalPCA(n_components=reduced_dim)
        reduced_embeddings = []

        # Process embeddings in batches
        embeddings = np.zeros((index.ntotal, index.d), dtype=np.float32)
        index.reconstruct_n(0, index.ntotal, embeddings)

        for start in range(0, index.ntotal, batch_size):
            end = min(start + batch_size, index.ntotal)
            batch = embeddings[start:end]
            reduced_batch = ipca.partial_fit_transform(batch)
            reduced_embeddings.append(reduced_batch)

        reduced_embeddings = np.vstack(reduced_embeddings)

        # Clustering with MiniBatchKMeans
        kmeans = MiniBatchKMeans(n_clusters=n_clusters, batch_size=batch_size, max_iter=100, random_state=42)
        cluster_labels = kmeans.fit_predict(reduced_embeddings)

        # Map documents to clusters
        clustered_docs = {doc_id: int(label) for doc_id, label in zip(document_ids, cluster_labels)}

        # Save cluster assignments to a file
        with open(cluster_file, "w") as f:
            json.dump(clustered_docs, f)

        logger.info(f"Clustering complete. Saved cluster assignments to {cluster_file}.")
        return {"message": "Clustering complete.", "cluster_file": cluster_file}

    except MemoryError:
        logger.error("MemoryError: Clustering failed due to insufficient memory.")
        raise HTTPException(status_code=500, detail="Clustering failed due to insufficient memory.")
    except Exception as e:
        logger.error(f"Error during clustering: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Clustering failed: {str(e)}")

@app.post("/evaluate_co_occurrence/")
async def evaluate_co_occurrence_endpoint(documents: List[str], n_components: int = 5):
    """
    Evaluate the co-occurrence matrix and its reduced SVD representation.
    
    Args:
    - documents: List of document texts.
    - n_components: Number of components for SVD reduction.

    Returns:
    - Variance metrics and a base64-encoded scatter plot image.
    """
    if not documents:
        return {"error": "No documents provided for evaluation"}

    # Build co-occurrence matrix
    co_occurrence_matrix, vocab = build_co_occurrence_matrix(documents)
    reduced_matrix = apply_svd(co_occurrence_matrix, n_components)

    # Compute cosine similarities
    raw_similarity = cosine_similarity(co_occurrence_matrix)
    reduced_similarity = cosine_similarity(reduced_matrix)

    # Flatten matrices
    raw_flat = raw_similarity.flatten()
    reduced_flat = reduced_similarity.flatten()

    # Create scatter plot
    plt.figure(figsize=(10, 6))
    plt.scatter(raw_flat, reduced_flat, alpha=0.3, s=10)
    plt.title("Raw vs. SVD-Reduced Co-occurrence Matrix Cosine Similarities")
    plt.xlabel("Raw Matrix Similarity")
    plt.ylabel("Reduced Matrix Similarity")
    plt.grid(True)

    # Save plot to a bytes buffer
    buf = BytesIO()
    plt.savefig(buf, format="png")
    buf.seek(0)
    plt.close()

    # Encode plot as base64
    plot_base64 = base64.b64encode(buf.read()).decode('utf-8')
    buf.close()

    # Compute variance metrics
    raw_variance = np.var(raw_similarity).item()  # Convert to Python float
    reduced_variance = np.var(reduced_similarity).item()  # Convert to Python float

    return {
        "raw_variance": raw_variance,
        "reduced_variance": reduced_variance,
        "plot_base64": plot_base64,
    }    

