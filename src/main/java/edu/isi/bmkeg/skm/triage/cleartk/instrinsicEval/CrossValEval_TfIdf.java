/** 
 * Copyright (c) 2007-2012, Regents of the University of Colorado 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 * Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. 
 */
package edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.DataWriter;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.transform.InstanceDataWriter;
import org.cleartk.ml.feature.transform.InstanceStream;
import org.cleartk.ml.feature.transform.extractor.TfidfExtractor;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.libsvm.LibSvmBooleanOutcomeDataWriter;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.uimafit.component.ViewTextCopierAnnotator;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.pipeline.SimplePipeline;
import org.uimafit.testing.util.HideOutput;
import org.uimafit.util.JCasUtil;

import com.google.common.base.Function;

import edu.isi.bmkeg.skm.cleartk.type.CatorgorizedFtdText;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.GoldDocumentCategoryAnnotator;
import edu.isi.bmkeg.skm.triage.cleartk.annotators.TfIdf_Annotator;

/**
 * <br>
 * Copyright (c) 2012, Regents of the University of Colorado <br>
 * All rights reserved.
 * <p>
 * This evaluation class provides a concrete example of how to train and
 * evaluate classifiers. Specifically this class will train a document
 * categorizer using a subset of the 20 newsgroups dataset. It evaluates
 * performance using 2-fold cross validation as well as a holdout set.
 * <p>
 * 
 * Key points: <br>
 * <ul>
 * <li>Creating training and evaluation pipelines
 * <li>Example of feature transformation / normalization
 * </ul>
 * 
 * 
 * @author Lee Becker
 */
