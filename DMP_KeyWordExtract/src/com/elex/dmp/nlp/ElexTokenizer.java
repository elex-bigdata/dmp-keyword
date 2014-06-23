package com.elex.dmp.nlp;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class ElexTokenizer extends Tokenizer {

	private List<String> tokens;
	private Iterator<String> tokenIter;
	private CharTermAttribute termAtt;

	@SuppressWarnings("deprecation")
	public ElexTokenizer(List<String> tokens) {
		this.tokens = tokens;
		this.tokenIter = tokens.iterator();
		termAtt = addAttribute(CharTermAttribute.class);
	}

	@Override
	public boolean incrementToken() throws IOException {
		clearAttributes();
		if (tokenIter.hasNext()) {
			String tokenstring = tokenIter.next();			
			termAtt.append(tokenstring);
			termAtt.setLength(tokenstring.length());
			return true;
		}
		return false;
	}

	@Override
	public void reset() throws IOException {
		tokenIter = tokens.iterator();
	}

	@Override
	public void reset(Reader input) throws IOException {

	}

	public void reset(List<String> tokens) {
		this.tokens = tokens;
		this.tokenIter = tokens.iterator();
	}

}
