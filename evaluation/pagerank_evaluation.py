# pip install requests networkx matplotlib numpy

import numpy as np
import networkx as nx
import matplotlib.pyplot as plt
import requests
from io import StringIO

def read_graph_from_url(url):
    """Reads a directed graph from an edge list file at a URL and assigns weights."""
    response = requests.get(url)
    response.raise_for_status()  # Ensure the request was successful
    data = StringIO(response.text)
    G = nx.read_edgelist(data, nodetype=int, create_using=nx.DiGraph())
    for edge in G.edges():
        G[edge[0]][edge[1]]['weight'] = 1
    return G

def id_title_from_url(url):
    """Reads paper metadata from a URL and maps paper IDs to titles."""
    pid_title = {}
    response = requests.get(url)
    response.raise_for_status()
    data = StringIO(response.text)
    for line in data:
        fields = line.strip().split('\t')
        pid = str(int(fields[0], 16))  # Convert HEX ID to decimal
        pid_title[pid] = fields[2]
    return pid_title

def calculate_pagerank(graph, alpha=0.9):
    """Calculates PageRank for a given graph."""
    pr = nx.pagerank(graph, alpha=alpha)
    sorted_pr = sorted(pr.items(), key=lambda x: x[1], reverse=True)
    return sorted_pr

def main():
    # URLs for the files
    graph_url = 'https://jlu.myweb.cs.uwindsor.ca/8380/sigmod_subgraph.txt'
    id_url = 'https://jlu.myweb.cs.uwindsor.ca/8380/sigmod_id.txt'

    # Read the graph
    G = read_graph_from_url(graph_url)
    print(f"Graph loaded with {G.number_of_nodes()} nodes and {G.number_of_edges()} edges.")

    # Calculate PageRank
    pr = calculate_pagerank(G)
    print(f"Top 5 papers by PageRank: {pr[:5]}")

    # Load paper ID-to-title mapping
    id_title_map = id_title_from_url(id_url)
    
    # Display top 5 papers with titles and PageRank scores
    top_titles = {}
    for pid, score in pr[:5]:
        title = id_title_map.get(str(pid), "Unknown Title")
        top_titles[title] = score
        print(f"Paper ID: {pid}, Title: {title}, PageRank Score: {score}")

    # In-degree analysis
    in_degrees = G.in_degree()
    top_in_degrees = sorted(in_degrees, key=lambda x: x[1], reverse=True)[:5]
    print(f"Top 5 papers by in-degree: {top_in_degrees}")

    # Plot in-degree distribution
    in_values = sorted(set(dict(in_degrees).values()))
    in_hist = [list(dict(in_degrees).values()).count(x) for x in in_values]
    plt.figure()
    plt.loglog(in_values, in_hist, 'ro-')
    plt.title('In-degree Distribution')
    plt.xlabel('Degree')
    plt.ylabel('Frequency')
    plt.savefig("in_degree_distribution.png")
    plt.show()

    # Clustering coefficient and largest component
    UG = G.to_undirected()
    clustering_coeffs = nx.clustering(UG)
    avg_clustering_coeff = float(sum(clustering_coeffs.values())) / len(clustering_coeffs)
    print(f"Average clustering coefficient: {avg_clustering_coeff}")

    # Get the largest connected component
    components = sorted(nx.connected_components(UG), key=len, reverse=True)
    largest_component = UG.subgraph(components[0])
    print(f"Largest component size: {largest_component.number_of_edges()} edges")

    # Visualize the largest component
    nx.draw(largest_component, node_size=0.01)
    plt.savefig('largest_component.png')
    plt.show()

if __name__ == "__main__":
    main()
