package template;

import logist.task.TaskDistribution;
import logist.topology.Topology.City;

import java.util.ArrayList;

/**
 * Created by marthall on 12.10.15.
 */
public class StateAction {

    // All the vehicles have this set to five, so we will just use this. Se report for further argument.
    private static int costPerKm = 5;

    private City nextCity;
    private boolean isDeliver;
    private final TaskDistribution dist;
    private ArrayList<State> nextPossibleStates;
    private double R = 0;

    public StateAction(City fromCity, City nextCity, boolean isDelivery, TaskDistribution dist, ArrayList<State> allStates) {
        this.nextCity = nextCity;
        this.isDeliver = isDelivery;
        this.dist = dist;
        this.nextPossibleStates = new ArrayList<>();

        this.setR(fromCity, isDelivery);
        this.generateNextStates(allStates);

    }

    private void setR(City fromCity, boolean isDelivery) {
        if(isDelivery)
            R = dist.reward(fromCity, nextCity);
        R -= fromCity.distanceTo(nextCity) * costPerKm;
    }

    private void generateNextStates(ArrayList<State> allStates) {
        for(State state : allStates) {
            if (nextCity == state.getCity()) {
                this.nextPossibleStates.add(state);
            }
        }
    }

    public double getR() {
        return R;
    }

    public boolean isDeliver() {
        return isDeliver;
    }
    public City getNextCity() {
        return nextCity;
    }

    public double getNextStateValue(){
        double sum = 0;
        for(State s : nextPossibleStates) {
            sum += dist.probability(s.getCity(), s.getTaskTo()) * s.getV();
        }
        return sum;
    }

    @Override
    public String toString() {
        String str = "";
            /*for(State s : nextPossibleStates)
                str += "\t" + s.city.toString() +  " with task to " + s.taskTo +  (isDeliver ? " Delivery " : " Move " )+  "\n";
            */
        str += (isDeliver ? " Deliver it! " : (" Move to " + nextCity.toString()));
        return str;
    }
}
