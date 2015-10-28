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

	public static int getHashInitialMapSize(int n) {
		if(n < 6)
			return 16;
		//an observation. 6 sample generates  => ~1800 states. 7=>~6.000
		//8 => ~20.000, 9 => ~60.000, 10 => 210.000, 11 => 660.000 etc.
		//this fits well..
		return (int) (1800*Math.pow(3,n-6));
	}

	enum Algorithm { BFS, ASTAR, DIJKSTRA }
	private HashMap<State,State> states;
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
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

		// Build State graph
		states = new HashMap<State, State>(getHashInitialMapSize(tasks.size()));
		System.out.println(tasks.size());
		State startState = new State(new HashSet<Task>(), tasks.clone(), vehicle.getCurrentCity(), 0, 0, states);
		startState.LEVEL = 1;
		startState.g = 0;
		//State goalState = new State(new HashSet<Task>(), new HashSet<Task>(), null, 99, states);
		
		states.put(startState, startState);
		Queue<State> state_queue = new LinkedList<State>();
		state_queue.add(startState);
		while(!state_queue.isEmpty())
		{
			State currentState = state_queue.poll();
			LinkedList<State> generated = currentState.buildChildren(vehicle);
			state_queue.addAll(generated);

			/*System.out.println("CURRENT STATE: " + currentState.toString());
			System.out.println("GENERATED;");
			for(State s: generated)
				System.out.println("\t" + s.toString());
			*/
		}

		System.out.println("Total number of states: " + states.size());

		/*Collections.sort(states, new Comparator<State>() {
			public int compare(State one, State other) {
				return one.LEVEL - other.LEVEL;
			}
		});
		*/
		/*for(State s: states.values()){
			System.out.println(s);
		}*/

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// ...
			BFS bfs = new BFS(vehicle, tasks, startState);
			plan = bfs.search();
			break;
		case DIJKSTRA:
			Dijkstra dijkstra = new Dijkstra(vehicle, tasks, startState);
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
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	
	
	
	private Plan AStar(Vehicle vehicle, TaskSet tasks) {
		return new Plan(vehicle.getCurrentCity());
	}


	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
