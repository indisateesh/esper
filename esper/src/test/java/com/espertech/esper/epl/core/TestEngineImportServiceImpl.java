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

package com.espertech.esper.epl.core;

import com.espertech.esper.client.ConfigurationPlugInAggregationFunction;
import com.espertech.esper.support.epl.SupportPluginAggregationMethodOne;
import junit.framework.TestCase;

import java.lang.reflect.Method;

public class TestEngineImportServiceImpl extends TestCase
{
    EngineImportServiceImpl engineImportService;

    public void setUp()
    {
        this.engineImportService = new EngineImportServiceImpl(true, true, true, false);
    }

    public void testResolveMethodNoArgTypes() throws Exception
    {
        Method method = engineImportService.resolveMethod("java.lang.Math", "cbrt");
        assertEquals(Math.class.getMethod("cbrt", new Class[] {double.class}), method);

        try
        {
            engineImportService.resolveMethod("java.lang.Math", "abs");
            fail();
        }
        catch (EngineImportException ex)
        {
            assertEquals("Ambiguous method name: method by name 'abs' is overloaded in class 'java.lang.Math'", ex.getMessage());
        }
    }

    public void testAddAggregation() throws EngineImportException
    {
        engineImportService.addAggregation("abc", new ConfigurationPlugInAggregationFunction("abc", "abcdef.G", null));
        engineImportService.addAggregation("abcDefGhk", new ConfigurationPlugInAggregationFunction("abcDefGhk", "ab", null));
        engineImportService.addAggregation("a", new ConfigurationPlugInAggregationFunction("a", "Yh", null));

        tryInvalidAddAggregation("g h", "");
        tryInvalidAddAggregation("gh", "j j");
        tryInvalidAddAggregation("abc", "hhh");
    }

    public void testResolveAggregationMethod() throws Exception
    {
        engineImportService.addAggregation("abc", new ConfigurationPlugInAggregationFunction("abc", SupportPluginAggregationMethodOne.class.getName(), null));
        assertTrue(engineImportService.resolveAggregation("abc") instanceof SupportPluginAggregationMethodOne);
    }

    public void testInvalidResolveAggregation(String funcName) throws Exception
    {
        try
        {
            engineImportService.resolveAggregation("abc");
        }
        catch (EngineImportUndefinedException ex)
        {
            // expected
        }
        
        engineImportService.addAggregation("abc", new ConfigurationPlugInAggregationFunction("abc", "abcdef.G", null));
        try
        {
            engineImportService.resolveAggregation("abc");
        }
        catch (EngineImportException ex)
        {
            // expected
        }
    }

    public void testResolveClass() throws Exception
    {
        String className = "java.lang.Math";
        Class expected = Math.class;
        assertEquals(expected, engineImportService.resolveClassInternal(className, false));

        engineImportService.addImport("java.lang.Math");
        assertEquals(expected, engineImportService.resolveClassInternal(className, false));

        engineImportService.addImport("java.lang.*");
        className = "String";
        expected = String.class;
        assertEquals(expected, engineImportService.resolveClassInternal(className, false));
    }

    public void testResolveClassInvalid()
    {
        String className = "Math";
        try
        {
            engineImportService.resolveClassInternal(className, false);
            fail();
        }
        catch (ClassNotFoundException e)
        {
            // Expected
        }
    }

    public void testAddImport() throws EngineImportException
    {
        engineImportService.addImport("java.lang.Math");
        assertEquals(1, engineImportService.getImports().length);
        assertEquals("java.lang.Math", engineImportService.getImports()[0]);

        engineImportService.addImport("java.lang.*");
        assertEquals(2, engineImportService.getImports().length);
        assertEquals("java.lang.Math", engineImportService.getImports()[0]);
        assertEquals("java.lang.*", engineImportService.getImports()[1]);
    }

    public void testAddImportInvalid()
    {
        try
        {
            engineImportService.addImport("java.lang.*.*");
            fail();
        }
        catch (EngineImportException e)
        {
            // Expected
        }

        try
        {
            engineImportService.addImport("java.lang..Math");
            fail();
        }
        catch (EngineImportException e)
        {
            // Expected
        }
    }

    private void tryInvalidAddAggregation(String funcName, String className)
    {
        try
        {
            engineImportService.addAggregation(funcName, new ConfigurationPlugInAggregationFunction(funcName, className, null));
        }
        catch (EngineImportException ex)
        {
            // expected
        }
    }
}
