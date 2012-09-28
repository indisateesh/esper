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

package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.context.ContextPartitionSelector;
import com.espertech.esper.type.NumberSetParameter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ContextController {
    public int getPathId();
    public void activate(EventBean optionalTriggeringEvent, Map<String, Object> optionalTriggeringPattern, ContextControllerState states, ContextInternalFilterAddendum filterAddendum);
    public ContextControllerFactory getFactory();
    public void deactivate();
    public Collection<Integer> getSelectedContextPartitionPathIds(ContextPartitionSelector contextPartitionSelector);

    public void setContextPartitionRange(List<NumberSetParameter> partitionRanges);
}
