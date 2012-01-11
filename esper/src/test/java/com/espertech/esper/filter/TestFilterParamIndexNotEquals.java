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

package com.espertech.esper.filter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.event.SupportEventBeanFactory;
import com.espertech.esper.support.filter.SupportEventEvaluator;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

public class TestFilterParamIndexNotEquals extends TestCase
{
    private SupportEventEvaluator testEvaluator;
    private SupportBean testBean;
    private EventBean testEventBean;
    private EventType testEventType;
    private List<FilterHandle> matchesList;

    public void setUp()
    {
        testEvaluator = new SupportEventEvaluator();
        testBean = new SupportBean();
        testEventBean = SupportEventBeanFactory.createObject(testBean);
        testEventType = testEventBean.getEventType();
        matchesList = new LinkedList<FilterHandle>();
    }

    public void testBoolean()
    {
        FilterParamIndexNotEquals index = new FilterParamIndexNotEquals(makeLookupable("boolPrimitive"));
        assertEquals(FilterOperator.NOT_EQUAL, index.getFilterOperator());
        assertEquals("boolPrimitive", index.getLookupable().getExpression());

        index.put(false, testEvaluator);

        verifyBooleanPrimitive(index, true, 1);
        verifyBooleanPrimitive(index, false, 0);
    }

    public void testString()
    {
        FilterParamIndexNotEquals index = new FilterParamIndexNotEquals(makeLookupable("string"));

        index.put("hello", testEvaluator);
        index.put("test", testEvaluator);

        verifyString(index, null, 0);
        verifyString(index, "dudu", 2);
        verifyString(index, "hello", 1);
        verifyString(index, "test", 1);
    }

    private void verifyBooleanPrimitive(FilterParamIndexBase index, boolean testValue, int numExpected)
    {
        testBean.setBoolPrimitive(testValue);
        index.matchEvent(testEventBean, matchesList);
        assertEquals(numExpected, testEvaluator.getAndResetCountInvoked());
    }

    private void verifyString(FilterParamIndexBase index, String testValue, int numExpected)
    {
        testBean.setString(testValue);
        index.matchEvent(testEventBean, matchesList);
        assertEquals(numExpected, testEvaluator.getAndResetCountInvoked());
    }

    private FilterSpecLookupable makeLookupable(String fieldName) {
        return new FilterSpecLookupable(fieldName, testEventType.getGetter(fieldName), testEventType.getPropertyType(fieldName));
    }
}
