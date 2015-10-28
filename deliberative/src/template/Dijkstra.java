package template;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class Dijkstra extends GenericSearch {

	public Dijkstra(Vehicle vehicle, TaskSet tasks, State startState) {
		super(vehicle, tasks, startState);
		this.queue = new PriorityQueue<State>();
	}

	@Override
	protected State getNextElement() {
		return this.queue.poll();
	}

	@Override
	protected void addElement(State element) {
		this.queue.add(element);
	}

}
