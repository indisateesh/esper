package net.esper.regression.event;

import junit.framework.TestCase;
import net.esper.client.*;
import net.esper.client.time.TimerControlEvent;
import net.esper.event.EventBean;
import net.esper.support.util.SupportUpdateListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import java.io.StringReader;

public class TestNoSchemaXMLEvent extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener updateListener;

    private static String XML =
        "<myevent>\n" +
        "  <element1>VAL1</element1>\n" +
        "  <element2>\n" +
        "    <element21 id=\"e21_1\">VAL21-1</element21>\n" +
        "    <element21 id=\"e21_2\">VAL21-2</element21>\n" +
        "  </element2>\n" +
        "  <element3 attrString=\"VAL3\" attrNum=\"5.6\" attrBool=\"true\"/>\n" +
        "  <element4><element41>VAL4-1</element41></element4>\n" +
        "</myevent>";

    public void setUp()
    {
        Configuration configuration = new Configuration();
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("myevent");
        xmlDOMEventTypeDesc.addXPathProperty("xpathElement1", "/myevent/element1", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathCountE21", "count(/myevent/element2/element21)", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrString", "/myevent/element3/@attrString", XPathConstants.STRING);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrNum", "/myevent/element3/@attrNum", XPathConstants.NUMBER);
        xmlDOMEventTypeDesc.addXPathProperty("xpathAttrBool", "/myevent/element3/@attrBool", XPathConstants.BOOLEAN);
        configuration.addEventTypeAlias("TestXMLNoSchemaType", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getProvider("TestNoSchemaXML", configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));

        String stmt =
                "select element1," +
                       "element4.element41 as nestedElement," +
                       "element2.element21('e21_2') as mappedElement," +
                       "element2.element21[2] as indexedElement," +
                       "xpathElement1, xpathCountE21, xpathAttrString, xpathAttrNum, xpathAttrBool, " +
                       "invalidelement," +
                       "element3.myattribute as invalidattr " +
                      "from TestXMLNoSchemaType.win:length(100)";

        EPStatement joinView = epService.getEPAdministrator().createEQL(stmt);
        joinView.addListener(updateListener);
    }

    public void testSimpleXML() throws Exception
    {
        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent("EventA");
        assertData("EventA");

        sendEvent("EventB");
        assertData("EventB");
    }

    public void testNestedXML() throws Exception
    {
        Configuration configuration = new Configuration();
        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName("a");
        xmlDOMEventTypeDesc.addXPathProperty("element1", "/a/b/c", XPathConstants.STRING);
        configuration.addEventTypeAlias("AEvent", xmlDOMEventTypeDesc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select b.c as type, element1, result1 from AEvent";
        EPStatement joinView = epService.getEPAdministrator().createEQL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent("<a><b><c></c></b></a>");
        EventBean event = updateListener.assertOneGetNewAndReset();
        assertEquals("", event.get("type"));
        assertEquals("", event.get("element1"));

        sendXMLEvent("<a><b></b></a>");
        event = updateListener.assertOneGetNewAndReset();
        assertEquals("", event.get("type"));
        assertEquals("", event.get("element1"));

        sendXMLEvent("<a><b><c>text</c></b></a>");
        event = updateListener.assertOneGetNewAndReset();
        assertEquals("text", event.get("type"));
        assertEquals("text", event.get("element1"));
    }

    public void testEventXML() throws Exception
    {
        Configuration configuration = new Configuration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "/event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "/event/@uid", XPathConstants.STRING);
        desc.setRootElementName("event");
        configuration.addEventTypeAlias("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEQL(stmt);
        joinView.addListener(updateListener);

        sendXMLEvent("<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"></event>");
        EventBean event = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", event.get("type"));
        assertEquals("terminal.55", event.get("uid"));
    }
    
    public void testElementNode() throws Exception
    {
        // test for Esper-129
        Configuration configuration = new Configuration();
        ConfigurationEventTypeXMLDOM desc = new ConfigurationEventTypeXMLDOM();
        desc.addXPathProperty("event.type", "//event/@type", XPathConstants.STRING);
        desc.addXPathProperty("event.uid", "//event/@uid", XPathConstants.STRING);
        desc.setRootElementName("batch-event");
        configuration.addEventTypeAlias("MyEvent", desc);

        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();
        updateListener = new SupportUpdateListener();

        String stmt = "select event.type as type, event.uid as uid from MyEvent";
        EPStatement joinView = epService.getEPAdministrator().createEQL(stmt);
        joinView.addListener(updateListener);

        String xml = "<batch-event>" +
                     "<event type=\"a-f-G\" uid=\"terminal.55\" time=\"2007-04-19T13:05:20.22Z\" version=\"2.0\"/>" +
                        "</batch-event>";
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document doc = builderFactory.newDocumentBuilder().parse(source);
        Element topElement = doc.getDocumentElement();

        epService.getEPRuntime().sendEvent(topElement);
        EventBean event = updateListener.assertOneGetNewAndReset();
        assertEquals("a-f-G", event.get("type"));
        assertEquals("terminal.55", event.get("uid"));
    }

    private void assertData(String element1)
    {
        assertNotNull(updateListener.getLastNewData());
        EventBean event = updateListener.getLastNewData()[0];

        assertEquals(element1, event.get("element1"));
        assertEquals("VAL4-1", event.get("nestedElement"));
        assertEquals("VAL21-2", event.get("mappedElement"));
        assertEquals("VAL21-2", event.get("indexedElement"));

        assertEquals(element1, event.get("xpathElement1"));
        assertEquals(2.0, event.get("xpathCountE21"));
        assertEquals("VAL3", event.get("xpathAttrString"));
        assertEquals(5.6, event.get("xpathAttrNum"));
        assertEquals(true, event.get("xpathAttrBool"));

        assertEquals("", event.get("invalidelement"));        // properties not found come back as empty string without schema
        assertEquals("", event.get("invalidattr"));     // attributes not supported when no schema supplied, use XPath
    }

    private void sendEvent(String value) throws Exception
    {
        String xml = XML.replaceAll("VAL1", value);
        log.debug(".sendEvent value=" + value);

        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(simpleDoc);
    }

    private void sendXMLEvent(String xml) throws Exception
    {
        StringReader reader = new StringReader(xml);
        InputSource source = new InputSource(reader);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        Document simpleDoc = builderFactory.newDocumentBuilder().parse(source);

        epService.getEPRuntime().sendEvent(simpleDoc);
    }

    private static final Log log = LogFactory.getLog(TestNoSchemaXMLEvent.class);
}

