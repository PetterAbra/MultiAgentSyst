package negotiator.groupn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import negotiator.utility.UtilitySpace;
import negotiator.issue.Value;
import negotiator.Bid;

public class Opponent {
	double[] weights;
	int[] changes;
	int[] earliestChange;
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
		changes = new int[issues];
		earliestChange = new int[issues];
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
		// Check if bid changed since last time
		if(bids.size() > 1){
			for(int i = 0; i < issueCount; ++i){
				try {
					if(!bid.getValue(i+1).equals(bids.get(bids.size()-2+1))){
						if(earliestChange[i] == 0)
							earliestChange[i] = bids.size();
						changes[i]++;
					}
				} catch (Exception e) {
					System.err.println("Error in addBid for opponent: bid.getValue()");
					e.printStackTrace();
				}
			}
		}
		calculateWeights();
	}
	
	private void calculateWeights(){
		for(int i = 0; i < issueCount; ++i){
			weights[i] = Math.pow(0.75,changes[i])*earliestChange[i];
		}
		
		// Normalize
		double sum = 0;
		for(int i = 0; i < issueCount; ++i){
			sum += weights[i];
		}
		for(int i = 0; i < issueCount; ++i){
			weights[i] /= sum;
		}
	}
	
	/**
	 * Returns how much utility the agent gets when accepting this agent's assumed values
	 * @param utilitySpace
	 * @return
	 * @throws Exception
	 */
	public double getUtility(UtilitySpace utilitySpace) throws Exception{
		Bid b =  new Bid();
		for(int i = 0; i < values.size(); ++i)
			b.setValue(i, values.get(i));
		return utilitySpace.getUtility(b);
	}
	
	public double getPredictedUtility(Bid bid){
		double utility = 0;
		for(int i = 0; i < values.size(); ++i){
			try {
				if(bid.getValue(i+1).equals(values.get(i)))
					utility += weights[i];
			} catch (Exception e) {
				System.err.println("Exception in getPredictedUtility");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return utility;
		
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
