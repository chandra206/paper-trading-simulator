import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import Market.PaperWallet;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocketInterface;
import java.util.Collections;


/*
The main termianl is being utilized by the StockStreamer. The StockStreamer uses it to listen
to the Alpaca Websocket. We need another thread just to listen to our commands like BUY, SELL, ... 
without stopping the streaming. Runnable is an INTERFACE. When Runnable is implimented, what is says is,
"I will run in the background while, the prices will be streamed in the foreground. 
I will not disturb, the websocket." Runnable as only one command under it, run().
Anything that does not need to be run in front of us, can be ran in the background 
thread we will use Runnable or extend Thread.
*/
public class CommandListener implements Runnable {

    private PaperWallet myPaperWallet;
    private ConcurrentHashMap<String, Double> priceCache;
    private StockMarketDataWebsocketInterface stockStreamer;
    

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public CommandListener(PaperWallet wallet, ConcurrentHashMap<String, Double> cache) {

        this.myPaperWallet = wallet;
        this.priceCache = (ConcurrentHashMap<String, Double>) cache;

    }

    public void setStockStream(StockMarketDataWebsocketInterface stockStream) {
       this.stockStreamer = stockStream; 
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println(
                ">>> Command Listener Active. (Type: STATUS, BUY [Ticker] [Qty], SELL [Ticker] [Qty], TRACK [Ticker], or EXIT)");

        while (true) {
            if (sc.hasNextLine()) {

                String input = sc.nextLine().toUpperCase();
                String[] parts = input.split(" ");
                String command = parts[0];

                switch (command) {
                    case "STATUS":
                        myPaperWallet.netStatus(priceCache); // Goes to the netStatus Method
                        break;

                    case "BUY":
                        handleBuy(parts);
                        break;

                    case "SELL":
                        handleSell(parts);
                        break;

                    case "TRACK":
                        handleTrack(parts);
                        break;

                    case "EXIT":
                        System.out.println("Closing Session. Data saving to S3...");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Unknown Command: " + command);
                        break;
                }
            }
        }
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    private void handleBuy(String[] parts) {
        if (parts.length == 3) {
            String ticker = parts[1];
            try {
                int qty = Integer.parseInt(parts[2]);
                if (priceCache.containsKey(ticker)) {
                    double livePrice = priceCache.get(ticker);
                    boolean success = myPaperWallet.buyStock(ticker, qty, livePrice);
                    if (success) {
                        System.out.println(">>> SUCCESS: Bought " + qty + " shares of " + ticker + " @ $" + livePrice);
                    }
                } else {
                    handleTrack(new String[] { "TRACK", ticker });
                    System.out.println(">>> Waiting for price data...");

                    //Waiting for the ticker's price to be added to the priceCache
                    int refreshesToCheckStream = 0;
                    while (refreshesToCheckStream < 10) { //Refresh 10 times.
                        
                        //Check if we have a valid price in the 'priceCache'
                        if (priceCache.containsKey(ticker) && priceCache.get(ticker) > 0.0) {
                            break; // Found a valid price! Exit loop.
                        }
                        try {
                            Thread.sleep(500); // When we do Thread.Sleep we need to do it in a try/catch with an InterruptedException.
                        } catch (InterruptedException e) {
                            System.out.println("!Error: Wait interrupted.");
                            Thread.currentThread().interrupt();
                            break;
                        }
                        refreshesToCheckStream++;
                        System.out.println("Waiting...");
                    }
                    System.out.println();

                    //BUY after adding to the priceCache
                    double livePrice = priceCache.get(ticker);
                    boolean success = myPaperWallet.buyStock(ticker, qty, livePrice);
                    if (success) {
                        System.out.println(">>> SUCCESS: Bought " + qty + " shares of " + ticker + " @ $" + livePrice);
                    }
                    
                }
            } catch (NumberFormatException e) {
                System.out.println("!Error: Quantity must be an integer.");
            }
        } else {
            System.out.println("Usage: BUY [TICKER] [QTY] (Example: BUY NVDA 15)");
        }
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    private void handleSell(String[] parts) {
        if (parts.length == 3) {
            String ticker = parts[1].toUpperCase();
            try {
                int qtyToSell = Integer.parseInt(parts[2]);

                //Checks to see if we are tracking the price of the stock, if we don't and we are not tracking, we cannot get the live price.
                if (priceCache.containsKey(ticker) && priceCache.get(ticker) > 0.0) { 
                    double livePrice = priceCache.get(ticker);
                    boolean success = myPaperWallet.sellStock(ticker, qtyToSell, livePrice);
                    if (success) {
                        System.out.println(">>>MANUEL SELL SUCCESS: Sold " + qtyToSell + " shares of " + ticker + " @ $" + livePrice);
                    }
                } else{
                    System.out.println("You do not own the ");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("!ERROR: Quantity must be a whole number.");
            }

        } else {
            System.out.println("Usage: SELL [TICKER] [QTY] (Example: SELL AAPL 5)");
        }
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    private void handleTrack(String[] parts) {
        String ticker = parts[1].toUpperCase();

        if (parts.length == 2) {
            if (priceCache.containsKey(ticker)) { //Already tracking
                System.out.println("!Already tracking " + ticker + ":");
                stockStreamer.setTradeSubscriptions(Collections.singleton(ticker));
            } else { //Not tracking
                System.out.println(">>> Initiating stream now...");
                priceCache.put(ticker, 0.0); //All we did was put the ticker in the priceCache
                stockStreamer.setTradeSubscriptions(Collections.singleton(ticker)); //Started streaming the prices of the ticker. 
            }

        } else {
            System.out.println("Usage: TICKER [Ticker] (Example: TRACK MSFT)");
        }
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

}