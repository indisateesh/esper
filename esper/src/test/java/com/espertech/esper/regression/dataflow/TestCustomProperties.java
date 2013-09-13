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

package com.espertech.esper.regression.dataflow;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Further relevant tests in JSONUtil/PopulateUtil
public class TestCustomProperties extends TestCase {

    private EPServiceProvider engine;

    public void setUp() {
        engine = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        engine.initialize();
    }

    /**
     * - GraphSource always has output ports:
     *   (A) Either as declared through @OutputTypes annotation
     *   (B) Or as assigned via stream (GraphSource -> OutStream&lt;Type&gt;)
     *
     * - Operator properties are explicit:
     *   (A) There is a public setter method
     *   (B) Or the @GraphOpProperty annotation is declared on a field or setter method (optionally can provide a name)
     *   (C) Or the @GraphOpProperty annotation is declared on a catch-all method
     *
     * - Graph op property types
     *   (A) Scalar type
     *   (B) or ExprNode
     *   (C) or Json for nested objects and array
     *   (D) or EPL select
     *
     * - Graph ops communicate the underlying events
     *   - should EventBean be need for event evaluation, the EventBean instance is pooled/shared locally by the op
     *   - if the event bus should evaluate the event, a new anonymous event gets created with the desired type attached dynamically
     *
     * - Exception handlings
     *   - Validation of syntax is performed during "createEPL"
     *   - Resolution of operators and types is performed during "instantiate"
     *   - Runtime exception handling depends on how the data flow gets started and always uses an exception handler (another subject therefore)
     */
    public void testInvalid() {
        String epl;

        epl = "create dataflow MyGraph ABC { field: { a: a}}";
        tryInvalid(epl, "Incorrect syntax near 'a' at line 1 column 42 [create dataflow MyGraph ABC { field: { a: a}}]");

        epl = "create dataflow MyGraph ABC { field: { a:1; b:2 }}";
        tryInvalid(epl, "Incorrect syntax near ';' expecting a right curly bracket '}' but found a semicolon ';' at line 1 column 42 [create dataflow MyGraph ABC { field: { a:1; b:2 }}]");
    }

