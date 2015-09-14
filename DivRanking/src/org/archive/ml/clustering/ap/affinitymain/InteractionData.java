package org.archive.ml.clustering.ap.affinitymain;

/**
 *
 * @author misiek (mw219725@gmail.com)
 */
//wrapper of edge between a pair of data points
public class InteractionData {

    private String from;
    private String to;
    private Double sim;

    public InteractionData(String from, String to, Double sim) {
        this.from = from;
        this.to = to;
        this.sim = sim;
    }

    public String getFrom() {
        return from;
    }

    public Double getSim() {
        return sim;
    }

    public String getTo() {
        return to;
    }
    
    public String toString(){
    	StringBuffer buffer = new StringBuffer();  
    	buffer.append(from);
    	buffer.append("\t");
    	buffer.append(to);
    	buffer.append("\t");
    	buffer.append(sim.toString()); 
    	return buffer.toString();
    }
}
