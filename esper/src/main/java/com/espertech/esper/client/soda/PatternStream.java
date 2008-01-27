/**************************************************************************************
 * Copyright (C) 2006 Esper Team. All rights reserved.                                *
 * http://esper.codehaus.org                                                          *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.client.soda;

import java.util.ArrayList;
import java.util.List;
import java.io.StringWriter;

/**
 * A stream of events that is generated by pattern matches.
 * <p>
 * Patterns matches are events that match pattern expressions. Pattern expressions are built using
 * {@link Patterns}.
 */
public class PatternStream extends ProjectedStream
{
    private PatternExpr expression;

    /**
     * Creates a pattern stream from a pattern expression.
     * @param expression pattern expression
     * @return stream
     */
    public static PatternStream create(PatternExpr expression)
    {
        return new PatternStream(expression);
    }

    /**
     * Creates a named pattern stream from a pattern expression.
     * @param expression pattern expression
     * @param optStreamName is the pattern stream name (as-name)
     * @return stream
     */
    public static PatternStream create(PatternExpr expression, String optStreamName)
    {
        return new PatternStream(expression, optStreamName);
    }

    /**
     * Ctor.
     * @param expression pattern expression
     */
    public PatternStream(PatternExpr expression)
    {
        this(expression, null);
    }

    /**
     * Ctor.
     * @param expression pattern expression
     * @param optStreamName is the pattern stream name (as-name)
     */
    public PatternStream(PatternExpr expression, String optStreamName)
    {
        super(new ArrayList<View>(), optStreamName);
        this.expression = expression;
    }

    /**
     * Returns the pattern expression providing events to the stream.
     * @return pattern expression
     */
    public PatternExpr getExpression()
    {
        return expression;
    }

    /**
     * Sets the pattern expression providing events to the stream.
     * @param expression is the pattern expression
     */
    public void setExpression(PatternExpr expression)
    {
        this.expression = expression;
    }

    public void toEQLProjectedStream(StringWriter writer)
    {
        writer.write("pattern [");
        expression.toEQL(writer);
        writer.write(']');
    }
}
