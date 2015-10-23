package template;

import java.util.*;

import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import javax.sound.midi.SysexMessage;

public class State {
	private Set<Task> currentTasks;
	private Set<Task> remainingTasks;
	private City currentCity;
	private Set<State> children;
	public int LEVEL;
	private HashMap<State,State> allStates;
	public State(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity, int level, HashMap<State,State> allStates) {
		this.currentTasks = currentTasks;
		this.remainingTasks = remainingTasks;
		this.currentCity = currentCity;
		this.children = new HashSet<State>();
		this.LEVEL = level;
		this.allStates = allStates;
	}

	public State alreadyAdded(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity){
		State dummy = new State(currentTasks, remainingTasks,currentCity, -1, null);
		if(allStates.containsKey(dummy))
			return allStates.get(dummy);
		return null;

		/*if(!this.allStates.contains(dummy))
			return null;
		for(State s: this.allStates){
			if(s.currentCity.equals(currentCity) && s.currentTasks.equals(currentTasks) && s.remainingTasks.equals(remainingTasks))
				return s;
		}
		return null;
		*/
	}


	public LinkedList<State> buildChildren(Vehicle vehicle) {
		LinkedList<State> newChildren = new LinkedList<State>();
		int totalWeightWeHave = 0;

		//We can drop current tasks if any.
		for(Task task : currentTasks) {
			Set<Task> tasks = new HashSet<Task>(this.currentTasks);
			tasks.remove(task);


			/* optimization?
			We are generating the next State where we drop the 'task'.
			To drop it, we go to task.deliveryCity. That's why we set currentCity of this new state as task.deliveryCity
			But, if there other packages that we can drop at that same city, why not drop them at the same time?
			*/
			int count = 0;
			for (Iterator<Task> i = tasks.iterator(); i.hasNext();) {
				Task t = i.next();
				if (t.deliveryCity == task.deliveryCity) {
					i.remove();
					count+=1;
				}
			}
			//System.out.println("Reduced " + count);

			State s = alreadyAdded(tasks, this.remainingTasks, task.deliveryCity);
			if(s == null){
				State candidate = new State(tasks, this.remainingTasks, task.deliveryCity, this.LEVEL+1, allStates);
				this.children.add(candidate);
				newChildren.add(candidate);
				allStates.put(candidate,candidate);
			}
			else{
				//System.out.println("Avoided!");
				this.children.add(s);
			}

			//we also sum up the total weight we have now.
			totalWeightWeHave += task.weight;
		}

		//We can pick if our capacity is enough
		for(Task task : remainingTasks) {
			if(vehicle.capacity() >= totalWeightWeHave + task.weight) {
				Set<Task> newCurrenttasks = new HashSet<Task>(this.currentTasks);
				newCurrenttasks.add(task);

				Set<Task> newRemainingTasks = new HashSet<Task>(this.remainingTasks);
				newRemainingTasks.remove(task);

				/* optimization?
				We are generating the next State where we pick the 'task'.
				To pick it, we go to task.pickupCity. That's why we set currentCity of this new state as task.pickupCity
				But, if there other packages (in currentTasks) that we can drop at that same city, why not drop them all
				while picking this new task?
				*/
				int count = 0;
				for (Iterator<Task> i = newCurrenttasks.iterator(); i.hasNext();) {
					Task t = i.next();
					if (t.deliveryCity == task.pickupCity && t!=task) {
						i.remove();
						count+=1;
					}
				}
				//System.out.println("Reduced " + count);

				State s = alreadyAdded(newCurrenttasks, newRemainingTasks, task.pickupCity);
				if (s == null) {
					State candidate = new State(newCurrenttasks, newRemainingTasks, task.pickupCity, this.LEVEL + 1, allStates);
					this.children.add(candidate);
					newChildren.add(candidate);
					allStates.put(candidate,candidate);
				} else {
					//System.out.println("Avoided!");
					this.children.add(s);
				}
			}
			else{
				System.out.println("Wohoo");
			}
		}
		return newChildren;
	}
	
	public void prettyPrint() {
		/*System.out.println(this.currentTasks.toString() + " : " + this.remainingTasks.toString());
		for(State child : this.children) {
			System.out.print(" | " + child.toString());
		}*/
	}

	@Override
	public String toString() {
		String str = "LVL: " + this.LEVEL + " ";
		str += "Current: ";
		for(Task s : this.currentTasks) {
			str += s.id + ",";
		}
		str += "Remaining: ";
		for(Task s : this.remainingTasks) {
			str += s.id + ",";
		}
		return str;
	}

	@Override
	public int hashCode() {
		return currentTasks.hashCode() + remainingTasks.hashCode() + currentCity.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final State other = (State) obj;
		return other.currentTasks.equals(this.currentTasks) && other.remainingTasks.equals(this.remainingTasks) && other.currentCity.equals(this.currentCity);
	}



}
