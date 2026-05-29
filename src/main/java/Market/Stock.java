package Market;
public class Stock{  

    private String stockID; // Stock symbol (e.g., "AAPL", "GOOGL")  
    private double currentPrice;
  

    public Stock(String stockID, double currentPrice) {
        if (stockID == null || stockID.isEmpty()) {
            throw new IllegalArgumentException("Stock ID cannot be null or empty");
        }
        this.stockID = stockID.toUpperCase();  
        this.currentPrice = currentPrice;
    } 
 
    public String getStockID() {
        return stockID;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }   
 
    public void setCurrentPrice(double price){
        this.currentPrice = price;
    }  
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check for reference equality
        if (obj == null || getClass() != obj.getClass()) return false; // Check for null and class type (LLNode)
        Stock other = (Stock) obj; // Safe to cast now
        // Compare based on stockID (assuming stockID uniquely identifies a Stock)
        return this.stockID.equals(other.stockID);
    }


    @Override
    public int hashCode(){
        return stockID.hashCode();
    }   
}
