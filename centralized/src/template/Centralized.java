package template;

//the list of imports

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

import jdk.nashorn.internal.ir.debug.*;
import logist.LogistPlatform;
import org.json.JSONWriter;
import template.TaskAction.PickupDelivery;
import logist.LogistSettings;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Action.Move;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

import org.json.*;

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
    private final long NUMBER_OF_ITERATIONS = 3000;
    private final long NUMBER_OF_NEIGHBOURS_GENERATED = 150;

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
        LogistSettings GLOBAL_SETTINGS = LogistPlatform.getSettings();
        String used_config_file = GLOBAL_SETTINGS.get(LogistSettings.FileKey.CONFIGURATION).getName().replaceFirst("[.][^.]+$", "");;

        long time_start = System.currentTimeMillis();

        JSONObject total_output = new JSONObject();
        // collecting task distribution data
        JSONObject task_distribution = new JSONObject();
        JSONArray probability_matrix = new JSONArray();
        JSONArray reward_matrix = new JSONArray();
        JSONArray weight_matrix = new JSONArray();
        JSONArray distance_matrix = new JSONArray();

        JSONArray city_names = new JSONArray(0);

        for (City left_city : topology.cities()) {
            JSONArray dist_row = new JSONArray(0);
            JSONArray reward_row = new JSONArray(0);
            JSONArray weight_row = new JSONArray(0);
            JSONArray distance_row = new JSONArray(0);
            city_names.put(left_city.name);
            for (City right_city : topology.cities()) {
                dist_row.put(distribution.probability(left_city, right_city));
                reward_row.put(distribution.reward(left_city, right_city));
                weight_row.put(distribution.weight(left_city, right_city));
                distance_row.put(left_city.distanceTo(right_city));
            }
            probability_matrix.put(dist_row);
            reward_matrix.put(reward_row);
            weight_matrix.put(weight_row);
            distance_matrix.put(distance_row);
        }
        task_distribution.put("probability_matrix", probability_matrix);
        task_distribution.put("reward_matrix", reward_matrix);
        task_distribution.put("weight_matrix", weight_matrix);
        task_distribution.put("distance_matrix", distance_matrix);
        task_distribution.put("city_names", city_names);
        total_output.put("task_distribution", task_distribution);

        JSONArray vehicle_starting_positions = new JSONArray(0);
        JSONArray vehicle_capacities = new JSONArray(0);
        JSONArray vehicle_cost_per_km = new JSONArray(0);

        List<Vehicle> vehicle_copies = new LinkedList<>(vehicles);
        //Collections.shuffle(vehicle_copies);

        for(Vehicle v: vehicle_copies) {
            vehicle_starting_positions.put(v.getCurrentCity().name);
            vehicle_capacities.put(v.capacity());
            vehicle_cost_per_km.put(v.costPerKm());
        }

        total_output.put("vehicle_starting_positions", vehicle_starting_positions);
        total_output.put("vehicle_capacities", vehicle_capacities);
        total_output.put("vehicle_cost_per_km", vehicle_cost_per_km);

        JSONArray runs = new JSONArray();

        for(int number_of_vehicles = 3; number_of_vehicles <= 3; number_of_vehicles++) {
            JSONArray avg_task_cost_vs_number_of_task = new JSONArray(0);
            System.out.print("Number of vehicles: " + number_of_vehicles + "\n\tNumber of tasks: ");
            for (int number_of_tasks = 1; number_of_tasks <= tasks.size(); number_of_tasks++) {
                System.out.print(number_of_tasks + ",");
                /***********
                 ALGORITHM CONFIG..
                 ***********/

                List<Vehicle> custom_vehicles = vehicle_copies.subList(0, number_of_vehicles);
                HashMap<Vehicle, LinkedList<TaskAction>> currentBest = null;
                double currentBestScore = Double.MAX_VALUE;
                double p = P;

                /***********
                 ALGORITHM START
                 ***********/

                initializePlan(custom_vehicles, new ArrayList<Task>(tasks).subList(0, number_of_tasks));
                for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
                    p -= P / NUMBER_OF_ITERATIONS;

                    PriorityQueue<HashMap<Vehicle, LinkedList<TaskAction>>> neighbours = this.getNeighbors(vehicleActions, custom_vehicles);
                    //System.out.println("\t" + i + ". iteration");

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

                    //System.out.println("Cost: " + totalCost(vehicleActions));
                    //prettyPrint(vehicleActions);
                }

                //System.out.println("\nBest:");
                //System.out.println(currentBestScore);
                //prettyPrint(currentBest);

                avg_task_cost_vs_number_of_task.put(currentBestScore / number_of_tasks);

                /***********
                 ALGORITHM END
                 ***********/
            }
            System.out.println();
            JSONObject this_run = new JSONObject();
            this_run.put("avg_task_cost_vs_number_of_task", avg_task_cost_vs_number_of_task);
            this_run.put("number_of_vehicles", number_of_vehicles);
            runs.put(this_run);
        }

        total_output.put("runs", runs);

        try {
            PrintWriter writer = new PrintWriter("vehicle_results/" + used_config_file + "_results_" + UUID.randomUUID().toString() + ".json");
            writer.write(total_output.toString(4));
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        System.exit(0);


        List<Plan> plans = new ArrayList<Plan>();

        for (Vehicle v : vehicles) {
            plans.add(getVehiclePlan(v));
        }

        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");

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

    private void initializePlanEven(List<Vehicle> vehicles, TaskSet tasks) {

        vehicleActions = new HashMap<Vehicle, LinkedList<TaskAction>>();

        for (Vehicle v : vehicles) {
            vehicleActions.put(v, new LinkedList<TaskAction>());
        }

        int i = 0;
        for (Task task : tasks) {
            Vehicle v = vehicles.get(i % vehicles.size());
            addTask(v, task);
            i++;
        }
    }

    private void initializePlanRandom(List<Vehicle> vehicles, TaskSet tasks) {
        vehicleActions = new HashMap<Vehicle, LinkedList<TaskAction>>();

        for (Vehicle v : vehicles) {
            vehicleActions.put(v, new LinkedList<TaskAction>());
        }

        ArrayList<Task> taskList = new ArrayList<Task>(tasks);
        Collections.shuffle(taskList);

        for (Task task : taskList) {
            Vehicle vehicle = vehicles.get(new Random().nextInt(vehicles.size()));
            addTask(vehicle, task);
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

        boolean vehicles_okay = false;
        for (List<TaskAction> ta : vehicleActions.values()) {
            if (ta.size() > 2) {
                vehicles_okay = true;
                break;
            }
        }
        if (!vehicles_okay)
            return candidate;

        int tries = 0;
        while (newActionsForVehicle == null && tries <= 100) {
            vehicle = vehicles.get(new Random().nextInt(vehicles.size()));
            newActionsForVehicle = getShuffledActions(vehicle, candidate.get(vehicle));
            tries++;
        }
        if(newActionsForVehicle!=null)
            candidate.put(vehicle, newActionsForVehicle);

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
            else{
                //System.out.println("external not valid!!!");
            }
        }
        return null;
    }

    /*
    This returns a swapped list of TaskActions.
     */
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
            if (Centralized.isValid(actions_copy, vehicle)) {
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
