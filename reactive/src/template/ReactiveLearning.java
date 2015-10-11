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

	private Random random;
	private double pPickup;

    private ArrayList<State> statesList;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);


        //Also the R(s,a) table.
        this.statesList = new ArrayList<State>();
        for(City c : topology.cities()){
            statesList.add(new State(c,null, td, topology,statesList));
			for(City n : topology.cities()){
				if(!n.equals(c)){
					statesList.add(new State(c,n, td, topology,statesList));
				}
			}
        }

		for(State s : statesList){
			s.buildActions();
		}

		double biggets_change;
		int iteration_count = 0;
		do{
			biggets_change = 0;
			for (State s : statesList) {
				double maxV = -Double.MAX_VALUE;
				State.StateAction bestAction = null;
				for (State.StateAction sa : s.actionsList) {
					double tempV = discount * sa.getNextStateValue() + sa.getR();
					if (tempV > maxV) {
						bestAction = sa;
						maxV = tempV;
					}
				}
				if(maxV - s.getV() > biggets_change)
					biggets_change = maxV - s.getV();
				s.setV(maxV);
				s.setBestAction(bestAction);
			}
			/*for(State s : statesList) {
				System.out.print(s.getV() + "\t");
			}
			System.out.print("\n");*/
			//System.out.println("Biggest change for this iteration: " + biggets_change);
			iteration_count += 1;
		}while(biggets_change > 1.0E-15);
		System.out.println("Iterated " + iteration_count + " times.");
		/*for(State s : statesList) {
			System.out.println(s.toString() + " -> " + s.getBestAction());
		}*/








		/*this.random = new Random();
		this.pPickup = discount;*/
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		City currentCity = vehicle.getCurrentCity();
		if (availableTask == null) {
			for(State s : statesList){
				if(s.getCity() == currentCity && s.getTaskTo()==null){
					action = new Move(s.getBestAction().getNextCity());
					return action;
				}
			}
			System.out.println("State couldn't identified!");
			return new Move(currentCity.randomNeighbor(random));
		} else {
			for(State s : statesList){
				if(s.getCity() == currentCity && s.getTaskTo()==availableTask.deliveryCity){
					/*action = new Move(s.getBestAction().getNextCity());
					return action;*/
					State.StateAction bestAction = s.getBestAction();
					if(bestAction.isDeliver()){
						action = new Pickup(availableTask);
						return action;
					}else{
						action = new Move(s.getBestAction().getNextCity());
						return action;
					}
				}
			}
			System.out.println("State couldn't identified!");
			action = new Pickup(availableTask);
		}
		return action;
	}
}
