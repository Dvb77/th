
public class Arc {
	
	// Instance Variables
    int from;
    int to;
    int capacity;
    int variableCost;
    int mode;// we say 0 = truck, 1 = train and barge = 2
    int duration;
    int fixedCost;
    
    public Arc(int from, int to, int capacity, int variableCost, int mode, int duration, int fixedCost) {
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.variableCost = variableCost;
        this.mode = mode;
        this.duration = duration;
        this.fixedCost = fixedCost;
    }
}
