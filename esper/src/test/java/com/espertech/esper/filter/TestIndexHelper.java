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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;

public class TestIndexHelper extends TestCase
{
    private EventType eventType;
    private ArrayDeque<FilterValueSetParam> parameters;
    private FilterValueSetParam parameterOne;
    private FilterValueSetParam parameterTwo;
    private FilterValueSetParam parameterThree;

    public void setUp()
    {
        eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        parameters = new ArrayDeque<FilterValueSetParam>();

        // Create parameter test list
        parameterOne = new FilterValueSetParamImpl(makeLookupable("intPrimitive"), FilterOperator.GREATER, 10);
        parameters.add(parameterOne);
        parameterTwo = new FilterValueSetParamImpl(makeLookupable("doubleBoxed"), FilterOperator.GREATER, 20d);
        parameters.add(parameterTwo);
        parameterThree = new FilterValueSetParamImpl(makeLookupable("string"), FilterOperator.EQUAL, "sometext");
        parameters.add(parameterThree);
    }

    public void testFindIndex()
    {
        List<FilterParamIndexBase> indexes = new LinkedList<FilterParamIndexBase>();

        // Create index list wity index that doesn't match
        FilterParamIndexBase indexOne = IndexFactory.createIndex(makeLookupable("boolPrimitive"), FilterOperator.EQUAL);
        indexes.add(indexOne);
        assertTrue(IndexHelper.findIndex(parameters, indexes) == null);

        // Create index list wity index that doesn't match
        indexOne = IndexFactory.createIndex(makeLookupable("doubleBoxed"), FilterOperator.GREATER_OR_EQUAL);
        indexes.clear();
        indexes.add(indexOne);
        assertTrue(IndexHelper.findIndex(parameters, indexes) == null);

        // Add an index that does match a parameter
        FilterParamIndexBase indexTwo = IndexFactory.createIndex(makeLookupable("doubleBoxed"), FilterOperator.GREATER);
        indexes.add(indexTwo);
        Pair<FilterValueSetParam, FilterParamIndexBase> pair = IndexHelper.findIndex(parameters, indexes);
        assertTrue(pair != null);
        assertEquals(parameterTwo, pair.getFirst());
        assertEquals(indexTwo, pair.getSecond());

        // Add another index that does match a parameter, should return first match however which is doubleBoxed
        FilterParamIndexBase indexThree = IndexFactory.createIndex(makeLookupable("intPrimitive"), FilterOperator.GREATER);
        indexes.add(indexThree);
        pair = IndexHelper.findIndex(parameters, indexes);
        assertEquals(parameterOne, pair.getFirst());
        assertEquals(indexThree, pair.getSecond());

        // Try again removing one index
        indexes.remove(indexTwo);
        pair = IndexHelper.findIndex(parameters, indexes);
        assertEquals(parameterOne, pair.getFirst());
        assertEquals(indexThree, pair.getSecond());
    }

    public void testFindParameter()
    {
        FilterParamIndexBase indexOne = IndexFactory.createIndex(makeLookupable("boolPrimitive"), FilterOperator.EQUAL);
        assertNull(IndexHelper.findParameter(parameters, indexOne));

        FilterParamIndexBase indexTwo = IndexFactory.createIndex(makeLookupable("string"), FilterOperator.EQUAL);
        assertEquals(parameterThree, IndexHelper.findParameter(parameters, indexTwo));

        FilterParamIndexBase indexThree = IndexFactory.createIndex(makeLookupable("intPrimitive"), FilterOperator.GREATER);
        assertEquals(parameterOne, IndexHelper.findParameter(parameters, indexThree));
    }

    private FilterSpecLookupable makeLookupable(String fieldName) {
        return new FilterSpecLookupable(fieldName, eventType.getGetter(fieldName), eventType.getPropertyType(fieldName));
    }
}
