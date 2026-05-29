package Market;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class Transaction implements Comparable<Transaction> {

    public static final String BUY = "BUY";
    public static final String SELL = "SELL";
    
    private int transactionID;
    private long timestamp;
    private Stock stock;
    private int quantity;
    private double pricePerStock;
    private double totalPrice;
    private String type;

    public Transaction(int transactionID, long timestamp, Stock stock, 
            int quantity, double pricePerStock, double totalPrice, String type) {
        if ((!type.equals(BUY) && !type.equals(SELL)) || type == null) {
            throw new IllegalArgumentException("Transaction type must be either 'BUY' or 'SELL'");
        } else if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        } else if (pricePerStock < 0) {
            throw new IllegalArgumentException("Price per stock cannot be negative");
        } else if (totalPrice < 0) {
            throw new IllegalArgumentException("Total price cannot be negative");
        } else if (stock == null) {
            throw new IllegalArgumentException("Stock cannot be null");
        }
        this.transactionID = transactionID;
        this.timestamp = timestamp;
        this.stock = stock;
        this.quantity = quantity;
        this.pricePerStock = pricePerStock;
        this.totalPrice = totalPrice;
        this.type = type;
    }
 
    public int getTransactionID(){
        return transactionID;
    }
 
    public long getTimestamp(){
        return timestamp;
    }
   
    public Stock getStock(){
        return stock;
    } 
    
    public int getQuantity(){
        return quantity;
    }
  
    public double getPricePerStock(){
        return pricePerStock;
    } 
      
    public double getTotalPrice(){
        return totalPrice;
    }
     
    public String getType(){
        return type;
    }
 
    public String getSimpleDate() {

        // 1. Convert the long timestamp into an Instant
        Instant instant = Instant.ofEpochMilli(this.timestamp);
    
        // 2. Convert to the system's local date and time
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    
        // 3. Define the pattern: Year-Month-Day Hour:Minute:Second
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
        return dateTime.format(formatter);
        
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionID='" + transactionID + '\'' +
                ", date=" + getSimpleDate() +
                ", stock='" + stock.getStockID() + '\'' +
                ", quantity=" + quantity +
                ", pricePerStock=" + pricePerStock +
                ", totalPrice=" + totalPrice +
                ", type=" + type +
                '}';
    }
 
    @Override
    public int compareTo(Transaction t) {
        return Double.compare(this.totalPrice, t.getTotalPrice());
        
    }
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; 
        if (!(obj instanceof Transaction other)) return false;  
        return this.getTransactionID() == other.getTransactionID() && this.getTimestamp() == other.getTimestamp() && this.getStock().equals(other.getStock()) && this.getQuantity() == other.getQuantity() && this.getPricePerStock() == other.getPricePerStock() && this.getTotalPrice() == other.getTotalPrice() && this.getType().equals(other.getType()); //int comparison
    }
}
