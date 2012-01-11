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

public class TestFilterParamIndexEquals extends TestCase
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

    public void testLong()
    {
        FilterParamIndexEquals index = makeOne("shortBoxed", testEventType);

        index.put(Short.valueOf((short) 1), testEvaluator);
        index.put(Short.valueOf((short) 20), testEvaluator);

        verifyShortBoxed(index, (short) 10, 0);
        verifyShortBoxed(index, (short) 1, 1);
        verifyShortBoxed(index, (short) 20, 1);
        verifyShortBoxed(index, null, 0);

        assertEquals(testEvaluator, index.get((short) 1));
        assertTrue(index.getReadWriteLock() != null);
        assertTrue(index.remove((short) 1));
        assertFalse(index.remove((short) 1));
        assertEquals(null, index.get((short) 1));
    }

    public void testBoolean()
    {
        FilterParamIndexEquals index = makeOne("boolPrimitive", testEventType);

        index.put(false, testEvaluator);

        verifyBooleanPrimitive(index, false, 1);
        verifyBooleanPrimitive(index, true, 0);
    }

    public void testString()
    {
        FilterParamIndexEquals index = makeOne("string", testEventType);

        index.put("hello", testEvaluator);
        index.put("test", testEvaluator);

        verifyString(index, null, 0);
        verifyString(index, "dudu", 0);
        verifyString(index, "hello", 1);
        verifyString(index, "test", 1);
    }

    public void testFloatPrimitive()
    {
        FilterParamIndexEquals index = makeOne("floatPrimitive", testEventType);

        index.put(1.5f, testEvaluator);

        verifyFloatPrimitive(index, 1.5f, 1);
        verifyFloatPrimitive(index, 2.2f, 0);
        verifyFloatPrimitive(index, 0, 0);
    }

    private void verifyShortBoxed(FilterParamIndexBase index, Short testValue, int numExpected)
    {
        testBean.setShortBoxed(testValue);
        index.matchEvent(testEventBean, matchesList);
        assertEquals(numExpected, testEvaluator.getAndResetCountInvoked());
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

    private void verifyFloatPrimitive(FilterParamIndexBase index, float testValue, int numExpected)
    {
        testBean.setFloatPrimitive(testValue);
        index.matchEvent(testEventBean, matchesList);
        assertEquals(numExpected, testEvaluator.getAndResetCountInvoked());
    }

    private FilterParamIndexEquals makeOne(String property, EventType testEventType) {
        return new FilterParamIndexEquals(makeLookupable(property));
    }

    private FilterSpecLookupable makeLookupable(String fieldName) {
        return new FilterSpecLookupable(fieldName, testEventType.getGetter(fieldName), testEventType.getPropertyType(fieldName));
    }
}