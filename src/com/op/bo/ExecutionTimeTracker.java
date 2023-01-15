package com.op.bo;

public class ExecutionTimeTracker {
	private String taskName;
	private long startTime;
	private long endTime;

	public ExecutionTimeTracker(String taskName) {
		super();
		this.taskName = taskName;
		startTime = System.currentTimeMillis();
	}

	public void end() {
		endTime = System.currentTimeMillis();

		long duration = endTime - startTime;

		long seconds = duration / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		System.out.println("### Completed Task [" + taskName + "] in " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
	}
}
