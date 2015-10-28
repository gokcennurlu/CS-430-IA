package template;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public abstract class GenericSearch {
	protected Queue<State> queue;
	private Vehicle vehicle;
	private TaskSet tasks;
	private State startState;

	public GenericSearch(Vehicle vehicle, TaskSet tasks, State startState) {
		super();
		this.vehicle = vehicle;
		this.tasks = tasks;
		this.startState = startState;
	}

	public Plan search () {
		this.queue.add(startState);	
		State solution = null;
		int count = 0;
		while (!this.queue.isEmpty()){
			State head = this.queue.poll();
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
					this.queue.add(child);
					child.inQueue = true;
				}
			}
			if(solution != null)
				break;
		}
		this.queue.clear();
		return generatePlan(vehicle, solution, tasks);
	}
	
	private Plan generatePlan(Vehicle vehicle, State endState, TaskSet tasks){
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
	
}
