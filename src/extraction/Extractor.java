package extraction;

import java.util.ArrayList;

import dataStructures.TextUnit;

public interface Extractor {

	public void process() throws Exception;

	public String getExtractedText();

	public ArrayList<TextUnit> getExtractedTextUnits();

}
