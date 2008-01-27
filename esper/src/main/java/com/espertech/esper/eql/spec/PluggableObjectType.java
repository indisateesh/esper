package com.espertech.esper.eql.spec;

import com.espertech.esper.collection.Pair;

import java.util.Map;

/**
 * Enumeration for types of plug-in objects.
 */
public enum PluggableObjectType
{
    /**
     * Pattern guard object type.
     */
    PATTERN_GUARD,

    /**
     * Pattern observer object type.
     */
    PATTERN_OBSERVER,

    /**
     * View object type.
     */
    VIEW
}
