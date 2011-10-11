package extraction.preprocessor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * Class with static functions to remove script and noscript nodes.
 * @author jyotika
 *
 */
public class RemovingNoScript {

	static Pattern noscript = Pattern.compile("<NOSCRIPT(.*?)</NOSCRIPT>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	static Pattern script = Pattern.compile("<SCRIPT(.*?)</SCRIPT>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
	/***
	 * This function removes all noscript nodes from the string specified in src. The string must be
	 * valid HTML. 
	 * @param src
	 * @return src with all noscript nodes removed.
	 * @throws Exception
	 */
	public static String removeNoScript(String src) throws Exception {
		
		Matcher m = noscript.matcher(src);
		String replaced = "";
		while (m.find()) {
			replaced  = m.replaceAll("<!-- Removed Script -->");
			
			m = noscript.matcher(replaced);
			
		}
		
		return replaced;
	}
	
	/***
	 * This function removes all script nodes from the string specified in src. The string must be
	 * valid HTML. 
	 * @param src
	 * @return src with all script nodes removed.
	 * @throws Exception
	 */
	public static String removeScript(String src) throws Exception {
		
		Matcher m = script.matcher(src);
		String replaced = "";
		while (m.find()) {
			replaced  = m.replaceAll("<!-- Removed Script -->");
			
			m = script.matcher(replaced);
			
		}
		
		return replaced;
	}
	
	public static void main(String[] args) throws Exception {
		
		
		String example = "<NOSCRIPT> stuff inside \n  and more </NOSCRIPt> blah blah <NOSCRIPT> stuff inside \n and some more </NOSCRIPt>";
		
		System.out.println("Using removeNoScript\n"+removeNoScript(example));
		System.out.println("Using removeScript\n"+removeScript(example));
		
		
		
	}
}
