package com.trideveloper.log;

public class Workflow {

    private final String module;

    private final String bo;

    private final String name;

    private final String event;

    private long firstTime = Long.MAX_VALUE;

    private long lastTime = Long.MIN_VALUE;

    private long minDuration = Long.MAX_VALUE;

    private long maxDuration = Long.MIN_VALUE;

    private long totalExecutionTime;

    private int executionCount;

    public Workflow(String module, String bo, String name, String event) {
        this.module = module;
        this.bo = bo;
        this.name = name;
        this.event = event;
    }

    public void addExecution(long time, long duration) {
        executionCount++;
        totalExecutionTime += duration;
        if (time < firstTime) firstTime = time;
        if (time > lastTime) lastTime = time;
        if (duration < minDuration) minDuration = duration;
        if (duration > maxDuration) maxDuration = duration;
    }

    public String getModule() {
        return module;
    }

    public String getBo() {
        return bo;
    }

    public String getName() {
        return name;
    }

    public String getEvent() {
        return event;
    }

    public long getFirstTime() {
        return firstTime;
    }

    public long getLastTime() {
        return lastTime;
    }

    public long getMinDuration() {
        return minDuration;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    public long getTotalExecutionTime() {
        return totalExecutionTime;
    }

    public int getExecutionCount() {
        return executionCount;
    }

}
