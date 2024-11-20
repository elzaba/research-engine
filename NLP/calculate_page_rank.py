# pip install networkx sentence-transformers scikit-learn numpy
# python calculate_page_rank.py

import networkx as nx
import json
import logging
from sentence_transformers import SentenceTransformer
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

# Setup logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def build_graph(paper_data, embeddings, batch_size=1000):
    """
    Build a graph where nodes represent papers and edges represent similarity based on cosine similarity.
    """
    G = nx.DiGraph()
    logger.info("Building graph...")
    
    # Add nodes (papers) to the graph
    for paper in paper_data:
        paper_id = paper['id']
        G.add_node(paper_id)
    
    logger.info("Nodes added to the graph.")
    logger.info("Calculating cosine similarities in batches...")

    num_papers = len(embeddings)
    threshold = 0.7  # Similarity threshold

    for start in range(0, num_papers, batch_size):
        end = min(start + batch_size, num_papers)
        batch_embeddings = embeddings[start:end]

        # Calculate pairwise similarities for the current batch with all embeddings
        similarity_matrix = cosine_similarity(batch_embeddings, embeddings)

        for i in range(len(batch_embeddings)):
            global_i = start + i
            for j in range(global_i + 1, num_papers):  # Only upper triangle to avoid duplicates
                similarity_score = similarity_matrix[i, j - start]  # Get similarity from batch matrix
                if similarity_score > threshold:
                    G.add_edge(paper_data[global_i]['id'], paper_data[j]['id'], weight=similarity_score)
                    G.add_edge(paper_data[j]['id'], paper_data[global_i]['id'], weight=similarity_score)

            logger.debug(f"Processed batch starting at index {start}")
    
    logger.info("Graph construction completed.")
    return G


def calculate_pagerank(graph):
    """
    Calculate PageRank scores for the graph.
    """
    logger.info("Calculating PageRank...")
    pagerank_scores = nx.pagerank(graph)
    logger.info("PageRank calculation completed.")
    return pagerank_scores

def generate_embeddings(texts):
    """
    Generate embeddings for the texts (papers' summaries).
    """
    logger.info("Generating embeddings for the summaries...")
    model = SentenceTransformer('paraphrase-MiniLM-L6-v2')  # Using a pre-trained model
    embeddings = model.encode(texts)
    logger.info(f"Generated embeddings for {len(texts)} papers.")
    return embeddings

def load_data(file_path):
    """
    Load and filter the dataset based on the required fields.
    """
    logger.info(f"Loading data from {file_path}...")
    with open(file_path, 'r') as f:
        data = json.load(f)
    
    # Extract summaries and ids
    texts = [item['summary'] for item in data if 'summary' in item]
    ids = [item['id'] for item in data if 'id' in item]
    
    logger.info(f"Loaded {len(texts)} papers with summaries.")
    return data, texts, ids

def save_pagerank_scores(scores, output_file):
    """
    Save the PageRank scores to a file.
    """
    logger.info(f"Saving PageRank scores to {output_file}...")
    with open(output_file, 'w') as f:
        json.dump(scores, f)
    logger.info("PageRank scores saved successfully.")

def main():
    # Define the dataset file and output for PageRank scores
    dataset_file = '../dataset/cs_research_papers.json'
    pagerank_output_file = '../src/main/resources/data/pagerank_scores.json'
    
    # Load the data
    data, texts, ids = load_data(dataset_file)

    # Generate embeddings for the summaries
    embeddings = generate_embeddings(texts)

    # Build the graph based on the generated embeddings
    graph = build_graph(data, embeddings)

    # Calculate PageRank scores
    pagerank_scores = calculate_pagerank(graph)

    # Save PageRank scores to a file
    save_pagerank_scores(pagerank_scores, pagerank_output_file)

if __name__ == "__main__":
    main()
