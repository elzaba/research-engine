import nltk
from nltk.corpus import brown
import os

# Download the Brown corpus if not already downloaded
nltk.download('brown')

def process_brown_corpus(output_path):
    with open(output_path, 'w', encoding='utf-8') as f:
        for category in brown.categories():
            for fileid in brown.fileids(categories=category):
                # Tokenize the text
                words = brown.words(fileids=fileid)
                # Write words as a single line separated by spaces
                f.write(' '.join(words) + '\n')

if __name__ == "__main__":
    output_path = "brown_corpus.txt"
    process_brown_corpus(output_path)
    print(f"Brown Corpus processed and saved to {os.path.abspath(output_path)}")
