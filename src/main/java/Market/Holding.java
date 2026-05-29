package Market;

public class Holding{
    
    private int id;
    private Stock stock;
    private int quantity; 
    private double cost; 
    private String purchaseTimestamp;
    
    public Holding(int ID, Stock stock, int quantity, double cost, String purchaseTimestamp) {
        if (stock == null) {
            throw new IllegalArgumentException("Stock cannot be null");
        } else if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        } else if (cost < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        this.id = ID;
        this.stock = stock;
        this.quantity = quantity;
        this.cost = cost;
        this.purchaseTimestamp = purchaseTimestamp;
    } 
 
    public String getpurchaseTimestamp() {
        return purchaseTimestamp;
    }

    public int getID() {
        return id;
    }

    public Stock getStock() {
        return stock;
    }

    public int getQuantity() {
        return quantity;
    }
 
    public void setQuantity(int qty) {

        double ratio = (double) qty / this.quantity;
        this.quantity = qty;
        // Update cost proportionally so your profit math stays correct
        this.cost = this.cost * ratio;
    }

    public int compareTo(Holding other) {
        return this.stock.getStockID().compareTo(other.stock.getStockID());
    }

    public double getCost() {
        return cost;
    }
 
    public String toString() {
        return "Holding{" +
                "stock=" + stock +
                ", quantity=" + quantity +
                '}';
    }
 
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Holding other = (Holding) obj;
        return id == other.id && stock.equals(other.stock) && quantity == other.quantity && cost == other.cost && purchaseTimestamp == other.purchaseTimestamp;
    }

}
