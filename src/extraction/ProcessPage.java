package extraction;

import java.util.HashMap;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.StyleTag;
import org.htmlparser.tags.TextareaTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;

import dataStructures.NoScriptTag;
import dataStructures.TextUnit;

import extraction.scoring.NormalizedSF;
import extraction.scoring.ScoringFunction;

import websoc_utils.Pair;

public class ProcessPage implements Extractor
	{

	Parser parser;

	String fileName;

	HashMap<Node, Pair<Integer, Integer>> counts;

	Node maxNode;

	ArrayList<TextUnit> extractedTextUnits;

	String extractedTextAsString;

	/** Maximum text any node holds. Need for the scoring function.*/
	int maxText = Integer.MIN_VALUE;

	/** Scoring function to use */
	public static ScoringFunction SCORER;

	
	/** Flag to indicate whether or not to include form tags.*/
	public static boolean ignoreFormTags = true;

	public ProcessPage(String fileName) {
		super();
		this.fileName = fileName;
		counts = new HashMap<Node, Pair<Integer, Integer>>();
		
	}

	/***************************************************************************
	 * Function to set the heuristic used by this class. This must be called
	 * before any call to process
	 * 
	 * @param function
	 */
	public static void setScoringFunction(ScoringFunction function) {

		SCORER = function;
	}

	/***************************************************************************
	 * Function to process the page. This function parses the html, and
	 * recursively calculates the heuristic. It also calls calculateMainNode,
	 * which calculates the node with the highest heuristic value.
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {

		if (SCORER == null)
			throw new Exception("Scoring Function not specified");

		PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
		factory.put("NOSCRIPT", new NoScriptTag());
		parser = new Parser(fileName);
		parser.setNodeFactory(factory);
		Node node;

		for (NodeIterator itr = parser.elements(); itr.hasMoreNodes();) {
			node = itr.nextNode();

			process(node);

		}

		calculateMainNode();

	}

	/***************************************************************************
	 * Recursive function to process a node. Determines if a node is a TextNode,
	 * RemarkNode or TagNode and calls the appropriate function. This function
	 * also calculates maxText.
	 * 
	 * @param node
	 * @return Pair holding the textCount and linkCount
	 * @throws Exception
	 */

	Pair<Integer, Integer> process(Node node) throws Exception {

		Pair<Integer, Integer> nodeCounts = null;

		if (node instanceof RemarkNode) {

			nodeCounts = new Pair<Integer, Integer>(0, 0);
		}

		else if (node instanceof TextNode) {

			TextNode textNode = (TextNode) node;
			nodeCounts = processTextNode(textNode);

		}

		else if (node instanceof TagNode) {

			TagNode tagNode = (TagNode) node;
			nodeCounts = processTagNode(tagNode);

		}

		int textCount = nodeCounts.getFirst();
		int linkCount = nodeCounts.getSecond();

		if (textCount > maxText)
			maxText = textCount;

		counts.put(node, new Pair<Integer, Integer>(textCount, linkCount));

		return new Pair<Integer, Integer>(textCount, linkCount);

	}

	/***************************************************************************
	 * Function to process a textNode. Returns the length of the text from the
	 * node as textCount and 0 as linkCount. If the text contains only nonword
	 * characters, textCount is 0.
	 * 
	 * @param textNode
	 * @return Pair holding textCount and linkCount
	 * @throws Exception
	 */
	Pair<Integer, Integer> processTextNode(TextNode textNode) throws Exception {
		int textCount = 0;
		int linkCount = 0;

		String textFromNode = Translate.decode(textNode.getText());

		if (!textFromNode.matches("[\\W]+")) {
			textCount = textFromNode.length();

		}
		return new Pair<Integer, Integer>(textCount, linkCount);
	}

	/***************************************************************************
	 * Function to process a TagNode. The textCount and linkCount of a TagNode
	 * are sum of the textCounts and linkCounts of its children. 
	 * 
	 * @param tagNode
	 * @return Pair holding textCount and linkCount
	 * @throws Exception
	 */

	Pair<Integer, Integer> processTagNode(TagNode tagNode) throws Exception {

		int textCount = 0;
		int linkCount = 0;

		// Ignore all form tags, if ignoreFormTags is set.

		if (ignoreFormTags) {
			if ((tagNode instanceof SelectTag) || (tagNode instanceof FormTag)
					|| (tagNode instanceof InputTag)
					|| (tagNode instanceof TextareaTag)
					|| (tagNode instanceof OptionTag)) {
				return new Pair<Integer, Integer>(textCount, linkCount);
			}
		}

		// Ignore script and style nodes
		if ((tagNode instanceof NoScriptTag) || (tagNode instanceof StyleTag)) {

			return new Pair<Integer, Integer>(textCount, linkCount);

		}

		if (tagNode instanceof CompositeTag) {

			CompositeTag ctag = (CompositeTag) tagNode;

			// For a LinkTag, textCount is set as 1

			if (ctag instanceof LinkTag) {
				linkCount = 1;
				textCount = 1;

			} else {
				NodeList children = ctag.getChildren();

				if (null != children) {

					for (NodeIterator itr = children.elements(); itr
							.hasMoreNodes();) {
						Node child = itr.nextNode();

						Pair<Integer, Integer> counts = process(child);
						textCount += counts.getFirst();
						linkCount += counts.getSecond();

					}

				}
			}
		}

		return new Pair<Integer, Integer>(textCount, linkCount);
	}

	/***************************************************************************
	 * This function calculates node which has the highest
	 * value of the scoring function and sets maxNode. It also extracts the text
	 * from maxNode and sets extractedTextUnits and extractedSetAsString
	 * 
	 * @throws Exception
	 */
	void calculateMainNode() throws Exception {

		extractedTextUnits = new ArrayList<TextUnit>();

		double maxScore = Double.MIN_VALUE;

		for (Node node : counts.keySet()) {

			Pair<Integer, Integer> count = counts.get(node);

			int numWords = count.getFirst();
			int numLinks = count.getSecond();

			double score = SCORER.getScore(numLinks, numWords, maxText);

			if (score > maxScore) {
				maxScore = score;
				maxNode = node;

			}
		}

		extractedTextUnits.addAll(HTMLUtils.getTextUnits(maxNode));
		

		StringBuffer buffer = new StringBuffer();

		for (TextUnit textUnit : extractedTextUnits) {
			buffer.append(textUnit + "\n");
		}

		extractedTextAsString = buffer.toString();
	}

	public String getExtractedText() {
		return extractedTextAsString;
	}

	public ArrayList<TextUnit> getExtractedTextUnits() {
		return extractedTextUnits;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		String file = "data/cedarExport/taggedDocuments/document_1590.raw"
				+ ".html";
		ProcessPage test = new ProcessPage(file);

		ScoringFunction sf = new NormalizedSF(0.95, 0.05);
		ProcessPage.setScoringFunction(sf);
		test.process();

		// //System.out.println("ARTICLE");

		/*
		 * ArrayList<TextUnit> textUnits = test.getTextUnits();
		 * 
		 * for (TextUnit textUnit: textUnits) {
		 * System.out.println("["+textUnit+"]"+ textUnit.getSize());
		 * System.out.println("--------------"); }
		 */
	}

}
