package template;

import java.util.PriorityQueue;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class Dijkstra extends GenericSearch {

	public Dijkstra(Vehicle vehicle, TaskSet tasks, State startState) {
		super(vehicle, tasks, startState);
		this.queue = new PriorityQueue<State>();
	}

	@Override
	protected State getNextFromQueue() {
		return this.queue.poll();
	}

	@Override
	protected void addToQueue(State element) {
		this.queue.add(element);
		
	}

	@Override
	protected void removeFromQueue(State element) {
		this.queue.remove(element);
	}

}
