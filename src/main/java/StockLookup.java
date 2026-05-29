import java.io.*;
import java.util.*;

public class StockLookup {

    private Map<String, String> nameToTicker = new HashMap<>();
    private Map<String, String> tickerToName = new HashMap<>();

    public StockLookup(String filePath) throws FileNotFoundException, IOException{
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))){
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String name = parts[0].trim().toLowerCase();
                    String ticker = parts[1].trim().toUpperCase();
                    nameToTicker.put(name, ticker);
                    tickerToName.put(ticker, name);
                }
            }
        } 
        
        catch (IOException e) {
            System.err.println("Error reading top100.txt: " + e.getMessage());
        }
    }

//_____________________________________________________________________________________________________________________

    public String getTicker(String input) {
        String cleanInput = input.trim().toLowerCase();
        // If they typed a name like "Apple", return "AAPL"
        if (nameToTicker.containsKey(cleanInput)) {
            return nameToTicker.get(cleanInput);
        }
        // If they already typed a ticker like "AAPL", just return it
        return input.toUpperCase();
    }
}
