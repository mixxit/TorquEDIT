package torquedit.tseditor.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.helpers.DefaultHandler;

public class XmlResourceParser 
	extends DefaultHandler {
	
	SAXParserFactory factory;
	SAXParser parser;
	List<XmlResource> retList = null;
	String curName = null;
	String curDesc = null;
	String characters = null;
	
	public XmlResourceParser() {
		
		try {
			factory = SAXParserFactory.newInstance();
		} catch(Throwable t) {
			throw new RuntimeException(t);
		}
		
	}
	
	public List<XmlResource> parse(InputStream ips) {
		try {
			retList = new ArrayList<XmlResource>();
			factory.newSAXParser().parse(ips, this);
			return retList;
		} catch(Throwable t) {
			throw new RuntimeException(t);
		}
	}
	
	public void characters(char[] ch, int start, int length) {
		char[] c = new char[length];
		System.arraycopy(ch, start, c, 0, length);
		characters = new String(c);
	}
	
	public void endElement(String uri, String localName, String qName)  {
		if (qName.toLowerCase().equals("name")) {
			curName = characters;
			characters = null;
			
		} else if (qName.toLowerCase().equals("description")) {
			curDesc = characters;
			characters = null;
			
		} else if (qName.toLowerCase().equals("resource")) {
			retList.add( new XmlResource(curName, curDesc));
			curName = null;
			curDesc = null;
		}
	}
	
}