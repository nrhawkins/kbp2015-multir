package edu.washington.multir.sententialextraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import edu.washington.multirframework.multiralgorithm.FullInference;
import edu.washington.multirframework.multiralgorithm.MILDocument;
import edu.washington.multirframework.multiralgorithm.Parse;
import edu.washington.multirframework.multiralgorithm.SparseBinaryVector;
import edu.washington.multirframework.multiralgorithm.Mappings;
import edu.washington.multirframework.multiralgorithm.Model;
import edu.washington.multirframework.multiralgorithm.Parameters;
import edu.washington.multirframework.multiralgorithm.Scorer;
import edu.washington.multir.preprocess.CorpusPreprocessing;
import edu.washington.multirframework.argumentidentification.ArgumentIdentification;
import edu.washington.multirframework.argumentidentification.NELAndCorefArgumentIdentification;
import edu.washington.multirframework.argumentidentification.NELArgumentIdentification;
import edu.washington.multirframework.argumentidentification.NERArgumentIdentification;
import edu.washington.multirframework.argumentidentification.DefaultSententialInstanceGeneration;
import edu.washington.multirframework.argumentidentification.SententialInstanceGeneration;
import edu.washington.multirframework.data.Argument;
import edu.washington.multirframework.data.KBArgument;
import edu.washington.multirframework.featuregeneration.DefaultFeatureGenerator;
import edu.washington.multirframework.featuregeneration.DefaultFeatureGeneratorMinusDirMinusDep;
import edu.washington.multirframework.featuregeneration.DefaultFeatureGeneratorMinusDirPath;
import edu.washington.multirframework.featuregeneration.FeatureGenerator;
/**
 * An extractor that provides extractions
 * from a document based on a trained
 * Multir model. Based on the DEFT framework
 * @author jgilme1
 *
 */
public class DocumentExtractor {
	
	private FeatureGenerator fg;
	private ArgumentIdentification ai;
	private SententialInstanceGeneration sig;
	
	private String dir;
	private Mappings mapping;
	private Model model;
	private Parameters params;
	private Scorer scorer;
	
	private Map<Integer, String> relID2rel = new HashMap<Integer, String>();


