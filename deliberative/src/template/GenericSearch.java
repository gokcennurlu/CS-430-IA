package template;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public abstract class GenericSearch {
	protected PriorityQueue<State> queue;
	private Vehicle vehicle;
	private TaskSet tasks;
	private State startState;
	private HashMap<State,State> states;

	public GenericSearch(Vehicle vehicle, TaskSet tasks, State startState) {
		super();
		this.vehicle = vehicle;
		this.tasks = tasks;
		this.startState = startState;
		
		this.states = new HashMap<State, State>();
		states.put(startState, startState);
	}
	
	public Plan search () {
		this.addToQueue(startState);	
		State solution = null;
		
		while (!this.queue.isEmpty()){
			State head = this.getNextFromQueue();
			
			if(head.isFinalState()) {
				System.out.println("Number of states searched: " + states.size());
				this.queue.clear();
				return generatePlan(vehicle, head);
			}
//			System.out.print(head.g);
//			System.out.print(" " + head.currentTasks.size());
//			System.out.println(" " + head.remainingTasks.size());
			//System.out.println("vis:" + head.visited);
			head.visited = true;
			//System.out.println("Now at " + head + " HASH: " + head.hashCode() + ". head: " + head.ancestorState);
			for(State child: head.getChildren(this.vehicle)){
				

				
				if(child.visited) continue;
				
				State alreadyAddedState = getAlreadyCreatedState(child);
				if (alreadyAddedState == null) {
					this.states.put(child, child);
					this.addToQueue(child);
				} else {
					// If the element is in the queue, but there is a shorter path there, update queue
					if (this.queue.comparator().compare(child, alreadyAddedState) < 0) {
						this.removeFromQueue(alreadyAddedState);
						this.addToQueue(child);
					}
					
				}
			}
		}
		this.queue.clear();
		return generatePlan(vehicle, solution);
	}
	
	protected abstract State getNextFromQueue();
	protected abstract void addToQueue(State element);
	protected abstract void removeFromQueue(State element);
	
	public State getAlreadyCreatedState(State state){
		if(states.containsKey(state))
			return states.get(state);
		return null;
	}
	
	private Plan generatePlan(Vehicle vehicle, State endState){
//		Generate plan from the solution to the start	
		State current = endState;
		List<State> stateList = new LinkedList<State>();

		City currentCity = vehicle.getCurrentCity();
		Plan plan = new Plan(currentCity);

//		Create a list of all states in plan
		while(current != null) {
			stateList.add(0,current);
			current = current.ancestor;
		}
		
		System.out.println(stateList);
		
		//for (State state : stateList) {
		//	System.out.println(state.g);
		//}
		
		for(int i = 0; i < stateList.size() - 1; i++){
//			System.out.println(stateList.get(i).g);
			State cur = stateList.get(i);
//			System.out.println("\t" + cur.toString());
			
//			Find change in current task in this state and the next state
			Set<Task> droppedTasks = new HashSet<Task>(cur.currentTasks);
			droppedTasks.removeAll(stateList.get(i+1).currentTasks);
//			System.out.println("\n\n\t\tDropped: " + droppedTasks);
			
//			Find change in remaining tasks in this state and next state 
			Set<Task> pickedTasks = new HashSet<Task>(cur.remainingTasks);
			pickedTasks.removeAll(stateList.get(i+1).remainingTasks);
//			System.out.println("\t\tPicked: " + pickedTasks);
			
//			System.out.println("\t\tNow at: " + stateList.get(i+1).currentCity);

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
	
}
