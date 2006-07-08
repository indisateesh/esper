package net.esper.eql.view;

import net.esper.schedule.ScheduleCallback;
import net.esper.schedule.ScheduleSlot;
import net.esper.view.ViewServiceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Output condition that is satisfied at the end
 * of every time interval of a given length.
 */
public final class OutputConditionTime implements OutputCondition
{
    private final long msecIntervalSize;
    private final OutputCallback outputCallback;
    private final ScheduleSlot scheduleSlot;

    private Long currentReferencePoint;
    private ViewServiceContext context; 
    private boolean isCallbackScheduled;



    /**
     * Constructor.
     * @param secIntervalSize is the number of seconds to batch events for.
     * @param context is the view context for time scheduling	
     * @param outputCallback is the callback to make once the condition is satisfied
     */
    public OutputConditionTime(double secIntervalSize, 
    						   ViewServiceContext context, 
    						   OutputCallback outputCallback)
    {
        if (secIntervalSize < 0.1)
        {
            throw new IllegalArgumentException("Output condition by time requires a millisecond interval size of at least 100 msec");
        }
        if (context == null)
        {
            String message = "OutputConditionTime requires a non-null view context";
            throw new NullPointerException(message);
        }
        if(outputCallback == null)
        {
        	throw new NullPointerException("Output condition by time requires a non-null callback");
        }
        
        this.msecIntervalSize = Math.round(1000 * secIntervalSize);
        this.context = context;   
        this.outputCallback = outputCallback;
        this.scheduleSlot = context.getScheduleBucket().allocateSlot();
    }
    
    /**
     * Returns the interval size in milliseconds.
     * @return batch size
     */
    public final long getMsecIntervalSize()
    {
        return msecIntervalSize;
    }
    
    public final void updateOutputCondition(int newEventsCount, int oldEventsCount)
    {
        if (log.isDebugEnabled())
        {
        	log.debug(".updateOutputCondition, " +
        			"  newEventsCount==" + newEventsCount +
        			"  oldEventsCount==" + oldEventsCount);
        }
        
        if (currentReferencePoint == null)
        {
        	currentReferencePoint = context.getSchedulingService().getTime();
        }
        
        // Schedule the next callback if there is none currently scheduled
        if (!isCallbackScheduled)
        {
        	scheduleCallback();
        }
    }

    public final String toString()
    {
        return this.getClass().getName() +
                " msecIntervalSize=" + msecIntervalSize;
    }

    private void scheduleCallback()
    {
    	isCallbackScheduled = true;
        long current = context.getSchedulingService().getTime();
        long afterMSec = computeWaitMSec(current, this.currentReferencePoint, this.msecIntervalSize);

        if (log.isDebugEnabled())
        {
            log.debug(".scheduleCallback Scheduled new callback for " +
                    " afterMsec=" + afterMSec +
                    " now=" + current +
                    " currentReferencePoint=" + currentReferencePoint +
                    " msecIntervalSize=" + msecIntervalSize);
        }

        ScheduleCallback callback = new ScheduleCallback() {
            public void scheduledTrigger()
            {
                OutputConditionTime.this.isCallbackScheduled = false;
                OutputConditionTime.this.outputCallback.continueOutputProcessing(true);
                scheduleCallback();
            }
        };
        context.getSchedulingService().add(afterMSec, callback, scheduleSlot);
    }

    /**
     * Given a current time and a reference time and an interval size, compute the amount of
     * milliseconds till the next interval.
     * @param current is the current time
     * @param reference is the reference point
     * @param interval is the interval size
     * @return milliseconds after current time that marks the end of the current interval
     */
    protected static long computeWaitMSec(long current, long reference, long interval)
    {
        // Example:  current c=2300, reference r=1000, interval i=500, solution s=200
        //
        // int n = ((2300 - 1000) / 500) = 2
        // r + (n + 1) * i - c = 200
        //
        // Negative example:  current c=2300, reference r=4200, interval i=500, solution s=400
        // int n = ((2300 - 4200) / 500) = -3
        // r + (n + 1) * i - c = 4200 - 3*500 - 2300 = 400
        //
        long n = (long) ( (current - reference) / (interval * 1f));
        if (reference > current)        // References in the future need to deduct one window
        {
            n = n - 1;
        }
        long solution = reference + (n + 1) * interval - current;

        if (solution == 0)
        {
            return interval;
        }
        return solution;
    }

    private static final Log log = LogFactory.getLog(OutputConditionTime.class);


}
