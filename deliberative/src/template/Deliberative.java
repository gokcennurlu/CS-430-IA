package template;

/* import table */
import java.util.*;
import java.util.concurrent.SynchronousQueue;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR, DIJKSTRA }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	TaskSet carried;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		

	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		
		HashSet<Task> agentTasks;
		
		if (carried != null) {
			agentTasks = new HashSet<Task>(carried);
		} else {
			agentTasks = new HashSet<Task>();
		}

		// Build State graph
		System.out.println(tasks.size());
		
		State startState = new State(agentTasks, tasks.clone(), vehicle.getCurrentCity(), null, topology);


		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			AStar astar = new AStar(vehicle, startState);
			plan = astar.search();
			break;
		case BFS:
			// ...
			BFS bfs = new BFS(vehicle, startState);
			plan = bfs.search();
			break;
		case DIJKSTRA:
			Dijkstra dijkstra = new Dijkstra(vehicle, startState);
			plan = dijkstra.search();
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);
			current = task.deliveryCity;
		}
		return plan;
	}
	
	private Plan AStar(Vehicle vehicle, TaskSet tasks) {
		return new Plan(vehicle.getCurrentCity());
	}


	@Override
	public void planCancelled(TaskSet carriedTasks) {
		this.carried = carriedTasks;
	}
}