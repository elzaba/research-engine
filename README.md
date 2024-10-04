# research-engine

This project is a simple academic paper search engine built using Spring Boot, Thymeleaf, and Apache Lucene. It allows users to search for research papers from a local dataset, returning indexed documents with pagination.

## Requirements
```
    Java 17 or higher
    IntelliJ IDEA or Spring Tool Suite 4 (STS4)
    Gradle (Included with the project)
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
  docs.path=/path/to/your/citeseer/dataset  # Change this to your dataset path
  index.path=/path/to/your/index  # Change this to your desired index path
  ```
- Ensure the docs.path points to the directory where the CiteSeer dataset is located. The index.path is where Lucene will store the indexed documents.

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

5. Index the Papers

- Once the application is running, you need to index the research papers before you can search them. You can do this through the terminal or command prompt.

- Run the following curl command in your terminal:
  ```
  curl -X GET http://localhost:8080/index
  ```
- This will trigger the indexing process. You should see a response indicating that indexing has started and completed successfully.
6. Search for Papers

    - Open your browser and navigate to http://localhost:8080.
    - Enter a search query in the search box and click Search.
    - The results will display based on the indexed papers.  
