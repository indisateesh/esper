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
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.dataflow.EPDataFlowExecutionException;
import com.espertech.esper.client.dataflow.EPDataFlowInstance;
import com.espertech.esper.client.dataflow.EPDataFlowInstantiationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;

public class SupportDataFlowAssertionUtil {
    private static final Log log = LogFactory.getLog(SupportDataFlowAssertionUtil.class);

    public static void tryInvalidRun(EPServiceProvider epService, String epl, String name, String message) {
        epService.getEPAdministrator().createEPL(epl);
        EPDataFlowInstance df = epService.getEPRuntime().getDataFlowRuntime().instantiate(name);

        try {
            df.run();
            Assert.fail();
        }
        catch (EPDataFlowExecutionException ex) {
            assertException(message, ex.getMessage());
        }
    }

    public static void tryInvalidInstantiate(EPServiceProvider epService, String name, String epl, String message) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);

        try {
            epService.getEPRuntime().getDataFlowRuntime().instantiate(name);
            Assert.fail();
        }
        catch (EPDataFlowInstantiationException ex) {
            log.info("Expected exception: " + ex.getMessage(), ex);
            assertException(message, ex.getMessage());
        }
        finally {
            stmt.destroy();
        }
    }

    public static void tryInvalidCreate(EPServiceProvider epService, String epl, String message) {
        try {
            epService.getEPAdministrator().createEPL(epl);
            Assert.fail();
        }
        catch (EPStatementException ex) {
            assertException(message, ex.getMessage());
        }
    }

    private static void assertException(String expected, String message) {
        String received;
        if (message.lastIndexOf("[") != -1) {
            received = message.substring(0, message.lastIndexOf("[") + 1);
        }
        else {
            received = message;
        }
        if (message.startsWith(expected)) {
            Assert.assertFalse("empty expected message, received:\n" + message, expected.trim().isEmpty());
            return;
        }
        Assert.fail("Expected:\n" + expected + "\nbut received:\n" + received + "\n");
    }
}
