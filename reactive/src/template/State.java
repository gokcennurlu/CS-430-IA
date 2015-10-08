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

    public void setBestAction(StateAction bestAction) {
        this.bestAction = bestAction;
    }

    public StateAction getBestAction() {
        return bestAction;
    }

    public class StateAction {
        private City nextCity;
        private boolean isDeliver;
        private ArrayList<State> nextPossibleStates;
        private double R = 0;

        public double getR() {
            return R;
        }
        //private long reward;

        public StateAction(City nextCity, City from, boolean isDelivery, ArrayList<State> states) {
            this.nextCity = nextCity;
            this.isDeliver = isDelivery;
            this.nextPossibleStates = new ArrayList<State>();
            for(State s : states){
                if(s.city == nextCity)
                    this.nextPossibleStates.add(s);
            }


            if(isDelivery)
                R = dist.reward(from,nextCity);
            R -= from.distanceTo(nextCity);
        }


        public double getNextStateValue(){
            double sum = 0;
            for(State s : nextPossibleStates) {
                sum += dist.probability(s.city, s.taskTo) * s.getV();
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

    private City taskTo;
    private TaskDistribution dist;

    public City getCity() {
        return city;
    }

    private City city;
    private Topology topology;
    private ArrayList<State> states;
    public ArrayList<StateAction> actionsList;
    private double V;
    private StateAction bestAction = null;

    public double getV() {
        return V;
    }

    public void setV(double v) {
        V = v;
    }

    public State(City city, City taskTo, TaskDistribution dist, Topology topology, ArrayList<State> states) {
        this.taskTo = taskTo;
        this.city= city;
        this.dist = dist;
        this.topology = topology;
        this.states = states;
        this.V = 1;
    }

    public void buildActions(){
        this.actionsList = new ArrayList<StateAction>();
        //first add neighbours and GO action
        for(City neighbour : this.city.neighbors()){
            this.actionsList.add(new StateAction(neighbour,city,false,states));
        }
        if(taskTo != null) {
            this.actionsList.add(new StateAction(taskTo, city, true, states));
        }
    }



    @Override
    public String toString() {
        String str = this.city.toString() + " with task to: " + taskTo;
        /*for(StateAction sa : actionsList)
            str += sa.toString(); */
        return str;

    }
}
