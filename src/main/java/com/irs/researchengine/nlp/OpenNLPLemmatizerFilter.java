package com.irs.researchengine.nlp;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import opennlp.tools.lemmatizer.DictionaryLemmatizer;
import opennlp.tools.postag.POSTaggerME;

import java.io.IOException;

public class OpenNLPLemmatizerFilter extends TokenFilter {

    private final POSTaggerME posTagger;
    private final DictionaryLemmatizer lemmatizer;
    private final CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    protected OpenNLPLemmatizerFilter(TokenStream input, POSTaggerME posTagger, DictionaryLemmatizer lemmatizer) {
        super(input);
        this.posTagger = posTagger;
        this.lemmatizer = lemmatizer;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }

        String token = charTermAttribute.toString();
        String posTag = posTagger.tag(new String[]{token})[0];  // Get POS tag for token
        String lemma = lemmatizer.lemmatize(new String[]{token}, new String[]{posTag})[0];  // Lemmatize token

        if (lemma != null && !lemma.equals("O")) {  // If lemma is found, use it
            charTermAttribute.setEmpty().append(lemma);
        }

        return true;
    }
}
