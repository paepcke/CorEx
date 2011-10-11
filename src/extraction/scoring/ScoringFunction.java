package extraction.scoring;

/***
 * Interface for a functions to calculate the heuristic used to
 * measure text to link ratios.
 * @author jyotika
 *
 */
public interface ScoringFunction {

	
	public double getScore(int numLinks, int numWords, int maxWords) throws Exception;
	
	public void printFunction();
}
