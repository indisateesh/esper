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

package com.espertech.esper.regression.epl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.bean.SupportSimpleBeanOne;
import com.espertech.esper.support.bean.SupportSimpleBeanTwo;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportQueryPlanIndexHook;
import com.espertech.esper.support.util.IndexAssertionEventSend;
import com.espertech.esper.support.util.IndexBackingTableInfo;
import junit.framework.TestCase;

public class TestSubselectIndex extends TestCase implements IndexBackingTableInfo
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();        
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testIndexChoicesOverdefinedWhere() {
        epService.getEPAdministrator().getConfiguration().addEventType("SSB1", SupportSimpleBeanOne.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SSB2", SupportSimpleBeanTwo.class);

        // test no where clause with unique
        IndexAssertionEventSend assertNoWhere = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "c0,c1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 1, 2, 3));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EX", 10, 11, 12));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EX", "E1"});
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 1, 2, 3));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EY", 10, 11, 12));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EY", null});
            }
        };
        runAssertion("s2,i2", "", BACKING_UNINDEXED, assertNoWhere);

        // test no where clause with unique on multiple props, exact specification of where-clause
        IndexAssertionEventSend assertSendEvents = new IndexAssertionEventSend() {
            public void run() {
                String[] fields = "c0,c1".split(",");
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E1", 1, 3, 10));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E2", 1, 2, 0));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanTwo("E3", 1, 3, 9));
                epService.getEPRuntime().sendEvent(new SupportSimpleBeanOne("EX", 1, 3, 9));
                EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"EX", "E3"});
            }
        };
        runAssertion("d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1 and ssb2.i2 = ssb1.i1", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("d2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1", BACKING_MULTI_DUPS, assertSendEvents);
        runAssertion("d2,i2", "where ssb2.d2 = ssb1.d1", BACKING_SINGLE_DUPS, assertSendEvents);
        runAssertion("d2,i2", "where ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("d2,i2", "where ssb2.d2 = ssb1.d1 and ssb2.l2 between 1 and 1000", BACKING_COMPOSITE, assertSendEvents);
        runAssertion("i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_DUPS, assertSendEvents);
        runAssertion("i2,d2,l2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("d2,l2,i2", "where ssb2.l2 = ssb1.l1 and ssb2.i2 = ssb1.i1 and ssb2.d2 = ssb1.d1 and ssb2.s2 between 'E3' and 'E4'", BACKING_MULTI_UNIQUE, assertSendEvents);
        runAssertion("l2", "where ssb2.l2 = ssb1.l1", BACKING_SINGLE_UNIQUE, assertSendEvents);
        runAssertion("l2", "where ssb2.l2 = ssb1.l1 and ssb1.i1 between 1 and 20", BACKING_SINGLE_UNIQUE, assertSendEvents);
    }

    private void runAssertion(String uniqueFields, String whereClause, String backingTable, IndexAssertionEventSend assertion) {
        String eplUnique = INDEX_CALLBACK_HOOK + "select s1 as c0, " +
                "(select s2 from SSB2.std:unique(" + uniqueFields + ") as ssb2 " + whereClause + ") as c1 " +
                "from SSB1 as ssb1";
        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        stmtUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryAndReset(0, null, backingTable);

        assertion.run();

        stmtUnique.destroy();
    }

    public void testUniqueIndexCorrelated() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("S0", SupportBean_S0.class);
        String[] fields = "c0,c1".split(",");

        // test std:unique
        String eplUnique = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select intPrimitive from SupportBean.std:unique(theString) where theString = s0.p00) as c1 " +
                "from S0 as s0";
        EPStatement stmtUnique = epService.getEPAdministrator().createEPL(eplUnique);
        stmtUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryAndReset(0, null, BACKING_SINGLE_UNIQUE);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 4});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 3});

        stmtUnique.destroy();

        // test std:firstunique
        String eplFirstUnique = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select intPrimitive from SupportBean.std:firstunique(theString) where theString = s0.p00) as c1 " +
                "from S0 as s0";
        EPStatement stmtFirstUnique = epService.getEPAdministrator().createEPL(eplFirstUnique);
        stmtFirstUnique.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryAndReset(0, null, BACKING_SINGLE_UNIQUE);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 2});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 1});

        stmtFirstUnique.destroy();

        // test intersection std:firstunique
        String eplIntersection = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select intPrimitive from SupportBean.win:time(1).std:unique(theString) where theString = s0.p00) as c1 " +
                "from S0 as s0";
        EPStatement stmtIntersection = epService.getEPAdministrator().createEPL(eplIntersection);
        stmtIntersection.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryAndReset(0, null, BACKING_SINGLE_UNIQUE);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        epService.getEPRuntime().sendEvent(new SupportBean("E2", 4));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(10, "E2"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {10, 4});

        epService.getEPRuntime().sendEvent(new SupportBean_S0(11, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{11, 3});

        stmtIntersection.destroy();

        // test grouped unique
        String eplGrouped = INDEX_CALLBACK_HOOK + "select id as c0, " +
                "(select longPrimitive from SupportBean.std:groupwin(theString).std:unique(intPrimitive) where theString = s0.p00 and intPrimitive = s0.id) as c1 " +
                "from S0 as s0";
        EPStatement stmtGrouped = epService.getEPAdministrator().createEPL(eplGrouped);
        stmtGrouped.addListener(listener);

        SupportQueryPlanIndexHook.assertSubqueryAndReset(0, null, BACKING_MULTI_UNIQUE);

        epService.getEPRuntime().sendEvent(makeBean("E1", 1, 100));
        epService.getEPRuntime().sendEvent(makeBean("E1", 2, 101));
        epService.getEPRuntime().sendEvent(makeBean("E1", 1, 102));

        epService.getEPRuntime().sendEvent(new SupportBean_S0(1, "E1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {1, 102L});

        stmtGrouped.destroy();
    }

    private SupportBean makeBean(String theString, int intPrimitive, long longPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        return bean;
    }
}
