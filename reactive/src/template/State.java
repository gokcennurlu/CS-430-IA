package template;

import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gökçen on 8.10.2015.
 */
public class State {

    private class StateAction {
        private City target;
        private long reward;

        public StateAction(City target) {
            this.target = target;
            if (target == null){ //if this action is an PICK action

                double total_weights = 0.0;
                double total_reward = 0.0;
                for (City c : topology.cities()) {
                    if (c != city) {
                        total_reward += (dist.reward(city, c) - city.distanceUnitsTo(c)) * dist.probability(city, c);
                        total_weights += dist.probability(city, c);
                    }
                }
                reward = (long) -(total_reward/total_weights);
            }
            else{
                reward = -city.distanceUnitsTo(target);
            }
        }

        public long getNetReward() {
            return reward;
        }

        @Override
        public String toString() {
            if(this.target != null)
                return "Move to: " + target.toString() + " : " + reward;
            else
                return "Pick the task average reward: " + reward;
        }
    }

    private boolean withTask;
    private TaskDistribution dist;
    private City city;
    private Topology topology;
    public ArrayList<StateAction> actionsList;

    public State(boolean withTask, City city, TaskDistribution dist, Topology topology) {
        this.withTask = withTask;
        this.city = city;
        this.dist = dist;
        this.topology = topology;
        this.actionsList = new ArrayList<StateAction>();

        //creating possible actions for this state object (actually this state is a (City,has_task) tuple.
        for(City neighbour : city.neighbors())
            actionsList.add(new StateAction(neighbour));
        if(withTask)
            actionsList.add(new StateAction(null)); //means create a dummy StateAction for PICK action
    }

    public void printRewards(){
        System.out.println(city.toString() + " - " + "Has Task?: " + withTask);
        for(StateAction sa : actionsList)
            System.out.println("\t" + sa.toString());
    }

//    public List<>


}
