package extraction.scoring;

/***
 * Weighted scoring function. The score is calculated as <p> linkWeight*(number_of_words- number_of_Links) /number_of_words + wordWeight*number_of_words/total_words </p>
 * @author jyotika
 *
 */

public class NormalizedSF implements ScoringFunction {

	final double linkWeight;

	final double wordWeight;

	public NormalizedSF(double linkWeight, double wordWeight) {
		super();
		this.linkWeight = linkWeight;
		this.wordWeight = wordWeight;
	}

	public double getScore(int numLinks, int numWords, int totalWords)
			throws Exception {

		double linkRatio = (numWords - numLinks) / (double) numWords;
		double wordRatio = (double) numWords / totalWords;

		return linkWeight * linkRatio + wordWeight * wordRatio;

	}

	public void printFunction() {

		System.out.println("linkWeight*(numWords- numLink)"
				+ "/numWords + wordWeight*numWords/totalWords");
	}

}
