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

public class ReactiveTemplate implements ReactiveBehavior {

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


		for(int i = 0 ; i < 100 ; i++) {
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
				s.setV(maxV);
				s.setBestAction(bestAction);
			}
			/*for(State s : statesList) {
				System.out.print(s.getV() + "\t");
			}
			System.out.print("\n");*/
		}

		for(State s : statesList) {
			System.out.println(s.toString() + " -> " + s.getBestAction());
		}








		this.random = new Random();
		this.pPickup = discount;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		return action;
	}
}
