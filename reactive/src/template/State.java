package template;

import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by G�k�en on 8.10.2015.
 */
public class State {

    public void setBestAction(StateAction bestAction) {
        this.bestAction = bestAction;
    }

    public StateAction getBestAction() {
        return bestAction;
    }

    public City getTaskTo() {
        return taskTo;
    }

    private City taskTo;
    private City city;
    private double V;
    public ArrayList<StateAction> actionsList;
    private StateAction bestAction;

    public State(City city, City taskTo) {
        this.taskTo = taskTo;
        this.city = city;
        this.V = 1;
        this.bestAction = null;
    }

    public void buildActions(TaskDistribution dist, ArrayList<State> allStates){
        this.actionsList = new ArrayList<StateAction>();

        // Possible cities to move to
        for(City neighbour : this.city.neighbors()){
            this.actionsList.add(new StateAction(city, neighbour, false, dist, allStates));
        }

        // Deliver action
        if(taskTo != null) {
            this.actionsList.add(new StateAction(city, taskTo, true, dist, allStates));
        }
    }

    public City getCity() {
        return city;
    }

    public void setV(double v) {
        V = v;
    }

    public double getV() {
        return V;
    }

    @Override
    public String toString() {
        String str = this.city.toString() + " with task to: " + taskTo;
        /*for(StateAction sa : actionsList)
            str += sa.toString(); */
        return str;

    }
}
