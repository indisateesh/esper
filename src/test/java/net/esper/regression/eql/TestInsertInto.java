package net.esper.regression.eql;

import net.esper.client.EPServiceProvider;
import net.esper.client.EPServiceProviderManager;
import net.esper.client.EPStatement;
import net.esper.client.EPStatementException;
import net.esper.client.time.TimerControlEvent;
import net.esper.client.time.CurrentTimeEvent;
import net.esper.support.util.SupportUpdateListener;
import net.esper.support.bean.SupportBean;
import net.esper.support.bean.SupportBean_A;
import junit.framework.TestCase;

public class TestInsertInto extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener feedListener;
    private SupportUpdateListener resultListenerDelta;
    private SupportUpdateListener resultListenerProduct;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider();
        epService.initialize();
        feedListener = new SupportUpdateListener();
        resultListenerDelta = new SupportUpdateListener();
        resultListenerProduct = new SupportUpdateListener();

        // Use external clocking for the test
        epService.getEPRuntime().sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
    }

    public void testVariantOne()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
                      "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";

        runAsserts(stmtText);
    }

    // TODO: test rstream
    public void testVariantOneJoin()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
                      "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100) as s0," +
                                SupportBean_A.class.getName() + ".win:length(100) as s1 " +
                      " where s0.string = s1.id";

        runAsserts(stmtText);
    }

    public void testVariantTwo()
    {
        String stmtText = "insert into Event_1 " +
                      "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";

        runAsserts(stmtText);
    }

    public void testVariantTwoJoin()
    {
        String stmtText = "insert into Event_1 " +
                      "select intPrimitive - intBoxed as delta, intPrimitive * intBoxed as product " +
                        "from " + SupportBean.class.getName() + ".win:length(100) as s0," +
                                  SupportBean_A.class.getName() + ".win:length(100) as s1 " +
                        " where s0.string = s1.id";

        runAsserts(stmtText);
    }

    public void testInvalidStreamUsed()
    {
        String stmtText = "insert into Event_1 (delta, product) " +
                      "select intPrimitive - intBoxed as deltaTag, intPrimitive * intBoxed as productTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";
        epService.getEPAdministrator().createEQL(stmtText);

        try
        {
            stmtText = "insert into Event_1 (delta) " +
                      "select intPrimitive - intBoxed as deltaTag " +
                      "from " + SupportBean.class.getName() + ".win:length(100)";
            epService.getEPAdministrator().createEQL(stmtText);
            fail();
        }
        catch (EPStatementException ex)
        {
            // expected
            assertEquals("Error starting view: Event type named 'Event_1' has already been declared with differing type information [insert into Event_1 (delta) select intPrimitive - intBoxed as deltaTag from net.esper.support.bean.SupportBean.win:length(100)]", ex.getMessage());
        }
    }

    private void runAsserts(String stmtText)
    {
        // Attach listener to feed
        EPStatement stmt = epService.getEPAdministrator().createEQL(stmtText);
        stmt.addListener(feedListener);

        // send event for joins to match on
        epService.getEPRuntime().sendEvent(new SupportBean_A("myId"));

        // Attach delta statement to statement and add listener
        stmtText = "select min(delta) as minD, max(delta) as maxD " +
                   "from Event_1.win:time(60)";
        stmt = epService.getEPAdministrator().createEQL(stmtText);
        stmt.addListener(resultListenerDelta);

        // Attach prodict statement to statement and add listener
        stmtText = "select min(product) as minP, max(product) as maxP " +
                   "from Event_1.win:time(60)";
        stmt = epService.getEPAdministrator().createEQL(stmtText);
        stmt.addListener(resultListenerProduct);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0)); // Set the time to 0 seconds

        // send events
        sendEvent(20, 10);
        assertReceivedFeed(10, 200);
        assertReceivedMinMax(10, 10, 200, 200);

        sendEvent(50, 25);
        assertReceivedFeed(25, 25 * 50);
        assertReceivedMinMax(10, 25, 200, 1250);

        sendEvent(5, 2);
        assertReceivedFeed(3, 2 * 5);
        assertReceivedMinMax(3, 25, 10, 1250);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10 * 1000)); // Set the time to 10 seconds

        sendEvent(13, 1);
        assertReceivedFeed(12, 13);
        assertReceivedMinMax(3, 25, 10, 1250);

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(61 * 1000)); // Set the time to 61 seconds
        assertReceivedMinMax(12, 12, 13, 13);
    }

    private void assertReceivedMinMax(int minDelta, int maxDelta, int minProduct, int maxProduct)
    {
        assertEquals(1, resultListenerDelta.getNewDataList().size());
        assertEquals(1, resultListenerDelta.getLastNewData().length);
        assertEquals(1, resultListenerProduct.getNewDataList().size());
        assertEquals(1, resultListenerProduct.getLastNewData().length);
        assertEquals(minDelta, resultListenerDelta.getLastNewData()[0].get("minD"));
        assertEquals(maxDelta, resultListenerDelta.getLastNewData()[0].get("maxD"));
        assertEquals(minProduct, resultListenerProduct.getLastNewData()[0].get("minP"));
        assertEquals(maxProduct, resultListenerProduct.getLastNewData()[0].get("maxP"));
        resultListenerDelta.reset();
        resultListenerProduct.reset();
    }

    private void assertReceivedFeed(int delta, int product)
    {
        assertEquals(1, feedListener.getNewDataList().size());
        assertEquals(1, feedListener.getLastNewData().length);
        assertEquals(delta, feedListener.getLastNewData()[0].get("delta"));
        assertEquals(product, feedListener.getLastNewData()[0].get("product"));
        feedListener.reset();
    }

    private void sendEvent(int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setString("myId");
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
