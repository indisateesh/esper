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

package com.espertech.esper.epl.spec;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.expression.ExprAndNode;
import com.espertech.esper.epl.expression.ExprValidationException;
import com.espertech.esper.epl.parse.EPLTreeWalker;
import com.espertech.esper.filter.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.epl.parse.SupportEPLTreeWalkerFactory;
import com.espertech.esper.support.epl.parse.SupportParserHelper;
import com.espertech.esper.support.view.SupportStatementContextFactory;
import junit.framework.TestCase;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.Tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class TestFilterStreamSpecRaw extends TestCase
{
    public void testNoExpr() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName());
        FilterSpecCompiled spec = compile(raw);
        assertEquals(SupportBean.class, spec.getFilterForEventType().getUnderlyingType());
        assertEquals(0, spec.getParameters().length);
    }

    public void testMultipleExpr() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() +
                "(intPrimitive-1>2 and intBoxed-5>3)");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(SupportBean.class, spec.getFilterForEventType().getUnderlyingType());
        assertEquals(1, spec.getParameters().length);
        // expecting unoptimized expressions to condense to a single boolean expression, more efficient this way

        FilterSpecParamExprNode exprNode = (FilterSpecParamExprNode) spec.getParameters()[0];
        assertEquals(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION, exprNode.getLookupable().getExpression());
        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, exprNode.getFilterOperator());
        assertTrue(exprNode.getExprNode() instanceof ExprAndNode);
    }

    public void testInvalid() throws Exception
    {
        tryInvalid("select * from " + SupportBean.class.getName() + "(intPrimitive=5L)");
        tryInvalid("select * from " + SupportBean.class.getName() + "(5d = byteBoxed)");
        tryInvalid("select * from " + SupportBean.class.getName() + "(5d > longBoxed)");
        tryInvalid("select * from " + SupportBean.class.getName() + "(longBoxed in (5d, 1.1d))");
    }

    private void tryInvalid(String text) throws Exception
    {
        try
        {
            FilterStreamSpecRaw raw = makeSpec(text);
            compile(raw);
            fail();
        }
        catch (ExprValidationException ex)
        {
            // expected
        }
    }

    public void testEquals() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() + "(intPrimitive=5)");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(1, spec.getParameters().length);
        assertEquals("intPrimitive", spec.getParameters()[0].getLookupable().getExpression());
        assertEquals(FilterOperator.EQUAL, spec.getParameters()[0].getFilterOperator());
        assertEquals(5, getConstant(spec.getParameters()[0]));
    }

    public void testEqualsAndLess() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() + "(theString='a' and intPrimitive<9)");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(2, spec.getParameters().length);
        Map<String, FilterSpecParam> parameters = mapParameters(spec.getParameters());

        assertEquals(FilterOperator.EQUAL, parameters.get("theString").getFilterOperator());
        assertEquals("a", getConstant(parameters.get("theString")));

        assertEquals(FilterOperator.LESS, parameters.get("intPrimitive").getFilterOperator());
        assertEquals(9, getConstant(parameters.get("intPrimitive")));
    }

    private Map<String, FilterSpecParam> mapParameters(FilterSpecParam[] parameters)
    {
        Map<String, FilterSpecParam> map = new HashMap<String, FilterSpecParam>();
        for (FilterSpecParam param : parameters)
        {
            map.put(param.getLookupable().getExpression(), param);
        }
        return map;
    }

    public void testCommaAndCompar() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() +
                "(doubleBoxed>1.11, doublePrimitive>=9.11 and intPrimitive<=9, theString || 'a' = 'sa')");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(4, spec.getParameters().length);
        Map<String, FilterSpecParam> parameters = mapParameters(spec.getParameters());

        assertEquals(FilterOperator.GREATER, parameters.get("doubleBoxed").getFilterOperator());
        assertEquals(1.11, getConstant(parameters.get("doubleBoxed")));

        assertEquals(FilterOperator.GREATER_OR_EQUAL, parameters.get("doublePrimitive").getFilterOperator());
        assertEquals(9.11, getConstant(parameters.get("doublePrimitive")));

        assertEquals(FilterOperator.LESS_OR_EQUAL, parameters.get("intPrimitive").getFilterOperator());
        assertEquals(9, getConstant(parameters.get("intPrimitive")));

        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, parameters.get(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION).getFilterOperator());
        assertTrue(parameters.get(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION) instanceof FilterSpecParamExprNode);
    }

    public void testNestedAnd() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() +
                "((doubleBoxed=1 and doublePrimitive=2) and (intPrimitive=3 and (theString like '%_a' and theString = 'a')))");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(5, spec.getParameters().length);
        Map<String, FilterSpecParam> parameters = mapParameters(spec.getParameters());

        assertEquals(FilterOperator.EQUAL, parameters.get("doubleBoxed").getFilterOperator());
        assertEquals(1.0, getConstant(parameters.get("doubleBoxed")));

        assertEquals(FilterOperator.EQUAL, parameters.get("doublePrimitive").getFilterOperator());
        assertEquals(2.0, getConstant(parameters.get("doublePrimitive")));

        assertEquals(FilterOperator.EQUAL, parameters.get("intPrimitive").getFilterOperator());
        assertEquals(3, getConstant(parameters.get("intPrimitive")));

        assertEquals(FilterOperator.EQUAL, parameters.get("theString").getFilterOperator());
        assertEquals("a", getConstant(parameters.get("theString")));

        assertEquals(FilterOperator.BOOLEAN_EXPRESSION, parameters.get(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION).getFilterOperator());
        assertTrue(parameters.get(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION) instanceof FilterSpecParamExprNode);
    }

    public void testIn() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() + "(doubleBoxed in (1, 2, 3))");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(1, spec.getParameters().length);

        assertEquals("doubleBoxed", spec.getParameters()[0].getLookupable().getExpression());
        assertEquals(FilterOperator.IN_LIST_OF_VALUES, spec.getParameters()[0].getFilterOperator());
        FilterSpecParamIn inParam = (FilterSpecParamIn) spec.getParameters()[0];
        assertEquals(3, inParam.getListOfValues().size());
        assertEquals(1.0, getConstant(inParam.getListOfValues().get(0)));
        assertEquals(2.0, getConstant(inParam.getListOfValues().get(1)));
        assertEquals(3.0, getConstant(inParam.getListOfValues().get(2)));
    }

    public void testNotIn() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() + "(theString not in (\"a\"))");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(1, spec.getParameters().length);

        assertEquals("theString", spec.getParameters()[0].getLookupable().getExpression());
        assertEquals(FilterOperator.NOT_IN_LIST_OF_VALUES, spec.getParameters()[0].getFilterOperator());
        FilterSpecParamIn inParam = (FilterSpecParamIn) spec.getParameters()[0];
        assertEquals(1, inParam.getListOfValues().size());
        assertEquals("a", getConstant(inParam.getListOfValues().get(0)));
    }

    public void testRanges() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() +
                "(intBoxed in [1:5] and doubleBoxed in (2:6) and floatBoxed in (3:7] and byteBoxed in [0:1))");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(4, spec.getParameters().length);
        Map<String, FilterSpecParam> parameters = mapParameters(spec.getParameters());

        assertEquals(FilterOperator.RANGE_CLOSED, parameters.get("intBoxed").getFilterOperator());
        FilterSpecParamRange rangeParam = (FilterSpecParamRange) parameters.get("intBoxed");
        assertEquals(1.0, getConstant(rangeParam.getMin()));
        assertEquals(5.0, getConstant(rangeParam.getMax()));

        assertEquals(FilterOperator.RANGE_OPEN, parameters.get("doubleBoxed").getFilterOperator());
        rangeParam = (FilterSpecParamRange) parameters.get("doubleBoxed");
        assertEquals(2.0, getConstant(rangeParam.getMin()));
        assertEquals(6.0, getConstant(rangeParam.getMax()));

        assertEquals(FilterOperator.RANGE_HALF_CLOSED, parameters.get("floatBoxed").getFilterOperator());
        rangeParam = (FilterSpecParamRange) parameters.get("floatBoxed");
        assertEquals(3.0, getConstant(rangeParam.getMin()));
        assertEquals(7.0, getConstant(rangeParam.getMax()));

        assertEquals(FilterOperator.RANGE_HALF_OPEN, parameters.get("byteBoxed").getFilterOperator());
        rangeParam = (FilterSpecParamRange) parameters.get("byteBoxed");
        assertEquals(0.0, getConstant(rangeParam.getMin()));
        assertEquals(1.0, getConstant(rangeParam.getMax()));
    }

    public void testRangesNot() throws Exception
    {
        FilterStreamSpecRaw raw = makeSpec("select * from " + SupportBean.class.getName() +
                "(intBoxed not in [1:5] and doubleBoxed not in (2:6) and floatBoxed not in (3:7] and byteBoxed not in [0:1))");
        FilterSpecCompiled spec = compile(raw);
        assertEquals(4, spec.getParameters().length);
        Map<String, FilterSpecParam> parameters = mapParameters(spec.getParameters());

        assertEquals(FilterOperator.NOT_RANGE_CLOSED, parameters.get("intBoxed").getFilterOperator());
        FilterSpecParamRange rangeParam = (FilterSpecParamRange) parameters.get("intBoxed");
        assertEquals(1.0, getConstant(rangeParam.getMin()));
        assertEquals(5.0, getConstant(rangeParam.getMax()));

        assertEquals(FilterOperator.NOT_RANGE_OPEN, parameters.get("doubleBoxed").getFilterOperator());
        rangeParam = (FilterSpecParamRange) parameters.get("doubleBoxed");
        assertEquals(2.0, getConstant(rangeParam.getMin()));
        assertEquals(6.0, getConstant(rangeParam.getMax()));

        assertEquals(FilterOperator.NOT_RANGE_HALF_CLOSED, parameters.get("floatBoxed").getFilterOperator());
        rangeParam = (FilterSpecParamRange) parameters.get("floatBoxed");
        assertEquals(3.0, getConstant(rangeParam.getMin()));
        assertEquals(7.0, getConstant(rangeParam.getMax()));

        assertEquals(FilterOperator.NOT_RANGE_HALF_OPEN, parameters.get("byteBoxed").getFilterOperator());
        rangeParam = (FilterSpecParamRange) parameters.get("byteBoxed");
        assertEquals(0.0, getConstant(rangeParam.getMin()));
        assertEquals(1.0, getConstant(rangeParam.getMax()));
    }

    private double getConstant(FilterSpecParamRangeValue param)
    {
        return ((RangeValueDouble) param).getDoubleValue();
    }

    private Object getConstant(FilterSpecParamInValue param)
    {
        InSetOfValuesConstant constant = (InSetOfValuesConstant) param;
        return constant.getConstant();
    }

    private Object getConstant(FilterSpecParam param)
    {
        FilterSpecParamConstant constant = (FilterSpecParamConstant) param;
        return constant.getFilterConstant();
    }

    private FilterSpecCompiled compile(FilterStreamSpecRaw raw) throws Exception
    {
        FilterStreamSpecCompiled compiled = (FilterStreamSpecCompiled) raw.compile(SupportStatementContextFactory.makeContext(), new HashSet<String>(), false, Collections.<Integer>emptyList());
        return compiled.getFilterSpec();
    }

    private static FilterStreamSpecRaw makeSpec(String expression) throws Exception
    {
        Pair<Tree, CommonTokenStream> ast = SupportParserHelper.parseEPL(expression);
        SupportParserHelper.displayAST(ast.getFirst());

        EPLTreeWalker walker = SupportEPLTreeWalkerFactory.makeWalker(ast.getFirst(), ast.getSecond());
        walker.startEPLExpressionRule();

        return (FilterStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);
    }
}
