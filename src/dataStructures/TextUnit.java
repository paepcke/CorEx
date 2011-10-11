package dataStructures;

/***
 * This class is a wrapper over a piece of text extracted from an HTML page. It contains a representation
 * of the text with all non-Word characters removed, which is the basis for comparison of two
 * TextUnits.
 * @author jyotika
 *
 */
public class TextUnit {

	String text;
	String noSpace;
	long size;
	
	
	public TextUnit(String text) {
		super();
		this.text = text;
		noSpace =  text.replaceAll("[\\W]", "");
		size = countWords();
	}

	private  long countWords() {
		long numWords = 0;
		int index = 0;
		boolean prevWhitespace = true;
		while (index < text.length()) {
			char c = text.charAt(index++);
			boolean currWhitespace = Character.isWhitespace(c);
			if (prevWhitespace && !currWhitespace) {
				numWords++;
			}
			prevWhitespace = currWhitespace;
		}
		return numWords;
	}
	
	
	@Override
	public int hashCode() {
		return noSpace.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof TextUnit) {
			
			TextUnit other = (TextUnit) obj;
			
			return noSpace.equals(other.noSpace);
		}
		return false;
	}

	@Override
	public String toString() {
		return text;
	}

	public long getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	
	
	
	
	
	
	
}
