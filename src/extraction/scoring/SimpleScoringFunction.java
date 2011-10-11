package extraction.scoring;

/***
 * A simple implementation of ScoringFunction which calculates the score as <p> (number_of_words - number_of_links) / number_of_words </p>
 * @author jyotika
 *
 */
public class SimpleScoringFunction implements ScoringFunction {
	

	public double getScore(int numLinks, int numWords, int maxWords)
			throws Exception {
		double linkRatio = (numWords - numLinks) / (double) numWords;
		
		return linkRatio;
	}

	public void printFunction() {
		System.out.println("(numWords- numLink)"
				+ "/numWords");

	}

}
