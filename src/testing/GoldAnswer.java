package testing;

import java.util.ArrayList;
import java.util.HashMap;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.lexer.Lexer;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;

import dataStructures.NoScriptTag;
import dataStructures.TextUnit;


import websoc_utils.StringUtils;

import extraction.*;
import extraction.preprocessor.RemovingNoScript;
import extraction.preprocessor.XHTML;

/***
 * Class to extract the gold answer from the cedar tagged files.
 * @author jyotika
 *
 */


public class GoldAnswer {

	Parser parser;

	String rawFile;
	
	String taggedFile;
	
	public ArrayList<TextUnit> goldList = new ArrayList<TextUnit>();
	
	/** Cedar labels to be extracted */
	static ArrayList<String> cedarLabels = new ArrayList<String>();
	
	ArrayList<TextUnit> listFromGoldAns = new ArrayList<TextUnit>();
	
	/** Map from a node to the text extracted from it */
	HashMap<TextUnit, Node> rawNodesMap = new HashMap<TextUnit, Node>();
	
	HashMap<TextUnit,TextUnit> parentTUMap = new HashMap<TextUnit, TextUnit>();
	
	
	public static void init() {
		cedarLabels.add("CedarContent");
		cedarLabels.add("CedarTitle");
		cedarLabels.add("CedarSubTitle");
		cedarLabels.add("CedarAuthor");
		cedarLabels.add("CedarLocation");
		cedarLabels.add("CedarCopyright");
		cedarLabels.add("CedarAPCopyright");
		cedarLabels.add("CedarSource");
		cedarLabels.add("CedarDate");

	}

	public GoldAnswer(String goldFile, String rawFile) {
		super();
		this.rawFile = rawFile;
		this.taggedFile = goldFile;
		
	}

	/***
	 * Wrapper function for processRaw(Node)
	 * @throws Exception
	 */
public void processRaw() throws Exception {
			
		PrototypicalNodeFactory factory = new PrototypicalNodeFactory ();
		factory.put("noscript", new NoScriptTag());
		parser = new Parser(rawFile);
		parser.setNodeFactory(factory);
		Node node;
		
		for (NodeIterator itr = parser.elements();itr.hasMoreNodes();) {
			node = itr.nextNode();
			
			processRaw(node);
		}
		
	
	}

	
	/***
	 * Recursive function to process the raw file and map every node to the text extracted from it. 
	 * Fills up rawNodesMap.
	 *  
	 * @param node
	 * @return TextUnit
	 * @throws Exception
	 */
	public TextUnit processRaw(Node node) throws Exception {
		
		if (node instanceof NoScriptTag)
			return new TextUnit("");
		
		if (node instanceof TextNode) {
			
			TextNode textNode = (TextNode) node;
			String textFromNode = Translate.decode(textNode.getText());
			
			if (!textFromNode.matches("[\\W]+")) {
				return new TextUnit(textFromNode);
			}

			return new TextUnit("");
		}
		
		NodeList children = node.getChildren();
		
		if (children == null) {
			return new TextUnit("");
		}
		
		StringBuffer buffer = new StringBuffer();
		
		for (NodeIterator itr = children.elements();itr.hasMoreNodes();) {
			Node child = itr.nextNode();
			
			buffer.append(processRaw(child));

		}
		
		String text = buffer.toString();
		rawNodesMap.put(new TextUnit(text), node);
		return new TextUnit(text);
		
	}
	
	/***
	 * Wrapper function for processTagged(Node)
	 * 
	 * @throws Exception
	 */
	public void processTagged() throws Exception {
		
		
		PrototypicalNodeFactory factory = new PrototypicalNodeFactory ();
		factory.put("noscript", new NoScriptTag());
		
			
		parser = new Parser(taggedFile);
		parser.setNodeFactory(factory);
		Node node;
		
		for (NodeIterator itr = parser.elements();itr.hasMoreNodes();) {
			node = itr.nextNode();
			processTagged(node);
		}
		
	
	}
	
