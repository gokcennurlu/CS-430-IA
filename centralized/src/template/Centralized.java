package template;

//the list of imports
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import template.TaskAction.PickupDelivery;
import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class Centralized implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    
    private HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
        Vehicle first = vehicles.get(0);
        
        vehicleActions = new HashMap<Vehicle, LinkedList<TaskAction>>();

        for (Vehicle v : vehicles) {
        	vehicleActions.put(v, new LinkedList<TaskAction>());
        }
        
        for (Task task : tasks) {
        	vehicleActions.get(first).add(new TaskAction(task, PickupDelivery.PICKUP));
        	vehicleActions.get(first).add(new TaskAction(task, PickupDelivery.DELIVERY));
        }
        
        System.out.println(this.totalCost(vehicleActions));
        
        LinkedList<HashMap<Vehicle, LinkedList<TaskAction>>> neighbors = this.getNeighbors(vehicleActions, vehicles);
        
        for (HashMap<Vehicle, LinkedList<TaskAction>> neighbor : neighbors) {
        	prettyPrint(neighbor);
        	System.out.println("Cost: " + totalCost(neighbor));
        	System.out.println("");
        }
        

//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);

        List<Plan> plans = new ArrayList<Plan>();
        plans.add(planVehicle1);
        while (plans.size() < vehicles.size()) {
            plans.add(Plan.EMPTY);
        }
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in "+duration+" milliseconds.");
        
        return plans;
    }
    
    private <K, V> void prettyPrint(HashMap<K, V> actionMap) {
    	for (K k : actionMap.keySet()) {
    		System.out.println(k + ": " + actionMap.get(k));
    	}
    }

    private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
        City current = vehicle.getCurrentCity();
        Plan plan = new Plan(current);

        for (Task task : tasks) {
            // move: current city => pickup location
            for (City city : current.pathTo(task.pickupCity)) {
                plan.appendMove(city);
            }

            plan.appendPickup(task);

            // move: pickup location => delivery location
            for (City city : task.path()) {
                plan.appendMove(city);
            }

            plan.appendDelivery(task);

            // set current city
            current = task.deliveryCity;
        }
        return plan;
    }
    
    private double vehicleCost(Vehicle vehicle, LinkedList<TaskAction> actions) {
    	
    	if (actions.isEmpty()) {return 0;}
    	
    	double sum = vehicle.getCurrentCity().distanceTo(actions.get(0).getCity());
    	
    	for (int i = 0; i < actions.size() - 1; i++) {
    		sum += actions.get(i).getCity().distanceTo(actions.get(i+1).getCity());
    	}
    	return sum;
    }
    
    private double totalCost(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions) {
    	double sum = 0;
    	for (Vehicle v : vehicleActions.keySet()) {
    		sum += vehicleCost(v, vehicleActions.get(v));
    	}
    	return sum;
    }
    
    private LinkedList<HashMap<Vehicle, LinkedList<TaskAction>>> getNeighbors(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions, List<Vehicle> vehicles) {
//		LinkedList<LinkedList<TaskAction>> neighbors = new LinkedList<LinkedList<TaskAction>>();
    	LinkedList<HashMap<Vehicle, LinkedList<TaskAction>>> candidates = new LinkedList<HashMap<Vehicle,LinkedList<TaskAction>>>();
    	int count = 0;
		while (candidates.size() < 10) {
			if (count++ > 1000) break;
			
			Vehicle randomVehicle = vehicles.get(new Random().nextInt(vehicles.size()));
			LinkedList<TaskAction> actions = (LinkedList<TaskAction>) vehicleActions.get(randomVehicle).clone();
			if (actions.size() == 0) { continue; }
			
			int i1 = new Random().nextInt(vehicleActions.get(randomVehicle).size());
			int i2 = new Random().nextInt(vehicleActions.get(randomVehicle).size());
			
			TaskAction ta1 = actions.get(i1);
			TaskAction ta2 = actions.get(i2);
			
			if (ta1.getTask().equals(ta2.getTask())) { continue; }
			
			Collections.swap(actions, i1, i2);
			if (Centralized.isValid(actions, randomVehicle)) {
				HashMap<Vehicle,LinkedList<TaskAction>> candidate = new HashMap<Vehicle, LinkedList<TaskAction>>();
				for (Vehicle v : vehicles) {
					if (v == randomVehicle) {
						candidate.put(v, actions);
					} else {
						candidate.put(v, vehicleActions.get(v));
					}
				}
				candidates.add(candidate);
			}
			
		}
    	return candidates;
    }
    
    private static boolean isValid(LinkedList<TaskAction> actions, Vehicle vehicle) {
    	return isValidWeight(actions, vehicle) && isValidOrder(actions);
    }
    
    private static boolean isValidWeight(LinkedList<TaskAction> actions, Vehicle vehicle) {
    	double sum = 0;
    	for (TaskAction ta : actions) {
    		if (ta.getType() == PickupDelivery.PICKUP) {
    			sum += ta.getTask().weight;
    			if (sum > vehicle.capacity()) {
    				return false;
    			}
    		} else {
    			sum -= ta.getTask().weight;
    		}
    	}
    	return true;
    }
    
    private static boolean isValidOrder(LinkedList<TaskAction> actions) {
    	HashSet<Task> pickedUpTasks = new HashSet<Task>();
    	for (TaskAction ta : actions) {
    		if (ta.getType() == PickupDelivery.PICKUP) {
    			pickedUpTasks.add(ta.getTask());
    		} else {
    			if (!pickedUpTasks.contains(ta.getTask())) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
}
