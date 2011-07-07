package org.openxdata.modelutils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import junit.framework.TestCase;

import org.fcitmuk.epihandy.FormDef;
import org.fcitmuk.epihandy.PageDef;
import org.fcitmuk.epihandy.xform.EpihandyXform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

public class ModelToXMLTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(ModelToXMLTest.class);

	FormDef sampleDef;
	String sampleXml;
	String convertedXml;
	FormDef reparsedDef;
	InputStream convertedStream;
	InputSource convertedSource;
	XPath xpath;

	{
		InputStream sampleStream = ModelToXMLTest.class
				.getResourceAsStream("/sample.xml");

		BufferedReader sampleReader = new BufferedReader(new InputStreamReader(
				sampleStream));
		StringBuilder buf = new StringBuilder();
		String line = null;
		try {
			while ((line = sampleReader.readLine()) != null)
				buf.append(line);
			buf.append('\n');
			sampleXml = buf.toString();
		} catch (IOException e) {
			String msg = "Failed to read in sample.xml";
			log.error(msg, e);
			throw new RuntimeException(msg);
		}

		sampleDef = EpihandyXform
				.fromXform2FormDef(new StringReader(sampleXml));

		convertedXml = ModelToXML.convert(sampleDef);

		reparsedDef = EpihandyXform.fromXform2FormDef(new StringReader(
				convertedXml));

		convertedStream = new ByteArrayInputStream(convertedXml.getBytes());
		convertedSource = new InputSource(convertedStream);

		XPathFactory xpathFactory = javax.xml.xpath.XPathFactory.newInstance();
		xpath = xpathFactory.newXPath();
		xpath.setNamespaceContext(new NamespaceContext());
	}

	public void testNullConversion() {
		try {
			ModelToXML.convert(null);
			fail("Should throw exception with null document.");
		} catch (Exception e) {
		}
	}

	public void testFormConversion() {
		assertEquals(sampleDef.getVariableName(), reparsedDef.getVariableName());
		assertEquals(sampleDef.getDescriptionTemplate(),
				reparsedDef.getDescriptionTemplate());
		assertEquals(sampleDef.getId(), reparsedDef.getId());
		assertEquals(sampleDef.getName(), reparsedDef.getName());
	}

	public void testPageConversion() {
		assertEquals("page counts didn't match", sampleDef.getPageCount(),
				reparsedDef.getPageCount());
		for (int i = 0; i < reparsedDef.getPageCount(); i++) {
			PageDef samplePage = sampleDef.getPageAt(i);
			PageDef reparsedPage = reparsedDef.getPageAt(i);
			assertNotNull("original page should not be null", samplePage);
			assertNotNull("reparsed page should not be null", reparsedPage);
			assertEquals("page numbers should mach", samplePage.getPageNo(),
					reparsedPage.getPageNo());
			assertEquals("page names should match", samplePage.getName(),
					reparsedPage.getName());
		}
	}

	public void testBindingConversion() throws Exception {
		String[] exprs = {
				"count(//xf:bind[@id='patientid' and @nodeset='/patientreg/patientid' and @type='xsd:string'])",
				"count(//xf:bind[@id='picture' and @nodeset='/patientreg/picture' and @type='xsd:base64Binary' and @format='image'])",
				"count(//xf:bind[@id='coughsound' and @nodeset='/patientreg/coughsound' and @format='audio' and @type='xsd:base64Binary'])",
				"count(//xf:bind[@id='recordvideo' and @nodeset='/patientreg/recordvideo' and @format='video' and @type='xsd:base64Binary'])",
				"count(//xf:bind[@id='location' and @nodeset='/patientreg/location' and @format='gps' and @type='xsd:string'])",
				"count(//xf:bind[@id='phone' and @nodeset='/patientreg/phone' and @format='phonenumber' and @type='xsd:string'])",
				"count(//xf:bind[@id='weight' and @nodeset='/patientreg/weight' and @type='xsd:decimal'])",
				"count(//xf:bind[@id='height' and @nodeset='/patientreg/height' and @type='xsd:int'])",
				"count(//xf:bind[@id='birthdate' and @nodeset='/patientreg/birthdate' and @type='xsd:date'])",
				"count(//xf:bind[@id='starttime' and @nodeset='/patientreg/starttime' and @type='xsd:time'])",
				"count(//xf:bind[@id='visitdate' and @nodeset='/patientreg/visitdate' and @type='xsd:dateTime'])" };
		for (String expr : exprs) {
			convertedStream.reset(); // Restore stream state
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " unique binding not present ", 1,
					matchCount.intValue());
		}
	}

	public void testBoundInputConversion() throws Exception {
		String[] oneCountexprs = { "count(//xf:input[@bind='patientid'])",
				"count(//xf:input[@bind='location'])",
				"count(//xf:input[@bind='phone'])",
				"count(//xf:input[@bind='weight'])",
				"count(//xf:input[@bind='height'])",
				"count(//xf:input[@bind='birthdate'])",
				"count(//xf:input[@bind='starttime'])",
				"count(//xf:input[@bind='visitdate'])" };
		for (String expr : oneCountexprs) {
			convertedStream.reset(); // Restore stream state
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " input not present ", 1, matchCount.intValue());
		}

		String[] zeroCountExprs = { "count(//xf:input[@bind='kid'])",
				"count(//xf:input[@bind='kidsex'])",
				"count(//xf:input[@bind='kidage'])",
				"count(//xf:input[@bind='title'])",
				"count(//xf:input[@bind='sex'])",
				"count(//xf:input[@bind='arvs'])",
				"count(//xf:input[@bind='picture'])",
				"count(//xf:input[@bind='coughsound'])",
				"count(//xf:input[@bind='recordvideo'])",
				"count(//xf:input[@bind='continent'])",
				"count(//xf:input[@bind='country'])",
				"count(//xf:input[@bind='district'])",
				"count(//xf:input[@bind='village'])" };
		for (String expr : zeroCountExprs) {
			convertedStream.reset(); // Restore stream state
			XPathExpression compiledExpr = xpath.compile(expr);
			Double matchCount = (Double) compiledExpr.evaluate(convertedSource,
					XPathConstants.NUMBER);
			assertEquals(expr + " input should not be present ", 0,
					matchCount.intValue());
		}
	}
}