	/***
	 * Recursive function to go through the DOM tree of the goldAnswer, and pull out all the text
	 * marked with any on the labels in 'cedarLabels'. It also maps every bit of text to the text 
	 * contained in its parent node, to handle cases where a span has been split by different labels.
	 * 
	 * @param node
	 * @throws Exception
	 */

	public void processTagged(Node node) throws Exception {
		
		if (node instanceof Span) {
			
			Span span = (Span) node;
			String className = span.getAttribute("class");
			
			if (className !=null && cedarLabels.contains(className)) {
			
				String text = getTextFromSpan(span);
				TextUnit unit = new TextUnit(text);
				
				if (null != span.getParent()) {
					
					TextUnit parentText = HTMLUtils.getTextFromNodeAsTU(span.getParent());
					parentTUMap.put(unit, parentText);
				}
				listFromGoldAns.add(unit);
			}
		}

		NodeList children = node.getChildren();
		
		if (children == null) {
			return;
		}
		
		for (NodeIterator itr = children.elements();itr.hasMoreNodes();) {
			Node child = itr.nextNode();
			
			processTagged(child);

		}
		
	}

	/***
	 * Function to extract text from a span
	 * 
	 * @param span
	 * @return
	 * @throws Exception
	 */
	public String getTextFromSpan(Span span) throws Exception {
		
		String textFromNode = Translate.decode(span.toPlainTextString());
		
		if (!textFromNode.matches("[\\W]+")) 
			return textFromNode;
		
		else 
			return null;
	}
	
	/***
	 * This function goes through each TextUnit in 'listFromGoldAns' and looks up the node that corresponds
	 * to it from 'rawNodesMap'. If a node is found, it adds all the TextUnits from that node to 'goldList'. 
	 * Else, it looks for the parent from 'parentTUMap' and tries to find a node that corresponds to
	 * the parent.
	 * 
	 * @throws Exception
	 */
	void generateTextUnits() throws Exception {

		for (TextUnit unit : listFromGoldAns) {

			Node node = rawNodesMap.get(unit);

			if (node != null) {
				ArrayList<TextUnit> units = HTMLUtils.getTextUnits(node);
				for (TextUnit newUnit : units)
					if (!goldList.contains(newUnit))
						goldList.add(newUnit);
			}

			else {
				TextUnit parentTextUnit = parentTUMap.get(unit);
				Node parentNode = rawNodesMap.get(parentTextUnit);

				if (null != parentNode) {

					ArrayList<TextUnit> parentUnits = HTMLUtils
							.getTextUnits(parentNode);

					for (TextUnit newUnit : parentUnits)
						if (!goldList.contains(newUnit))
							goldList.add(newUnit);
				} else {
					System.err.println("ERROR: " + taggedFile + " " + unit);
					goldList.add(unit);

				}
			}

		}

	}
	
	/***
	 * Top level function to call all the necessary functions.
	 * 
	 * @return The gold list of TextUnits
	 * @throws Exception
	 */
	public ArrayList<TextUnit> process() throws Exception {
		
		processRaw();
		processTagged();
		generateTextUnits();
		
		return goldList;
	}
	
	public static void main(String[] args) throws Exception{
		
		
		Lexer.STRICT_REMARKS = false;
		
		GoldAnswer.init();
		
		String raw = StringUtils.readEntire("data/cedarExport/taggedDocuments/document_1313.raw.html");
		raw = RemovingNoScript.removeNoScript(raw);
		raw = XHTML.convertToXHTML(raw);
		
		GoldAnswer test = new GoldAnswer("data/cedarExport/taggedDocuments/document_1313.tagged.html",
				raw);
		
		ArrayList<TextUnit> goldList = test.process();
		
		System.out.println("-----------GOLD ANSWER-----------");
		
		for (TextUnit unit: goldList) {
			System.out.println(unit);
		}
		
	
		
		
			
	}
	
	
}
