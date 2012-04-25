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

import com.espertech.esper.client.dataflow.EPDataFlowSignalFinalMarker;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.interfaces.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Iterator;

public class MyObjectArrayGraphSource implements DataFlowSourceOperator {
    private static final Log log = LogFactory.getLog(MyObjectArrayGraphSource.class);

    private final Iterator<Object[]> iterator;

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    public MyObjectArrayGraphSource(Iterator<Object[]> iterator) {
        this.iterator = iterator;
    }

    public void next() {
        if (iterator.hasNext()) {
            Object[] next = iterator.next();
            if (log.isDebugEnabled()) {
                log.debug("submitting row " + Arrays.toString(next));
            }
            graphContext.submit(next);
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("submitting punctuation");
            }
            graphContext.submitSignal(new EPDataFlowSignalFinalMarker() {});
        }
    }

    public DataFlowOpInitializeResult initialize(DataFlowOpInitializateContext context) throws Exception {
        return null;
    }

    public void open(DataFlowOpOpenContext openContext) {
    }

    public void close(DataFlowOpCloseContext openContext) {
    }
}
