/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package org.docx4j.openpackaging.parts;


import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.text.StrTokenizer;
import org.docx4j.jaxb.NamespacePrefixMappings;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/** OPC Parts are either XML, or binary (or text) documents.
 * 
 *  Most are XML documents.
 *  
 *  docx4j aims to represent XML parts using JAXB.  However, 
 *  at present there are some parts for which we don't have
 *  JAXB representations. 
 *  
 *  Until such time as a JAXB representation for an XML Part exists,
 *  the Part should extend this class.   
 *  
 * */
public abstract class XmlPart extends Part {
	
	public XmlPart(PartName partName) throws InvalidFormatException {
		super(partName);
	}

	public XmlPart() throws InvalidFormatException {
		super();
	}
	
	/**
	 * This part's XML contents.  Not guaranteed to be up to date.
	 * Whether it is or not will depend on how the class which extends
	 * Part chooses to treat it.  It may be that the class uses some
	 * other internal representation for its data. 
	 */
	protected Document doc;
	private static XPathFactory xPathFactory;
	private static XPath xPath;
	
	private static DocumentBuilderFactory documentFactory;
	private static DocumentBuilder documentBuilder;
	
	static {
		
		// Crimson doesn't support setTextContent; this.writeDocument also fails.
		// We've already worked around the problem with setTextContent,
		// but rather than do the same for writeDocument,
		// let's just stop using it.
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
			"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
		
		xPathFactory = XPathFactory.newInstance();
		xPath = xPathFactory.newXPath();		
		
		documentFactory = DocumentBuilderFactory.newInstance();
		documentFactory.setNamespaceAware(true);
		try {
			documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}		
		
	}
	
	
	private NamespacePrefixMappings nsContext;
	private NamespacePrefixMappings getNamespaceContext() {
		if (nsContext==null) {
			nsContext = new NamespacePrefixMappings();
			xPath.setNamespaceContext(nsContext);
		}
		return nsContext;
	}

	public void setDocument(InputStream is) throws Docx4JException {
		try {
			doc = documentBuilder.parse(is);
		} catch (Exception e) {
			throw new Docx4JException("Problems parsing InputStream", e);
		} 
	}	

	public void setDocument( org.w3c.dom.Document doc ) {
		this.doc = doc;
	}
	
	public abstract Document getDocument() throws Docx4JException;
	
	public String getXPath(String xpathString, String prefixMappings)  throws Docx4JException {
		try {
			getNamespaceContext().registerPrefixMappings(prefixMappings);
			
			String result = xPath.evaluate(xpathString, doc );
			log.debug(xpathString + " ---> " + result);
			return result;
		} catch (Exception e) {
			throw new Docx4JException("Problems evaluating xpath '" + xpathString + "'", e);
		}
	}
	
	/**
	 * Set the value of the node referenced in the xpath expression.
	 * 
	 * @param xpath
	 * @param value
	 * @param prefixMappings a string such as "xmlns:ns0='http://schemas.medchart'"
	 * @return
	 * @throws Docx4JException
	 */
	public boolean setNodeValueAtXPath(String xpath, String value, String prefixMappings) throws Docx4JException {

		try {
			getNamespaceContext().registerPrefixMappings(prefixMappings);

			Node n = (Node)xPath.evaluate(xpath, doc, XPathConstants.NODE );
			if (n==null) {
				log.debug("xpath returned null");
				return false;
			}
			log.debug(n.getClass().getName());
			
			// Method 1: Crimson throws error
			// Could avoid with System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
			// 		"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
			//n.setTextContent(value);
			
			// Method 2: crimson ignores
			// n.setNodeValue(value);

			// Method 3: createTextNode, then append it
			// First, need to delete/replace existing text node 
			if (n.getChildNodes() !=null
					&& n.getChildNodes().getLength() > 0) {
				NodeList nodes = n.getChildNodes();
				for (int i = nodes.getLength(); i>0; i--) {
					n.removeChild( nodes.item(i-1));
				}
			}
			Text t = n.getOwnerDocument().createTextNode(value);
			n.appendChild(t);			
			
			// cache is now invalid
			return true;
		} catch (Exception e) {
			throw new Docx4JException("Problem setting value at xpath " + xpath);
		} 
		
	}
	
		
}