    private void tryInvalid(String epl, String message) {
        try {
            engine.getEPAdministrator().createEPL(epl);
            fail();
        }
        catch (EPStatementException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    public void testCustomProps() {
        engine.getEPAdministrator().getConfiguration().addImport(MyOperatorOne.class);
        engine.getEPAdministrator().getConfiguration().addImport(MyOperatorTwo.class);

        // test simple properties
        MyOperatorOne.getOperators().clear();
        EPStatement stmtGraph = engine.getEPAdministrator().createEPL("create dataflow MyGraph " + MyOperatorOne.class.getSimpleName() + " {" +
                "  theString = 'a'," +
                "  theInt: 1," +
                "  theBool: true," +
                "  theLongOne: 1L," +
                "  theLongTwo: 2," +
                "  theLongThree: null," +
                "  theDoubleOne: 1d," +
                "  theDoubleTwo: 2," +
                "  theFloatOne: 1f," +
                "  theFloatTwo: 2," +
                "  theStringWithSetter: 'b'," +
                "  theSystemProperty: systemProperties('log4j.configuration')" +
                "}");
        engine.getEPRuntime().getDataFlowRuntime().instantiate("MyGraph");
        assertEquals(1, MyOperatorOne.getOperators().size());
        MyOperatorOne instanceOne = MyOperatorOne.getOperators().get(0);

        assertEquals("a", instanceOne.getTheString());
        assertEquals(null, instanceOne.getTheNotSetString());
        assertEquals(1, instanceOne.getTheInt());
        assertEquals(true, instanceOne.isTheBool());
        assertEquals(1L, (long) instanceOne.getTheLongOne());
        assertEquals(2, instanceOne.getTheLongTwo());
        assertEquals(null, instanceOne.getTheLongThree());
        assertEquals(1.0, instanceOne.getTheDoubleOne());
        assertEquals(2.0, instanceOne.getTheDoubleTwo());
        assertEquals(1f, instanceOne.getTheFloatOne());
        assertEquals(2f, instanceOne.getTheFloatTwo());
        assertEquals(">b<", instanceOne.getTheStringWithSetter());
        assertNotNull(instanceOne.getTheSystemProperty());
        stmtGraph.destroy();

        // test array etc. properties
        MyOperatorTwo.getOperators().clear();
        engine.getEPAdministrator().createEPL("create dataflow MyGraph " + MyOperatorTwo.class.getSimpleName() + " {\n" +
                "  theStringArray: ['a', \"b\"],\n" +
                "  theIntArray: [1, 2, 3],\n" +
                "  theObjectArray: ['a', 1],\n" +
                "  theMap: {\n" +
                "    a : 10,\n" +
                "    b : 'xyz'\n" +
                "  },\n" +
                "  theInnerOp: {\n" +
                "    fieldOne: 'x',\n" + 
                "    fieldTwo: 2\n" +
                "  },\n" +
                "  theInnerOpInterface: {\n" +
                "    class: '" + MyOperatorTwoInterfaceImplTwo.class.getName() + "'\n" +
                "  },\n" +  // NOTE the last comma here, it's acceptable
                "}");
        engine.getEPRuntime().getDataFlowRuntime().instantiate("MyGraph");
        assertEquals(1, MyOperatorTwo.getOperators().size());
        MyOperatorTwo instanceTwo = MyOperatorTwo.getOperators().get(0);

        EPAssertionUtil.assertEqualsExactOrder(new String[] {"a", "b"}, instanceTwo.getTheStringArray());
        EPAssertionUtil.assertEqualsExactOrder(new int[]{1, 2, 3}, instanceTwo.getTheIntArray());
        EPAssertionUtil.assertEqualsExactOrder(new Object[] {"a", 1}, instanceTwo.getTheObjectArray());
        EPAssertionUtil.assertPropsMap(instanceTwo.getTheMap(), "a,b".split(","), new Object[]{10, "xyz"});
        assertEquals("x", instanceTwo.getTheInnerOp().fieldOne);
        assertEquals(2, instanceTwo.getTheInnerOp().fieldTwo);
        assertTrue(instanceTwo.getTheInnerOpInterface() instanceof MyOperatorTwoInterfaceImplTwo);
    }

    @DataFlowOperator
    public static class MyOperatorOne {

        private static List<MyOperatorOne> operators = new ArrayList<MyOperatorOne>();

        public static List<MyOperatorOne> getOperators() {
            return operators;
        }

        public MyOperatorOne() {
            operators.add(this);
        }

        @DataFlowOpParameter
        private String theString;
        @DataFlowOpParameter
        private String theNotSetString;
        @DataFlowOpParameter
        private int theInt;
        @DataFlowOpParameter
        private boolean theBool;
        @DataFlowOpParameter
        private Long theLongOne;
        @DataFlowOpParameter
        private long theLongTwo;
        @DataFlowOpParameter
        private Long theLongThree;
        @DataFlowOpParameter
        private double theDoubleOne;
        @DataFlowOpParameter
        private Double theDoubleTwo;
        @DataFlowOpParameter
        private float theFloatOne;
        @DataFlowOpParameter
        private Float theFloatTwo;
        @DataFlowOpParameter
        private String theSystemProperty;

        private String theStringWithSetter;

        public String getTheString() {
            return theString;
        }

        public String getTheNotSetString() {
            return theNotSetString;
        }

        public int getTheInt() {
            return theInt;
        }

        public boolean isTheBool() {
            return theBool;
        }

        public Long getTheLongOne() {
            return theLongOne;
        }

        public long getTheLongTwo() {
            return theLongTwo;
        }

        public Long getTheLongThree() {
            return theLongThree;
        }

        public float getTheFloatOne() {
            return theFloatOne;
        }

        public Float getTheFloatTwo() {
            return theFloatTwo;
        }

        public double getTheDoubleOne() {
            return theDoubleOne;
        }

        public Double getTheDoubleTwo() {
            return theDoubleTwo;
        }

        public String getTheStringWithSetter() {
            return theStringWithSetter;
        }

        public void setTheStringWithSetter(String theStringWithSetter) {
            this.theStringWithSetter = ">" + theStringWithSetter + "<";
        }

        public String getTheSystemProperty() {
            return theSystemProperty;
        }
    }

    @DataFlowOperator
    public static class MyOperatorTwo {

        private static List<MyOperatorTwo> operators = new ArrayList<MyOperatorTwo>();

        public static List<MyOperatorTwo> getOperators() {
            return operators;
        }

        public MyOperatorTwo() {
            operators.add(this);
        }

        private String[] theStringArray;
        private int[] theIntArray;
        private Object[] theObjectArray;
        private Map<String, Object> theMap;
        private MyOperatorTwoInner theInnerOp;
        private MyOperatorTwoInterface theInnerOpInterface;

        public String[] getTheStringArray() {
            return theStringArray;
        }

        public void setTheStringArray(String[] theStringArray) {
            this.theStringArray = theStringArray;
        }

        public int[] getTheIntArray() {
            return theIntArray;
        }

        public void setTheIntArray(int[] theIntArray) {
            this.theIntArray = theIntArray;
        }

        public Object[] getTheObjectArray() {
            return theObjectArray;
        }

        public void setTheObjectArray(Object[] theObjectArray) {
            this.theObjectArray = theObjectArray;
        }

        public Map<String, Object> getTheMap() {
            return theMap;
        }

        public void setTheMap(Map<String, Object> theMap) {
            this.theMap = theMap;
        }

        public MyOperatorTwoInner getTheInnerOp() {
            return theInnerOp;
        }

        public void setTheInnerOp(MyOperatorTwoInner theInnerOp) {
            this.theInnerOp = theInnerOp;
        }

        public MyOperatorTwoInterface getTheInnerOpInterface() {
            return theInnerOpInterface;
        }

        public void setTheInnerOpInterface(MyOperatorTwoInterface theInnerOpInterface) {
            this.theInnerOpInterface = theInnerOpInterface;
        }
    }

    public static class MyOperatorTwoInner {
        @DataFlowOpParameter
        private String fieldOne;
        @DataFlowOpParameter
        private int fieldTwo;
    }

    public static interface MyOperatorTwoInterface {}

    public static class MyOperatorTwoInterfaceImplOne implements MyOperatorTwoInterface {}
    public static class MyOperatorTwoInterfaceImplTwo implements MyOperatorTwoInterface {}
}
