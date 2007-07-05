package net.esper.view.std;

import net.esper.core.StatementContext;
import net.esper.event.EventAdapterService;
import net.esper.event.EventBean;
import net.esper.event.EventType;
import net.esper.view.CloneableView;
import net.esper.view.View;
import net.esper.view.ViewSupport;
import net.esper.view.Viewable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This view simply adds a property to the events posted to it. This is useful for the group-merge views.
 */
public final class AddPropertyValueView extends ViewSupport implements CloneableView
{
    private final StatementContext statementContext;
    private final String[] propertyNames;
    private final Object[] propertyValues;
    private final EventType eventType;
    private boolean mustAddProperty;

    // Keep a history of posted old events to avoid reconstructing the event
    // and adhere to the contract of posting the same reference to child views
    private Map<EventBean, EventBean> newToOldEventMap;

    /**
     * Constructor.
     * @param fieldNames is the name of the field that is added to any events received by this view.
     * @param mergeValues is the values of the field that is added to any events received by this view.
     * @param mergedResultEventType is the event type that the merge view reports to it's child views
     * @param statementContext contains required view services
     */
    public AddPropertyValueView(StatementContext statementContext, String[] fieldNames, Object[] mergeValues, EventType mergedResultEventType)
    {
        this.propertyNames = fieldNames;
        this.propertyValues = mergeValues;
        this.eventType = mergedResultEventType;
        this.statementContext = statementContext;
        newToOldEventMap = new HashMap<EventBean, EventBean>();
    }

    public View cloneView(StatementContext statementContext)
    {
        return new AddPropertyValueView(statementContext, propertyNames, propertyValues, eventType);
    }

    public void setParent(Viewable parent)
    {
        if (log.isDebugEnabled())
        {
            log.debug(".setParent parent=" + parent);
        }
        super.setParent(parent);

        if (parent.getEventType() != eventType)
        {
            mustAddProperty = true;
        }
        else
        {
            mustAddProperty = false;
        }
    }

    /**
     * Returns field name for which to set the merge value for.
     * @return field name to use to set value
     */
    public final String[] getPropertyNames()
    {
        return propertyNames;
    }

    /**
     * Returns the value to set for the field.
     * @return value to set
     */
    public final Object[] getPropertyValues()
    {
        return propertyValues;
    }

    public final void update(EventBean[] newData, EventBean[] oldData)
    {
        if (!mustAddProperty)
        {
            updateChildren(newData, oldData);
            return;
        }

        EventBean[] newEvents = null;
        EventBean[] oldEvents = null;

        if (newData != null)
        {
            newEvents = new EventBean[newData.length];

            int index = 0;
            for (EventBean newEvent : newData)
            {
                EventBean event = addProperty(newEvent, propertyNames, propertyValues, eventType, statementContext.getEventAdapterService());
                newEvents[index++] = event;

                newToOldEventMap.put(newEvent, event);
            }
        }

        if (oldData != null)
        {
            oldEvents = new EventBean[oldData.length];

            int index = 0;
            for (EventBean oldEvent : oldData)
            {
                EventBean outgoing = newToOldEventMap.remove(oldEvent);
                if (outgoing != null)
                {
                    oldEvents[index++] = outgoing;
                }
                else
                {
                    EventBean event = addProperty(oldEvent, propertyNames, propertyValues, eventType, statementContext.getEventAdapterService());
                    oldEvents[index++] = event;
                }
            }
        }

        updateChildren(newEvents, oldEvents);
    }

    public final EventType getEventType()
    {
        return eventType;
    }

    public final Iterator<EventBean> iterator()
    {
        final Iterator<EventBean> parentIterator = parent.iterator();

        return new Iterator<EventBean>()
        {
            public boolean hasNext()
            {
                return parentIterator.hasNext();
            }

            public EventBean next()
            {
                EventBean nextEvent = parentIterator.next();
                if (mustAddProperty)
                {
                    EventBean event = addProperty(nextEvent, propertyNames, propertyValues, eventType,
                            statementContext.getEventAdapterService());
                    return event;
                }
                else
                {
                    return nextEvent;
                }
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Add a property to the event passed in.
     * @param originalEvent - event to add property to
     * @param propertyNames - names of properties to add
     * @param propertyValues - value of properties to add
     * @param targetEventType - new event type
     * @param eventAdapterService - service for generating events and handling event types
     * @return event with added property
     */
    protected static EventBean addProperty(EventBean originalEvent,
                                       String[] propertyNames,
                                       Object[] propertyValues,
                                       EventType targetEventType,
                                       EventAdapterService eventAdapterService)
    {
        Map<String, Object> values = new HashMap<String, Object>();

        // Copy properties of original event, add property value
        for (String property : originalEvent.getEventType().getPropertyNames())
        {
            values.put(property, originalEvent.get(property));
        }

        for (int i = 0; i < propertyNames.length; i++)
        {
            values.put(propertyNames[i], propertyValues[i]);
        }

        return eventAdapterService.createMapFromValues(values, targetEventType);
    }

    public final String toString()
    {
        return this.getClass().getName() + " propertyNames=" + Arrays.toString(propertyNames) +
                " propertyValue=" + Arrays.toString(propertyValues);
    }

    private static final Log log = LogFactory.getLog(AddPropertyValueView.class);
}
