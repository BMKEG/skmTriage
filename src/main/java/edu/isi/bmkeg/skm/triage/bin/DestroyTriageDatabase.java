package edu.isi.bmkeg.skm.triage.bin;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.isi.bmkeg.skm.triage.cleartk.utils.Options_ImplBase;
import edu.isi.bmkeg.vpdmf.controller.VPDMfKnowledgeBaseBuilder;

public class DestroyTriageDatabase {

	private static Logger logger = Logger.getLogger(BuildTriageDatabase.class);

	public static class Options extends Options_ImplBase {
		
		@Option(name = "-l", usage = "Database login", required = true, metaVar = "LOGIN")
		public String login = "";

		@Option(name = "-p", usage = "Database password", required = true, metaVar = "PASSWD")
		public String password = "";

		@Option(name = "-db", usage = "Database name", required = true, metaVar  = "DBNAME")
		public String dbName = "";

	}

	public static void main(String[] args) {

		Options options = new Options();
		
		CmdLineParser parser = new CmdLineParser(options);
		
		try {
			parser.parseArgument(args);
			
			URL url = ClassLoader.getSystemClassLoader().getResource("edu/isi/bmkeg/skm/triage/triage-mysql.zip");
			String buildFilePath = url.getFile();
			File buildFile = new File( buildFilePath );
			VPDMfKnowledgeBaseBuilder builder = new VPDMfKnowledgeBaseBuilder(buildFile, 
					options.login, options.password, options.dbName); 
			
			if (!builder.checkIfKbExists(options.dbName)) {
				System.err.println("ERROR: Database " + options.dbName + " doesn't exist.");
				System.exit(-1);
			}
			
			builder.destroyDatabase(options.dbName);
			
			logger.info("Triage Database " + options.dbName + " successfully destroyed.");
				
		} catch (CmdLineException e1) {
			
			System.err.println(e1.getMessage());
			System.err.print("Arguments: ");
			parser.printSingleLineUsage(System.err);
			System.err.println("\n\n Options: \n");
			parser.printUsage(System.err);
			System.exit(-1);

		} catch (Exception e) {
			
			e.printStackTrace();
			System.exit(-1);
		
		}
		
	}

}
