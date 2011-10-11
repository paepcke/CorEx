package extraction.preprocessor;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.ccil.cowan.tagsoup.XMLWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import websoc_utils.*;

/***
 * This class converts given html input into xhtml. 
 * @author jyotika
 *
 */
public class XHTML {

	private static HTMLSchema theSchema = null;
	
	/***
	 * This function converts te HTML specified in src into XHTML. src must contain valid HTML.
	 * @param src
	 * @return src converted to XHTML
	 * @throws Exception
	 */
	public static String convertToXHTML(String src) throws Exception {
		
		OutputStream os = new ByteArrayOutputStream();
		
		process(src,os);
		
		return os.toString();
		
	}
	
	private static void process(String src,OutputStream os)
			throws Exception {
		XMLReader r;
		r = new Parser();
		theSchema = new HTMLSchema();
		r.setProperty(Parser.schemaProperty, theSchema);

		Writer w = new OutputStreamWriter(os);
		
		ContentHandler h = chooseContentHandler(w);
		
		r.setContentHandler(h);
		
		Reader reader = new StringReader(src);
		
		InputSource  s = new InputSource(reader);
		
		r.parse(s);
	}
	
	private static ContentHandler chooseContentHandler(Writer w) {
		XMLWriter x = new XMLWriter(w);
		
		x.setPrefix(theSchema.getURI(), "");
		return x;
		}
	
	
	public static void main(String []args ) throws Exception {
		
		String content = StringUtils.readEntire("data/cedarExport/taggedDocuments/document_10.raw.html");
		System.out.println(convertToXHTML(content));
	}
}
