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

package com.espertech.esper.regression.script;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.Arrays;

public class TestScriptExpression extends TestCase {

    private static final boolean TEST_MVEL = false;

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() throws Exception {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        listener = new SupportUpdateListener();
    }

    public void tearDown() throws Exception {
        listener = null;
    }

    public void testDocSamples() {
        epService.getEPAdministrator().getConfiguration().addEventType(ColorEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType(RFIDEvent.class);
        String epl;

        epl = "expression double fib(num) [" +
                "fib(num); " +
                "function fib(n) { " +
                "  if(n <= 1) " +
                "    return n; " +
                "  return fib(n-1) + fib(n-2); " +
                "};" +
                "]" +
                "select fib(intPrimitive) from SupportBean";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));

        if (TEST_MVEL) {
            epl = "expression mvel:printColors(colors) [" +
                    "String c = null;" +
                    "for (c : colors) {" +
                    "   System.out.println(c);" +
                    "}" +
                    "]" +
                    "select printColors(colors) from ColorEvent";
            EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
            stmt.addListener(listener);
            epService.getEPRuntime().sendEvent(new ColorEvent());
            stmt.destroy();
        }

        epl = "expression js:printColors(colorEvent) [" +
                "importClass (java.lang.System);" +
                "importClass (java.util.Arrays);" +
                "System.out.println(Arrays.toString(colorEvent.getColors()));" +
                "]" +
                "select printColors(colorEvent) from ColorEvent as colorEvent";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);
        epService.getEPRuntime().sendEvent(new ColorEvent());
        epService.getEPAdministrator().destroyAllStatements();

        epl = "expression boolean js:setFlag(name, value, returnValue) [\n" +
                "  if (returnValue) epl.setScriptAttribute(name, value);\n" +
                "  returnValue;\n" +
                "]\n" +
                "expression js:getFlag(name) [\n" +
                "  epl.getScriptAttribute(name);\n" +
                "]\n" +
                "select getFlag('loc') as flag from RFIDEvent(zone = 'Z1' and \n" +
                "  (setFlag('loc', true, loc = 'A') or setFlag('loc', false, loc = 'B')) )";
        epService.getEPAdministrator().createEPL(epl);
    }

    public void testInvalidRegardlessDialect() {
        // parameter defined twice
        tryInvalidExact("expression js:abc(p1, p1) [/* text */] select * from SupportBean",
                "Invalid script parameters for script 'abc', parameter 'p1' is defined more then once [expression js:abc(p1, p1) [/* text */] select * from SupportBean]");

        // invalid dialect
        tryInvalidExact("expression dummy:abc() [10] select * from SupportBean",
                "Failed to obtain script engine for dialect 'dummy' for script 'abc' [expression dummy:abc() [10] select * from SupportBean]");

        // not found
        tryInvalidExact("select abc() from SupportBean",
                "Error starting statement: Unknown single-row function, expression declaration, script or aggregation function named 'abc' could not be resolved [select abc() from SupportBean]");

        // test incorrect number of parameters
        tryInvalidExact("expression js:abc() [10] select abc(1) from SupportBean",
                "Error starting statement: Invalid number of parameters for script 'abc', expected 0 parameters but received 1 parameters [expression js:abc() [10] select abc(1) from SupportBean]");

        // test expression name overlap
        tryInvalidExact("expression js:abc() [10] expression js:abc() [10] select abc() from SupportBean",
                "Script name 'abc' has already been defined with the same number of parameters [expression js:abc() [10] expression js:abc() [10] select abc() from SupportBean]");

        // test expression name overlap with parameters
        tryInvalidExact("expression js:abc(p1) [10] expression js:abc(p2) [10] select abc() from SupportBean",
                "Script name 'abc' has already been defined with the same number of parameters [expression js:abc(p1) [10] expression js:abc(p2) [10] select abc() from SupportBean]");

        // test script name overlap with expression declaration
        tryInvalidExact("expression js:abc() [10] expression abc {10} select abc() from SupportBean",
                "Script name 'abc' overlaps with another expression of the same name [expression js:abc() [10] expression abc {10} select abc() from SupportBean]");

        // fails to resolve return type
        tryInvalidExact("expression dummy js:abc() [10] select abc() from SupportBean",
                "Error starting statement: Failed to resolve return type 'dummy' specified for script 'abc' [expression dummy js:abc() [10] select abc() from SupportBean]");
    }

    public void testInvalidScriptJS() {

        tryInvalidContains("expression js:abc[dummy abc = 1;] select * from SupportBean",
                "missing ; before statement");

        tryInvalidContains("expression js:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                "invalid return");

        tryInvalidExact("expression js:abc[] select * from SupportBean",
                "Incorrect syntax near ']' at line 1 column 18 near reserved keyword 'select' [expression js:abc[] select * from SupportBean]");

        // empty script
        epService.getEPAdministrator().createEPL("expression js:abc[\n] select * from SupportBean");

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("expression js:abc() [throw new Error(\"Some error\");] select * from SupportBean.win:keepall() where abc() = 1");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        }
        catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
        }

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("expression js:abc[dummy;] select * from SupportBean.win:keepall() where abc() = 1");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        }
        catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
        }

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("@Name('ABC') expression int[] js:callIt() [ var myarr = new Array(2, 8, 5, 9); myarr; ] select callIt().countOf(v => v < 6) from SupportBean").addListener(listener);
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        }
        catch (Exception ex) {
            assertTrue("Message is: " + ex.getMessage(), ex.getMessage().contains("Unexpected exception in statement 'ABC': Non-array value provided to collection"));
        }
    }

    public void testInvalidScriptMVEL() {

        if (!TEST_MVEL) {
            return;
        }

        // mvel return type check
        tryInvalidExact("expression java.lang.String mvel:abc[10] select * from SupportBean where abc()",
                "Return type and declared type not compatible for script 'abc', known return type is java.lang.Integer versus declared return type java.lang.String [expression java.lang.String mvel:abc[10] select * from SupportBean where abc()]");

        // undeclared variable
        tryInvalidExact("expression mvel:abc[dummy;] select * from SupportBean",
                "For script 'abc' the variable 'dummy' has not been declared and is not a parameter [expression mvel:abc[dummy;] select * from SupportBean]");

        // invalid assignment
        tryInvalidContains("expression mvel:abc[dummy abc = 1;] select * from SupportBean",
                "Line: 1, Column: 11");

        // syntax problem
        tryInvalidContains("expression mvel:abc(aa) [return aa..bb(1);] select abc(1) from SupportBean",
                "unable to resolve method using strict-mode: java.lang.Integer..bb");

        // empty brackets
        tryInvalidExact("expression mvel:abc[] select * from SupportBean",
                "Incorrect syntax near ']' at line 1 column 20 near reserved keyword 'select' [expression mvel:abc[] select * from SupportBean]");

        // empty script
        epService.getEPAdministrator().createEPL("expression mvel:abc[/* */] select * from SupportBean");

        // unused expression
        epService.getEPAdministrator().createEPL("expression mvel:abc(aa) [return aa..bb(1);] select * from SupportBean");

        // execution problem
        epService.getEPAdministrator().destroyAllStatements();
        epService.getEPAdministrator().createEPL("expression mvel:abc() [Integer a = null; a + 1;] select * from SupportBean.win:keepall() where abc() = 1");
        try {
            epService.getEPRuntime().sendEvent(new SupportBean());
            fail();
        }
        catch (Exception ex) {
            assertTrue(ex.getMessage().contains("Unexpected exception executing script 'abc' for statement '"));
        }
    }

    public void tryInvalidExact(String expression, String message) {
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    public void tryInvalidContains(String expression, String part) {
        try {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        }
        catch (EPStatementException ex) {
            assertTrue("Message not containing text '" + part + "' : " + ex.getMessage(), ex.getMessage().contains(part));
        }
    }

    public void testScripts() {

        // test different return types
        tryReturnTypes("js");
        if (TEST_MVEL) {
            tryReturnTypes("mvel");
        }

        // test void return type
        tryVoidReturnType("js");
        if (TEST_MVEL) {
            tryVoidReturnType("js");
        }

        // test enumeration method
        // Not supported: tryEnumeration("expression int[] js:callIt() [ var myarr = new Array(2, 8, 5, 9); myarr; ]"); returns NativeArray which is a Rhino-specific array wrapper
        if (TEST_MVEL) {
            tryEnumeration("expression int[] mvel:callIt() [ {2, 8, 5, 9} ]");
        }

        // test script props
        trySetScriptProp("js");
        if (TEST_MVEL) {
            trySetScriptProp("mvel");
        }

        // test variable
        tryPassVariable("js");
        if (TEST_MVEL) {
            tryPassVariable("mvel");
        }

        // test passing an event
        tryPassEvent("js");
        if (TEST_MVEL) {
            tryPassEvent("mvel");
        }

        // test returning an object
        tryReturnObject("js");
        if (TEST_MVEL) {
            tryReturnObject("mvel");
        }

        // test datetime method
        tryDatetime("js");
        if (TEST_MVEL) {
            tryDatetime("mvel");
        }

        // test unnamed expression
        tryUnnamedInSelectClause("js");
        if (TEST_MVEL) {
            tryUnnamedInSelectClause("mvel");
        }

        // test import
        epService.getEPAdministrator().getConfiguration().addImport(MyImportedClass.class);
        tryImports("expression MyImportedClass js:callOne() [ importClass(" + MyImportedClass.class.getName() + "); new MyImportedClass() ] ");
        if (TEST_MVEL) {
            tryImports("expression MyImportedClass mvel:callOne() [ import " + MyImportedClass.class.getName() + "; new MyImportedClass() ] ");
        }

        // test overloading script
        epService.getEPAdministrator().getConfiguration().addImport(MyImportedClass.class);
        tryOverloaded("js");
        if (TEST_MVEL) {
            tryOverloaded("mvel");
        }

        // test nested invocation
        tryNested("js");
        if (TEST_MVEL) {
            tryNested("mvel");
        }
    }

    public void testParserMVELSelectNoArgConstant() {
        if (TEST_MVEL) {
            tryParseMVEL("\n\t  10    \n\n\t\t", Integer.class, 10);
            tryParseMVEL("10", Integer.class, 10);
            tryParseMVEL("5*5", Integer.class, 25);
            tryParseMVEL("\"abc\"", String.class, "abc");
            tryParseMVEL(" \"abc\"     ", String.class, "abc");
            tryParseMVEL("'def'", String.class, "def");
            tryParseMVEL(" 'def' ", String.class, "def");
            tryParseMVEL(" new String[] {'a'}", String[].class, new String[] {"a"});
        }

        tryParseJS("\n\t  10    \n\n\t\t", Object.class, 10.0);
        tryParseJS("10", Object.class, 10.0);
        tryParseJS("5*5", Object.class, 25.0);
        tryParseJS("\"abc\"", Object.class, "abc");
        tryParseJS(" \"abc\"     ", Object.class, "abc");
        tryParseJS("'def'", Object.class, "def");
        tryParseJS(" 'def' ", Object.class, "def");
    }

    public void testJavaScriptStatelessReturnPassArgs() {
        Object[][] testData;
        String expression;

        expression = "fib(num);" +
                    "function fib(n) {" +
                    "  if(n <= 1) return n; " +
                    "  return fib(n-1) + fib(n-2); " +
                    "};";
        testData = new Object[][] {
                {new SupportBean("E1", 20), 6765.0},
        };
        trySelect("expression double js:abc(num) [ " + expression + " ]", "abc(intPrimitive)", Double.class, testData);

        testData = new Object[][] {
                {new SupportBean("E1", 5), 50.0},
                {new SupportBean("E1", 6), 60.0}
        };
        trySelect("expression js:abc(myint) [ myint * 10 ]", "abc(intPrimitive)", Object.class, testData);
    }

    public void testMVELStatelessReturnPassArgs() {
        if (!TEST_MVEL) {
            return;
        }

        Object[][] testData;
        String expression;

        testData = new Object[][] {
                {new SupportBean("E1", 5), 50},
                {new SupportBean("E1", 6), 60}
        };
        trySelect("expression mvel:abc(myint) [ myint * 10 ]", "abc(intPrimitive)", int.class, testData);

        expression = "if (theString.equals('E1')) " +
                "  return myint * 10;" +
                "else " +
                "  return myint * 5;";
        testData = new Object[][] {
                {new SupportBean("E1", 5), 50},
                {new SupportBean("E1", 6), 60},
                {new SupportBean("E2", 7), 35}
        };
        trySelect("expression mvel:abc(myint, theString) [" + expression +  "]", "abc(intPrimitive, theString)", Object.class, testData);
        trySelect("expression int mvel:abc(myint, theString) [" + expression +  "]", "abc(intPrimitive, theString)", Integer.class, testData);

        expression = "a + Integer.toString(b)";
        testData = new Object[][] {
                {new SupportBean("E1", 5), "E15"},
                {new SupportBean("E1", 6), "E16"},
                {new SupportBean("E2", 7), "E27"}
        };
        trySelect("expression mvel:abc(a, b) [" + expression +  "]", "abc(theString, intPrimitive)", String.class, testData);
    }

    private void tryVoidReturnType(String dialect) {
        Object[][] testData;
        String expression;

        expression = "expression void " + dialect + ":mysetter() [ epl.setScriptAttribute('a', 1); ]";
        testData = new Object[][] {
                {new SupportBean("E1", 20), null},
                {new SupportBean("E1", 10), null},
        };
        trySelect(expression, "mysetter()", Object.class, testData);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void trySetScriptProp(String dialect) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "expression " + dialect + ":getFlag() [" +
                "  epl.getScriptAttribute('flag');" +
                "]" +
                "expression boolean " + dialect + ":setFlag(flagValue) [" +
                "  epl.setScriptAttribute('flag', flagValue);" +
                "  flagValue;" +
                "]" +
                "select getFlag() as val from SupportBean(theString = 'E1' or setFlag(intPrimitive > 0))");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 10));
        assertEquals(true, listener.assertOneGetNewAndReset().get("val"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryPassVariable(String dialect) {

        Object[][] testData;
        String expression;

        epService.getEPAdministrator().createEPL("create variable long THRESHOLD = 100");

        expression = "expression long " + dialect + ":thresholdAdder(numToAdd, th) [ th + numToAdd; ]";
        testData = new Object[][] {
                {new SupportBean("E1", 20), 120L},
                {new SupportBean("E1", 10), 110L},
        };
        trySelect(expression, "thresholdAdder(intPrimitive, THRESHOLD)", Long.class, testData);

        epService.getEPRuntime().setVariableValue("THRESHOLD", 1);
        testData = new Object[][] {
                {new SupportBean("E1", 20), 21L},
                {new SupportBean("E1", 10), 11L},
        };
        trySelect(expression, "thresholdAdder(intPrimitive, THRESHOLD)", Long.class, testData);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryPassEvent(String dialect) {

        Object[][] testData;
        String expression;

        expression = "expression int " + dialect + ":callIt(bean) [ bean.getIntPrimitive() + 1; ]";
        testData = new Object[][] {
                {new SupportBean("E1", 20), 21},
                {new SupportBean("E1", 10), 11},
        };
        trySelect(expression, "callIt(sb)", Integer.class, testData);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryReturnObject(String dialect) {

        String expression = "expression " + SupportBean.class.getName() + " " + dialect + ":callIt() [ new " + SupportBean.class.getName() + "('E1', 10); ]";
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression + " select callIt() as val0, callIt().getTheString() as val1 from SupportBean as sb");
        stmt.addListener(listener);
        assertEquals(SupportBean.class, stmt.getEventType().getPropertyType("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.theString,val0.intPrimitive,val1".split(","), new Object[]{"E1", 10, "E1"});

        stmt.destroy();
    }

    private void tryDatetime(String dialect) {

        long msecDate = DateTime.parseDefaultMSec("2002-05-30T9:00:00.000");
        String expression = "expression long " + dialect + ":callIt() [ " + msecDate + "]";
        String epl = expression + " select (callIt()).getHourOfDay() as val0, (callIt()).getDayOfWeek() as val1 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{9, 5});

        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtTwo = epService.getEPAdministrator().create(model);
        stmtTwo.addListener(listener);
        assertEquals(epl, stmtTwo.getText());

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{9, 5});

        stmtTwo.destroy();
    }

    private void tryNested(String dialect) {

        String epl = "expression int " + dialect + ":abc(p1, p2) [p1*p2*10]\n" +
                     "expression int " + dialect + ":abc(p1) [p1*10]\n" +
                     "select abc(abc(2), 5) as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{1000});

        stmt.destroy();
    }

    private void tryReturnTypes(String dialect) {

        String epl = "expression string " + dialect + ":one() ['x']\n" +
                     "select one() as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        assertEquals(String.class, stmt.getEventType().getPropertyType("c0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{"x"});

        stmt.destroy();
    }

    private void tryOverloaded(String dialect) {

        String epl = "expression int " + dialect + ":abc() [10]\n" +
                     "expression int " + dialect + ":abc(p1) [p1*10]\n" +
                     "expression int " + dialect + ":abc(p1, p2) [p1*p2*10]\n" +
                     "select abc() as c0, abc(2) as c1, abc(2,3) as c2 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2".split(","), new Object[]{10, 20, 60});

        stmt.destroy();
    }

    private void tryUnnamedInSelectClause(String dialect) {

        String expressionOne = "expression int " + dialect + ":callOne() [1] ";
        String expressionTwo = "expression int " + dialect + ":callTwo(a) [1] ";
        String expressionThree = "expression int " + dialect + ":callThree(a,b) [1] ";
        String epl = expressionOne + expressionTwo + expressionThree + " select callOne(),callTwo(1),callThree(1, 2) from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EventBean outEvent = listener.assertOneGetNewAndReset();
        for (String col : Arrays.asList("callOne()","callTwo(1)","callThree(1, 2)")) {
            assertEquals(Integer.class, stmt.getEventType().getPropertyType(col));
            assertEquals(1, outEvent.get(col));
        }

        stmt.destroy();
    }

    private void tryImports(String expression) {

        String epl = expression + " select callOne() as val0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0.p00".split(","), new Object[]{MyImportedClass.VALUE_P00});

        stmt.destroy();
    }

    private void tryEnumeration(String expression) {

        String epl = expression + " select (callIt()).countOf(v => v < 6) as val0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        assertEquals(Integer.class, stmt.getEventType().getPropertyType("val0"));

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{2});

        stmt.destroy();

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(epl);
        assertEquals(epl, model.toEPL());
        EPStatement stmtTwo = epService.getEPAdministrator().create(model);
        stmtTwo.addListener(listener);
        assertEquals(epl, stmtTwo.getText());

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0".split(","), new Object[]{2});

        stmtTwo.destroy();
    }

    private void trySelect(String scriptPart, String selectExpr, Class expectedType, Object[][] testdata) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(scriptPart +
                    " select " + selectExpr + " as val from SupportBean as sb");
        stmt.addListener(listener);
        assertEquals(expectedType, stmt.getEventType().getPropertyType("val"));

        for (int row = 0; row < testdata.length; row++) {
            Object theEvent = testdata[row][0];
            Object expected = testdata[row][1];

            epService.getEPRuntime().sendEvent(theEvent);
            EventBean outEvent = listener.assertOneGetNewAndReset();
            assertEquals(expected, outEvent.get("val"));
        }

        stmt.destroy();
    }

    private void tryParseJS(String js, Class type, Object value) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                    "expression js:getResultOne [" +
                    js +
                    "] " +
                    "select getResultOne() from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(type, stmt.getEventType().getPropertyType("getResultOne()"));
        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(value, theEvent.get("getResultOne()"));
        stmt.destroy();
    }

    private void tryParseMVEL(String mvelExpression, Class type, Object value) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                    "expression mvel:getResultOne [" +
                    mvelExpression +
                    "] " +
                    "select getResultOne() from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "getResultOne()".split(","), new Object[]{value});
        stmt.destroy();

        stmt = epService.getEPAdministrator().createEPL(
                    "expression mvel:getResultOne [" +
                    mvelExpression +
                    "] " +
                    "expression mvel:getResultTwo [" +
                    mvelExpression +
                    "] " +
                    "select getResultOne() as val0, getResultTwo() as val1 from SupportBean");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(type, stmt.getEventType().getPropertyType("val0"));
        assertEquals(type, stmt.getEventType().getPropertyType("val1"));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "val0,val1".split(","), new Object[]{value, value});

        stmt.destroy();
    }

    private static class ColorEvent {
        private String[] colors = {"Red", "Blue"};

        public String[] getColors() {
            return colors;
        }
    }

    private static class RFIDEvent {
        private String zone;
        private String loc;

        public String getZone() {
            return zone;
        }

        public String getLoc() {
            return loc;
        }
    }
}
