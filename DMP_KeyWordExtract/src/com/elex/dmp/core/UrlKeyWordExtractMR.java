package com.elex.dmp.core;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.cybozu.labs.langdetect.LangDetectException;
import com.elex.dmp.nlp.ElexAnalyzer;
import com.elex.dmp.nlp.LangDetector;


public class UrlKeyWordExtractMR extends Configured implements Tool {

	public static class MyMapper extends
			TableMapper<ImmutableBytesWritable, Put> {
		
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
		

		@Override
		protected void setup(Context context) throws IOException,InterruptedException {
					
			ld = new LangDetector();			
			analyzer = new ElexAnalyzer();	
			merge = new StringBuilder(300);
			result = new StringBuilder(100);
			delList = new ArrayList<Delete>();
			configuration = HBaseConfiguration.create();
			ud = new HTable(configuration, "dmp_url_detail");
			ud.setAutoFlush(false);
		}

		@Override
		protected void map(ImmutableBytesWritable key, Result values,Context context) throws IOException, InterruptedException {
			merge.delete(0, merge.toString().length());
			rowkey = Bytes.add(Bytes.toBytes("T"), Bytes.tail(key.get(), key.get().length-1));
			put = new Put(rowkey);	
			
			for (KeyValue kv : values.raw()) {
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
			
			del = new Delete(key.get());
			delList.add(del);
			del.setWriteToWAL(false);
			count++;
			if(count%1000==0){
				ud.batch(delList);
				delList.clear();
			}
			
			context.write(new ImmutableBytesWritable(rowkey), put);
			
		}
		
		@Override
		protected void cleanup(Context context) throws IOException,
				InterruptedException {
			ud.batch(delList);
			ud.flushCommits();
			ud.close();
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
				


	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		int res = ToolRunner.run(new Configuration(), new UrlKeyWordExtractMR(),otherArgs);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = new Configuration();
        conf = HBaseConfiguration.create(conf);
        Job job = Job.getInstance(conf,"urlKeyWordExtract");//new Job�ķ�ʽ��2.0�汾���ѱ�����
        job.setJarByClass(UrlKeyWordExtractMR.class);               
		Scan s = new Scan();
		byte[] start = Bytes.toBytes("F");
		byte[] stop = Bytes.toBytes("G");
		s.setCaching(500);
		s.setStartRow(start);
		s.setStopRow(stop);
		s.addColumn(Bytes.toBytes("ud"), Bytes.toBytes("t"));
		s.addColumn(Bytes.toBytes("ud"), Bytes.toBytes("m"));
		s.addColumn(Bytes.toBytes("ud"), Bytes.toBytes("url"));
		TableMapReduceUtil.initTableMapperJob("dmp_url_detail", s, MyMapper.class,ImmutableBytesWritable.class, Put.class, job);	
		TableMapReduceUtil.initTableReducerJob("dmp_url_detail", null, job);
		return job.waitForCompletion(true)?0:1;
	}

}
