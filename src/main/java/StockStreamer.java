
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.util.apitype.MarketDataWebsocketSourceType;
import net.jacobpeterson.alpaca.model.util.apitype.TraderAPIEndpointType;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocketInterface;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataListenerAdapter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import Market.PaperWallet;
import Market.WALManager;


//We will need to make changes to this file to implimemet our Lambda function.

public class StockStreamer {
    
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
        myPaperWallet.setWalManager(walManager); // Connects them
        System.out.println("Wallet initialized with: $" + String.format("%.2f", myPaperWallet.getBalance()));
        System.out.println("--------------------------------------------------");
        ConcurrentHashMap<String, Double> priceCache = new ConcurrentHashMap<>(); 
        // stores the latest price for
        // every stock ticker you are
        // tracking. Updates min by min.

        // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________


        // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

        try {

            String realKey = SecretRetriever.getAlpacaKey();
            String realSecret = SecretRetriever.getAlpacaSecret();

            System.out.println(
                    "--------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("--- VAULT OPENED SUCCESSFULLY ---");
            System.out.println(
                    "-------------------------------------------------------------------------------------------------------------------------------------------------------------------");

            // --- STEP 3: START THE ALPACA EAR (v10) ---
            AlpacaAPI alpacaAPI = new AlpacaAPI(
                    realKey,
                    realSecret,
                    TraderAPIEndpointType.PAPER,
                    MarketDataWebsocketSourceType.IEX);

            StockMarketDataWebsocketInterface stockStream = alpacaAPI.stockMarketDataStream();

            // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

            // Set the listener using the Adapter
            stockStream.setListener(new StockMarketDataListenerAdapter() {

                long lastPrintTime = 0;

                @Override
                public void onTrade(StockTradeMessage trade) {

                    long currentTime = System.currentTimeMillis();
                    String symbol = trade.getSymbol();
                    double currentPrice = trade.getPrice();

                    priceCache.put(symbol, currentPrice);

                    if (currentTime - lastPrintTime >= 60_000) {
                        LocalTime now = LocalTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                        String formattedTime = now.format(formatter);
                        System.out.println("LIVE TRADE UPDATE (BY PER MIN): " + trade.getSymbol() + " @ $" + trade.getPrice() + " @ " + formattedTime + " |");
                        lastPrintTime = currentTime;
                    }


                }
            });

            // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

            StockLookup lookup = new StockLookup("top100.txt");
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter Company Name or Ticker (e.g., Apple or AAPL): ");

            String userInput = scanner.nextLine();
            String finalTicker = lookup.getTicker(userInput);

            System.out.println("Resolved to Ticker: " + finalTicker);

            // Connect and Subscribe
            stockStream.connect();
            System.out.println(
                    "-------------------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.println("Connection Successful to Alpaca Websocket");
            System.out.println(
                    "-------------------------------------------------------------------------------------------------------------------------------------------------------------------");

            // Give it a second to authenticate before subscribing
            Thread.sleep(2000);

            System.out.println();
            stockStream.setTradeSubscriptions(Collections.singleton(finalTicker));
            System.out.println("Listening for " + finalTicker + " trades...");

            CommandListener listenerLogic = new CommandListener(myPaperWallet, priceCache);
            listenerLogic.setStockStream(stockStream);
            Thread commandThread = new Thread(listenerLogic);
            commandThread.start();
            //________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

            ZoneId estZone = ZoneId.of("America/New_York");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            //Empty dates to use later
            String previousDate = "";
            String currentDate = "";
            boolean marketClosedProcessed = false;

            System.out.println("Market Automation System Active. Monitoring for 4:00 PM EST...");

            while (true) {
                    
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