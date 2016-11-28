package edu.washington.multirframework.argumentidentification;

import java.util.ArrayList;
import java.util.List;

//import edu.knowitall.repr.sentence.Sentence;
//import edu.knowitall.tool.typer.Type;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;
import edu.washington.multirframework.data.Argument;
//import edu.washington.multir.taggers.Taggers;
//import edu.washington.cs.knowitall.kbp2014.multir.slotfiller.taggers.Taggers;
//import scala.collection.JavaConversions;

public class KBPTaggersArgumentIdentification implements ArgumentIdentification {

	@Override
	public List<Argument> identifyArguments(Annotation d, CoreMap s) {
		List<Argument> arguments = new ArrayList<>();
		String sentenceText = s.get(CoreAnnotations.TextAnnotation.class);
	
		//List<Type> crimeTypes = JavaConversions.asJavaList(Taggers.crimeTagger.tag(new Sentence(sentenceText)));
		//List<Type> religionTypes = JavaConversions.asJavaList(Taggers.religionTagger.tag(new Sentence(sentenceText)));
		//List<Type> jobTypes = JavaConversions.asJavaList(Taggers.jobTagger.tag(new Sentence(sentenceText)));
		//List<Type> educationTypes = JavaConversions.asJavaList(Taggers.educationTagger.tag(new Sentence(sentenceText)));

		
		
		return arguments;
	}

}
