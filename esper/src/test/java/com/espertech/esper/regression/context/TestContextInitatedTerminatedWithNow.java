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

package com.espertech.esper.regression.context;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.util.SupportModelHelper;
import junit.framework.TestCase;

public class TestContextInitatedTerminatedWithNow extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addEventType("SupportBean", SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(configuration);
        epService.initialize();

        listener = new SupportUpdateListener();
    }

    public void tearDown() {
        listener = null;
    }

    public void testNonOverlapping() {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String contextExpr =  "create context MyContext " +
                "start @Now end after 10 sec";
        epService.getEPAdministrator().createEPL(contextExpr);

        String[] fields = new String[] {"cnt"};
        String streamExpr = "context MyContext " +
                "select count(*) as cnt from SupportBean output last when terminated";
        EPStatement stream = epService.getEPAdministrator().createEPL(streamExpr);
        stream.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(8000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(19999));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(30000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {0L});

        SupportModelHelper.compileCreate(epService, streamExpr);

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testOverlappingWithPattern() {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        String contextExpr =  "create context MyContext " +
                "initiated by @Now and pattern [every timer:interval(10)] terminated after 10 sec";
        epService.getEPAdministrator().createEPL(contextExpr);

        String[] fields = new String[] {"cnt"};
        String streamExpr = "context MyContext " +
                "select count(*) as cnt from SupportBean output last when terminated";
        EPStatement stream = epService.getEPAdministrator().createEPL(streamExpr);
        stream.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(8000));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(9999));
        assertFalse(listener.isInvoked());
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {3L});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10100));
        epService.getEPRuntime().sendEvent(new SupportBean("E5", 5));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(19999));
        assertFalse(listener.isInvoked());

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {2L});

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(30000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {0L});

        epService.getEPRuntime().sendEvent(new SupportBean("E6", 6));

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(40000));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{1L});

        SupportModelHelper.compileCreate(epService, streamExpr);

        epService.getEPAdministrator().destroyAllStatements();
    }

    public void testNestedOverlappingAndPattern() {
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context PartitionedByKeys partition by theString from SupportBean, " +
                "context TimedImmediate initiated @now and pattern[every timer:interval(10)] terminated after 10 seconds");
        runAssertion();
    }

    public void testNestedNonOverlapping() {
        epService.getEPAdministrator().createEPL("create context NestedContext " +
                "context PartitionedByKeys partition by theString from SupportBean, " +
                "context TimedImmediate start @now end after 10 seconds");
        runAssertion();
    }

    private void runAssertion() {

        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        String[] fields = "c0,c1".split(",");
        EPStatement statementOne = epService.getEPAdministrator().createEPL("context NestedContext " +
                "select theString as c0, sum(intPrimitive) as c1 from SupportBean \n" +
                "output last when terminated");
        statementOne.addListener(listenerOne);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(10000));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getDataListsFlattened(), fields,
                new Object[][]{{"E1", 1}, {"E2", 2}}, null);
        listenerOne.reset();

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E3", 4));
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(20000));
        EPAssertionUtil.assertPropsPerRow(listenerOne.getDataListsFlattened(), fields,
                new Object[][]{{"E1", 3}, {"E3", 4}}, null);
    }

    public void testInvalid() {
        // for overlapping contexts, @now without condition is not allowed
        tryInvalid("create context TimedImmediate initiated @now terminated after 10 seconds",
                "Incorrect syntax near 'terminated' (a reserved keyword) expecting 'and' but found 'terminated' at line 1 column 45 [create context TimedImmediate initiated @now terminated after 10 seconds]");

        // for non-overlapping contexts, @now with condition is not allowed
        tryInvalid("create context TimedImmediate start @now and after 5 seconds end after 10 seconds",
                "Incorrect syntax near 'and' (a reserved keyword) expecting 'end' but found 'and' at line 1 column 41 [create context TimedImmediate start @now and after 5 seconds end after 10 seconds]");

        // for overlapping contexts, @now together with a filter condition is not allowed
        tryInvalid("create context TimedImmediate initiated @now and SupportBean terminated after 10 seconds",
                "Invalid use of 'now' with initiated-by stream, this combination is not supported [create context TimedImmediate initiated @now and SupportBean terminated after 10 seconds]");
    }

    private void tryInvalid(String epl, String expected) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(expected, ex.getMessage());
        }
    }

}
