package template;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

public class State {
	private Set<Task> currentTasks;
	private Set<Task> remainingTasks;
	private City currentCity;
	private Set<State> children;
	public int LEVEL;
	private Set<State> allStates;
	public State(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity, int level, Set<State> allStates) {
		this.currentTasks = currentTasks;
		this.remainingTasks = remainingTasks;
		this.currentCity = currentCity;
		this.children = new HashSet<State>();
		this.LEVEL = level;
		this.allStates = allStates;
	}
	
	public void addChild(State child) {
		this.children.add(child);
	}

	public State alreadyAdded(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity){
		for(State s: this.allStates){
			if(s.currentCity.equals(currentCity) && s.currentTasks.equals(currentTasks) && s.remainingTasks.equals(remainingTasks))
				return s;
		}
		return null;
	}


	public LinkedList<State> buildChildren(State goalState) {
		//System.out.println("Remaining: " + this.remainingTasks.size() + "   Current: " + this.currentTasks.size());
		
		/*if (this.remainingTasks.isEmpty() && this.currentTasks.size() == 1) {
			//System.out.println("hey");
			this.children.add(goalState);
			return;
		}*/

		LinkedList<State> newChildren = new LinkedList<State>();
		for(Task task : currentTasks) {
			Set<Task> tasks = new HashSet<Task>(this.currentTasks);
			tasks.remove(task);

			State s = alreadyAdded(tasks, this.remainingTasks, task.deliveryCity);
			if(s == null){
				State candidate = new State(tasks, this.remainingTasks, task.deliveryCity, this.LEVEL+1, allStates);
				this.children.add(candidate);
				newChildren.add(candidate);
				allStates.add(candidate);
			}
			else{
				//System.out.println("Avoided!");
				this.children.add(s);
			}
		}
		
		for(Task task : remainingTasks) {
			Set<Task> newCurrenttasks = new HashSet<Task>(this.currentTasks);
			newCurrenttasks.add(task);
			
			Set<Task> newRemainingTasks = new HashSet<Task>(this.remainingTasks);
			newRemainingTasks.remove(task);


			State s = alreadyAdded(newCurrenttasks, newRemainingTasks, task.pickupCity);
			if(s == null){
				State candidate = new State(newCurrenttasks, newRemainingTasks, task.pickupCity, this.LEVEL+1, allStates);
				this.children.add(candidate);
				newChildren.add(candidate);
				allStates.add(candidate);
			}
			else{
				//System.out.println("Avoided!");
				this.children.add(s);
			}
		}
		
		/*for (State child : this.children) {
			child.buildChildren(goalState);
		}*/
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
}
