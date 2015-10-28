package template;

import java.util.*;

import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import javax.sound.midi.SysexMessage;

public class State implements Comparator<State>, Comparable<State> {
	public Set<Task> currentTasks;
	public Set<Task> remainingTasks;
	public City currentCity;

	public boolean visited;
	public boolean inQueue = false;
	public State ancestorState;
	private Topology topology;
	
	public double g;

	public Set<State> children;
	public int LEVEL;
	private HashMap<State,State> allStates;
	public State(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity, int level, double g, HashMap<State,State> allStates) {
		this.currentTasks = currentTasks;
		this.remainingTasks = remainingTasks;
		this.currentCity = currentCity;
		this.children = new HashSet<State>();
		this.LEVEL = level;
		this.g = g;
		this.allStates = allStates;
	}

	public State alreadyAdded(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity){
		State dummy = new State(currentTasks, remainingTasks,currentCity, -1, 0, null);
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

	public boolean isFinalState(){
		return (this.currentTasks.isEmpty() && this.remainingTasks.isEmpty());
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
				double g = this.currentCity.distanceTo(task.deliveryCity);
				State candidate = new State(tasks, this.remainingTasks, task.deliveryCity, this.LEVEL+1, g, allStates);
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
					double g = this.currentCity.distanceTo(task.deliveryCity);
					State candidate = new State(newCurrenttasks, newRemainingTasks, task.pickupCity, this.LEVEL + 1, g, allStates);
					this.children.add(candidate);
					newChildren.add(candidate);
					allStates.put(candidate,candidate);
				} else {
					//System.out.println("Avoided!");
					this.children.add(s);
				}
			}
			else{
				System.out.println("Due to Vehicle Capacity, impossible to pick new task.");
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
		//str+= "\t\tfrom: " + this.ancestorState;
		return str;
	}

	@Override
	public int hashCode() {
		return currentTasks.hashCode()*31 + remainingTasks.hashCode() + 17*currentCity.hashCode();
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

	@Override
	public int compare(State o1, State o2) {
		if (o1.g < o2.g) return -1;
		return 1;
	}

	@Override
	public int compareTo(State o) {
		if (this.g < o.g) return -1;
		return 1;
	}

}
