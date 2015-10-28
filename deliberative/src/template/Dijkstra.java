package template;

import java.util.Comparator;
import java.util.PriorityQueue;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class Dijkstra extends GenericSearch {

	public Dijkstra(Vehicle vehicle, TaskSet tasks, State startState) {
		super(vehicle, tasks, startState);
		this.queue = new PriorityQueue<State>(10, new Comparator<State>() {
			@Override
			public int compare(State o1, State o2) {
				if (o1.g < o2.g) return -1;
				return 1;
			}
		});
	}
}
