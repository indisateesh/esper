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

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;

public class TestFromClauseMethod extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType(SupportBean.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        listener = new SupportUpdateListener();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void test2StreamMaxAggregation() {
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // ESPER 556
        stmtText = "select max(col1) as maxcol1 from SupportBean.std:unique(theString), method:" + className + ".fetchResult100() ";

        String[] fields = "maxcol1".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{9}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{9}});

        stmt.destroy();
    }

    public void test2JoinHistoricalSubordinateOuterMultiField()
    {
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select intPrimitive,intBoxed,col1,col2 from SupportBean.win:keepall() " +
                   "left outer join " +
                   "method:" + className + ".fetchResult100() " +
                   "on intPrimitive = col1 and intBoxed = col2";

        String[] fields = "intPrimitive,intBoxed,col1,col2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        stmt.addListener(listener);

        sendSupportBeanEvent(2, 4);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{2, 4, 2, 4}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{2, 4, 2, 4}});

        stmt.destroy();
    }

    public void test2JoinHistoricalSubordinateOuter()
    {
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchResult12(0) as s0 " +
                   "left outer join " +
                   "method:" + className + ".fetchResult23(s0.value) as s1 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                    "method:" + className + ".fetchResult23(s0.value) as s1 " +
                    "right outer join " +
                    "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                    "method:" + className + ".fetchResult23(s0.value) as s1 " +
                    "full outer join " +
                    "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                    "method:" + className + ".fetchResult12(0) as s0 " +
                    "full outer join " +
                    "method:" + className + ".fetchResult23(s0.value) as s1 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(stmtText);
    }

    public void test2JoinHistoricalIndependentOuter()
    {
        String[] fields = "valueOne,valueTwo".split(",");
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchResult12(0) as s0 " +
                   "left outer join " +
                   "method:" + className + ".fetchResult23(0) as s1 on s0.value = s1.value";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}});
        stmt.destroy();

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                    "method:" + className + ".fetchResult23(0) as s1 " +
                    "right outer join " +
                    "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}});
        stmt.destroy();

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                    "method:" + className + ".fetchResult23(0) as s1 " +
                    "full outer join " +
                    "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}, {null, 3}});
        stmt.destroy();

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                    "method:" + className + ".fetchResult12(0) as s0 " +
                    "full outer join " +
                    "method:" + className + ".fetchResult23(0) as s1 on s0.value = s1.value";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}, {null, 3}});
        stmt.destroy();
    }

    private void assertJoinHistoricalSubordinateOuter(String expression)
    {
        String[] fields = "valueOne,valueTwo".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}});
        stmt.destroy();
    }

    public void test2JoinHistoricalOnlyDependent()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        EPStatement setStmt = epService.getEPAdministrator().createEPL("on SupportBean set lower=intPrimitive,upper=intBoxed");
        assertEquals(StatementType.ON_SET, ((EPStatementSPI) setStmt).getStatementMetadata().getStatementType());

        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select value,result from method:" + className + ".fetchBetween(lower, upper), " +
                                        "method:" + className + ".fetchIdDelimited(value)";
        assertJoinHistoricalOnlyDependent(stmtText);

        stmtText = "select value,result from " +
                                        "method:" + className + ".fetchIdDelimited(value), " +
                                        "method:" + className + ".fetchBetween(lower, upper)";
        assertJoinHistoricalOnlyDependent(stmtText);
    }

    public void test2JoinHistoricalOnlyIndependent()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set lower=intPrimitive,upper=intBoxed");

        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchBetween(lower, upper) as s0, " +
                                        "method:" + className + ".fetchBetweenString(lower, upper) as s1";
        assertJoinHistoricalOnlyIndependent(stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                                        "method:" + className + ".fetchBetweenString(lower, upper) as s1, " +
                                        "method:" + className + ".fetchBetween(lower, upper) as s0 ";
        assertJoinHistoricalOnlyIndependent(stmtText);
    }

    private void assertJoinHistoricalOnlyIndependent(String expression)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "valueOne,valueTwo".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(5, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{5, "5"}});

        sendSupportBeanEvent(1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, "1"}, {1, "2"}, {2, "1"}, {2, "2"}});

        sendSupportBeanEvent(0, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        stmt.destroy();
        sendSupportBeanEvent(0, -1);
        assertFalse(listener.isInvoked());
    }

    private void assertJoinHistoricalOnlyDependent(String expression)
    {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "value,result".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(5, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{5, "|5|"}});

        sendSupportBeanEvent(1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, "|1|"}, {2, "|2|"}});

        sendSupportBeanEvent(0, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(4, 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4, "|4|"}, {5, "|5|"}, {6, "|6|"}});

        stmt.destroy();
        sendSupportBeanEvent(0, -1);
        assertFalse(listener.isInvoked());
    }

    public void testNoJoinIterateVariables()
    {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set lower=intPrimitive,upper=intBoxed");

        // Test int and singlerow
        String className = SupportStaticMethodLib.class.getName();
        String stmtText = "select value from method:" + className + ".fetchBetween(lower, upper)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, null);

        sendSupportBeanEvent(5, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, new Object[][]{{5}, {6}, {7}, {8}, {9}, {10}});

        sendSupportBeanEvent(10, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, null);

        sendSupportBeanEvent(4, 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, new Object[][]{{4}});

        stmt.destroy();
        assertFalse(listener.isInvoked());
    }
    
    public void testMapReturnTypeMultipleRow()
    {
        String[] fields = "theString,intPrimitive,mapstring,mapint".split(",");
        String joinStatement = "select theString, intPrimitive, mapstring, mapint from " +
                SupportBean.class.getName() + ".win:keepall() as s1, " +
                "method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchMapArray(theString, intPrimitive)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendBeanEvent("E1", 0);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendBeanEvent("E2", -1);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendBeanEvent("E3", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 1, "|E3_0|", 100});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 1, "|E3_0|", 100}});

        sendBeanEvent("E4", 2);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 1, "|E3_0|", 100}, {"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101}});

        sendBeanEvent("E5", 3);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"E5", 3, "|E5_0|", 100}, {"E5", 3, "|E5_1|", 101}, {"E5", 3, "|E5_2|", 102}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 1, "|E3_0|", 100},
                {"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101},
                {"E5", 3, "|E5_0|", 100}, {"E5", 3, "|E5_1|", 101}, {"E5", 3, "|E5_2|", 102}});

        stmt.destroy();
    }

    public void testMapReturnTypeSingleRow()
    {
        String joinStatement = "select theString, intPrimitive, mapstring, mapint from " +
                SupportBean.class.getName() + ".win:keepall() as s1, " +
                "method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchMap(theString, intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);
        String[] fields = new String[] {"theString", "intPrimitive", "mapstring", "mapint"};

        sendBeanEvent("E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "|E1|", 2});

        sendBeanEvent("E2", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 3, "|E2|", 4});

        sendBeanEvent("E3", 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 0, null, null});

        sendBeanEvent("E4", -1);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    public void testArrayNoArg()
    {
        String joinStatement = "select id, theString from " +
                SupportBean.class.getName() + ".win:length(3) as s1, " +
                "method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayNoArg";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayNoArg(stmt);

        joinStatement = "select id, theString from " +
                SupportBean.class.getName() + ".win:length(3) as s1, " +
                "method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayNoArg()";
        stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayNoArg(stmt);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(joinStatement);
        assertEquals(joinStatement, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryArrayNoArg(stmt);

        model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("id", "theString"));
        model.setFromClause(FromClause.create()
            .add(FilterStream.create(SupportBean.class.getName(), "s1").addView("win", "length", Expressions.constant(3)))
            .add(MethodInvocationStream.create(SupportStaticMethodLib.class.getName(), "fetchArrayNoArg")));
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(joinStatement, model.toEPL());

        tryArrayNoArg(stmt);
    }

    private void tryArrayNoArg(EPStatement stmt)
    {
        stmt.addListener(listener);
        String[] fields = new String[] {"id", "theString"};

        sendBeanEvent("E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"1", "E1"});

        sendBeanEvent("E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"1", "E2"});

        stmt.destroy();
    }

    public void testArrayWithArg()
    {
        String joinStatement = "select irstream id, theString from " +
                SupportBean.class.getName() + "().win:length(3) as s1, " +
                " method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayGen(intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayWithArg(stmt);

        joinStatement = "select irstream id, theString from " +
                "method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayGen(intPrimitive) as s0, " +
                SupportBean.class.getName() + ".win:length(3)";
        stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayWithArg(stmt);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(joinStatement);
        assertEquals(joinStatement, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryArrayWithArg(stmt);

        model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("id", "theString").streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH));
        model.setFromClause(FromClause.create()
            .add(MethodInvocationStream.create(SupportStaticMethodLib.class.getName(), "fetchArrayGen", "s0")
                .addParameter(Expressions.property("intPrimitive")))
                .add(FilterStream.create(SupportBean.class.getName()).addView("win", "length", Expressions.constant(3)))
            );
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(joinStatement, model.toEPL());

        tryArrayWithArg(stmt);
    }

    private void tryArrayWithArg(EPStatement stmt)
    {
        stmt.addListener(listener);
        String[] fields = new String[] {"id", "theString"};

        sendBeanEvent("E1", -1);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2", 0);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E3", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", "E3"});

        sendBeanEvent("E4", 2);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E4"}, {"B", "E4"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendBeanEvent("E5", 3);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E5"}, {"B", "E5"}, {"C", "E5"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendBeanEvent("E6", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E6"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"A", "E3"}});
        listener.reset();

        sendBeanEvent("E7", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E7"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"A", "E4"}, {"B", "E4"}});
        listener.reset();

        stmt.destroy();
    }

    public void testObjectNoArg()
    {
        String joinStatement = "select id, theString from " +
                SupportBean.class.getName() + "().win:length(3) as s1, " +
                " method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchObjectNoArg()";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);
        String[] fields = new String[] {"id", "theString"};

        sendBeanEvent("E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"2", "E1"});

        sendBeanEvent("E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"2", "E2"});
    }

    public void testObjectWithArg()
    {
        String joinStatement = "select id, theString from " +
                SupportBean.class.getName() + "().win:length(3) as s1, " +
                " method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchObject(theString)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        stmt.addListener(listener);
        String[] fields = new String[] {"id", "theString"};

        sendBeanEvent("E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"|E1|", "E1"});

        sendBeanEvent(null);
        assertFalse(listener.isInvoked());

        sendBeanEvent("E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"|E2|", "E2"});
    }

    public void testInvocationTargetEx()
    {
        String joinStatement = "select s1.theString from " +
                SupportBean.class.getName() + "().win:length(3) as s1, " +
                " method:com.espertech.esper.support.epl.SupportStaticMethodLib.throwExceptionBeanReturn()";

        epService.getEPAdministrator().createEPL(joinStatement);

        try {
            sendBeanEvent("E1");
            fail(); // default test configuration rethrows this exception
        }
        catch (EPException ex) {
            // fine
        }
    }

    public void testInvalid()
    {
        tryInvalid("select * from SupportBean, method:.abc where 1=2",
                   "Incorrect syntax near '.' at line 1 column 34, please check the method invocation join within the from clause [select * from SupportBean, method:.abc where 1=2]");

        tryInvalid("select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayGen()",
                   "Error starting statement: Method footprint does not match the number or type of expression parameters, expecting no parameters in method: Could not find static method named 'fetchArrayGen' in class 'com.espertech.esper.support.epl.SupportStaticMethodLib' taking no parameters (nearest match found was 'fetchArrayGen' taking type(s) 'int') [select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayGen()]");

        tryInvalid("select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchObjectAndSleep(1)",
                   "Error starting statement: Method footprint does not match the number or type of expression parameters, expecting a method where parameters are typed 'Integer': Could not find static method named 'fetchObjectAndSleep' in class 'com.espertech.esper.support.epl.SupportStaticMethodLib' with matching parameter number and expected parameter type(s) 'Integer' (nearest match found was 'fetchObjectAndSleep' taking type(s) 'String, int, long') [select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchObjectAndSleep(1)]");

        tryInvalid("select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.sleep(100) where 1=2",
                   "Error starting statement: Invalid return type for static method 'sleep' of class 'com.espertech.esper.support.epl.SupportStaticMethodLib', expecting a Java class [select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.sleep(100) where 1=2]");

        tryInvalid("select * from SupportBean, method:AClass. where 1=2",
                   "Incorrect syntax near 'where' (a reserved keyword) expecting an identifier but found 'where' at line 1 column 42, please check the view specifications within the from clause [select * from SupportBean, method:AClass. where 1=2]");

        tryInvalid("select * from SupportBean, method:Dummy.abc where 1=2",
                   "Error starting statement: Could not load class by name 'Dummy', please check imports [select * from SupportBean, method:Dummy.abc where 1=2]");

        tryInvalid("select * from SupportBean, method:Math where 1=2",
                   "No method name specified for method-based join [select * from SupportBean, method:Math where 1=2]");

        tryInvalid("select * from SupportBean, method:Dummy.dummy().win:length(100) where 1=2",
                   "Error starting statement: Method data joins do not allow views onto the data, view 'win:length' is not valid in this context [select * from SupportBean, method:Dummy.dummy().win:length(100) where 1=2]");

        tryInvalid("select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.dummy where 1=2",
                   "Error starting statement: Could not find static method named 'dummy' in class 'com.espertech.esper.support.epl.SupportStaticMethodLib' [select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.dummy where 1=2]");

        tryInvalid("select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.minusOne() where 1=2",
                   "Error starting statement: Invalid return type for static method 'minusOne' of class 'com.espertech.esper.support.epl.SupportStaticMethodLib', expecting a Java class [select * from SupportBean, method:com.espertech.esper.support.epl.SupportStaticMethodLib.minusOne() where 1=2]");

        tryInvalid("select * from SupportBean, xyz:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayNoArg() where 1=2",
                   "Expecting keyword 'method', found 'xyz' [select * from SupportBean, xyz:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchArrayNoArg() where 1=2]");

        tryInvalid("select * from method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s1.value, s1.value) as s0, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s1",
                   "Error starting statement: Circular dependency detected between historical streams [select * from method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s1.value, s1.value) as s0, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s1]");

        tryInvalid("select * from method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s0, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s1",
                   "Error starting statement: Parameters for historical stream 0 indicate that the stream is subordinate to itself as stream parameters originate in the same stream [select * from method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s0, method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s1]");

        tryInvalid("select * from method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s0",
                   "Error starting statement: Parameters for historical stream 0 indicate that the stream is subordinate to itself as stream parameters originate in the same stream [select * from method:com.espertech.esper.support.epl.SupportStaticMethodLib.fetchBetween(s0.value, s0.value) as s0]");
    }

    private void tryInvalid(String stmt, String message)
    {
        try
        {
            epService.getEPAdministrator().createEPL(stmt);
            fail();
        }
        catch (EPStatementException ex)
        {
            assertEquals(message, ex.getMessage());
        }
    }

    private void sendBeanEvent(String theString)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(String theString, int intPrimitive)
    {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(int intPrimitive, int intBoxed)
    {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }
}
