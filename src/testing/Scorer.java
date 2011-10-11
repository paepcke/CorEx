package testing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.htmlparser.lexer.Lexer;

import dataStructures.TextUnit;

import extraction.Extractor;
import extraction.ProcessPage;
import extraction.SubsetProcessPage;
import extraction.preprocessor.RemovingNoScript;
import extraction.preprocessor.XHTML;
import extraction.scoring.NormalizedSF;
import extraction.scoring.ScoringFunction;
import extraction.scoring.SimpleScoringFunction;

import websoc_utils.*;

public class Scorer {

	/***
	 * Function to calculate the size of the intersection between the two sets
	 * @param goldList
	 * @param extracted
	 * @return
	 */
	public static double intersectionSize(ArrayList<TextUnit> goldList,
			ArrayList<TextUnit> extracted, boolean block) {

		double intersectionSize = 0;

		ArrayList<TextUnit> goldListCopy = new ArrayList<TextUnit>(goldList);
		for (TextUnit textUnit : extracted) {

			if (goldListCopy.contains(textUnit)) {
				goldListCopy.remove(textUnit);
				
				if (block) {
					intersectionSize += 1;
				} else {
					intersectionSize += textUnit.getSize();
				}
			}

		}

		//System.out.println(intersectionSize+" "+goldList.size()+" "+extracted.size());

		return intersectionSize;

	}

	/***
	 * Function to calculate the total size of an ArrayList of TextUnits.
	 * @param list
	 * @return double
	 */

	public static double getSize(ArrayList<TextUnit> list) {

		double size = 0;

		for (TextUnit unit : list)
			size += unit.getSize();

		return size;
	}


	/***
	 * Function to print the correct usage
	 *
	 */
	public static void printUsage() {
		System.out
				.println("Arguments: -list listFileName -outFile outFileName -gpath path -rpath [ -f -noForms -subset -block -xhtml -removens] "
						+ "\n listFileName contains the docIds"
						+ "\n gpath lists the path to the directory containing the tagged files"
						+ "\n rpath lists the path to the directory containing the raw" +
								" files"
						+ "\n outFile name of the file to write the output to"
						+ "\n Use -f to calculate and print precision and recall "
						+ "\n Use -noForms to ignore form tags "
						+ "\n Use -subset to extract text using the subset method."
						+ "\n Use -removens to remove the noscript tags."
						+ "\n Use -xhtml to convert the document to xhtml."
						+ "\n Use -block to calculate precision and recall at block-level.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		//		Get command line arguments
		Map<String, String> options = new HashMap<String, String>();

		options.putAll(CommandLineUtils.simpleCommandLineParser(args));

		if (options.isEmpty()) {
			printUsage();
			System.exit(0);
		}
		if (options.containsKey("-help")) {
			printUsage();
			System.exit(0);
		}
		
		CommandLineUtils.printCommandLineOptions(options);

		//Get the arguments


		String outFile = options.get("-outFile");
		String listFileName = options.get("-list");
		String goldPath = options.get("-gpath");
		String rawPath = options.get("-rpath");

		boolean f = false;
		boolean noForms = false;
		boolean subset = false;
		boolean block = false;
		boolean xhtml = false;
		boolean removens = false;
		
		if (options.containsKey("-f"))
			f = true;

		if (options.containsKey("-noForms"))
			noForms = true;

		if (options.containsKey("-subset"))
			subset = true;

		if (options.containsKey("-block"))
			block = true;
		
		if (options.containsKey("-xhtml"))
			xhtml = true;
		
		if (options.containsKey("-removens"))
			removens = true;
		
		
		// Read the testSet
		ArrayList<String> testSet = StringUtils.readListFile(listFileName);

		//Initializations
		Lexer.STRICT_REMARKS = false;
		ScoringFunction function = new SimpleScoringFunction();//NormalizedSF(0.99, 0.01);
		GoldAnswer.init();

		if (subset) {
			SubsetProcessPage.setScoringFunction(function);
			SubsetProcessPage.ignoreFormTags = noForms;

		} else {
			ProcessPage.setScoringFunction(function);
			ProcessPage.ignoreFormTags = noForms;
		}

		double totalPrecision = 0;
		double totalRecall = 0;
		double totalIntSize = 0;
		double totalExtSize = 0;
		double totalGoldSize = 0;

		double averagePrecision = 0;
		double averageRecall = 0;
		
		// Prepare output file.
		BufferedWriter writer = null;

		if (outFile != null)
			writer = new BufferedWriter(new FileWriter(outFile));

		int count = 0;

		System.out.print("page\t");

		if (f) {
			System.out.print("precision\trecall\n");
		}

		for (String docId : testSet) {

			try {
				count++;

				String goldAnswer = goldPath + "/document_" + docId
						+ ".tagged.html";
				String rawPage = StringUtils.readEntire(rawPath + "/document_" + docId + ".raw.html");

				if (removens) {
					
					String removensString = RemovingNoScript.removeNoScript(rawPage);
					rawPage = removensString;
				}
				
				if (xhtml) {
					
					String xhtmlString = XHTML.convertToXHTML(rawPage);
					rawPage = xhtmlString;
				}
				
				GoldAnswer goldSetExtractor = new GoldAnswer(goldAnswer,
						rawPage);
				ArrayList<TextUnit> goldList = goldSetExtractor.process();

				
				
				Extractor contentExtractor;

				// The isFile parameter in the content extractor is the inverse of preprocess,
				// since when no pre-processing is done, the file is directly fed to the
				// contentExtractor.
				
				if (subset) {
					contentExtractor = new SubsetProcessPage(rawPage);
				} else {
					contentExtractor = new ProcessPage(rawPage);
				}

				contentExtractor.process();
				ArrayList<TextUnit> extractedList = contentExtractor
						.getExtractedTextUnits();

				double intSize = intersectionSize(goldList, extractedList, block);
				double extractedSize;
				double goldSize;

				if (block) {
					extractedSize = extractedList.size();
					goldSize = goldList.size();
				} else {
					extractedSize = getSize(extractedList);
					goldSize = getSize(goldList);
				}

			//	System.out.print(docId + "\t");

				double precision = 0;
				double recall = 0;

				if (f) {
					precision = intSize / extractedSize;
					recall = intSize / goldSize;

					averagePrecision += precision;
					averageRecall += recall;
					
					totalIntSize += intSize;
					totalExtSize += extractedSize;
					totalGoldSize += goldSize;

			//		System.out.print(precision + "\t" + recall + "\t");

				}

			//	System.out.print("\n");

				if (null != outFile) {
					writer.write(docId + "\t");

					if (f)
						writer.write(precision + "\t" + recall + "\t");

					writer.newLine();
				}
			} catch (Exception e) {
				
			}

		}

		if (f) {
			totalPrecision = totalIntSize / totalExtSize;
			totalRecall = totalIntSize / totalGoldSize;

			System.out.println("Average Precision: " + totalPrecision+" "+averagePrecision/count);
			System.out.println("Average Recall: " + totalRecall+" "+averageRecall/count);
		}

		if (null != outFile)
			writer.close();
	}

}
