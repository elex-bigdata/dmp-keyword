package com.elex.dmp.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.cybozu.labs.langdetect.LangDetectException;
import com.elex.dmp.nlp.ElexAnalyzer;
import com.elex.dmp.nlp.LangDetector;

public class UrlKeyWordExtractRunable implements Runnable {

	
	private LangDetector ld;
	private Analyzer analyzer;
	private StringBuilder merge;
	private StringReader sr;
	private StringBuilder result;
	private String temp;
	private Put put;
	private Delete del;
	private List<Delete> delList;
	private String lang;
	private HTable ud;
	private Configuration configuration;
	private int count;
	private byte[] rowkey;
	private Scan scan;
	


	public UrlKeyWordExtractRunable(Scan s) throws IOException {
		ld = new LangDetector();
		analyzer = new ElexAnalyzer();			
		merge = new StringBuilder(300);
		result = new StringBuilder(100);
		delList = new ArrayList<Delete>();
		configuration = HBaseConfiguration.create();
		ud = new HTable(configuration, "dmp_url_detail");		
		ud.setAutoFlush(false);
		this.scan = s;
				
		
	}
	
	
	protected String sed(StringReader sr) throws IOException {
		result.delete(0, result.length());
		TokenStream ts = analyzer.tokenStream("contents", sr);
		while (ts.incrementToken()) {
			temp = ts.getAttribute(CharTermAttribute.class).toString()
					.trim();
			if (!temp.equals(" ")) {
				result.append(temp + " ");
			}
		}
		return result.toString();
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		
		
		UrlKeyWordExtractRunable extractor1 = new UrlKeyWordExtractRunable(getScan(args[0],args[1]));
		Thread t1 = new Thread(extractor1);
		t1.start();		
				
	}
	
	public static Scan getScan(String begin,String end){
		Scan s = new Scan();
		byte[] start = Bytes.toBytes(begin);
		byte[] stop = Bytes.toBytes(end);
		s.setCaching(500);
		s.setStartRow(start);
		s.setStopRow(stop);
		s.addColumn(Bytes.toBytes("ud"), Bytes.toBytes("t"));
		s.addColumn(Bytes.toBytes("ud"), Bytes.toBytes("m"));
		s.addColumn(Bytes.toBytes("ud"), Bytes.toBytes("url"));
		return s;
	}


	@Override
	public void run() {
		
		ResultScanner rs;
		try {
			rs = ud.getScanner(scan);
			for (Result r : rs) {
				byte[] key = r.getRow();
				merge.delete(0, merge.toString().length());
				rowkey = Bytes.add(Bytes.toBytes("T"), Bytes.tail(key, key.length-1));
				put = new Put(rowkey);	
				
				for (KeyValue kv : r.raw()) {
					if ("ud".equals(Bytes.toString(kv.getFamily())) && "t".equals(Bytes.toString(kv.getQualifier()))) {
						merge.append(" "+Bytes.toString(kv.getValue()));
						put.add(Bytes.toBytes("ud"), Bytes.toBytes("t"),kv.getValue());
					}
					if ("ud".equals(Bytes.toString(kv.getFamily())) && "m".equals(Bytes.toString(kv.getQualifier()))) {
						merge.append(" "+Bytes.toString(kv.getValue()));
						put.add(Bytes.toBytes("ud"), Bytes.toBytes("m"),kv.getValue());
					}
					if ("ud".equals(Bytes.toString(kv.getFamily())) && "url".equals(Bytes.toString(kv.getQualifier()))) {
						put.add(Bytes.toBytes("ud"), Bytes.toBytes("url"),kv.getValue());
					}
				}
				
						
							
				sr = new StringReader(merge.toString());
				try {
					lang = ld.detect(merge.toString());
					put.add(Bytes.toBytes("ud"), Bytes.toBytes("l"),Bytes.toBytes(lang));
					if (lang.startsWith("en")) {
						put.add(Bytes.toBytes("ud"), Bytes.toBytes("k"),Bytes.toBytes(sed(sr)));
					}
				} catch (LangDetectException e) {
					put.add(Bytes.toBytes("ud"), Bytes.toBytes("l"),Bytes.toBytes("None"));
					put.add(Bytes.toBytes("ud"), Bytes.toBytes("merge"),Bytes.toBytes(merge.toString()));
				}
				
				
				
				put.setWriteToWAL(false);
				
				del = new Delete(key);
				delList.add(del);
				del.setWriteToWAL(false);
				count++;
				if(count%1000==0){
					try {
						ud.batch(delList);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					delList.clear();
				}
				ud.put(put);
			}
			try {
				ud.batch(delList);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ud.flushCommits();
			ud.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
