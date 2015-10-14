package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveLearning implements ReactiveBehavior {


    private ArrayList<State> statesList;
    private double discount;

    @Override
    public void setup(Topology topology, TaskDistribution td, Agent agent) {

        // Reads the discount factor from the agents.xml file.
        // If the property is not present it defaults to 0.95
        this.discount = agent.readProperty("discount-factor", Double.class, 0.95);
        //this.discount = 0.80;

        //Also the R(s,a) table.
        this.generateStates(topology);

        for(State state : statesList){
            state.buildActions(td, statesList);
        }

        this.valueIteration();

		/*for(State s : statesList) {
			System.out.println(s.toString() + " -> " + s.getBestAction());
		}*/

    }

    @Override
    public Action act(Vehicle vehicle, Task availableTask) {
        Action action;

        City currentCity = vehicle.getCurrentCity();
        if (availableTask == null) {
            for(State s : statesList){
                if(s.getCity() == currentCity && s.getTaskTo() == null){
                    return new Move(s.getBestAction().getNextCity());
                }
            }
            System.out.println("State couldn't identified!");
            return new Move(currentCity.randomNeighbor(new Random()));

        } else {
            for(State s : statesList){
                if(s.getCity() == currentCity && s.getTaskTo() == availableTask.deliveryCity){
					/*action = new Move(s.getBestAction().getNextCity());
					return action;*/
                    StateAction bestAction = s.getBestAction();
                    if(bestAction.isDeliver()){
                        return new Pickup(availableTask);
                    }else{
                        return new Move(s.getBestAction().getNextCity());
                    }
                }
            }
            System.out.println("State couldn't identified!");
            return new Pickup(availableTask);
        }
    }

    private void generateStates(Topology topology){
        this.statesList = new ArrayList<State>();
        for(City city : topology.cities()){

            statesList.add(new State(city, null));

            for(City deliverCity : topology.cities()){
                if(!deliverCity.equals(city)){
                    statesList.add(new State(city, deliverCity));
                }
            }
        }
    }

    private void valueIteration() {
        double biggestChange;
        int iterationCount = 0;

        do{
            biggestChange = this.valueIterationStep();
            iterationCount += 1;
        } while(biggestChange > 1.0E-15);

        System.out.println("Iterated " + iterationCount + " times.");
    }

    private double valueIterationStep() {
        double biggestChange = 0;
        for (State state : statesList) {
            double maxV = -Double.MAX_VALUE;
            StateAction bestAction = null;

            for (StateAction sa : state.actionsList) {
                double tempV = discount * sa.getNextStateValue() + sa.getR();
                if (tempV > maxV) {
                    bestAction = sa;
                    maxV = tempV;
                }
            }
            if(maxV - state.getV() > biggestChange) {
                biggestChange = maxV - state.getV();
            }
            state.setV(maxV);
            state.setBestAction(bestAction);
        }
        return biggestChange;
    }
}
