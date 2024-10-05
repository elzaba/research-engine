package com.irs.researchengine.nlp;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.CharArraySet;


import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.InputStream;

public class CustomAnalyzer extends Analyzer {

    private POSTaggerME posTagger;
    private DictionaryLemmatizer lemmatizer;

    public CustomAnalyzer() {
        try {
            // Load the POS tagger model from OpenNLP
            InputStream posModelStream = getClass().getResourceAsStream("/models/en-pos-maxent.bin");
            POSModel posModel = new POSModel(posModelStream);
            posTagger = new POSTaggerME(posModel);

            // Load the lemmatizer dictionary from OpenNLP
            InputStream lemmatizerDictStream = getClass().getResourceAsStream("/models/en-lemmatizer.dict");
            lemmatizer = new DictionaryLemmatizer(lemmatizerDictStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load OpenNLP models", e);
        }
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        // Tokenization step
        StandardTokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = new LowerCaseFilter(tokenizer);  // Lowercase conversion
        // Use default stop words from StandardAnalyzer
        CharArraySet stopWords = EnglishAnalyzer.ENGLISH_STOP_WORDS_SET;
        tokenStream = new StopFilter(tokenStream, stopWords);  // Stop word filtering

        // Add lemmatization via OpenNLP
        tokenStream = new OpenNLPLemmatizerFilter(tokenStream, posTagger, lemmatizer);

        // Adding N-gram tokenization for fuzzy matching and autocomplete
        tokenStream = new NGramTokenFilter(tokenStream, 2, 3, true);  // Generates 2-gram and 3-gram tokens

        return new TokenStreamComponents(tokenizer, tokenStream);
    }
}
