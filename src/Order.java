
public class Order
{
    // Instance Variables
    int orderNumber;
    int orderVolume;
    int origin;
    int destination;
    int releaseTime;
    int LPA;
    int preferredMode;
 
   
    public Order(int orderNumber, int orderVolume, int origin, int destination, int releaseTime, int LPA, int preferredMode) {
        this.orderNumber = orderNumber;
        this.orderVolume = orderVolume;
        this.origin = origin;
        this.destination = destination;
        this.releaseTime = releaseTime;
        this.LPA = LPA;
        this.preferredMode = preferredMode;

    }
}
   