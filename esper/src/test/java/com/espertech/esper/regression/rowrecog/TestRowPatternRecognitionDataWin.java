/*
 * *************************************************************************************
 *  Copyright (C) 2008 EsperTech, Inc. All rights reserved.                            *
 *  http://esper.codehaus.org                                                          *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestRowPatternRecognitionDataWin extends TestCase
{
    private static final Log log = LogFactory.getLog(TestRowPatternRecognitionDataWin.class);

    public void testUnboundStreamNoIterator()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        String[] fields = "string,value".split(",");
        String text = "select * from MyEvent " +
                "match_recognize (" +
                "  measures A.theString as string, A.value as value" +
                "  all matches pattern (A) " +
                "  define " +
                "    A as PREV(A.theString, 1) = theString" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportRecogBean("s1", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("s2", 2));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("s1", 3));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("s3", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("s2", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("s1", 6));
        assertFalse(stmt.iterator().hasNext());
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new SupportRecogBean("s1", 7));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"s1", 7}});
        assertFalse(stmt.iterator().hasNext());

        /*
          Optionally send some more events.

        for (int i = 0; i < 100000; i++)
        {
            epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 1));
        }
        epService.getEPRuntime().sendEvent(new SupportRecogBean("P2", 1));
         */
    }

    public void testTimeWindow()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        sendTimer(0, epService);
        String[] fields = "a_string,b_string,c_string".split(",");
        String text = "select * from MyEvent.win:time(5 sec) " +
                "match_recognize (" +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string" +
                "  all matches pattern ( A B C ) " +
                "  define " +
                "    A as (A.value = 1)," +
                "    B as (B.value = 2)," +
                "    C as (C.value = 3)" +
                ")";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(50, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E1", 1));

        sendTimer(1000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E2", 2));
        assertFalse(stmt.iterator().hasNext());
        assertFalse(listener.isInvoked());

        sendTimer(6000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E3", 3));
        assertFalse(stmt.iterator().hasNext());
        assertFalse(listener.isInvoked());

        sendTimer(7000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E4", 1));

        sendTimer(8000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E5", 2));

        sendTimer(11500, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("E6", 3));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{"E4", "E5", "E6"}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{"E4", "E5", "E6"}});

        sendTimer(11999, epService);
        assertTrue(stmt.iterator().hasNext());

        sendTimer(12000, epService);
        assertFalse(stmt.iterator().hasNext());
        assertFalse(listener.isInvoked());
    }

    public void testTimeBatchWindow()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("MyEvent", SupportRecogBean.class);
        EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();

        sendTimer(0, epService);
        String[] fields = "a_string,b_string,c_string".split(",");
        String text = "select * from MyEvent.win:time_batch(5 sec) " +
                "match_recognize (" +
                "  partition by cat " +
                "  measures A.theString as a_string, B.theString as b_string, C.theString as c_string" +
                "  all matches pattern ( (A | B) C ) " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    B as B.theString like 'B%'," +
                "    C as C.theString like 'C%' and C.value in (A.value, B.value)" +
                ") order by a_string";

        EPStatement stmt = epService.getEPAdministrator().createEPL(text);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendTimer(50, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A1", "001", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B1", "002", 1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B2", "002", 4));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        sendTimer(4000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("C1", "002", 4));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("C2", "002", 5));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B3", "003", -1));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{null, "B2", "C1"}});

        sendTimer(5050, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, "B2", "C1"}});
        assertFalse(stmt.iterator().hasNext());

        sendTimer(6000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("C3", "003", -1));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("C4", "001", 1));
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        sendTimer(10050, epService);
        assertFalse(listener.isInvoked());
        assertFalse(stmt.iterator().hasNext());

        sendTimer(14000, epService);
        epService.getEPRuntime().sendEvent(new SupportRecogBean("A2", "002", 0));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("B4", "003", 10));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("C5", "002", 0));
        epService.getEPRuntime().sendEvent(new SupportRecogBean("C6", "003", 10));
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields,
                new Object[][]{{null, "B4", "C6"}, {"A2", null, "C5"}});

        sendTimer(15050, epService);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields,
                new Object[][]{{null, "B4", "C6"}, {"A2", null, "C5"}});
        assertFalse(stmt.iterator().hasNext());
    }

    private void sendTimer(long time, EPServiceProvider epService)
    {
        CurrentTimeEvent theEvent = new CurrentTimeEvent(time);
        EPRuntime runtime = epService.getEPRuntime();
        runtime.sendEvent(theEvent);
    }
}
