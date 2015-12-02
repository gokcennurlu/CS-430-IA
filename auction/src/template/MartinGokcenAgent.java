package template;

//the list of imports
import java.util.ArrayList;
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
public class MartinGokcenAgent implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private List<Vehicle> vehicles;
	private List<Task> tasks = new ArrayList<Task>();
	
	private HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions;
	
	
	private final double P = 0.5;
    private final long NUMBER_OF_ITERATIONS = 10000;
    private final long NUMBER_OF_NEIGHBOURS_GENERATED = 100;
    
	private double probDist;
	private double expectedWeight;
	
	private double offset = 0;
	
	private static Double[] relativeCost = {292.39721251811449, 250.19311178891138, 223.3364512888997, 203.65685722424573, 186.06612443375516, 
		  									172.190616249333, 160.1163853010473, 149.94706327484013, 141.34619674511441, 133.41919424678204, 
		  									127.48861861500615, 122.27127163505274, 118.08310394272384, 113.85651764489593, 110.43018402418167, 
		  									107.49786873321126, 104.70926663374449, 102.39462967264383, 100.57591994061242, 99.409970220229852, 
		  									97.629300087767177, 96.433769468693256, 95.609902768125707, 94.763123555142002, 93.67094357339947, 
		  									92.874824999182252, 92.38782166591055, 91.480110339014857, 91.390512203990582, 90.628732620524531};

	private static Double[] stdCost = {174.12026995848635, 113.72423920217895, 91.587188792844898, 76.159143227576237, 66.212674988307029, 
		  							   58.182474020136446, 51.332419888630504, 45.834058622421502, 41.138798835320301, 37.127237433333605, 
		  							   33.87725412493117, 31.015873229753314, 28.665329041512688, 26.752348321936317, 24.72689519247443, 
		  							   23.198406028870096, 21.772094144934727, 20.838135320994066, 20.181441907269768, 19.97504325149589, 
		  							   18.892739236914828, 18.536248731521237, 18.183039798115519, 17.877122784756029, 17.614214060237032, 
		  							   17.215959803034139, 17.202344537508274, 17.01299031835222, 16.935462033732463, 16.927320383707762};
	
	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicles = agent.vehicles();
		
		this.setProbDist();
		
	}
	
	private void setProbDist() {
		double expectedDistance = 0;
		double expectedWeight = 0;
		
		for (City c1: topology.cities()) {
			for (City c2: topology.cities()) {
				expectedDistance += c1.distanceTo(c2) * distribution.probability(c1, c2);
				expectedWeight += this.distribution.weight(c1, c2);
			}
		}
		
		expectedDistance /= Math.pow(topology.cities().size(), 2);
		expectedWeight /= Math.pow(topology.cities().size(), 2);
		
		System.out.println(expectedWeight);
		
		this.probDist = expectedDistance;
		this.expectedWeight = expectedWeight;
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		for (Long b : bids) {
//			System.out.print(b + ", ");
		}
//		System.out.println();

		if (winner == agent.id()) {
			
			this.tasks.add(previous);
		}
	}
	
	
	@Override
	public Long askPrice(Task task) {
		
		int nTasks = this.tasks.size();
//		System.out.println(nTasks);
		double bid;
		
		if (nTasks <= 10) {
			bid = getBid(10, true);
		} else if (nTasks >= 30) {
			
			bid = getBid(29, true);
		} else {
			bid = getBid(nTasks, true);
		}
		
//		if (nTasks >= 10 && nTasks % 5 == 0) {
//			this.plan(this.vehicles, this.tasks);
//			double currentCost = this.totalCost(this.vehicleActions);
//			System.out.println(currentCost / nTasks + " : " + bid);
//		}
		
		return (long) Math.round(bid);
	}
	
	public double getBid(int taskNo, boolean std) {
		
		double maxCap = 0;
		for (Vehicle v : this.vehicles) {
			double cap = (v.capacity() * 3) / (v.costPerKm() * this.expectedWeight);
			if (cap > maxCap) {
				maxCap = cap;
			}
		}
		

        double bid;

		if (std) {
            bid = ((stdCost[taskNo] + relativeCost[taskNo]) / Math.pow(maxCap, 0.7)) * Math.pow(probDist, 0.9);
		} else {
            bid = (relativeCost[taskNo] / Math.pow(maxCap, 0.7)) * Math.pow(probDist, 0.9);
        }
		
		System.out.println("relativeCost[" + taskNo + "] " + relativeCost[taskNo]);
		System.out.println("maxCap " + maxCap);
		System.out.println("probDist " + Math.pow(probDist, 0.9));
		System.out.println("bid " + bid);
		return bid;
	}

	
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		ArrayList<Task> taskList = new ArrayList<Task>(tasks);
		return this.plan(vehicles, taskList);
	}
	
		
	public List<Plan> plan(List<Vehicle> vehicles, List<Task> tasks) {
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		
		if (tasks.isEmpty()) {
			ArrayList<Plan> plans = new ArrayList<Plan>();
			for (Vehicle v: vehicles) {
				plans.add(new Plan(v.getCurrentCity()));
			}
			return plans;
		}

		initializePlan(vehicles, tasks);

        HashMap<Vehicle, LinkedList<TaskAction>> currentBest = null;
        double currentBestScore = Double.MAX_VALUE;

        double p = P;

        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            p -= P/NUMBER_OF_ITERATIONS;

            PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> neighbours = this.getNeighbors(vehicleActions, vehicles);
//            System.out.println("\n" + i + ". iteration");

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

//            System.out.println("Cost: " + totalCost(vehicleActions));
            prettyPrint(vehicleActions);
        }

//        System.out.println("\nBest:");
        System.out.println(currentBestScore);
        prettyPrint(currentBest);

        List<Plan> plans = new ArrayList<Plan>();
        
        vehicleActions = currentBest;

        for (Vehicle v : vehicles) {
            plans.add(getVehiclePlan(v));
        }

        return plans;
	}
	
	private void initializePlan(List<Vehicle> vehicles, List<Task> tasks) {

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
//            System.out.println(((Vehicle) k).name() + ": " + actionMap.get(k));
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
            if (MartinGokcenAgent.isValid(actions_copy, vehicle)) {
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
