package template;

//the list of imports
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.TaskAction.PickupDelivery;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class Auction implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private ArrayList<Task> tasks;
	
	private HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions;
	
	
	private final double P = 0.5;
    private final long NUMBER_OF_ITERATIONS = 10000;
    private final long NUMBER_OF_NEIGHBOURS_GENERATED = 100;
    
	private double probDist;
	
	private static Double[] relativeCost = {72.53229085,  62.76702683,  56.27905611,  51.18460038,  46.83434602,
	         						 		43.33172275,  40.16169398,  37.47419601,  35.10735268,  32.78082349,
	         						 		30.99231227,  29.62498571,  28.32739963,  27.06917061,  25.99423901,
	         						 		25.0648311,   24.29343858,  23.49816073,  22.96113556,  22.62625053,
	         						 		22.16498332,  21.82078866,  21.54757402,  21.27267976,  20.96656143,
	         						 		20.72181377,  20.6519584,   20.33708508,  20.23479885,  20.04374474};

	private static Double[] stdCost = {37.7160012649, 21.903644033, 17.394863267, 13.9894612435, 12.0290776211,
	                   				   10.5180192608, 8.95592129026, 7.86955648946, 6.95324604716, 6.4505197325,
	                   				   6.03729345193, 5.49782047656, 5.17289482591, 5.05492585293, 4.74854459112,
	                   				   4.54875299158, 4.29993084859, 4.31248733566, 4.1849076903, 4.20259017841,
	                   				   3.91511100545, 3.8535743298, 3.72516359229, 3.75616045505, 3.76483734618,
	                   				   3.62209282896, 3.62855431103, 3.60829406413, 3.58761525734, 3.53657227031};
	
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		
		this.tasks = new ArrayList<Task>();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		
		this.setProbDist();
		
	}
	
	private void setProbDist() {
		double expectedDistance = 0;
		
		for (City c1: topology.cities()) {
			for (City c2: topology.cities())
				expectedDistance += c1.distanceTo(c2) * distribution.probability(c1, c2);
		}
		
		expectedDistance /= Math.pow(topology.cities().size(), 2);
		
		this.probDist = expectedDistance;
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		for (Long b : bids) {
			System.out.print(b + ", ");
		}
		System.out.println();

		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
	}
	
	
	@Override
	public Long askPrice(Task task) {
		
		int nTasks = this.tasks.size();
		double bid;
		
		if (nTasks <= 10) {
			bid = (Auction.relativeCost[10] + Auction.stdCost[10]) * this.probDist;
		} else {
			bid = (Auction.relativeCost[nTasks] + Auction.stdCost[nTasks]) * this.probDist;
		}
		
		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		initializePlan(vehicles, tasks);

        HashMap<Vehicle, LinkedList<TaskAction>> currentBest = null;
        double currentBestScore = Double.MAX_VALUE;

        double p = P;

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            p -= P/NUMBER_OF_ITERATIONS;

            PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> neighbours = this.getNeighbors(vehicleActions, vehicles);
            System.out.println("\n" + i + ". iteration");

            double oldCost = totalCost(vehicleActions);
            
            if (new Random().nextDouble() > p) {
                vehicleActions = neighbours.poll();
                
            } else {
                ArrayList<HashMap<Vehicle, LinkedList<TaskAction>>> neighbourList = new ArrayList<HashMap<Vehicle, LinkedList<TaskAction>>>();
                
                for (HashMap<Vehicle, LinkedList<TaskAction>> neighbour : neighbours) {
                    neighbourList.add(neighbour);
                }
                
                vehicleActions = neighbourList.get(new Random().nextInt(neighbourList.size() / 5)); 
            }

            if (totalCost(vehicleActions) < currentBestScore) {
                currentBest = vehicleActions;
                currentBestScore = totalCost(vehicleActions);
            }

            System.out.println("Cost: " + totalCost(vehicleActions));
            prettyPrint(vehicleActions);
        }

        System.out.println("\nBest:");
        System.out.println(currentBestScore);
        prettyPrint(currentBest);

        List<Plan> plans = new ArrayList<Plan>();
        
        vehicleActions = currentBest;

        for (Vehicle v : vehicles) {
            plans.add(getVehiclePlan(v));
        }

        return plans;
	}
	
	private void initializePlan(List<Vehicle> vehicles, TaskSet tasks) {

        Vehicle first = vehicles.get(0);

        vehicleActions = new HashMap<Vehicle, LinkedList<TaskAction>>();

        for (Vehicle v : vehicles) {
            vehicleActions.put(v, new LinkedList<TaskAction>());
        }

        for (Task task : tasks) {
            addTask(first, task);
        }
    }
	
	private Plan getVehiclePlan(Vehicle vehicle) {
        Plan plan = new Plan(vehicle.getCurrentCity());
        City current = vehicle.getCurrentCity();

        for (TaskAction ta : vehicleActions.get(vehicle)) {
            if (ta.getType() == PickupDelivery.PICKUP) {

                for (City city : current.pathTo(ta.getTask().pickupCity)) {
                    plan.appendMove(city);	
                }
                plan.appendPickup(ta.getTask());
                current = ta.getTask().pickupCity;

            } else {
                for (City city : current.pathTo(ta.getTask().deliveryCity)) {
                    plan.appendMove(city);	
                }
                plan.appendDelivery(ta.getTask());
                current = ta.getTask().deliveryCity;
            }
        }

        return plan;
    }
	
	private <K, V> void prettyPrint(HashMap<K, V> actionMap) {
        for (K k : actionMap.keySet()) {
            System.out.println(((Vehicle) k).name() + ": " + actionMap.get(k));
        }
    }
	
	
	private PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> getNeighbors(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions, List<Vehicle> vehicles) {

        PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> candidates = new PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>>(new Comparator<HashMap<Vehicle, LinkedList<TaskAction>>>() {
            @Override
            public int compare(HashMap<Vehicle, LinkedList<TaskAction>> o1, HashMap<Vehicle, LinkedList<TaskAction>> o2) {
                return Double.compare(totalCost(o1), totalCost(o2));
            }
        });

        //    	  Alternative method for creating neighbors of only one type
        //        if (new Random().nextDouble() < 0.5) {
        //        	for (int i = 0; i < NUMBER_OF_NEIGHBOURS_GENERATED; i++) {
        //        		candidates.add(getInternalSwapCandidate(vehicleActions, vehicles));
        //        	}
        //        } else {
        //        	for (int i = 0; i < NUMBER_OF_NEIGHBOURS_GENERATED; i++) {
        //        		candidates.add(getExternalSwapCandidate(vehicleActions, vehicles));
        //        	}
        //        }

        while (candidates.size() < NUMBER_OF_NEIGHBOURS_GENERATED) {

            HashMap<Vehicle, LinkedList<TaskAction>> candidate;

            if (new Random().nextDouble() < 0.01) {
                candidate = getInternalSwapCandidate(vehicleActions, vehicles);
            } else {
                candidate = getExternalSwapCandidate(vehicleActions, vehicles);
            }
            candidates.add(candidate);
        }
        return candidates;
    }
	
	private HashMap<Vehicle, LinkedList<TaskAction>> getInternalSwapCandidate(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions, List<Vehicle> vehicles) {
        //We pick a vehicle with non-empty task list and swap its tasks

        LinkedList<TaskAction> newActionsForVehicle = null;
        HashMap<Vehicle, LinkedList<TaskAction>> candidate = (HashMap<Vehicle, LinkedList<TaskAction>>) vehicleActions.clone();
        Vehicle vehicle = null;

        int tries = 100;
        while(newActionsForVehicle == null && tries > 0) {
            vehicle = vehicles.get(new Random().nextInt(vehicles.size()));
            newActionsForVehicle = getShuffledActions(vehicle, candidate.get(vehicle));
            tries--;
        }

        if (newActionsForVehicle != null) {
        	candidate.put(vehicle, newActionsForVehicle);
        }

        return candidate;
    }
	
	private HashMap<Vehicle, LinkedList<TaskAction>> getExternalSwapCandidate(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions, List<Vehicle> vehicles) {
        //We pick a vehicle (first vehicle) with non-empty task list.
        //Then pick a random TaskAction and remove both TaskActions corresponding to this task
        //and put those into another vehicle(different than first one)

        boolean isValid = false;

        while (!isValid) {

            HashMap<Vehicle, LinkedList<TaskAction>> candidate = (HashMap<Vehicle, LinkedList<TaskAction>>) vehicleActions.clone();

            Vehicle firstVehicle = null;
            while (firstVehicle == null || vehicleActions.get(firstVehicle).isEmpty()) {
                firstVehicle = vehicles.get(new Random().nextInt(vehicles.size()));
            }

            Vehicle secondVehicle = null;
            while (secondVehicle == null || secondVehicle == firstVehicle) {
                secondVehicle = vehicles.get(new Random().nextInt(vehicles.size()));
            }

            LinkedList<TaskAction> firstVehicleActions = (LinkedList<TaskAction>) vehicleActions.get(firstVehicle).clone();
            LinkedList<TaskAction> secondVehicleActions = (LinkedList<TaskAction>) vehicleActions.get(secondVehicle).clone();

            //remove corresponding TaskActions from firstVehicle
            int i1 = new Random().nextInt(firstVehicleActions.size());
            Task taskToSeek = firstVehicleActions.get(i1).getTask();

            Iterator<TaskAction> it = firstVehicleActions.iterator();
            while (it.hasNext()) {
                if (it.next().getTask() == taskToSeek) {
                    it.remove();
                }
            }

            TaskAction pickupTask = new TaskAction(taskToSeek, PickupDelivery.PICKUP);
            TaskAction deliveryTask = new TaskAction(taskToSeek, PickupDelivery.DELIVERY);

            int firstIndex = new Random().nextInt(secondVehicleActions.size() + 1); // +1 to add to end
            secondVehicleActions.add(firstIndex, pickupTask);

            int secondIndex = firstIndex + 1 + new Random().nextInt(secondVehicleActions.size() - firstIndex);
            secondVehicleActions.add(secondIndex, deliveryTask);

            if (isValid(secondVehicleActions, secondVehicle)) {
                candidate.put(firstVehicle, firstVehicleActions);
                candidate.put(secondVehicle, secondVehicleActions);
                return candidate;
            }
        }
        return null;
    }
	
	private LinkedList<TaskAction> getShuffledActions(Vehicle vehicle, LinkedList<TaskAction> actions) {

        // avoiding infinite loop in cases like 'actions = [p1,d1]'
        if (actions.size() <= 2) {
            return null;
        }

        LinkedList<TaskAction> actions_copy = (LinkedList<TaskAction>) actions.clone();

        int tries = 0;
        while (true) {

            if (tries++ > 50) {
                return null;
            }

            int i1 = new Random().nextInt(actions_copy.size());
            int i2 = new Random().nextInt(actions_copy.size());

            TaskAction ta1 = actions_copy.get(i1);
            TaskAction ta2 = actions_copy.get(i2);

            if (ta1.getTask().equals(ta2.getTask()))
                continue;

            Collections.swap(actions_copy, i1, i2);
            if (Auction.isValid(actions_copy, vehicle)) {
                return actions_copy;
            }
            //Not a valid or improved swap action. Swap them back.
            Collections.swap(actions_copy, i1, i2);
        }
    }
    
    private void addTask(Vehicle vehicle, Task task) {
        vehicleActions.get(vehicle).add(new TaskAction(task, PickupDelivery.PICKUP));
        vehicleActions.get(vehicle).add(new TaskAction(task, PickupDelivery.DELIVERY));
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
    
    private double vehicleCost(Vehicle vehicle, LinkedList<TaskAction> actions) {

        if (actions.isEmpty()) {
            return 0;
        }

        double sum = vehicle.getCurrentCity().distanceTo(actions.get(0).getCity());

        for (int i = 0; i < actions.size() - 1; i++) {
            sum += actions.get(i).getCity().distanceTo(actions.get(i + 1).getCity());
        }
        return sum * vehicle.costPerKm();
    }

    private double totalCost(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions) {
        double sum = 0;
        for (Vehicle v : vehicleActions.keySet()) {
            sum += vehicleCost(v, vehicleActions.get(v));
        }
        return sum;
    }
	
}
