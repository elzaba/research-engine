import requests
import json
import base64
from io import BytesIO
from PIL import Image

def evaluate_co_occurrence(api_url, documents, n_components=5):
    """
    Makes a POST request to the evaluate_co_occurrence API endpoint to evaluate
    the co-occurrence matrix and its reduced SVD representation.

    Args:
    - api_url: The URL of the API endpoint.
    - documents: List of document texts.
    - n_components: Number of components for SVD reduction.

    Returns:
    - Dictionary with results: Variance metrics and the base64-encoded scatter plot image.
    """
    headers = {
        'Content-Type': 'application/json',
    }

    # Prepare payload
    payload = documents  # Send just the list of documents as the payload

    # Send POST request to the API
    response = requests.post(api_url, headers=headers, json=payload)

    if response.status_code == 200:
        # Return the API response JSON
        return response.json()
    else:
        # Handle error response
        print(f"Error: {response.status_code}, {response.text}")
        return None

def display_base64_image(base64_str):
    """
    Converts a base64-encoded image string to an image and displays it.
    
    Args:
    - base64_str: The base64 string representing the image.
    """
    # Decode the base64 string to bytes
    image_data = base64.b64decode(base64_str)
    
    # Create an image from the bytes and display it
    image = Image.open(BytesIO(image_data))
    image.show()

# Example usage
if __name__ == "__main__":
    api_url = "http://127.0.0.1:8000/evaluate_co_occurrence/" 
    documents = [
        "Existing machine learning inference platforms typically assume a homogeneous infrastructure and do not take into account the more complex and tiered computing infrastructure that includes edge devices, local hubs, edge datacenters, and cloud datacenters.",
        "Advances in machine learning and artificial intelligence have begun to approximate and even surpass human performance, but machine systems reliably struggle to generalize information to untrained situations.",
        "Machine learning on sets towards sequential output is an important and ubiquitous task, with applications ranging from language modeling and meta-learning to multi-agent strategy games and power grid optimization."
    ]
    
    # Call the API
    result = evaluate_co_occurrence(api_url, documents, n_components=5)

    if result:
        print("Raw Variance:", result.get('raw_variance'))
        print("Reduced Variance:", result.get('reduced_variance'))
        print("Base64-encoded Plot Image:", result.get('plot_base64'))
        
        # Display the plot image
        display_base64_image(result.get('plot_base64'))