	public DocumentExtractor(String pathToMultirFiles, FeatureGenerator fg,
			ArgumentIdentification ai, SententialInstanceGeneration sig){
		this.fg = fg;
		this.ai = ai;
		this.sig = sig;
		dir = pathToMultirFiles;
		try {
			mapping = new Mappings();
			mapping.read(dir + "/mapping");

			model = new Model();
			model.read(dir + "/model");

			params = new Parameters();
			params.model = model;
			params.deserialize(dir + "/params");

			scorer = new Scorer();
			
			for(String key :mapping.getRel2RelID().keySet()){
				Integer id = mapping.getRel2RelID().get(key);
				relID2rel.put(id, key);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void extractFromDocument(String pathToDocument) throws IOException, InterruptedException{
		
		Annotation doc = CorpusPreprocessing.getTestDocument(pathToDocument);
		List<Pair<String,Double>> extractions = new ArrayList<>();
		
		List<CoreMap> sentences = doc.get(CoreAnnotations.SentencesAnnotation.class);
		for(CoreMap s : sentences){
			String senText = s.get(CoreAnnotations.TextAnnotation.class);
			List<Argument> args = ai.identifyArguments(doc, s);
			List<Pair<Argument,Argument>> sigs = sig.generateSententialInstances(args, s);
			for(Pair<Argument,Argument> p : sigs){
				Argument arg1 = p.first;
				Argument arg2 = p.second;
				String arg1ID = null;
				String arg2ID = null;
				if(p.first instanceof KBArgument){
					arg1ID = ((KBArgument)p.first).getKbId();
				}
				if(p.second instanceof KBArgument){
					arg2ID = ((KBArgument)p.second).getKbId();
				}
				List<String> features = 
						fg.generateFeatures(arg1.getStartOffset(), arg1.getEndOffset(), 
								arg2.getStartOffset(), arg2.getEndOffset(),
								arg1ID,arg2ID,s, doc);
				Pair<Triple<String,Double,Double>,Map<Integer,Double>> result = getPrediction(features,arg1,arg2,senText);
				if(result !=null){
					Triple<String,Double,Double> relationScoreTriple = getPrediction(features,arg1,arg2,senText).first;
						String extractionString = arg1.getArgName() + " " + relationScoreTriple.first + " " + arg2.getArgName() + "\n" + senText;
						extractions.add(new Pair<String,Double>(extractionString,relationScoreTriple.third));
				}
			}
		}
		
		for(Pair<String,Double> extr: extractions){
			String extrString = extr.first;
			Double score = extr.second;
			
			System.out.println(extrString + "\t" + score);
		}
	}
	
	public Triple<String,Double,Double> extractFromSententialInstance(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc){
		
		//System.out.println("de");
		
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);

		String arg1ID = null;
		String arg2ID = null;
		if(arg1 instanceof KBArgument){
			arg1ID = ((KBArgument)arg1).getKbId();
		}
		if(arg2 instanceof KBArgument){
			arg2ID = ((KBArgument)arg2).getKbId();
		}	
		
		//System.out.println("de: Generating Features: ");	
		
		List<String> features = 
				fg.generateFeatures(arg1.getStartOffset(), arg1.getEndOffset(), 
						arg2.getStartOffset(), arg2.getEndOffset(), 
						arg1ID, arg2ID, sentence, doc);
		
		//System.out.println("de: Features: " + features.size());
		//System.out.println("de: Features: " + features);
		//System.out.println("de: arg1: " + arg1.getArgName() + arg1.getStartOffset() + " " + arg1.getEndOffset());
		//System.out.println("de: arg2: " + arg2.getArgName() + arg2.getStartOffset() + " " + arg2.getEndOffset());
		
        //System.out.println("Sentence Text: " + senText);
		
		
		Pair<Triple<String,Double,Double>,Map<Integer,Double>> p = getPrediction(features,arg1,arg2,senText);
		return p.first;
	}
	
	public Pair<Triple<String,Double,Double>,Map<Integer,Double>> extractFromSententialInstanceWithFeatureScores(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc){
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		String arg1ID = null;
		String arg2ID = null;
		if(arg1 instanceof KBArgument){
			arg1ID = ((KBArgument)arg1).getKbId();
		}
		if(arg2 instanceof KBArgument){
			arg2ID = ((KBArgument)arg2).getKbId();
		}
		List<String> features = 
				fg.generateFeatures(arg1.getStartOffset(), 
						arg1.getEndOffset(), arg2.getStartOffset(), arg2.getEndOffset(), 
						arg1ID, arg2ID,sentence, doc);
		Pair<Triple<String,Double,Double>,Map<Integer,Double>> p = getPrediction(features,arg1,arg2,senText);
		return p;
	}
	
	public Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> extractFromSententialInstanceWithAllFeatureScores(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc){
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		String arg1ID = null;
		String arg2ID = null;
		if(arg1 instanceof KBArgument){
			arg1ID = ((KBArgument)arg1).getKbId();
		}
		if(arg2 instanceof KBArgument){
			arg2ID = ((KBArgument)arg2).getKbId();
		}
		List<String> features = 
				fg.generateFeatures(arg1.getStartOffset(), 
						arg1.getEndOffset(), arg2.getStartOffset(), arg2.getEndOffset(), 
						arg1ID, arg2ID,sentence, doc);
		Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> p = getPredictionWithFeatureScoreMap(features,arg1,arg2,senText);
		return p;
	}
	
	public Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> extractFromSententialInstanceWithAllFeatureScoresIgnoreNA(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc){
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		String arg1ID = null;
		String arg2ID = null;
		if(arg1 instanceof KBArgument){
			arg1ID = ((KBArgument)arg1).getKbId();
		}
		if(arg2 instanceof KBArgument){
			arg2ID = ((KBArgument)arg2).getKbId();
		}
		List<String> features = 
				fg.generateFeatures(arg1.getStartOffset(), 
						arg1.getEndOffset(), arg2.getStartOffset(), arg2.getEndOffset(), 
						arg1ID, arg2ID,sentence, doc);
		Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> p = getPredictionWithFeatureScoreMapIgnoreNA(features,arg1,arg2,senText);
		return p;
	}
	
	public Map<Integer,Double> getFeatureScores(Argument arg1, Argument arg2, CoreMap sentence, Annotation doc, int rel){
		String senText = sentence.get(CoreAnnotations.TextAnnotation.class);
		String arg1ID = null;
		String arg2ID = null;
		if(arg1 instanceof KBArgument){
			arg1ID = ((KBArgument)arg1).getKbId();
		}
		if(arg2 instanceof KBArgument){
			arg2ID = ((KBArgument)arg2).getKbId();
		}
		List<String> features = 
				fg.generateFeatures(arg1.getStartOffset(), arg1.getEndOffset(), 
						arg2.getStartOffset(), arg2.getEndOffset(), 
						arg1ID,arg2ID, sentence, doc);
		MILDocument milDoc = new MILDocument();
		
		milDoc.arg1 = arg1.getArgName();
		milDoc.arg2 = arg2.getArgName();
		milDoc.Y = new int[1];
		milDoc.numMentions = 1;// sentence level prediction
		milDoc.setCapacity(1);
		SparseBinaryVector sv = milDoc.features[0] = new SparseBinaryVector();
		
		
		SortedSet<Integer> ftrset = new TreeSet<Integer>();
		int totalfeatures = 0;
		int featuresInMap = 0;
		//System.out.println("Features:");
		for (String f : features) {
			//System.out.println(f);
			totalfeatures ++;
			int ftrid = mapping.getFeatureID(f, false);
			if (ftrid >= 0) {
				featuresInMap++;
				ftrset.add(ftrid);
			}
		}
		//if( arg1.getArgName().equals("China") && (arg2.getArgName().equals("Beijing"))){
//			System.out.println("Total Features = " + totalfeatures);
//			for(String f : features){
//				System.out.println(f);
//			}
//			System.out.println("Num features in training = " + featuresInMap);
		//}
		
		sv.num = ftrset.size();
		sv.ids = new int[sv.num];
		
		//System.out.println("Features...");
		int k = 0;
		for (int f : ftrset) {
			//System.out.print(f + " ");
			sv.ids[k++] = f;
		}
		//System.out.println();
		
		String relation = "";
		Double conf = 0.0;
		Map<Integer,Map<Integer,Double>> mentionFeatureScoreMap = new HashMap<>();
		Parse parse = FullInference.infer(milDoc, scorer, params,mentionFeatureScoreMap, rel);
		return mentionFeatureScoreMap.get(0);
		
	}

	/**
	 * Conver features and args to MILDoc
	 * and run Multir sentential extraction
	 * algorithm, return null if no extraction
	 * was predicted.
	 * @param features
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private Pair<Triple<String,Double,Double>,Map<Integer,Double>> getPrediction(List<String> features, Argument arg1,
			Argument arg2, String senText) {
		
		MILDocument doc = new MILDocument();
		
		doc.arg1 = arg1.getArgName();
		doc.arg2 = arg2.getArgName();
		doc.Y = new int[1];
		doc.numMentions = 1;// sentence level prediction
		doc.setCapacity(1);
		SparseBinaryVector sv = doc.features[0] = new SparseBinaryVector();
		
		SortedSet<Integer> ftrset = new TreeSet<Integer>();
		int totalfeatures = 0;
		int featuresInMap = 0;
		//System.out.println("Features:");
		for (String f : features) {
			//System.out.println(f);
			totalfeatures ++;
			int ftrid = mapping.getFeatureID(f, false);
			if (ftrid >= 0) {
				featuresInMap++;
				ftrset.add(ftrid);
			}
		}
		//if( arg1.getArgName().equals("China") && (arg2.getArgName().equals("Beijing"))){
//			System.out.println("Total Features = " + totalfeatures);
//			for(String f : features){
//				System.out.println(f);
//			}
//			System.out.println("Num features in training = " + featuresInMap);
		//}
				
		sv.num = ftrset.size();
		sv.ids = new int[sv.num];

		//System.out.println("gp: sv.num " + sv.num);
		//System.out.println("gp: model num rel: " + model.numRelations + " " + model.numFeatures(0) + " " + model.numFeatures(1));
	
		
		//System.out.println("Features...");
		int k = 0;
		for (int f : ftrset) {
			//System.out.print(f + " ");
			sv.ids[k++] = f;
		}
		//System.out.println();
		
		String relation = "";
		Double conf = 0.0;
		Map<Integer,Map<Integer,Double>> mentionFeatureScoreMap = new HashMap<>();
		
		//System.out.println("doc: features size: " + doc.features.length);
		//System.out.println("doc: numMentions: " + doc.numMentions);
		//System.out.println("params: relParams size: " + params.relParameters.length);
		//System.out.println("params: dense vector length: " + params.relParameters[0].vals.length);
		//System.out.println("params: dense vector NA: ");		
		//for(int i =0; i < params.relParameters[0].vals.length-1; i++){
		//  System.out.print(params.relParameters[0].vals[i] + " ");
		//}
		//System.out.println();
		//System.out.println("params: dense vector deathplace: ");		
		//for(int i =0; i < params.relParameters[16].vals.length-1; i++){
		//  System.out.print(params.relParameters[16].vals[i] + " ");
		//}
        //System.out.println();
		
		Parse parse = FullInference.infer(doc, scorer, params, mentionFeatureScoreMap);
		
		//System.out.println("parse: state length: " + parse.Z.length);
        //System.out.println("parse: state: " + parse.Z[0]);
        //System.out.println("parse: score " + parse.score);
        //System.out.println("parse: allscores length: " + parse.allScores[0].length);
        //for(int i =0; i < parse.allScores[0].length-1; i++){
        //   System.out.print(parse.allScores[0][i] + " ");
        // }
        //System.out.println();
        //System.out.println("parse: Y length: " + parse.Y.length);		
        

		//System.out.println("mention Feature Score Map size: " + mentionFeatureScoreMap.size());
        //System.out.println("mfsm 0: " + mentionFeatureScoreMap.get(0));
 		
		//System.out.println(senText);
		//System.out.println(arg1.getArgName() + "\t" + arg2.getArgName());
		//System.out.println("Score = " +parse.score);
		//System.out.println("parse.Y: " + parse.Y[0]);
		
        //System.out.println("parse.Z[0]" + parse.Z[0]);
		
		int[] Yp = parse.Y;
		if (parse.Z[0] > 0) {
			relation = relID2rel.get(parse.Z[0]);
			Arrays.sort(parse.allScores[0]);
//			double secondHighestScore = parse.allScores[0][parse.allScores[0].length-2];
//			double combinedScore = parse.score + secondHighestScore;
			double combinedScore = parse.score;
			
			for(int i =0; i < parse.allScores[0].length-1; i++){
				double s = parse.allScores[0][i];
				if( s > 0.0){
					combinedScore +=s;
				}
			}
			double confidence = (combinedScore <= 0.0 || parse.score <= 0.0) ? .1 : (parse.score/combinedScore);
			if(combinedScore == parse.score && parse.score > 0.0){
				confidence = .001;
			}
			conf = confidence;
		} else {
			//System.out.println("getPrediction: setting relation to NA");
			Map<Integer,Map<Integer,Double>> negMentionFeatureScoreMap = new HashMap<>();
			Parse negParse = FullInference.infer(doc, scorer, params,negMentionFeatureScoreMap,0);
			Triple<String,Double,Double> t = new Triple<>("NA",conf,parse.score);
			Pair<Triple<String,Double,Double>,Map<Integer,Double>> p = new Pair<>(t,negMentionFeatureScoreMap.get(0));
			return p;
		}

		Triple<String,Double,Double> t = new Triple<>(relation,conf,parse.score);
		Pair<Triple<String,Double,Double>,Map<Integer,Double>> p = new Pair<>(t,mentionFeatureScoreMap.get(0));
		return p;
	}
	
	/**
	 * Conver features and args to MILDoc
	 * and run Multir sentential extraction
	 * algorithm, return null if no extraction
	 * was predicted.
	 * @param features
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> getPredictionWithFeatureScoreMap(List<String> features, Argument arg1,
			Argument arg2, String senText) {
		
		MILDocument doc = new MILDocument();
		
		doc.arg1 = arg1.getArgName();
		doc.arg2 = arg2.getArgName();
		doc.Y = new int[1];
		doc.numMentions = 1;// sentence level prediction
		doc.setCapacity(1);
		SparseBinaryVector sv = doc.features[0] = new SparseBinaryVector();
		
		
		SortedSet<Integer> ftrset = new TreeSet<Integer>();
		for (String f : features) {
			int ftrid = mapping.getFeatureID(f, false);
			if (ftrid >= 0) {
				ftrset.add(ftrid);
			}
		}

		sv.num = ftrset.size();
		sv.ids = new int[sv.num];
		
		int k = 0;
		for (int f : ftrset) {
			sv.ids[k++] = f;
		}
		
		String relation = "";
		Double conf = 0.0;
		Map<Integer,Map<Integer,Map<Integer,Double>>> mentionFeatureScoreMap = new HashMap<>();
		Parse parse = FullInference.inferWithFeatureScoreMap(doc, scorer, params,mentionFeatureScoreMap);

		int[] Yp = parse.Y;
		if (parse.Z[0] > 0) {
			relation = relID2rel.get(parse.Z[0]);
			Arrays.sort(parse.allScores[0]);

			double combinedScore = parse.score;
			
			for(int i =0; i < parse.allScores[0].length-1; i++){
				double s = parse.allScores[0][i];
				if( s > 0.0){
					combinedScore +=s;
				}
			}
			double confidence = (combinedScore <= 0.0 || parse.score <= 0.0) ? .1 : (parse.score/combinedScore);
			if(combinedScore == parse.score && parse.score > 0.0){
				confidence = .001;
			}
			conf = confidence;
		} else {
			Triple<String,Double,Double> t = new Triple<>("NA",conf,parse.score);
			Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> p = new Pair<>(t,mentionFeatureScoreMap.get(0));
			return p;
		}

		Triple<String,Double,Double> t = new Triple<>(relation,conf,parse.score);
		Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> p = new Pair<>(t,mentionFeatureScoreMap.get(0));
		return p;
	}
	
	/**
	 * Conver features and args to MILDoc
	 * and run Multir sentential extraction
	 * algorithm, return null if no extraction
	 * was predicted.
	 * @param features
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	private Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> getPredictionWithFeatureScoreMapIgnoreNA(List<String> features, Argument arg1,
			Argument arg2, String senText) {
		
		MILDocument doc = new MILDocument();
		
		doc.arg1 = arg1.getArgName();
		doc.arg2 = arg2.getArgName();
		doc.Y = new int[1];
		doc.numMentions = 1;// sentence level prediction
		doc.setCapacity(1);
		SparseBinaryVector sv = doc.features[0] = new SparseBinaryVector();
		
		
		SortedSet<Integer> ftrset = new TreeSet<Integer>();
		for (String f : features) {
			int ftrid = mapping.getFeatureID(f, false);
			if (ftrid >= 0) {
				ftrset.add(ftrid);
			}
		}

		sv.num = ftrset.size();
		sv.ids = new int[sv.num];
		
		int k = 0;
		for (int f : ftrset) {
			sv.ids[k++] = f;
		}
		
		String relation = "";
		Double conf = 0.0;
		Map<Integer,Map<Integer,Map<Integer,Double>>> mentionFeatureScoreMap = new HashMap<>();
		Parse parse = FullInference.inferWithFeatureScoreMap(doc, scorer, params,mentionFeatureScoreMap);

		int[] Yp = parse.Y;
		
		//ignore NA
		if(parse.Z[0] == 0){
			
			double highestNonNAScore = Double.NEGATIVE_INFINITY;
			int topRel = 0;
			for(int i =1; i < parse.allScores[0].length; i++){
				if(parse.allScores[0][i] > highestNonNAScore){
					highestNonNAScore = parse.allScores[0][i];
					topRel = i;
				}
			}
			parse.Z[0] = topRel;
			parse.score = parse.allScores[0][topRel];
		}
		
		if (parse.Z[0] > 0) {
			relation = relID2rel.get(parse.Z[0]);
			Arrays.sort(parse.allScores[0]);

			double combinedScore = parse.score;
			
			for(int i =0; i < parse.allScores[0].length-1; i++){
				double s = parse.allScores[0][i];
				if( s > 0.0){
					combinedScore +=s;
				}
			}
			double confidence = (combinedScore <= 0.0 || parse.score <= 0.0) ? .1 : (parse.score/combinedScore);
			if(combinedScore == parse.score && parse.score > 0.0){
				confidence = .001;
			}
			conf = confidence;
		} else {
			Triple<String,Double,Double> t = new Triple<>("NA",conf,parse.score);
			Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> p = new Pair<>(t,mentionFeatureScoreMap.get(0));
			return p;
		}

		Triple<String,Double,Double> t = new Triple<>(relation,conf,parse.score);
		Pair<Triple<String,Double,Double>,Map<Integer,Map<Integer,Double>>> p = new Pair<>(t,mentionFeatureScoreMap.get(0));
		return p;
	}
	
	
	public SententialInstanceGeneration getSig(){return sig;}


	public Mappings getMapping(){return mapping;}
	
	
	/**
	 * args[0] is path to Multir Files directory
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException{
		
		DocumentExtractor de = new DocumentExtractor(args[0],
				new DefaultFeatureGeneratorMinusDirMinusDep(), NERArgumentIdentification.getInstance(), DefaultSententialInstanceGeneration.getInstance());
		
		String testDir = args[1];
		File f = new File(args[1]);
		int count = 0;
		for(File doc : f.listFiles()){
			de.extractFromDocument(doc.getAbsolutePath());
			System.out.println("Processed file " + count);
			count ++;
		}		
	}

}

