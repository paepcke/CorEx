package testing;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;


import dataStructures.TextUnit;
import extraction.HTMLUtils;

public class DimensionsForBreakDown {

	int totalTextSize = 0;
	
	int numNodes = 0;
	
	int numImages = 0;
	
	String fileName;
	
	
	
	public DimensionsForBreakDown(String fileName) {
		super();
		this.fileName = fileName;
	}

	public void process() throws Exception {
		
		Parser parser = new Parser(fileName);
		Node node;
		
		for (NodeIterator itr = parser.elements();itr.hasMoreNodes();) {
			node = itr.nextNode();
			process(node);
			
			TextUnit unit = HTMLUtils.getTextFromNodeAsTU(node);
			totalTextSize += unit.getSize();
			
		}
		
		
	
	}
	
	public void process(Node node) throws Exception {
		
		if (! (node instanceof RemarkNode)) {
			numNodes++;
		}
		
		if (node instanceof ImageTag) {
			numImages++;
		}
		
		NodeList children = node.getChildren();
		
		if (children == null) {
			return;
		}
		
		for (NodeIterator itr = children.elements();itr.hasMoreNodes();) {
			Node child = itr.nextNode();
			
			process(child);

		}
		
	}
	
	
	public int getNumImages() {
		return numImages;
	}

	public void setNumImages(int numImages) {
		this.numImages = numImages;
	}

	public int getNumNodes() {
		return numNodes;
	}

	public void setNumNodes(int numNodes) {
		this.numNodes = numNodes;
	}

	public int getTotalTextSize() {
		return totalTextSize;
	}

	public void setTotalTextSize(int totalTextSize) {
		this.totalTextSize = totalTextSize;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		DimensionsForBreakDown t = new DimensionsForBreakDown("data/cedarExport/taggedDocuments/document_3049.tagged" +
				".html");
		t.process();
		
		System.out.println(t.getNumImages());
		System.out.println(t.getTotalTextSize());
	}

}
