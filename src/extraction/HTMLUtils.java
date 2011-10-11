package extraction;

import java.util.ArrayList;

import org.htmlparser.Node;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.ScriptTag;
import org.htmlparser.tags.StyleTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;

import dataStructures.NoScriptTag;
import dataStructures.TextUnit;


public class HTMLUtils {

	/***************************************************************************
	 * Function to extract text from a node.
	 * 
	 * @param node
	 * @return ArrayList<TextUnit>
	 * @throws Exception
	 */

	public static ArrayList<TextUnit> getTextUnits(Node node) throws Exception {

		ArrayList<TextUnit> textUnits = new ArrayList<TextUnit>();

		// If node is a TextNode, return the text if text contains word
		// characters.
		if (node instanceof TextNode) {

			TextNode textNode = (TextNode) node;
			String textFromNode = Translate.decode(textNode.getText());

			if (!textFromNode.matches("[\\W]+")) {
				textUnits.add(new TextUnit(textFromNode));
			}
			return textUnits;

		}

		
		 // If node is a TagNode, extract text from all its children and combine
		 // them in an ArrayList, which is returned.
		 

		if (node instanceof TagNode) {
			TagNode tagNode = (TagNode) node;

			// Ignore script, style and form tags.
			if ((tagNode instanceof ScriptTag)
					|| (tagNode instanceof NoScriptTag)
					|| (tagNode instanceof StyleTag)
					|| (tagNode instanceof FormTag)) {

				return textUnits;

			}
		}

		NodeList children = node.getChildren();

		if (children == null)
			return textUnits;

		for (NodeIterator itr = children.elements(); itr.hasMoreNodes();) {

			Node child = itr.nextNode();
			textUnits.addAll(getTextUnits(child));

		}

		return textUnits;
	}
	
	public static TextUnit getTextFromNodeAsTU(Node node) throws Exception{
		
		ArrayList<TextUnit> textUnits = getTextUnits(node);
		
		StringBuffer buffer = new StringBuffer();
		
		for (TextUnit textUnit : textUnits) {
			buffer.append("\n" + textUnit.toString());
		}
		
		String text = buffer.toString();
		
		return new TextUnit(text);

	
	}
}
