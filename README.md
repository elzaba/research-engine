# research-engine

This project is an academic paper search engine built with Spring Boot, Lucene, and Angular. This search engine allows users to index and search research papers from Arxiv dataset. It supports both traditional keyword-based and semantic search using a FastAPI-based Python service for improved relevance.

## Features
- **Custom Analyzer**: Utilizes OpenNLP for POS tagging and lemmatization, improving query matching accuracy.
- **Auto-Complete Suggestions**: Edge n-gram tokenization on titles provides auto-complete suggestions. 
- **N-Gram Tokenization**: Bigrams and trigrams improve fuzzy search, handling misspellings and incomplete queries.
- **Proximity Search**: Allows users to specify word distances, enhancing phrase-based queries.
- **Semantic Search**: Provides semantic relevance in search results by leveraging Sentence-BERT embeddings and FAISS similarity search.
- **User Interface**: Supports both Thymeleaf and Angular UIs.

## Requirements
```
    Java 17 or higher
    IntelliJ IDEA or Spring Tool Suite 4 (STS4)
    Gradle (Included with the project)
    Python 3.6+ (for semantic search)
```

Setup Instructions
1. Clone the Repository

- Clone the repository to your local machine:
  ```
  git clone https://github.com/elzaba/research-engine.git
  cd research-engine
  ```

2. Configure the application.properties file

- Open the src/main/resources/application.properties file and update the paths:
  ```
  spring.application.name=research-engine
  index.path=/path/to/your/index  # Change this to your desired index path
  faiss.api.url=http://127.0.0.1:8000
  ```
- `index.path` is where Lucene will store the indexed documents.
- `faiss.api.url` is the endpoint for semantic search service.

3. Set Up the Project in IntelliJ or STS4
- IntelliJ IDEA

    - Open IntelliJ IDEA.
    - Go to File > Open and select the root folder of the cloned project.
    - IntelliJ will automatically detect the project and import all the dependencies.
    - Wait for the Gradle build to finish.

- Spring Tool Suite 4 (STS4)

    - Open STS4.
    - Go to File > Import.
    - Choose Existing Gradle Project and navigate to the root folder of the cloned project.
    - Click Finish to import the project.
    - Wait for the Gradle build to finish.

4. Build and Run the Project

- Once the project is imported:

    - In IntelliJ:
        - Click the Run button in the toolbar, or right-click the ResearchEngineApplication class and select Run.
    - In STS4:
        - Right-click the project in the Package Explorer, then select Run As > Spring Boot App.

5. Set Up the Semantic Search Service (Python)
   
- The semantic search service provides enhanced search relevance by capturing semantic similarity in queries. Set up this Python-based FastAPI service in the python directory.

  1. Navigate to the python folder:
     ```
     cd python
     ```
  2. Install dependencies:
     ```
     pip install fastapi uvicorn sentence-transformers faiss-cpu
     ```
  3. Start the FastAPI application:
     ```
     python3 -m uvicorn FastApiService:app --reload
     ```

6. Index the Papers

- Once the application is running, you need to index the research papers before you can search them. You can do this through the terminal or command prompt.
- Create the dataset (Option 1):
  ```
  curl -X POST http://localhost:8080/api/create-dataset
  ```
  - This will fetch computer science-related papers from Arxiv and create a dataset in JSON format.
- **Alternative Option**: Download the dataset from this [link](https://drive.google.com/file/d/1LQL9NVH-CN33EVOF0xnoiOBITo5VbVKp/view?usp=drive_link)
  and update the application.properties file in the resources directory by setting:
  ```
  dataset.path=/path/to/your/downloaded/dataset/directory
  ```
  - The dataset contains over 300,000 records.
- Index the papers into lucene:
  ```
  curl -X POST http://localhost:8080/api/index
  ```
  - This will trigger the indexing process. You should see a response indicating that indexing has started and completed successfully.
- Semantic Search (FAISS Indexing):
  ```
  curl -X POST http://localhost:8080/api/index-faiss
  ```
  - This step is required to enable vector-based semantic search functionality.
7. Search for Papers

    - Open your browser and navigate to http://localhost:8080 for thymeleaf UI.
    - Access the Angular UI at http://localhost:4200.
    - Enter a search query in the search box and click Search.
    - The results will display based on the indexed papers.  
