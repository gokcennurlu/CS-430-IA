package template;

import logist.task.Task;
import logist.topology.Topology.City;

public class TaskAction {
	public static enum PickupDelivery {
		PICKUP,
		DELIVERY
	}
	
	private Task task;
	private PickupDelivery type;
	
	public TaskAction(Task task, PickupDelivery type) {
		this.task = task;
		this.type = type;
	}
	
	public City getCity() {
		if (this.type == PickupDelivery.PICKUP) {
			return this.task.pickupCity;
		} else {
			return this.task.deliveryCity;
		}
	}

	public Task getTask() {
		return task;
	}

	public PickupDelivery getType() {
		return type;
	}
	
	@Override
	public String toString() {
		if (this.type == PickupDelivery.PICKUP) {
			return "P" + this.task.id;
		} else {
			return "D" + this.task.id;
		}
	}
	
}
