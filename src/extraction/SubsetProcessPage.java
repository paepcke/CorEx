package extraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.InputTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.tags.StyleTag;
import org.htmlparser.tags.TextareaTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;

import dataStructures.TextUnit;

import websoc_utils.Pair;
import extraction.scoring.*;




public class SubsetProcessPage implements Extractor {

	Parser parser;

	String html;

	/** Indicates whether the string in 'html' is a file or the actual html;*/
	boolean isFile;

	HashMap<Node, Pair<Integer, Integer>> counts;

	HashMap<ArrayList<Node>, Pair<Integer, Integer>> bestSubsets;
	
	HashMap<ArrayList<Node>, Integer> levels;

	HashMap<Node, NodeList> subsets;

	ArrayList<Node> maxSet = new ArrayList<Node>();

	String extractedText;

	int level = 0;
	
	ArrayList<TextUnit> extractedTextUnits;

	String extractedTextAsString;
	
	/** Maximum text any node holds. Need for the scoring function.*/
	int maxText = Integer.MIN_VALUE;

	/** Scoring function to use. Default is a NormalizedScoringFunction with weights 0.99, 0.01 */
	public static ScoringFunction SCORER = new NormalizedSF(0.99,0.01);

	/** Cut-off for including a child in the subset (Used by processTagNode).*/
	public static double CHILDSCORETHRESHOLD = 0.99;

	/** Flag to indicate whether or not to include form tags. Default is true*/
	public static boolean ignoreFormTags = true;

	public SubsetProcessPage(String fileName) {
		super();
		this.html = fileName;
		counts = new HashMap<Node, Pair<Integer, Integer>>();
		bestSubsets = new HashMap<ArrayList<Node>, Pair<Integer, Integer>>();
		levels = new HashMap<ArrayList<Node>, Integer>();
		
	}
	

	/***************************************************************************
	 * Function to set the heuristic used by this class.
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

		parser = new Parser(html);
		
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

		level++;
		
		
		
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

		level--;
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
	 * are sum of the textCounts and linkCounts of its children. This function
	 * also calculates the right subset of children for each node and adds it to
	 * bestSubsets.
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
		if ((tagNode instanceof ScriptTag) || (tagNode instanceof StyleTag)) {

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

					ArrayList<Node> subset = new ArrayList<Node>();
					int subsetText = 0;
					int subsetLink = 0;

					for (NodeIterator itr = children.elements(); itr
							.hasMoreNodes();) {
						Node child = itr.nextNode();

						Pair<Integer, Integer> counts = process(child);
						double childTextCount = counts.getFirst();
						double childLinkCount = counts.getSecond();

						textCount += childTextCount;
						linkCount += childLinkCount;

						double childScore;

						if (childTextCount == 0)
							childScore = 0;
						else
							childScore = (childTextCount - childLinkCount)
									/ childTextCount;

						if (childScore > 0.9) {
							subset.add(child);
							subsetText += childTextCount;
							subsetLink += childLinkCount;
						}

					}

					bestSubsets.put(subset, new Pair<Integer, Integer>(
							subsetText, subsetLink));
					levels.put(subset, level);
				}
			}
		}

		return new Pair<Integer, Integer>(textCount, linkCount);
	}

	/***************************************************************************
	 * This function calculates subset in bestSubsets which has the highest
	 * value of the scoring function and sets maxSet. It also extracts the text
	 * from maxSet and sets extractedTextUnits and extractedSetAsString
	 * 
	 * @throws Exception
	 */
	void calculateMainNode() throws Exception {

		StringBuffer buffer = new StringBuffer();
		extractedTextUnits = new ArrayList<TextUnit>();
		
		double maxScore = Double.MIN_VALUE;

		for (ArrayList<Node> nodes : bestSubsets.keySet()) {

			Pair<Integer, Integer> count = bestSubsets.get(nodes);

			int numWords = count.getFirst();
			int numLinks = count.getSecond();

			double score = SCORER.getScore(numLinks, numWords, maxText);

			if (score > maxScore) {
				maxScore = score;
				maxSet = nodes;

			} else if (score == maxScore) {
				if (levels.get(nodes) < levels.get(maxSet)) {
					maxScore = score;
					maxSet = nodes;
				}
			}
			
			
		}

		//System.out.println(levels.get(maxSet));
		for (Node node : maxSet) {
			
			String text = HTMLUtils.getTextFromNodeAsTU(node).getText();
			extractedTextUnits.addAll(HTMLUtils.getTextUnits(node));
			
			if (text != null)
				buffer.append(text);
		}

	//	findClosestHeading(maxSet.get(0).getParent());
		
		

		extractedText = buffer.toString();
	}

	public String getExtractedText() {
		return extractedText;
	}

	public ArrayList<TextUnit> getExtractedTextUnits() {
		return extractedTextUnits;
	}



}
