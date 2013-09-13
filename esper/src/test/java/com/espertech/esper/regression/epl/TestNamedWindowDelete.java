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
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.epl.named.NamedWindowProcessor;
import com.espertech.esper.support.bean.*;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationEnum;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestNamedWindowDelete extends TestCase
{
    private EPServiceProviderSPI epService;
    private SupportUpdateListener listenerWindow;
    private SupportUpdateListener listenerWindowTwo;
    private SupportUpdateListener listenerSelect;
    private SupportUpdateListener listenerDelete;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getLogging().setEnableQueryPlan(true);
        epService = (EPServiceProviderSPI) EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listenerWindow = new SupportUpdateListener();
        listenerWindowTwo = new SupportUpdateListener();
        listenerSelect = new SupportUpdateListener();
        listenerDelete = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listenerDelete = null;
        listenerSelect = null;
        listenerWindow = null;
        listenerDelete = null;
    }
    
    public void testFirstUnique() {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_A", SupportBean_A.class);

        String[] fields = new String[] {"theString","intPrimitive"};
        String stmtTextCreateOne = "create window MyWindowOne.std:firstunique(theString) as select * from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        epService.getEPAdministrator().createEPL("insert into MyWindowOne select * from SupportBean");
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL("on SupportBean_A a delete from MyWindowOne where theString=a.id");
        stmtDelete.addListener(listenerDelete);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        epService.getEPRuntime().sendEvent(new SupportBean("A", 2));

        epService.getEPRuntime().sendEvent(new SupportBean_A("A"));
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"A", 1});

        epService.getEPRuntime().sendEvent(new SupportBean("A", 3));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"A", 3}});

        epService.getEPRuntime().sendEvent(new SupportBean_A("A"));
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
    }

    public void testStaggeredNamedWindow() throws Exception
    {
        runAssertionStaggered(EventRepresentationEnum.OBJECTARRAY);
        runAssertionStaggered(EventRepresentationEnum.DEFAULT);
        runAssertionStaggered(EventRepresentationEnum.MAP);
    }

    private void runAssertionStaggered(EventRepresentationEnum outputType) throws Exception {

        String[] fieldsOne = new String[] {"a1", "b1"};
        String[] fieldsTwo = new String[] {"a2", "b2"};

        // create window one
        String stmtTextCreateOne = outputType.getAnnotationText() + " create window MyWindowOne.win:keepall() as select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName();
        EPStatement stmtCreateOne = epService.getEPAdministrator().createEPL(stmtTextCreateOne);
        stmtCreateOne.addListener(listenerWindow);
        assertEquals(0, getCount("MyWindowOne"));
        assertEquals(outputType.getOutputClass(), stmtCreateOne.getEventType().getUnderlyingType());

        // create window two
        String stmtTextCreateTwo = outputType.getAnnotationText() + " create window MyWindowTwo.win:keepall() as select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName();
        EPStatement stmtCreateTwo = epService.getEPAdministrator().createEPL(stmtTextCreateTwo);
        stmtCreateTwo.addListener(listenerWindowTwo);
        assertEquals(0, getCount("MyWindowTwo"));
        assertEquals(outputType.getOutputClass(), stmtCreateTwo.getEventType().getUnderlyingType());

        // create delete stmt
        String stmtTextDelete = "on MyWindowOne delete from MyWindowTwo where a1 = a2";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerDelete);
        assertEquals(StatementType.ON_DELETE, ((EPStatementSPI) stmtDelete).getStatementMetadata().getStatementType());

        // create insert into
        String stmtTextInsert = "insert into MyWindowOne select theString as a1, intPrimitive as b1 from " + SupportBean.class.getName() + "(intPrimitive > 0)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);
        stmtTextInsert = "insert into MyWindowTwo select theString as a2, intPrimitive as b2 from " + SupportBean.class.getName() + "(intPrimitive < 0)";
        epService.getEPAdministrator().createEPL(stmtTextInsert);

        sendSupportBean("E1", -10);
        EPAssertionUtil.assertProps(listenerWindowTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"E1", -10});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsTwo, new Object[][]{{"E1", -10}});
        assertFalse(listenerWindow.isInvoked());
        assertEquals(1, getCount("MyWindowTwo"));

        sendSupportBean("E2", 5);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E2", 5});
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsOne, new Object[][]{{"E2", 5}});
        assertFalse(listenerWindowTwo.isInvoked());
        assertEquals(1, getCount("MyWindowOne"));

        sendSupportBean("E3", -1);
        EPAssertionUtil.assertProps(listenerWindowTwo.assertOneGetNewAndReset(), fieldsTwo, new Object[]{"E3", -1});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsTwo, new Object[][]{{"E1", -10}, {"E3", -1}});
        assertFalse(listenerWindow.isInvoked());
        assertEquals(2, getCount("MyWindowTwo"));

        sendSupportBean("E3", 1);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fieldsOne, new Object[]{"E3", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreateOne.iterator(), fieldsOne, new Object[][]{{"E2", 5}, {"E3", 1}});
        EPAssertionUtil.assertProps(listenerWindowTwo.assertOneGetOldAndReset(), fieldsTwo, new Object[]{"E3", -1});
        EPAssertionUtil.assertPropsPerRow(stmtCreateTwo.iterator(), fieldsTwo, new Object[][]{{"E1", -10}});
        assertEquals(2, getCount("MyWindowOne"));
        assertEquals(1, getCount("MyWindowTwo"));

        stmtDelete.destroy();
        stmtCreateOne.destroy();
        stmtCreateTwo.destroy();
        listenerDelete.reset();
        listenerSelect.reset();
        listenerWindow.reset();
        listenerWindowTwo.reset();
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowOne", true);
        epService.getEPAdministrator().getConfiguration().removeEventType("MyWindowTwo", true);
    }

    public void testDeletePattern() throws Exception
    {
        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create delete stmt
        String stmtTextDelete = "on pattern [every ea=" + SupportBean_A.class.getName() + " or every eb=" + SupportBean_B.class.getName() + "] " + " delete from MyWindow";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // send 1 event
        String[] fields = new String[] {"a", "b"};
        sendSupportBean("E1", 1);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, null);
        assertEquals(1, getCount("MyWindow"));

        // Delete all events using A, 1 row expected
        sendSupportBean_A("A1");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E1", 1}});
        assertEquals(0, getCount("MyWindow"));

        // send 1 event
        sendSupportBean("E2", 2);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2}});
        assertEquals(1, getCount("MyWindow"));

        // Delete all events using B, 1 row expected
        sendSupportBean_B("B1");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E2", 2}});
        assertEquals(0, getCount("MyWindow"));

        stmtDelete.destroy();
        stmtCreate.destroy();
    }

    public void testDeleteAll() throws Exception
    {
        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyWindow";
        EPStatement stmtDelete = epService.getEPAdministrator().createEPL(stmtTextDelete);
        stmtDelete.addListener(listenerDelete);
        EPAssertionUtil.assertEqualsAnyOrder(stmtDelete.getEventType().getPropertyNames(), new String[]{"a", "b"});

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // create consumer
        String[] fields = new String[] {"a", "b"};
        String stmtTextSelect = "select irstream MyWindow.a as a, b from MyWindow as s1";
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL(stmtTextSelect);
        stmtSelect.addListener(listenerSelect);

        // Delete all events, no result expected
        sendSupportBean_A("A1");
        assertFalse(listenerWindow.isInvoked());
        assertFalse(listenerSelect.isInvoked());
        assertFalse(listenerDelete.isInvoked());
        assertEquals(0, getCount("MyWindow"));

        // send 1 event
        sendSupportBean("E1", 1);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, null);
        assertEquals(1, getCount("MyWindow"));

        // Delete all events, 1 row expected
        sendSupportBean_A("A2");
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertProps(listenerSelect.assertOneGetOldAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        EPAssertionUtil.assertProps(listenerDelete.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E1", 1}});
        assertEquals(0, getCount("MyWindow"));

        // send 2 events
        sendSupportBean("E2", 2);
        sendSupportBean("E3", 3);
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2", 2}, {"E3", 3}});
        assertFalse(listenerDelete.isInvoked());
        assertEquals(2, getCount("MyWindow"));

        // Delete all events, 2 rows expected
        sendSupportBean_A("A2");
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[1], fields, new Object[]{"E3", 3});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);
        assertEquals(2, listenerDelete.getLastNewData().length);
        EPAssertionUtil.assertProps(listenerDelete.getLastNewData()[0], fields, new Object[]{"E2", 2});
        EPAssertionUtil.assertProps(listenerDelete.getLastNewData()[1], fields, new Object[]{"E3", 3});
        EPAssertionUtil.assertPropsPerRow(stmtDelete.iterator(), fields, new Object[][]{{"E2", 2}, {"E3", 3}});
        assertEquals(0, getCount("MyWindow"));
    }

    public void testDeleteCondition() throws Exception
    {
        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        // create delete stmt
        String stmtTextDelete = "on " + SupportBean_A.class.getName() + " delete from MyWindow where 'X' || a || 'X' = id";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create delete stmt
        stmtTextDelete = "on " + SupportBean_B.class.getName() + " delete from MyWindow where b < 5";
        epService.getEPAdministrator().createEPL(stmtTextDelete);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString as a, intPrimitive as b from " + SupportBean.class.getName();
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        // send 3 event
        sendSupportBean("E1", 1);
        sendSupportBean("E2", 2);
        sendSupportBean("E3", 3);
        assertEquals(3, getCount("MyWindow"));
        listenerWindow.reset();
        String[] fields = new String[] {"a", "b"};
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E2", 2}, {"E3", 3}});

        // delete E2
        sendSupportBean_A("XE2X");
        assertEquals(1, listenerWindow.getLastOldData().length);
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E2", 2});
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}});
        assertEquals(2, getCount("MyWindow"));

        sendSupportBean("E7", 7);
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1", 1}, {"E3", 3}, {"E7", 7}});
        assertEquals(3, getCount("MyWindow"));

        // delete all under 5
        sendSupportBean_B("B1");
        assertEquals(2, listenerWindow.getLastOldData().length);
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[0], fields, new Object[]{"E1", 1});
        EPAssertionUtil.assertProps(listenerWindow.getLastOldData()[1], fields, new Object[]{"E3", 3});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E7", 7}});
        assertEquals(1, getCount("MyWindow"));
    }

    public void testCoercionKeyMultiPropIndexes()
    {
        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select " +
                                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from " + SupportBean.class.getName();
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);

        List<EPStatement> deleteStatements = new LinkedList<EPStatement>();
        String stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='DB') as s0 delete from MyWindow as win where win.intPrimitive = s0.doubleBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='DP') as s0 delete from MyWindow as win where win.intPrimitive = s0.doublePrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IB') as s0 delete from MyWindow where MyWindow.intPrimitive = s0.intBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IPDP') as s0 delete from MyWindow as win where win.intPrimitive = s0.intPrimitive and win.doublePrimitive = s0.doublePrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IPDP2') as s0 delete from MyWindow as win where win.doublePrimitive = s0.doublePrimitive and win.intPrimitive = s0.intPrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='IPDPIB') as s0 delete from MyWindow as win where win.doublePrimitive = s0.doublePrimitive and win.intPrimitive = s0.intPrimitive and win.intBoxed = s0.intBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(4, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='CAST') as s0 delete from MyWindow as win where win.intBoxed = s0.intPrimitive and win.doublePrimitive = s0.doubleBoxed and win.intPrimitive = s0.intBoxed";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(4, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        // create insert into
        String stmtTextInsertOne = "insert into MyWindow select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed "
                                    + "from " + SupportBean.class.getName() + "(theString like 'E%')";
        epService.getEPAdministrator().createEPL(stmtTextInsertOne);

        sendSupportBean("E1", 1, 10, 100d, 1000d);
        sendSupportBean("E2", 2, 20, 200d, 2000d);
        sendSupportBean("E3", 3, 30, 300d, 3000d);
        sendSupportBean("E4", 4, 40, 400d, 4000d);
        listenerWindow.reset();

        String[] fields = new String[] {"theString"};
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E3"}, {"E4"}});

        sendSupportBean("DB", 0, 0, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean("DB", 0, 0, 0d, 3d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}, {"E4"}});

        sendSupportBean("DP", 0, 0, 5d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean("DP", 0, 0, 4d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E1"}, {"E2"}});

        sendSupportBean("IB", 0, -1, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean("IB", 0, 1, 0d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}});

        sendSupportBean("E5", 5, 50, 500d, 5000d);
        sendSupportBean("E6", 6, 60, 600d, 6000d);
        sendSupportBean("E7", 7, 70, 700d, 7000d);
        listenerWindow.reset();

        sendSupportBean("IPDP", 5, 0, 500d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E5"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E6"}, {"E7"}});

        sendSupportBean("IPDP2", 6, 0, 600d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E6"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E7"}});

        sendSupportBean("IPDPIB", 7, 70, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBean("IPDPIB", 7, 70, 700d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E7"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}});

        sendSupportBean("E8", 8, 80, 800d, 8000d);
        listenerWindow.reset();
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}, {"E8"}});

        sendSupportBean("CAST", 80, 8, 0, 800d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E8"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, new Object[][]{{"E2"}});

        for (EPStatement stmt : deleteStatements)
        {
            stmt.destroy();
        }
        deleteStatements.clear();

        // late delete on a filled window
        stmtTextDelete = "on " + SupportBean.class.getName() + "(theString='LAST') as s0 delete from MyWindow as win where win.intPrimitive = s0.intPrimitive and win.doublePrimitive = s0.doublePrimitive";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        sendSupportBean("LAST", 2, 20, 200, 2000d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2"});
        EPAssertionUtil.assertPropsPerRow(stmtCreate.iterator(), fields, null);

        for (EPStatement stmt : deleteStatements)
        {
            stmt.destroy();
        }

        // test single-two-field index reuse
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean_ST0", SupportBean_ST0.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create window WinOne.win:keepall() as SupportBean");
        epService.getEPAdministrator().createEPL("on SupportBean_ST0 select * from WinOne where theString = key0");
        assertEquals(1, epService.getNamedWindowService().getNamedWindowIndexes("WinOne").length);

        epService.getEPAdministrator().createEPL("on SupportBean_ST0 select * from WinOne where theString = key0 and intPrimitive = p00");
        assertEquals(2, epService.getNamedWindowService().getNamedWindowIndexes("WinOne").length);
    }

    public void testCoercionRangeMultiPropIndexes()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);

        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select " +
                                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);
        String stmtText = "insert into MyWindow select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);
        String[] fields = new String[] {"theString"};

        sendSupportBean("E1", 1, 10, 100d, 1000d);
        sendSupportBean("E2", 2, 20, 200d, 2000d);
        sendSupportBean("E3", 3, 30, 3d, 30d);
        sendSupportBean("E4", 4, 40, 4d, 40d);
        sendSupportBean("E5", 5, 50, 500d, 5000d);
        sendSupportBean("E6", 6, 60, 600d, 6000d);
        listenerWindow.reset();

        List<EPStatement> deleteStatements = new LinkedList<EPStatement>();
        String stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindow as win where win.intPrimitive between s2.doublePrimitiveTwo and s2.doubleBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", 0, 0, 0d, null);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBeanTwo("T", 0, 0, -1d, 1d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindow as win where win.intPrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", -2, 2, 0d, 0d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindow as win " +
                "where win.intPrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo and win.doublePrimitive between s2.intPrimitiveTwo and s2.intBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", -3, 3, -3d, 3d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindow as win " +
                "where win.doublePrimitive between s2.intPrimitiveTwo and s2.intPrimitiveTwo and win.intPrimitive between s2.intPrimitiveTwo and s2.intPrimitiveTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", -4, 4, -4, 4d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindow as win where win.intPrimitive <= doublePrimitiveTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", 0, 0, 5, 1d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E5"});

        stmtTextDelete = "on SupportBeanTwo as s2 delete from MyWindow as win where win.intPrimitive not between s2.intPrimitiveTwo and s2.intBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", 100, 200, 0, 0d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E6"});

        // delete
        for (EPStatement stmt : deleteStatements) {
            stmt.destroy();
        }
        deleteStatements.clear();
        assertEquals(0, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);
    }

    public void testCoercionKeyAndRangeMultiPropIndexes()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBeanTwo", SupportBeanTwo.class);

        // create window
        String stmtTextCreate = "create window MyWindow.win:keepall() as select " +
                                "theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        EPStatement stmtCreate = epService.getEPAdministrator().createEPL(stmtTextCreate);
        stmtCreate.addListener(listenerWindow);
        String stmtText = "insert into MyWindow select theString, intPrimitive, intBoxed, doublePrimitive, doubleBoxed from SupportBean";
        epService.getEPAdministrator().createEPL(stmtText);
        String[] fields = new String[] {"theString"};

        sendSupportBean("E1", 1, 10, 100d, 1000d);
        sendSupportBean("E2", 2, 20, 200d, 2000d);
        sendSupportBean("E3", 3, 30, 300d, 3000d);
        sendSupportBean("E4", 4, 40, 400d, 4000d);
        listenerWindow.reset();

        List<EPStatement> deleteStatements = new LinkedList<EPStatement>();
        String stmtTextDelete = "on SupportBeanTwo delete from MyWindow where theString = stringTwo and intPrimitive between doublePrimitiveTwo and doubleBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(1, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("T", 0, 0, 1d, 200d);
        assertFalse(listenerWindow.isInvoked());
        sendSupportBeanTwo("E1", 0, 0, 1d, 200d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E1"});

        stmtTextDelete = "on SupportBeanTwo delete from MyWindow where theString = stringTwo and intPrimitive = intPrimitiveTwo and intBoxed between doublePrimitiveTwo and doubleBoxedTwo";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("E2", 2, 0, 19d, 21d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E2"});

        stmtTextDelete = "on SupportBeanTwo delete from MyWindow where intBoxed between doubleBoxedTwo and doublePrimitiveTwo and intPrimitive = intPrimitiveTwo and theString = stringTwo ";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(2, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("E3", 3, 0, 29d, 34d);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E3"});

        stmtTextDelete = "on SupportBeanTwo delete from MyWindow where intBoxed between intBoxedTwo and intBoxedTwo and intPrimitive = intPrimitiveTwo and theString = stringTwo ";
        deleteStatements.add(epService.getEPAdministrator().createEPL(stmtTextDelete));
        assertEquals(3, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);

        sendSupportBeanTwo("E4", 4, 40, 0d, null);
        EPAssertionUtil.assertProps(listenerWindow.assertOneGetOldAndReset(), fields, new Object[]{"E4"});

        // delete
        for (EPStatement stmt : deleteStatements) {
            stmt.destroy();
        }
        deleteStatements.clear();
        assertEquals(0, epService.getNamedWindowService().getNamedWindowIndexes("MyWindow").length);
    }

    private SupportBean_A sendSupportBean_A(String id)
    {
        SupportBean_A bean = new SupportBean_A(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean_B sendSupportBean_B(String id)
    {
        SupportBean_B bean = new SupportBean_B(id);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive, Integer intBoxed,
                                        double doublePrimitive, Double doubleBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        bean.setDoublePrimitive(doublePrimitive);
        bean.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBeanTwo sendSupportBeanTwo(String theString, int intPrimitive, Integer intBoxed,
                                        double doublePrimitive, Double doubleBoxed)
    {
        SupportBeanTwo bean = new SupportBeanTwo();
        bean.setStringTwo(theString);
        bean.setIntPrimitiveTwo(intPrimitive);
        bean.setIntBoxedTwo(intBoxed);
        bean.setDoublePrimitiveTwo(doublePrimitive);
        bean.setDoubleBoxedTwo(doubleBoxed);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private SupportBean sendSupportBean(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
        return bean;
    }

    private long getCount(String windowName) throws Exception
    {
        NamedWindowProcessor processor = ((EPServiceProviderSPI)epService).getNamedWindowService().getProcessor(windowName);
        return processor.getProcessorInstance(null).getCountDataWindow();
    }
}
