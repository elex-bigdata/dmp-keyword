package com.elex.dmp.nlp;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class LangDetector {
	
	private List<String> jsonFiles;
	
	public LangDetector() {
		try {
			jsonFiles = new ArrayList<String>();
			addProfile();
			init(jsonFiles);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LangDetectException e) {
			e.printStackTrace();
		}
		
	}
	
	
    public void init(List<String> json_profiles) throws LangDetectException {
        DetectorFactory.loadProfile(json_profiles);
    }
    
    public void init(String profilesDirectory) throws LangDetectException {
        DetectorFactory.loadProfile(profilesDirectory);
    }
    public String detect(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.detect();
    }
    public ArrayList<Language> detectLangs(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.getProbabilities();
    }
    
    private String readFile(BufferedReader ir) throws IOException{
		
		StringBuilder sb = new StringBuilder(1000);
		String line = ir.readLine();
		while(line != null){
			sb.append(line);
			line = ir.readLine();
		}
		ir.close();
		return sb.toString();
		
	}
	
	private void addProfile() throws IOException{
		String[] profiles = {"af","ar","bg","bn","cs","da","de","el","en","es","et","fa","fi","fr","gu","he",
				"hi","hr","hu","id","it","ja","kn","ko","lt","lv","mk","ml","mr","ne","nl","no","pa","pl",
				"pt","ro","ru","sk","sl","so","sq","sv","sw","ta","te","th","tl","tr","uk","ur","vi","zh-cn","zh-tw"};
		for(String profile:profiles){
			jsonFiles.add(readFile(new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("profiles/"+profile)))));
			
		}
		
	}
}