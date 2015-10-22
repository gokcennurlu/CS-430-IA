package template;

import java.util.HashSet;
import java.util.Set;

import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

public class State {
	private Set<Task> currentTasks;
	private Set<Task> remainingTasks;
	private City currentCity;
	private Set<State> children;
	
	public State(Set<Task> currentTasks, Set<Task> remainingTasks) {
		this.currentTasks = currentTasks;
		this.remainingTasks = remainingTasks;
//		this.currentCity = currentCity;
		this.children = new HashSet<State>();
	}
	
	public void addChild(State child) {
		this.children.add(child);
	}
	
	public Set<State> buildChildren(State goalState) {
		//System.out.println("Remaining: " + this.remainingTasks.size() + "   Current: " + this.currentTasks.size());
		
		/*if (this.remainingTasks.isEmpty() && this.currentTasks.size() == 1) {
			//System.out.println("hey");
			this.children.add(goalState);
			return;
		}*/
		
		for(Task task : currentTasks) {
			Set<Task> tasks = new HashSet<Task>(this.currentTasks);
			tasks.remove(task);
			
			this.children.add(new State(tasks, this.remainingTasks));
		}
		
		for(Task task : remainingTasks) {
			Set<Task> newCurrenttasks = new HashSet<Task>(this.currentTasks);
			newCurrenttasks.add(task);
			
			Set<Task> newRemainingTasks = new HashSet<Task>(this.remainingTasks);
			newRemainingTasks.remove(task);
			
			this.children.add(new State(newCurrenttasks, newRemainingTasks));
		}
		
		/*for (State child : this.children) {
			child.buildChildren(goalState);
		}*/
		return this.children;
	}
	
	public void prettyPrint() {
		System.out.println(this.currentTasks.toString() + " : " + this.remainingTasks.toString());
		for(State child : this.children) {
			System.out.print(" | " + child.toString());
		}
	}
	
}
