package template;

import java.util.*;

import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology;
import logist.topology.Topology.City;

import javax.sound.midi.SysexMessage;

public class State {
	public Set<Task> currentTasks;
	public Set<Task> remainingTasks;
	public City currentCity;

	public boolean visited;
	public State ancestor;
	private Topology topology;
	
	public double g;
	public double h;

	public HashSet<State> children;
	public int LEVEL;

	public State(Set<Task> currentTasks, Set<Task> remainingTasks,City currentCity, State ancestor, Topology topology) {
		this.currentTasks = currentTasks;
		this.remainingTasks = remainingTasks;
		this.currentCity = currentCity;
		this.children = new HashSet<State>();
		this.ancestor = ancestor;
		this.topology = topology;
		
		if (ancestor == null) {
			this.LEVEL = 0;
			this.g = 0;
		} else {
			this.LEVEL = ancestor.LEVEL + 1;
			this.g = ancestor.g + ancestor.currentCity.distanceTo(this.currentCity);
		}

		this.h = h();
		
	}

	public boolean isFinalState(){
		return (this.currentTasks.isEmpty() && this.remainingTasks.isEmpty());
	}

	public HashSet<State> getChildren(Vehicle vehicle) {
		HashSet<State> newChildren = new HashSet<State>();
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

			this.children.add(new State(tasks, this.remainingTasks, task.deliveryCity, this, topology));

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
				for (Iterator<Task> i = newCurrenttasks.iterator(); i.hasNext();) {
					Task t = i.next();
					if (t.deliveryCity == task.pickupCity && t!=task) {
						i.remove();
					}
				}

				State candidate = new State(newCurrenttasks, newRemainingTasks, task.pickupCity, this, topology);
				this.children.add(candidate);
			}
			else{
				System.out.println("Due to Vehicle Capacity, impossible to pick new task.");
			}
		}
		return this.children;
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
	
	public double f() {
		return this.g + this.h;
	}
	
	private double h() {
		
		// Generate all the cities that need to be visited
		HashSet<City> needToBeVisited = new HashSet<City>();
		for (Task task : this.remainingTasks) {
			needToBeVisited.add(task.deliveryCity);
			needToBeVisited.add(task.pickupCity);
		}
		for (Task task : this.currentTasks) {
			needToBeVisited.add(task.deliveryCity);
		}
		
		
		HashSet<City> alreadyConnected = new HashSet<City>();
		alreadyConnected.add(this.currentCity);
		needToBeVisited.remove(this.currentCity);
		
		double minTotalDistance = 0;
		
		while (alreadyConnected.size() < new Integer(needToBeVisited.size())) {
		
			double minDistance = Double.MAX_VALUE;
			City nextToBeVisited = null;
			for (City from : alreadyConnected) {
				for (City to: needToBeVisited) {
					double dist = from.distanceTo(to);
					if (dist < minDistance) {
						minDistance = dist;
						nextToBeVisited = to;
					}
				}
			}
			minTotalDistance += minDistance;
			minDistance = Double.MAX_VALUE;
			alreadyConnected.add(nextToBeVisited);
			needToBeVisited.remove(nextToBeVisited);
		}
		return minTotalDistance;
	}
}
