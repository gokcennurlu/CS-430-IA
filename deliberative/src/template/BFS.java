package template;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class BFS extends GenericSearch {
	
	public BFS(Vehicle vehicle, TaskSet tasks, State startState) {
		super(vehicle, tasks, startState);
		this.queue = new PriorityQueue<State>(10, new Comparator<State>() {
			@Override
			public int compare(State o1, State o2) {
				if (o1.LEVEL < o2.LEVEL) return -1;
				return 1;
			}
		});
	}
}
