import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import Market.PaperWallet;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.util.apitype.MarketDataWebsocketSourceType;
import net.jacobpeterson.alpaca.model.util.apitype.TraderAPIEndpointType;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocketInterface;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataListenerAdapter;


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
    private StockMarketDataWebsocketInterface alpacaWebSocket;

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

    public CommandListener(PaperWallet wallet, ConcurrentHashMap<String, Double> cache) {

        this.myPaperWallet = wallet;
        this.priceCache = (cache != null) ? cache : new ConcurrentHashMap<>();

        String keyID = SecretRetriever.getAlpacaKey(); 
        String secretKey = SecretRetriever.getAlpacaSecret();

        AlpacaAPI alpacaAPI = new AlpacaAPI(keyID, secretKey, TraderAPIEndpointType.PAPER,MarketDataWebsocketSourceType.IEX);
        this.alpacaWebSocket = alpacaAPI.stockMarketDataStream();

        //LISTENER BLOCK
        this.alpacaWebSocket.setListener(new StockMarketDataListenerAdapter(){

                @Override
                public void onTrade(StockTradeMessage trade) {

                    // 1. Instantly hot-swap the live price into your global cache
                    priceCache.put(trade.getSymbol(), trade.getPrice());
                    
                    // 2. Print the visual ticker tape to the user's terminal
                    System.out.println(">>> LIVE TICK [" + trade.getSymbol() + "]: $" + trade.getPrice());
                }
        });

        this.alpacaWebSocket.connect();

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
                    System.out.println("!ERROR: Price data for " + ticker + " is currently unavailable from S3.");
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
                        System.out.println(">>>MANUAL SELL SUCCESS: Sold " + qtyToSell + " shares of " + ticker + " @ $" + livePrice);
                    }
                } else{
                    System.out.println("You do not own the stock or price data for " + ticker + " is currently unavailable.");
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
                System.out.println(">>> Initiating stream now of "+ ticker + "...");
                try {
                alpacaWebSocket.setTradeSubscriptions(Collections.singleton(ticker));
            } catch (Exception e) {
                System.out.println("!ERROR: Failed to connect to Alpaca stream.");
            }
            } else { //Not tracking
                System.out.println("Usage: TRACK [TICKER] (Example: TRACK MSFT)"); 
            }

        } else {
            System.out.println("Usage: TICKER [Ticker] (Example: TRACK MSFT)");
        }
    }

    // ________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________________

}