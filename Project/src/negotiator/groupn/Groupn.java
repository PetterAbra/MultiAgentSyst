package negotiator.groupn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import negotiator.AgentID;
import negotiator.Bid;
import negotiator.DeadlineType;
import negotiator.Domain;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Value;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Groupn extends AbstractNegotiationParty {
	int numberOfIssues;
	double reservationPoint;
	double targetPoint; // Will decrease until reaches reservationPoint
	List<Double> weights;
	List<Integer> orderWeights;
	Map<String,Opponent> opponents;
	int numberOfOpponents;
	double discountingFactor;	// Seems to be 1.0, so ignore this
	Bid latestBid;
	Bid maxBid;
	Bid ownBid;
	Set<Bid> allBids;
	Map<Integer,Value> values;
	private double latestUtility;
	String name;
	static int partiesAdded = 0;
	
	/**
	 * Please keep this constructor. This is called by genius.
	 *
	 * @param utilitySpace Your utility space.
	 * @param deadlines The deadlines set for this negotiation.
	 * @param timeline Value counting from 0 (start) to 1 (end).
	 * @param randomSeed If you use any randomization, use this seed for it.
	 */
	public Groupn(UtilitySpace utilitySpace,
				  Map<DeadlineType, Object> deadlines,
				  Timeline timeline,
				  long randomSeed) {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);
		numberOfIssues = utilitySpace.getNrOfEvaluators();
		reservationPoint = utilitySpace.getReservationValue();
		weights = new ArrayList<Double>();
		orderWeights = new ArrayList<Integer>();
		discountingFactor = utilitySpace.getDiscountFactor();
		try {
			maxBid = utilitySpace.getMaxUtilityBid();
			ownBid = new Bid(maxBid);
			targetPoint = utilitySpace.getUtility(maxBid);
		} catch (Exception e) {
			System.err.println("No maximum bid exists!");
			return;
		}
		values = maxBid.getValues();
		for(int i = 0; i < numberOfIssues; ++i){
			weights.add(utilitySpace.getWeight(i));
			orderWeights.add(i);
		}
		Collections.sort(orderWeights, new Comparator<Integer>() {
		    public int compare(Integer left, Integer right) {
		        return Double.compare(weights.get(left), weights.get(right));
		    }
		});
		Collections.reverse(orderWeights);
		partiesAdded++;
		name = "Party " + partiesAdded;
		System.out.println("----------" + name + "----------");
		System.out.println("weights and order:");
		System.out.println(weights.toString());
		System.out.println(orderWeights.toString());
		System.out.println("Max utility: " + targetPoint);
		System.out.println("Discount: " + utilitySpace.getDiscountFactor());
		System.out.println("Reservation point: " + reservationPoint);
		opponents = new HashMap<String,Opponent>();
		numberOfOpponents = 0;
		generateAllBids();
		
	}
	
	
	/**
	 * Generate all bids. Since getNumberOfPossibleBids() is unfinished,
	 * just random generate them randomly instead
	 */
	public void generateAllBids(){
		allBids = new HashSet<Bid>();
		int combinations = 1;
		Domain domain = utilitySpace.getDomain();
		allBids.add(maxBid);
		for(int i = 0; i < 100000; ++i){
			allBids.add(domain.getRandomBid());
		}
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The first party in
	 * the first round is a bit different, it can only propose an offer.
	 *
	 * @param validActions Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class> validActions) {

		// Concede some. Currently linear
		double range = (1.0 - reservationPoint);
		double timePassed = timeline.getCurrentTime()/timeline.getTotalTime();
		targetPoint = 1.0-Math.pow(timePassed,discountingFactor)*range;
		
		// if we are the first party, offer max
		if (!validActions.contains(Accept.class)) {
				return new Offer(maxBid);
		}
		else {
			// Case: Offer is good enough
			if(latestUtility >= targetPoint)
				return new Accept();
			else{
				Bid bid = generateBid();
				if(bid.equals(latestBid))
					return new Accept();
				else
					return new Offer(generateBid());
			}
		}
	}
	
	
	public Bid generateBid(){
		double score = Double.NEGATIVE_INFINITY;
		for(Bid bid : allBids){
			boolean doContinue = false;
			try {
				doContinue = utilitySpace.getUtility(bid) < targetPoint;
			} catch (Exception e) {
				System.err.println("exception in generateBid");
				e.printStackTrace();
			}
			if(doContinue)
				continue;
			double rating = rate(bid);
			if(rating > score){
				score = rating;
				ownBid = bid;
			}
		}
		
		return ownBid;
	}
	
	public double rate(Bid bid){
		double ownutility = 0;
		try {
			ownutility = utilitySpace.getUtility(bid);
		} catch (Exception e) {
			System.err.println("exception in rate(Bid)");
			e.printStackTrace();
		}
		double utilityAverage = 0;
		double[] opponentUtilities = new double[numberOfOpponents];
		int i = 0;
		for(Map.Entry<String, Opponent> entry : opponents.entrySet()){
			Opponent op = entry.getValue();
			opponentUtilities[i] = op.getPredictedUtility(bid);
			utilityAverage += opponentUtilities[i];
			i++;
		}
		utilityAverage /= (numberOfOpponents);
		double rating = ownutility;
		double utilityVariation = 0;
		for(i = 0; i < numberOfOpponents; ++i){
			double factor = opponentUtilities[i]-utilityAverage;
			utilityVariation += factor*factor;
		}
		utilityVariation = utilityVariation/numberOfOpponents;
		double utilityStandardDeviation = Math.sqrt(utilityVariation);
		double deviationImportance = 1.0;
		rating = utilityAverage - deviationImportance*utilityStandardDeviation/(numberOfOpponents);
		return rating;
	}
	
	
	/**
	 * Strategy: Change the lowest important value to one that the opponents want
	 * @return
	 */
	public Bid generateBidSimple(){
		int lowestDisagreedIssue = getLowestDisagreedIssue();
		if(lowestDisagreedIssue == 0)	// no disagreements found!
			return ownBid;
		Value lowestDisagreedValue = getLowestDisagreedValue();
		System.out.println("Lowest disagreed issue was " + lowestDisagreedIssue);
		System.out.println("Lowest disagreed value was " + lowestDisagreedValue);
		if(lowestDisagreedValue != null)
			ownBid.setValue(lowestDisagreedIssue, lowestDisagreedValue);
		
		return ownBid;
	}
	
	public int getLowestDisagreedIssue(){
		for(int i = 0; i < numberOfIssues; ++i){
			int issue = orderWeights.get(numberOfIssues-1-i)+1;
			for(Map.Entry<String, Opponent> entry : opponents.entrySet()){
				Opponent op = entry.getValue();
				Value opponentValue = op.getLatestValue(issue);
				//System.out.println("dey were " + v + ", " +values + ", issue: " + issue);
				//System.out.println("and " + values.get(issue));
				Value ownValue = null;
				try {
					ownValue = ownBid.getValue(issue);
				} catch (Exception e) {
					System.out.println("Illegal value for issue " + issue + " in getLowestDisagreedValue");
					e.printStackTrace();
				}
				if(!opponentValue.equals(ownValue))
					return issue;
			}
		}
		System.out.println("Couldn't find a disagreed value");
		return 0;
	}
	
	public Value getLowestDisagreedValue(){
		for(int i = 0; i < numberOfIssues; ++i){
			int issue = orderWeights.get(numberOfIssues-1-i)+1;
			for(Map.Entry<String, Opponent> entry : opponents.entrySet()){
				Opponent op = entry.getValue();
				Value opponentValue = op.getLatestValue(issue);
				Value ownValue = null;
				try {
					ownValue = ownBid.getValue(issue);
				} catch (Exception e) {
					System.out.println("Illegal value for issue " + issue + " in getLowestDisagreedValue");
					e.printStackTrace();
				}
				if(!opponentValue.equals(ownValue))
					return opponentValue;
			}
		}
		System.out.println("Couldn't find a disagreed value");
		return null;
	}
	
	

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict their utility.
	 *
	 * @param sender The party that did the action.
	 * @param action The action that party did.
	 */
	@Override
	public void receiveMessage(Object sender, Action action) {
		super.receiveMessage(sender, action);
		String agentID = sender.toString();
		if(agentID.equals("Protocol"))
			return;
		Bid bid = Action.getBidFromAction(action);
		Opponent opponent;
		if(opponents.get(agentID) == null){
			opponent =  new Opponent(numberOfIssues,reservationPoint,agentID);
			opponents.put(agentID,opponent);
			numberOfOpponents++;
			
		}
		else{
			opponent = opponents.get(agentID);
		}
		if(bid != null){
			opponent.addBid(bid);
			latestBid = bid;
			try {
				latestUtility = utilitySpace.getUtility(latestBid);
			} catch (Exception e) {
				System.err.println("Couldn't calculate utility of bid: " + latestBid.toString());
				e.printStackTrace();
			}
		}
		
	}

}