public class CrossValEval_TfIdf extends
		CrossValidationEvaluation {

	private static Logger logger = Logger
			.getLogger(CrossValEval_TfIdf.class);

	public List<String> trainingArguments;
	
	public String dataWriterClassName;

	public static void main(String[] args) throws Exception {
		CrossValEvalOptions options = new CrossValEvalOptions();
		options.parseOptions(args);

		if (options.trainingArguments.size() == 0
				&& options.dataWriterClassName
						.equals(LibSvmBooleanOutcomeDataWriter.class.getName())) {
			options.trainingArguments = Arrays.asList("-t", "0");
		}

		CrossValEval_TfIdf eval = new CrossValEval_TfIdf(
				options.baseDir, 
				options.trainingArguments,
				options.dataWriterClassName,
				options.nFolds,
				options.dataDirectory);

		eval.runMain();

	}

	public CrossValEval_TfIdf(File baseDirectory,
			List<String> trainingArguments,
			String dataWriterClassName,			
			int nFolds,
			File dataDir) {

		super(baseDirectory, dataDir);
		this.trainingArguments = trainingArguments;
		this.dataWriterClassName = dataWriterClassName;
		this.nFolds = nFolds;
	}

	@Override
	public void train(CollectionReader collectionReader, File outputDirectory)
			throws Exception {

		/** 
		 * Step 1: Extract features and serialize the raw instance objects
		 */
		logger.info("1. Extracting features and writing raw instances data");

		// Create and run the document classification training pipeline
		AggregateBuilder builder = new AggregateBuilder();

		// NLP pre-processing components
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));
		builder.add(AnalysisEngineFactory
				.createPrimitiveDescription(GoldDocumentCategoryAnnotator.class));
		
		// TF-IDF annotator, need to use InstandDataWriter to save the raw 
		// scores for subsequent inclusion intot 
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				TfIdf_Annotator.class,
				CleartkAnnotator.PARAM_IS_TRAINING, true,
				DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, InstanceDataWriter.class.getName(),
				DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, outputDirectory));

		// run the pipeline
		SimplePipeline.runPipeline(collectionReader,
				builder.createAggregateDescription());

		/**
		 * Step 2: Transform features and write training data
		 * In this phase, the normalization statistics are computed and the raw
		 * features are transformed into normalized features.
		 * Then the adjusted values are written with a DataWriter
		 * for training
		 */
		logger.info("2. Collection feature normalization statistics");

		// Load the serialized instance data
		Iterable<Instance<Boolean>> instances = InstanceStream
				.loadFromDirectory(outputDirectory);

		// Collect TF*IDF stats for computing tf*idf values on extracted tokens
		URI unigramTfIdfDataURI = TfIdf_Annotator.createTokenTfIdfDataURI(outputDirectory, "unigram");
		TfidfExtractor<Boolean, DocumentAnnotation> extractor1 = new TfidfExtractor<Boolean, DocumentAnnotation>("unigram");
		extractor1.train(instances);
		extractor1.save(unigramTfIdfDataURI);

		/*URI bigramTfIdfDataURI = MGI_FeatureEngineering_Annotator.createTokenTfIdfDataURI(outputDirectory, "bigram");
		TfidfExtractor<Boolean, DocumentAnnotation> extractor2 = new TfidfExtractor<Boolean, DocumentAnnotation>("bigram");
		extractor2.train(instances);
		extractor2.save(bigramTfIdfDataURI);*/
		
		/**
		 * Step 3: Iterate through the instances of existing features and run the tfIdf
		 * transform on them
		 */
		logger.info("3. Write out model training data");
		DataWriter<Boolean> dataWriter = createDataWriter(dataWriterClassName, outputDirectory);	
		for (Instance<Boolean> instance : instances) {
			Instance<Boolean> instance2 = extractor1.transform(instance);
			//Instance<Boolean> instance3 = extractor2.transform(instance2);
			dataWriter.write(instance2);
		}
		dataWriter.finish();

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		logger.info("4. Train model and write model.jar file.\n");
		String[] tArgs = this.trainingArguments
				.toArray(new String[this.trainingArguments.size()]);
		HideOutput hider = new HideOutput();
		JarClassifierBuilder.trainAndPackage(outputDirectory, tArgs);
		hider.restoreOutput();
	}

	@Override
	protected AnnotationStatistics<String> test (CollectionReader collectionReader, 
			File directory) throws Exception {
		
		AnnotationStatistics<String> stats = new AnnotationStatistics<String>();

		// Create the document classification pipeline
		AggregateBuilder builder = new AggregateBuilder();

		// NLP pre-processing components
		builder.add(SentenceAnnotator.getDescription());
		builder.add(TokenAnnotator.getDescription());
		builder.add(DefaultSnowballStemmer.getDescription("English"));

		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				ViewTextCopierAnnotator.class,
				ViewTextCopierAnnotator.PARAM_SOURCE_VIEW_NAME,
				SYSTEM_VIEW_NAME,
				ViewTextCopierAnnotator.PARAM_DESTINATION_VIEW_NAME,
				GOLD_VIEW_NAME));

		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				GoldDocumentCategoryAnnotator.class
				), SYSTEM_VIEW_NAME, GOLD_VIEW_NAME);

		File classifierJarPath = new File(directory, "model.jar");

		builder.add(AnalysisEngineFactory.createPrimitiveDescription(
				TfIdf_Annotator.class,
				TfIdf_Annotator.PARAM_UNI_TF_IDF_URI, 
				TfIdf_Annotator.createTokenTfIdfDataURI(directory, "unigram"),
				//MGI_FeatureEngineering_Annotator.PARAM_BI_TF_IDF_URI, 
				//MGI_FeatureEngineering_Annotator.createTokenTfIdfDataURI(directory, "bigram"),
				CleartkAnnotator.PARAM_IS_TRAINING, false,
				GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
				classifierJarPath));

		AnalysisEngine engine = builder.createAggregate();

		// Run and evaluate
		Function<CatorgorizedFtdText, ?> getSpan = AnnotationStatistics.annotationToSpan();
		Function<CatorgorizedFtdText, String> getCategory = AnnotationStatistics.annotationToFeatureValue("category");

		for (JCas jCas : new JCasIterable(collectionReader, engine)) {
			JCas goldView = jCas.getView(GOLD_VIEW_NAME);
			JCas systemView = jCas.getView(SYSTEM_VIEW_NAME);

			// Get results from system and gold views, and update results
			// accordingly
			Collection<CatorgorizedFtdText> goldCategories = JCasUtil.select(
					goldView, CatorgorizedFtdText.class);
			Collection<CatorgorizedFtdText> systemCategories = JCasUtil.select(
					systemView, CatorgorizedFtdText.class);
			stats.add(goldCategories, systemCategories, getSpan, getCategory);
		
		}

		return stats;

	}
	
	@SuppressWarnings("unchecked")
	private DataWriter<Boolean> createDataWriter(String dataWriterClassName, File outputDirectory) {
		
		try {

			Class<? extends DataWriter<Boolean>> cls = 			
			(Class<? extends DataWriter<Boolean>>) Class.forName(dataWriterClassName).asSubclass(DataWriter.class);
			return cls.getConstructor(File.class).newInstance(outputDirectory);

		} catch (Exception e) {
		
			throw new IllegalStateException("Failed to create an instance of DataWriter<boolean> from classname: " + dataWriterClassName, e);
		
		}
	
	}

}