package com.elex.dmp.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class ElexAnalyzer extends Analyzer{

	private volatile boolean initialized = false;
	private StopAnalyzer analyzer;
	//private MaxentTagger tagger;
	private StanfordCoreNLP pipeline;
	
	public ElexAnalyzer() throws IOException {
		super();
		
		//初始化StanfordCoreNLP
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		pipeline = new StanfordCoreNLP(props, false);
		
		//StopAnalyzer加载自定义停用词表
		Set<String> stop = new HashSet<String>();
		//ClassLoader  loader = Thread.currentThread().getContextClassLoader(); 
	    //InputStream  input  =  loader.getResourceAsStream("stopwords.txt");  
		BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("stopwords.txt")));
		String stopWord = "";
		while ((stopWord = reader.readLine()) != null) {
			stop.add(stopWord.trim().toLowerCase());			
		}
		reader.close();		
		analyzer = new StopAnalyzer(Version.LUCENE_36, stop);
		
		//初始化词性标注器
		//tagger = new MaxentTagger(Thread.currentThread().getContextClassLoader().getResource("wsj-0-18-caseless-left3words-distsim.tagger").getFile());
		
		//全部初始化完成
		initialized = true;
	}

	@Override
	public void close() {
		super.close();
	}

	
	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		if(!initialized)
			return null;
		List<String> tokens;
		try {
			tokens = tokenizeReader(reader);
			return new ElexTokenizer(tokens);
		} catch (IOException e) {
			System.out.println("com.elex.dmp.common.ElexAnalyzer.tokenStream() failed! because IOException");
		}
		return null;
		
	}
	
	/**
	 * 分词、词干提取、词性标注、词形归并
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public List<String> tokenizeReader(Reader reader) throws IOException {
	    List<String> result = new ArrayList<String>(1000);
	    TokenStream ts = analyzer.tokenStream(null, reader);
		//ts = new PorterStemFilter(ts);//词干提取
		CharTermAttribute term=ts.getAttribute(CharTermAttribute.class);
		String tag,word;
		Annotation document;
		List<CoreLabel> token;	
		while(ts.incrementToken()){
			document = pipeline.process(term.toString());
			token = document.get(TokensAnnotation.class);
			word = token.get(0).get(TextAnnotation.class);
			tag = token.get(0).get(PartOfSpeechAnnotation.class);
	    	 if(accept(word,tag)){	 
	    		result.add(token.get(0).get(LemmaAnnotation.class));//获取词元	    		 
	    	 }
		}
	    return result;
	  }
	
	/**
	 * 根据词性选词
	 * @param word
	 * @param type
	 * @return
	 */
	 private boolean accept(String word,String type){
		  boolean accept=false;
		  
		  if(type.startsWith("NN")	//Noun, singular or mass
//				||type.startsWith("CC")	//Coordinating conjunction
//				||type.startsWith("CD")	//Cardinal number
//	    		||type.startsWith("DT")	//Determiner
//	    		||type.startsWith("EX")	//Existential there
//	    		||type.startsWith("FW")	//Foreign word
//	    		||type.startsWith("IN")	//Preposition or subordinating conjunction
//	    		||type.startsWith("JJ")	//Adjective
//	    		||type.startsWith("LS")	//List item marker
//	    		||type.startsWith("MD")	//Modal
//	    		||type.startsWith("PDT") //Predeterminer
//	    		||type.startsWith("POS") //Possessive ending
//	    		||type.startsWith("PRP") //Personal pronoun
//	    		||type.startsWith("RB")	//Adverb
//	    		||type.startsWith("RP")	//Particle
//	    		||type.startsWith("SYM") //Symbol
//	    		||type.startsWith("TO")	//to
//	    		||type.startsWith("VB")	//Verb, base form
//	    		||type.startsWith("UH")	//Interjection
//	    		||type.startsWith("WDT")	//Wh-determiner
//	    		||type.startsWith("WP")	//Wh-pronoun
//	    		||type.startsWith("WRB")	//Wh-adverb
		    		){
			  return true;
		  }
		  
		  if(word.contains("nbsp")){
			  return accept;
		  }
		  
		  return accept;
	  }

}
