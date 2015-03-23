package negotiator.groupn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import negotiator.utility.UtilitySpace;
import negotiator.issue.Value;
import negotiator.Bid;

public class Opponent {
	double[] weights;
	double reservation;
	double certainity;	// Maybe want one for each weight
	int issueCount;
	double nice;
	String name;
	List<Bid> bids;
	List<Double> utilitiesBids;
	Map<Integer,Value> values;	// The assumed values on issues. Will be the first bid values unless speculated otherwise.
	
	
	public Opponent(int issues,double reservationInitialAssumption,String name){
		issueCount = issues;
		weights = new double[issues];
		reservation = reservationInitialAssumption;
		bids = new ArrayList<Bid>();
		utilitiesBids = new ArrayList<Double>();
		this.name = name;
	}
	
	public void addBid(Bid bid){
		//System.out.println("Added bid to opponent " + name + ": " + bid + " (bids size: " + bids.size() + ")" + ", getValues: " + bid.getValues());
		if(bids.size() == 0){
			values = bid.getValues();
		}
		bids.add(bid);
	}
	
	/**
	 * Returns how much utility the agent gets when accepting this agent's assumed values
	 * @param utilitySpace
	 * @return
	 * @throws Exception
	 */
	public double getMutualValue(UtilitySpace utilitySpace) throws Exception{
		Bid b =  new Bid();
		for(int i = 0; i < values.size(); ++i)
			b.setValue(i, values.get(i));
		return utilitySpace.getUtility(b);
	}
	
	public Value getValue(int issue){
		//System.out.println("So opponent " + name + " values: " + values);
		return values.get(issue);
	}
	
	public Value getLatestValue(int issue){
		//System.out.println("So opponent " + name + " values: " + values);
		try {
			return bids.get(bids.size()-1).getValue(issue);
		} catch (Exception e) {
			System.err.println("tried to access illegal value in Opponent.getLatestValue");
			e.printStackTrace();
			return null;
		}
	}
	
	

}
