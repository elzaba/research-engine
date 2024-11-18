# python -m venv env
# source env/bin/activate  # For Linux/Mac
# .\env\Scripts\activate   # For Windows
# pip install faiss-cpu annoy sentence-transformers matplotlib numpy
# python evaluate_semantic_index.py


import time
import json
import numpy as np
import faiss
from annoy import AnnoyIndex
from sentence_transformers import SentenceTransformer
import matplotlib.pyplot as plt
import logging

# Configure logging for detailed information
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Load SentenceTransformer model
model = SentenceTransformer('paraphrase-MiniLM-L6-v2')
embedding_dim = model.get_sentence_embedding_dimension()

# Load dataset and extract only required fields (id and summary)
dataset_path = "../dataset/cs_research_papers.json"
with open(dataset_path, 'r') as file:
    data = json.load(file)
texts = [item['summary'] for item in data if 'summary' in item]
ids = [item['id'] for item in data if 'id' in item]

# Generate embeddings
logger.info("Generating embeddings for the dataset...")
embeddings = np.array([model.encode(text) for text in texts], dtype="float32")
logger.info("Embeddings generated.")

# Evaluation of FAISS IndexFlatL2
logger.info("Evaluating FAISS IndexFlatL2...")
start_time = time.time()
index_flat = faiss.IndexFlatL2(embedding_dim)
index_flat.add(embeddings)
indexing_time_flat = time.time() - start_time
logger.info(f"Indexing time (IndexFlatL2): {indexing_time_flat:.2f} seconds")

# Evaluation of FAISS IndexHNSWFlat
logger.info("Evaluating FAISS IndexHNSWFlat...")
index_hnsw = faiss.IndexHNSWFlat(embedding_dim, 32)  # 32 is the HNSW parameter for graph quality
start_time = time.time()
index_hnsw.add(embeddings)
indexing_time_hnsw = time.time() - start_time
logger.info(f"Indexing time (IndexHNSWFlat): {indexing_time_hnsw:.2f} seconds")

# Evaluation of Annoy
logger.info("Evaluating Annoy...")
annoy_index = AnnoyIndex(embedding_dim, 'euclidean')
for i, vec in enumerate(embeddings):
    annoy_index.add_item(i, vec)
start_time = time.time()
annoy_index.build(10)  # 10 trees for building Annoy index
indexing_time_annoy = time.time() - start_time
logger.info(f"Indexing time (Annoy): {indexing_time_annoy:.2f} seconds")

# Measure query time for each method
query = "example query text"
query_embedding = model.encode(query)

# Query time for FAISS IndexFlatL2
start_time = time.time()
distances_flat, indices_flat = index_flat.search(np.array([query_embedding], dtype="float32"), 5)
query_time_flat = time.time() - start_time
logger.info(f"Query time (IndexFlatL2): {query_time_flat:.4f} seconds")

# Query time for FAISS IndexHNSWFlat
start_time = time.time()
distances_hnsw, indices_hnsw = index_hnsw.search(np.array([query_embedding], dtype="float32"), 5)
query_time_hnsw = time.time() - start_time
logger.info(f"Query time (IndexHNSWFlat): {query_time_hnsw:.4f} seconds")

# Query time for Annoy
start_time = time.time()
indices_annoy = annoy_index.get_nns_by_vector(query_embedding, 5, include_distances=True)
query_time_annoy = time.time() - start_time
logger.info(f"Query time (Annoy): {query_time_annoy:.4f} seconds")

# Plot results
methods = ['IndexFlatL2', 'IndexHNSWFlat', 'Annoy']
indexing_times = [indexing_time_flat, indexing_time_hnsw, indexing_time_annoy]
query_times = [query_time_flat, query_time_hnsw, query_time_annoy]

plt.figure(figsize=(10, 5))
plt.subplot(1, 2, 1)
plt.bar(methods, indexing_times, color=['blue', 'green', 'orange'])
plt.title('Indexing Time Comparison')
plt.ylabel('Time (seconds)')

plt.subplot(1, 2, 2)
plt.bar(methods, query_times, color=['blue', 'green', 'orange'])
plt.title('Query Time Comparison')
plt.ylabel('Time (seconds)')

plt.tight_layout()
plt.show()
