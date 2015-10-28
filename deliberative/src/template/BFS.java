package template;

import java.util.LinkedList;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class BFS extends GenericSearch {
	
	public BFS(Vehicle vehicle, TaskSet tasks, State startState) {
		super(vehicle, tasks, startState);
		this.queue = new LinkedList<State>();
	}
}
