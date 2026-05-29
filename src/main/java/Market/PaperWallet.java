package Market;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PaperWallet {
    
    public void setWalManager(WALManager walManager) {
        this.walManager = walManager;
    }

    private double balance;
    private List<Holding> portfolio;
    private List<Transaction> history;
    private int transactionCount;
    private WALManager walManager;

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public PaperWallet(double initalBalance) {

        this.balance = initalBalance;
        this.portfolio = new ArrayList<>();
        this.history = new ArrayList<>();
        this.transactionCount = 0;

    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public boolean buyStock(String ticker, int quantity, double currentPrice) {
        return buyStock(ticker, quantity, currentPrice, false);
    }

    public boolean buyStock(String ticker, int quantity, double currentPrice, boolean isRecovery) {

        // Total cost of buy said stock
        double totalCost = currentPrice * quantity;

        // You are broke
        if (this.balance < totalCost) {
            double amountNeeded = totalCost - this.balance;
            System.out.println("INSUFFICENT BALANCE");
            System.out.println("You need $" + amountNeeded + " more to successfully complete the transaction.");
            return false;
        }

        // Update the balance after purchase of said stock
        this.balance = this.balance - totalCost;

        // Recording the Transaction
        // int transactionID, int date, Stock stock, int quantity, double pricePerStock,
        // double totalPrice, String type)
        int currentID = (int) transactionCount;
        long currentTimestamp = System.currentTimeMillis();
        //THIS IS USED TO ADD TO THE TRANSACTION HISTORY FOR THE USER TO READ, WE WILL ADD TO HISTORY (ArrayList)
        Transaction t = new Transaction(currentID, currentTimestamp, new Stock(ticker, currentPrice), (int) quantity, (double) currentPrice, (double) totalCost, "BUY");
        
        /*
        The !isRecovery is to ensure that the system only prints the transactions that are made 
        during that time and not the ones curing recovery. Basically, new transaction not old 
        onces that are being replayed.
        */
        if (!isRecovery) { 
            System.out.println(t);
        }

        String dateForHolding = t.getSimpleDate();

        // Create a the Stock Object and put it in a LL
        //THIS IS TO DISPLAY TO THE USER IN THEIR DASHBOARD
        Holding newHolding = new Holding(transactionCount, new Stock(ticker, currentPrice), quantity, totalCost, dateForHolding);
        portfolio.add(newHolding);

        history.add(t);

        /*
        Checks to make sure this is a new transaction and not a replay of an old transcation.
        It enters the logTransaction method in WALManager and adds to the .csv file. */
        if (!isRecovery && this.walManager != null) {
            this.walManager.logTransaction("BUY", ticker, quantity, currentPrice);
        }

        transactionCount++;

        return true;
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public boolean sellStock(String ticker, int quantityToSell, double currentPrice) {
        return sellStock(ticker, quantityToSell, currentPrice, false);
    }

    public boolean sellStock(String ticker, int quantityToSell, double currentPrice, boolean isRecovery) {

        if (quantityToSell <= 0) {
            return false;
        }

        /*
         * Since each purchase of the stock will be split up into differnet lots,
         * we need a temporary container for the specific Holding object that our
         * algorithm chooses as the best one to sell
         */
        Holding bestTarget = null;

        double maxHoldingValue = -1;

        for (int i = 0; i < portfolio.size(); i++) { // Traverse through the list
            Holding h = portfolio.get(i);
            if (h.getStock().getStockID().equalsIgnoreCase(ticker)) { // checks if the current stock is the stock we looking for
                double currentValue = h.getQuantity() * currentPrice;
                if (currentValue > maxHoldingValue) {
                    maxHoldingValue = currentValue;
                    bestTarget = h;
                }
            }
        }

        if (bestTarget == null) {
            System.out.println("You don't have any stock under the name: " + ticker);
            return false;
        }

        int heldQuantity = bestTarget.getQuantity();

        if (heldQuantity > quantityToSell) { // Sell part of the large quantity

            bestTarget.setQuantity(heldQuantity - quantityToSell); // Update the quantity of that lot
            this.balance = this.balance + (quantityToSell * currentPrice);
            long currentTimestamp = System.currentTimeMillis();
            Transaction t = new Transaction(transactionCount++, currentTimestamp, bestTarget.getStock(), quantityToSell, currentPrice, (quantityToSell * currentPrice), "SELL");
            history.add(t);

            if (!isRecovery && this.walManager != null) {
                this.walManager.logTransaction("SELL", ticker, quantityToSell, currentPrice);
            }
            

        } else { // If holding is larger than what we want to sell: remove the holding entirely

            this.balance = this.balance + (heldQuantity * currentPrice);
            portfolio.remove(bestTarget);
            long currentTimestamp = System.currentTimeMillis();
            history.add(new Transaction(transactionCount++, currentTimestamp, bestTarget.getStock(), quantityToSell, currentPrice, (quantityToSell * currentPrice), "SELL"));

            if (!isRecovery && this.walManager != null) {
                this.walManager.logTransaction("SELL", ticker, quantityToSell, currentPrice);
            }

            // If user wants to sell more than this lot
            quantityToSell = quantityToSell - heldQuantity;
            if (quantityToSell > 0) {
                sellStock(ticker, quantityToSell, currentPrice);
            }
        }
        return true;
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public double getBalance() {
        return balance;
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public List<Holding> getPortfolio() {
        return portfolio;

    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public Map<String, Object> netStatus(ConcurrentHashMap<String, Double> priceCache){

        double unrealizedPL = 0; //NET PROFIT/LOSS
        double principalSpent = 0; 
        double currentHoldingsValue = 0;
        System.out.println("--- LIVE PORTFOLIO STATUS ---");
        
        for(int i = 0; i < portfolio.size(); i++){

            Holding h = portfolio.get(i);

            //DATA NEEDED TO CALCULATE NET ACCOUNT STATUS
            String ticker = h.getStock().getStockID();
            double currentPrice = priceCache.getOrDefault(ticker, h.getStock().getCurrentPrice());
            double currentLotValue = h.getQuantity() * currentPrice;
            double initialPrice = h.getCost();

            //CURRENT HOLDINGS VALUE
            currentHoldingsValue = currentHoldingsValue + currentLotValue; //HOW MUCH ALL MY INVESMENTS ARE WORTH

            //MONEY SPENT ON THE INVESTMENTS
            principalSpent = principalSpent + initialPrice; 

            //MONEY MADE/LOST ON THOSE INVESTMENTS
            double delta = currentLotValue - initialPrice;
            unrealizedPL = unrealizedPL + delta;

            System.out.println(String.format("[%s] Qty: %d | Current Value: $%.2f", ticker, h.getQuantity(), currentLotValue));

        }

        //FINAL DASHBOARD STATUS/MIN
        System.out.println("___________________________________________________________________________________________");
        System.out.println("PURCHASING POWER: $" + this.balance);
        System.out.println("CURRENT HOLDINGS VALUE: $" + currentHoldingsValue);
        System.out.println("MONEY SPENT ON INVESTMENTS: $" + principalSpent);
        System.out.println("UNREALIZED P/L: $" + unrealizedPL);
        System.out.println("TOTAL NET WORTH: $" + String.format("%.2f", (this.balance + currentHoldingsValue)));
        System.out.println("___________________________________________________________________________________________");

        Map<String, Object> summaryData = new LinkedHashMap<>();
        summaryData.put("purchasingPower", this.balance);
        summaryData.put("currentHoldingsValue", currentHoldingsValue);
        summaryData.put("moneySpentOnInvestments", principalSpent);
        summaryData.put("unrealizedPL", unrealizedPL);
        summaryData.put("totalNetWorth", String.format("%.2f", (this.balance + currentHoldingsValue)));
        return summaryData;

    }

    

}
