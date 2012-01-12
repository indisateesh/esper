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

import com.espertech.esper.pattern.EvalFactoryNode;

public class ContextDetailConditionPattern implements ContextDetailCondition {

    private final EvalFactoryNode patternRaw;
    private final boolean inclusive;

    private transient PatternStreamSpecCompiled patternCompiled;

    public ContextDetailConditionPattern(EvalFactoryNode patternRaw, boolean inclusive) {
        this.patternRaw = patternRaw;
        this.inclusive = inclusive;
    }

    public EvalFactoryNode getPatternRaw() {
        return patternRaw;
    }

    public PatternStreamSpecCompiled getPatternCompiled() {
        return patternCompiled;
    }

    public void setPatternCompiled(PatternStreamSpecCompiled patternCompiled) {
        this.patternCompiled = patternCompiled;
    }

    public boolean isInclusive() {
        return inclusive;
    }
}
