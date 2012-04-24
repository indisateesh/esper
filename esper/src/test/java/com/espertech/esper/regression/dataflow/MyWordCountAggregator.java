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

import com.espertech.esper.client.dataflow.EPDataFlowSignal;
import com.espertech.esper.dataflow.annotations.DataFlowContext;
import com.espertech.esper.dataflow.annotations.DataFlowOperator;
import com.espertech.esper.dataflow.annotations.OutputType;
import com.espertech.esper.dataflow.annotations.OutputTypes;
import com.espertech.esper.dataflow.interfaces.EPDataFlowEmitter;
import com.espertech.esper.dataflow.interfaces.EPDataFlowSignalHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@DataFlowOperator
@OutputTypes(value = {
    @OutputType(name = "stats", type = MyWordCountStats.class)
    })
public class MyWordCountAggregator implements EPDataFlowSignalHandler {
    private static final Log log = LogFactory.getLog(MyWordCountAggregator.class);

    @DataFlowContext
    private EPDataFlowEmitter graphContext;

    private final MyWordCountStats aggregate = new MyWordCountStats();

    public void onInput(int lines, int words, int chars) {
        aggregate.add(lines, words, chars);
        log.debug("Aggregated: " + aggregate);
    }

    public void onSignal(EPDataFlowSignal signal) {
        log.debug("Received punctuation, submitting totals: " + aggregate);
        graphContext.submit(aggregate);
    }
}
