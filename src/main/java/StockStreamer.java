import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import Market.PaperWallet;
import Market.WALManager;

/*
MarketOrchestrator.java starts the game by split the command terminal in two parts.
One part that goes and listens to all of the User inputs like BUY/SELL/TRACK/....
The other part of the terminal is a 24/7 loop that checks the time and 
updates the HashMap with updated prices for BUY and SELL commands, it also checks for
the time of market closure to ensure user data is saved and uploaded to S3 every day at 4:00 PM EST.
*/

public class StockStreamer{
    
    public static void main(String[] args) {

        // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

        // 1. Initialize Scanner for the initial setup
        Scanner setupScanner = new Scanner(System.in);

        System.out.println("--------------------------------------------------");
        System.out.print("Enter starting Paper Wallet balance (e.g., 10000.00): ");

        double startingBalance = 10000.00; // Default value

        if (setupScanner.hasNextDouble()) {
            startingBalance = setupScanner.nextDouble();
        }

        // Clear the buffer so the later "Enter Company Name" scanner works correctly
        setupScanner.nextLine();

        // 2. Initialize the Wallet with your input
        PaperWallet myPaperWallet = new PaperWallet(startingBalance);
        WALManager walManager = new WALManager();
        myPaperWallet.setWalManager(walManager); // Connects them together so that the PaperWallet can use the WALManager to log transactions
        System.out.println("Wallet initialized with: $" + String.format("%.2f", myPaperWallet.getBalance()));
        System.out.println("--------------------------------------------------");
        ConcurrentHashMap<String, Double> priceCache = new ConcurrentHashMap<>(); 
        // stores the latest price for
        // every stock ticker you are
        // tracking. Updates min by min.

        // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________


        // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

        try {

            CommandListener listenerLogic = new CommandListener(myPaperWallet, priceCache);
            Thread commandThread = new Thread(listenerLogic);
            commandThread.start();

            ZoneId estZone = ZoneId.of("America/New_York");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            //Empty dates to use later
            String previousDate = "";
            String currentDate = "";
            boolean marketClosedProcessed = false;

            System.out.println("Market Automation System Active. Monitoring for 4:00 PM EST...");

            while (true) {

                ConcurrentHashMap<String, Double> freshData = S3Manager.extractStockData(); 

                if(freshData != null && !freshData.isEmpty()){
                    priceCache.putAll(freshData);
                }
                    
                //Gets the time and date of TODAY
                LocalDate today = LocalDate.now(estZone);
                LocalTime now = LocalTime.now(estZone);

                //Notes the time marketCloses
                LocalTime marketClose = LocalTime.of(16, 0); //Market Closes are 4:00PM EST
                LocalTime resetTime = LocalTime.of(0, 0); //******* 

                //If the time is, after market is cloed AND before market Opens 
                //When market is closed
                if (now.isAfter(marketClose) && !marketClosedProcessed) {

                    try{
                        //Update currentDate with today's date
                        currentDate = today.format(formatter);

                        //Call to create the file with account Summary
                        S3Manager.SummarytoJSON(myPaperWallet, priceCache, currentDate, previousDate);

                        walManager.WALCleanup();

                        //This is to make sure that when we go back to the S3 the next day, we are able to make a new on and delete the old one/
                        previousDate = currentDate;
                        marketClosedProcessed = true;

                        System.out.println(">>> Workflow Complete. System hibernating until tomorrow.");

                    } catch(Exception e){
                        System.err.println("CRITICAL: Workflow failed! " + e.getMessage());
                    }   
                }

                //Preparing things so the system to run again the next day
                if (now.isAfter(resetTime) && now.isBefore(LocalTime.of(9, 0))) {

                    marketClosedProcessed = false;

                }

                try { 
                    Thread.sleep(60000); 
                } catch (InterruptedException e) { 
                    break; 
                }

                
            }
            
            //________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________
        }

        catch (Exception e) {
            System.err.println("CRITICAL ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        finally {
            System.out.println("Cleaning up connections...");
        }

        // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________
    }
    
}