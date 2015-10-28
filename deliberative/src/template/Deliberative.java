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

	enum Algorithm { BFS, ASTAR }
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
		State startState = new State(new HashSet<Task>(), tasks.clone(), vehicle.getCurrentCity(),0, states);
		startState.LEVEL = 1;
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
			plan = BFS(vehicle, tasks, startState);
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

	public Plan generatePlan(Vehicle vehicle, State endState, TaskSet tasks){
		State current = endState;
		List<State> stateList = new LinkedList<State>();
		List<Task> finalTasks = new LinkedList<Task>();

		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);

		while(current != null) {
			stateList.add(0,current);
			current = current.ancestorState;
		}
		for(int i = 0; i < stateList.size() - 1; i++){
			State cur = stateList.get(i);
			System.out.println("\t" + cur.toString());
			Set<Task> droppedTasks = new HashSet<Task>(cur.currentTasks);
			droppedTasks.removeAll(stateList.get(i+1).currentTasks);
			System.out.println("\n\n\t\tDropped: " + droppedTasks);
			Set<Task> pickedTasks = new HashSet<Task>(cur.remainingTasks);
			pickedTasks.removeAll(stateList.get(i+1).remainingTasks);
			System.out.println("\t\tPicked: " + pickedTasks);
			System.out.println("\t\tNow at: " + stateList.get(i+1).currentCity);

			City target = null;
			if(!pickedTasks.isEmpty())
				target = pickedTasks.iterator().next().pickupCity;
			else if(!droppedTasks.isEmpty())
				target = droppedTasks.iterator().next().deliveryCity;

			if(target!=null)
				for (City city : cur.currentCity.pathTo(target))
					plan.appendMove(city);

			for(Task picked: pickedTasks){
				plan.appendPickup(picked);
			}
			for(Task dropped: droppedTasks)
				plan.appendDelivery(dropped);
		}
		return plan;
	}
	
	private Plan AStar(Vehicle vehicle, TaskSet tasks) {
		return new Plan(vehicle.getCurrentCity());
	}
	private Plan BFS(Vehicle vehicle, TaskSet tasks, State startState) {
		Queue<State> bfs_queue = new LinkedList<State>();
		bfs_queue.add(startState);
		State solution = null;
		int count = 0;
		while (!bfs_queue.isEmpty()){
			State head = bfs_queue.poll();
			//System.out.println("vis:" + head.visited);
			head.visited = true;
			//System.out.println("Now at " + head + " HASH: " + head.hashCode() + ". head: " + head.ancestorState);
			for(State child: head.children){
				if(!child.visited && !child.inQueue){
					child.ancestorState = head;
					if(child.isFinalState()) {
						solution = child;
						break;
					}
					bfs_queue.add(child);
					child.inQueue = true;
				}
			}
			if(solution != null)
				break;
		}
		bfs_queue.clear();
		return generatePlan(vehicle, solution, tasks);
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
