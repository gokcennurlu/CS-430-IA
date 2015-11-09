package template;

//the list of imports

import java.util.*;

import template.TaskAction.PickupDelivery;
import logist.LogistSettings;
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
 */
@SuppressWarnings("unused")
public class Centralized implements CentralizedBehavior {

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;

    private final double P = 0.5;
    private final long NUMBER_OF_ITERATIONS = 500;
    private final long NUMBER_OF_NEIGHBOURS_GENERATED = 80;
    private final long MAX_NUMBER_OF_ONE_VEHICLE_SWAP_TRIES = 100;
    private final long MAX_NUMBER_OF_SWAP_BETWEEN_VEHICLES_TRIES_OUTER = 300;
    private final long MAX_NUMBER_OF_SWAP_BETWEEN_VEHICLES_TRIES_INNER = 100;
    private final double TWO_VEHICLE_SWAP_COST_ALLOWANCE = 0;
    private final double ONE_VEHICLE_SWAP_COST_ALLOWANCE = 0;


    private HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions;

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
                      Agent agent) {

        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config/settings_default.xml");
        } catch (Exception exc) {
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


        /*
        Preparing initial plan
         */
        Vehicle first = vehicles.get(0);

        vehicleActions = new HashMap<Vehicle, LinkedList<TaskAction>>();

        for (Vehicle v : vehicles) {
            vehicleActions.put(v, new LinkedList<TaskAction>());
        }

        for (Task task : tasks) {
            vehicleActions.get(first).add(new TaskAction(task, PickupDelivery.PICKUP));
            vehicleActions.get(first).add(new TaskAction(task, PickupDelivery.DELIVERY));
        }

        /*Task[] taskList = (Task[]) tasks.toArray(new Task[tasks.size()]);
        for (int i = 0 ; i < tasks.size()/2 ; i++) {
            vehicleActions.get(first).add(new TaskAction(taskList[i], PickupDelivery.PICKUP));
            vehicleActions.get(first).add(new TaskAction(taskList[i], PickupDelivery.DELIVERY));
        }
        for (int i = tasks.size()/2 ; i < tasks.size() ; i++) {
            vehicleActions.get(last).add(new TaskAction(taskList[i], PickupDelivery.PICKUP));
            vehicleActions.get(last).add(new TaskAction(taskList[i], PickupDelivery.DELIVERY));
        }*/

        //System.out.println(this.totalCost(vehicleActions));
        HashMap<Vehicle, LinkedList<TaskAction>> oldVehicleActions = null;
        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> neighbours = this.getNeighbors(vehicleActions, vehicles);
            System.out.println(i + ". iteration:" + neighbours.size());
            if (new Random().nextDouble() < P && !neighbours.isEmpty()) {
                //oldVehicleActions = vehicleActions;
                vehicleActions = neighbours.poll();
                System.out.println("\t\tCost: " + totalCost(vehicleActions));
                prettyPrint(vehicleActions);
            }else if(neighbours.isEmpty()){
                break;
                //vehicleActions = oldVehicleActions; //backtracking?
            }
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
        System.out.println("The plan was generated in " + duration + " milliseconds.");

        return plans;
    }

    private <K, V> void prettyPrint(HashMap<K, V> actionMap) {
        for (K k : actionMap.keySet()) {
            System.out.println(((Vehicle) k).name() + ": " + actionMap.get(k));
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

        if (actions.isEmpty()) {
            return 0;
        }

        double sum = vehicle.getCurrentCity().distanceTo(actions.get(0).getCity());

        for (int i = 0; i < actions.size() - 1; i++) {
            sum += actions.get(i).getCity().distanceTo(actions.get(i + 1).getCity());
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

    /*
    This returns a swapped (and VALID, and BETTER) list of taskactions.
    returns Null if no possible improvement found after MAX_NUMBER_OF_ONE_VEHICLE_SWAP_TRIES tries.
     */
    private LinkedList<TaskAction> getShuffledActions(LinkedList<TaskAction> actions, Vehicle vehicle) {
        if (actions.size() <= 2) //avoding infinite loop in cases like 'actions = [p1,d1]'
            return null;
        LinkedList<TaskAction> actions_copy = (LinkedList<TaskAction>) actions.clone();
        int tries = 0;
        double currentListCost = vehicleCost(vehicle, actions);
        while (true) {
            // TODO: We might want to change number of tries according to size of 'actions' of the vehicle.
            // for now I made it like below:
            if (tries++ > actions.size()*2) {
                //System.out.println("getShuffledActions: Max tries exceeded. Size: " + actions.size());
                return null;
            }

            int i1 = new Random().nextInt(actions_copy.size());
            int i2 = new Random().nextInt(actions_copy.size());

            TaskAction ta1 = actions_copy.get(i1);
            TaskAction ta2 = actions_copy.get(i2);

            if (ta1.getTask().equals(ta2.getTask()))
                continue;

            Collections.swap(actions_copy, i1, i2);
            if (Centralized.isValid(actions_copy, vehicle) && vehicleCost(vehicle, actions_copy) < currentListCost + ONE_VEHICLE_SWAP_COST_ALLOWANCE) {
                return actions_copy;
            }
            //Not a valid or improved swap action. Swap them back.
            Collections.swap(actions_copy, i1, i2);
        }
    }

    private PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> getNeighbors(HashMap<Vehicle, LinkedList<TaskAction>> vehicleActions, List<Vehicle> vehicles) {
        PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> candidates = new PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>>(new Comparator<HashMap<Vehicle, LinkedList<TaskAction>>>() {
            @Override
            public int compare(HashMap<Vehicle, LinkedList<TaskAction>> o1, HashMap<Vehicle, LinkedList<TaskAction>> o2) {
                return Double.compare(totalCost(o1), totalCost(o2));
            }
        });
        int count = 0;
        double currentCost = totalCost(vehicleActions);

        while (candidates.size() < NUMBER_OF_NEIGHBOURS_GENERATED) {
            if (count++ > NUMBER_OF_NEIGHBOURS_GENERATED*10) {
                //System.out.println("getNeighbors: Max tries exceeded");
                break;
            }

            HashMap<Vehicle, LinkedList<TaskAction>> candidate = new HashMap<Vehicle, LinkedList<TaskAction>>();

            //We roll a dice here:
            //TODO we might want to remove this dice rolling and request fixed number neightbors of from each method
            //and we might want to prepare different methods like 2-opt,3-opt I think.
            if (new Random().nextDouble() < 0.2) {
                //We pick a vehicle with non-empty task list and swap its tasks
                int shuffleTries = 0;
                LinkedList<TaskAction> newActionsForVehicle = null;
                Vehicle randomVehicle = null;
                while(shuffleTries++ < 2) {
                    while (randomVehicle == null || vehicleActions.get(randomVehicle).isEmpty()) {
                        randomVehicle = vehicles.get(new Random().nextInt(vehicles.size()));
                    }
                    newActionsForVehicle = getShuffledActions(vehicleActions.get(randomVehicle), randomVehicle);
                    if (newActionsForVehicle != null)
                        break;
                }
                if(newActionsForVehicle == null)
                    continue;
                for (Vehicle v : vehicles) {
                    if (v == randomVehicle) {
                        candidate.put(v, newActionsForVehicle);
                    } else {
                        candidate.put(v, vehicleActions.get(v));
                    }
                }
                int profit = (int) (vehicleCost(randomVehicle, vehicleActions.get(randomVehicle)) - vehicleCost(randomVehicle, newActionsForVehicle));
                //System.out.println("\t\tSWAP on one vehicle\t\t\tProfit: " + profit);
                candidates.add(candidate); //we know this is valid and better then original.
            } else {
                //We pick a vehicle (first vehicle) with non-empty task list.
                //Then pick a random taskaction and remove it and its sister.
                //and put those into another vehicle(different than first one)

                // I TRIED inserting in front, but it USUALLY fails since adding something results with much worse cost
                // This might be OK for "carry one task at a time" but not in this case.
                int triesOuter = 0;
                boolean keepTrying = true;
                while (keepTrying && triesOuter++ < MAX_NUMBER_OF_SWAP_BETWEEN_VEHICLES_TRIES_OUTER) {
                    Vehicle firstVehicle = null;
                    while (firstVehicle == null || vehicleActions.get(firstVehicle).isEmpty()) {
                        firstVehicle = vehicles.get(new Random().nextInt(vehicles.size()));
                    }

                    Vehicle secondVehicle = null;
                    while (secondVehicle == null || secondVehicle == firstVehicle) {
                        secondVehicle = vehicles.get(new Random().nextInt(vehicles.size()));
                    }

                    LinkedList<TaskAction> firstVehicleActions = (LinkedList<TaskAction>) vehicleActions.get(firstVehicle).clone();

                    //remove corresponding TaskActions from firstVehicle
                    int i1 = new Random().nextInt(firstVehicleActions.size());
                    Task taskToSeek = firstVehicleActions.get(i1).getTask();
                    Iterator<TaskAction> it = firstVehicleActions.iterator();
                    while (it.hasNext()) {
                        TaskAction ta = it.next();
                        if (ta.getTask() == taskToSeek) {
                            it.remove();
                        }
                    }

                    double differenceFromFirstVehicle = vehicleCost(firstVehicle, vehicleActions.get(firstVehicle)) - vehicleCost(firstVehicle, firstVehicleActions);
                    double secondVehicleInitialCost = vehicleCost(secondVehicle, vehicleActions.get(secondVehicle));
                    double secondVehicleCostTreshold = secondVehicleInitialCost + differenceFromFirstVehicle + TWO_VEHICLE_SWAP_COST_ALLOWANCE;

                    TaskAction pickupTask = new TaskAction(taskToSeek, PickupDelivery.PICKUP);
                    TaskAction deliveryTask = new TaskAction(taskToSeek, PickupDelivery.DELIVERY);

                    //add them to secondVehicle. try to find insert positions so that cost of second vehicle is
                    //less than 'secondVehicleCostTreshold'
                    int tryCount = 0;
                    while (true) {
                        if (tryCount++ > MAX_NUMBER_OF_SWAP_BETWEEN_VEHICLES_TRIES_INNER) {
                            //System.out.println("Couldn't find good insert positions in second vehicle.");
                            break;
                        }
                        LinkedList<TaskAction> secondVehicleActions = (LinkedList<TaskAction>) vehicleActions.get(secondVehicle).clone();
                        int firstIndex = new Random().nextInt(secondVehicleActions.size() + 1); // +1 to add to end
                        secondVehicleActions.add(firstIndex, pickupTask);
                        int secondIndex;
                        if (firstIndex == secondVehicleActions.size() - 1)
                            secondIndex = firstIndex + 1;
                        else
                            secondIndex = firstIndex + 1 + new Random().nextInt(secondVehicleActions.size() - 1 - firstIndex);
                        secondVehicleActions.add(secondIndex, deliveryTask);

                        if (isValid(secondVehicleActions, secondVehicle) && vehicleCost(secondVehicle, secondVehicleActions) < secondVehicleCostTreshold) {
                            double profit = (secondVehicleInitialCost - vehicleCost(secondVehicle, secondVehicleActions) + differenceFromFirstVehicle);
                            //create new candidate
                            for (Vehicle v : vehicles) {
                                if (v != secondVehicle && v != firstVehicle)
                                    candidate.put(v, vehicleActions.get(v));
                            }
                            candidate.put(firstVehicle, firstVehicleActions);
                            candidate.put(secondVehicle, secondVehicleActions);
                            System.out.println("\t\tSWAP between two vehicles\tProfit: " + profit);
                            candidates.add(candidate);
                            keepTrying = false;
                            break;
                        }
                    }
                }

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
