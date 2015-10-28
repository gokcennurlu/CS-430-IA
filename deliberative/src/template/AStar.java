package template;

import java.util.Comparator;
import java.util.PriorityQueue;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class AStar extends GenericSearch {

	public AStar(Vehicle vehicle, TaskSet tasks, State startState) {
		super(vehicle, tasks, startState);
		this.queue = new PriorityQueue<State>(10, new Comparator<State>() {
			@Override
			public int compare(State o1, State o2) {
				if (o1.f() < o2.f()) return -1;
				return 1;
			}
		});
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
