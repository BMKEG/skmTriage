package edu.isi.bmkeg.skm.triage.cleartk.instrinsicEval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.cleartk.classifier.libsvm.LIBSVMBooleanOutcomeDataWriter;
import org.cleartk.util.Options_ImplBase;
import org.kohsuke.args4j.Option;

public class CrossValEvalOptions extends Options_ImplBase {
	
	@Option(name = "-data", usage = "Specify the data base directory. Training " +
			"documents will be found in <base>/train and testing documents will " +
			"be found in <base>/test")
	public File dataDirectory;
	
	@Option(name = "-nFolds", usage = "Number of folds to use in the evaluation")
	public int nFolds = 1;
	
	@Option(name = "-base", usage = "specify the directory in which to write out the trained model files")
	public File baseDir = new File(
			"target/document_classification/models");

	@Option(name = "-data-writer", usage = "specify the DataWriter class name")
	public String dataWriterClassName = LIBSVMBooleanOutcomeDataWriter.class.getName();
	
	@Option(name = "-training-args", usage = "specify training arguments to be passed to the learner.  For multiple values specify -ta for each - e.g. '-ta -t -ta 0'")
	public List<String> trainingArguments = new ArrayList<String>();

}