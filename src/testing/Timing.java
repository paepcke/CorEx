package testing;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.htmlparser.lexer.Lexer;

import websoc_utils.*;
import extraction.Extractor;
import extraction.ProcessPage;
import extraction.SubsetProcessPage;
import extraction.preprocessor.RemovingNoScript;
import extraction.preprocessor.XHTML;
import extraction.scoring.NormalizedSF;
import extraction.scoring.ScoringFunction;
import extraction.scoring.SimpleScoringFunction;

/***
 * Class to calculate the time taken by different configurations.
 * @author jyotika
 *
 */
public class Timing {
	
	/***
	 * Function to print the correct usage
	 *
	 */
	public static void printUsage() {
		System.out
				.println("Arguments: -list listFileName -rpath pathToRawFiles [ -noForms -subset -xhtml -removens] "
						+ "\n listFileName contains the docIds"
						+ "\n rpath lists the path to the directory containing the raw" +
								" files"
						+ "\n Use -noForms to ignore form tags "
						+ "\n Use -subset to extract text using the subset method."
						+ "\n Use -removens to remove the noscript tags."
						+ "\n Use -xhtml to convert the document to xhtml.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		
		System.out.println("Timing");
		//		Get command line arguments
		Map<String, String> options = new HashMap<String, String>();

		options.putAll(CommandLineUtils.simpleCommandLineParser(args));
		
		//Get the arguments

		if (options.isEmpty()) {
			printUsage();
			System.exit(0);
		}
		if (options.containsKey("-help")) {
			printUsage();
			System.exit(0);
		}

		CommandLineUtils.printCommandLineOptions(options);

 
		String listFileName = options.get("-list");
		String rawPath = options.get("-rpath");

		boolean noForms = false;
		boolean subset = false;
		boolean xhtml = false;
		boolean removens = false;
		
		if (options.containsKey("-noForms"))
			noForms = true;

		if (options.containsKey("-subset"))
			subset = true;
		
		if (options.containsKey("-xhtml"))
			xhtml = true;
		
		if (options.containsKey("-removens"))
			removens = true;

		// Read the testSet
		ArrayList<String> testSet = StringUtils.readListFile(listFileName);

		//Initializations
		Lexer.STRICT_REMARKS = false;
		ScoringFunction function = new NormalizedSF(0.99, 0.01);
		
		if (subset) {
			SubsetProcessPage.setScoringFunction(function);
			SubsetProcessPage.ignoreFormTags = noForms;

		} else {
			ProcessPage.setScoringFunction(function);
			ProcessPage.ignoreFormTags = noForms;
		}

		int count = 0;
		
		long startTime = System.currentTimeMillis();

		for (String docId : testSet) {

			try {
				count++;

				String rawPage = StringUtils.readEntire(rawPath + "/document_" + docId + ".raw.html");

				if (removens) {
					
					String removensString = RemovingNoScript.removeNoScript(rawPage);
					rawPage = removensString;
				}
				
				if (xhtml) {
					
					String xhtmlString = XHTML.convertToXHTML(rawPage);
					rawPage = xhtmlString;
				}
				Extractor contentExtractor;

				if (subset) {
					contentExtractor = new SubsetProcessPage(rawPage);
				} else {
					contentExtractor = new ProcessPage(rawPage);
				}

				contentExtractor.process();
				
			} catch (Exception e) {
				
			}

		}
		long endTime = System.currentTimeMillis();
		long timeTaken = endTime - startTime;
			
		System.out.println("Number of files: "+count + "Time Taken: "+timeTaken+"(ms)");
	}



}
