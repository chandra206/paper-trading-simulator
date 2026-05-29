package Market;
import java.io.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Paths;


public class WALManager {

    private static final String EBS_PATH = "D:\\";

    // 50 MegaBytes 
    // 50 * 1024 = 51200 KiloBytes
    // 50 * 1024 * 1024 = 52428800 Bytes
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    //Track file number
    private int currentFileIndex = 1;

    private String LOG_1_PATH;

    //All it does it makes the file name
    public String getLogFileName(int index){

        int FILE1_NUMBER = 0;
        LocalTime now = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = now.format(formatter);
        LOG_1_PATH = "D:\\trade_" + formattedTime + "_" + FILE1_NUMBER + ".csv";
        return EBS_PATH + "trade_" + formattedTime + "_" + index + ".csv"; 
    
    }

    //________________________________________________________________________________________________________________________________________________________________________________________________

    //Logging the file 
    public void logTransaction(String action, String ticker, int qty, double price){
        
        String fileName = getLogFileName(currentFileIndex); //Get the file name

        if (currentFileIndex == 1) {

            this.LOG_1_PATH = fileName; // Capture the EXACT name used today  

        }
        
        File file = new File(fileName); //Creating the object not the file itself

        if (file.exists() && file.length() >= MAX_FILE_SIZE) { //Checks if the file size exceeds the file size limit (10MB)
            System.out.println(">>> WAL File: " + fileName + " is full. Cycling to create next file...");
            currentFileIndex++;
            fileName = getLogFileName(currentFileIndex);
        }

        try{
            // FileWriter is a class that acts as the ink to write
            // The fileName: which file to start writing in. 
            // true: Starts the line after the current full one, No deleting, only editting
            //false: Deletes the entire file.
            FileWriter fileWriter = new FileWriter(fileName, true); //Creates the file itself
            
            // BufferWriter is the RAM manager.
            // Instead of send directly to the disk, it holds the data in a RAM bucket which is faster. 
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        
            // This flushes everything out by pushing everything from the RAM bucket into th disk
            PrintWriter out = new PrintWriter(bufferedWriter);

            /*
            We might think that what if it crashes and data in the RAM is lost
            but, everything happens so fast that it is negligible.
            */

            /* HOW IT WORKS:
            1. fileWriter opens the file
            2. bufferWriter adds the transaction to the RAM
            3. out, warns the others, "I am closing and pushing everything I have into the bw bucket."
            4. bufferWriter, "I am closing as well, I am dumping everything the entire RAM bucket onto fileWriter."
            5. fileWriter, "I am now physically writing this to the disk and locking the file."
            */

            /*
            The whole idea is that the RAM bucket acts as the bridge between the land. 
            If the RAM bucket did not exist, it would take longer, like getting a boat and then paddling across.
            */

            //Creates timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            //Createa the log entry
            // Format: TIME, ACTION, TICKER, QTY, PRICE
            String logEntry = timestamp + "," + action + "," + ticker + "," + qty + "," + price;

            //Prints the log entry to the file
            out.print(logEntry);

            System.out.println(timestamp + "," + action + "," + ticker + "," + qty + "," + price);

        } catch (IOException e) {
            System.err.println("CRITICAL: Could not write to WAL! " + e.getMessage());
        }

    }

//________________________________________________________________________________________________________________________________________________________________________________________________

    //Reading the Files
    public void recoverState(PaperWallet wallet){

        System.out.println(">>> Checking for crash recovery files...");
        int index = 1;
        boolean foundFiles = false;

        while (true) {
            String fileName = getLogFileName(index); //Gets the file name
            File file = new File(fileName);

            if (!file.exists()) { //checks if file exists,if not, then it exits the whole process
                System.out.println("File: " + fileName + " does not exist.");
                break;
            }

            System.out.println(">>> Recovering from segment:" + fileName + "...");
            foundFiles = true;

            try(Scanner scanner = new Scanner(file)){
                
                String line = scanner.nextLine();
                
                if (line.trim().isEmpty()) {
                    continue;
                }

                String [] parts = line.split(",");
                String action = parts[1];
                String ticker = parts[2];
                int qty = Integer.parseInt(parts[3]);
                double price = Double.parseDouble(parts[4]);


                if (action.equals("BUY")) {
                    wallet.buyStock(ticker, qty, price, true);
                } if(action.equals("SELL")){
                    wallet.sellStock(ticker, qty, price, true);
                }

            } catch (Exception e) {
                System.out.println("Error reading " + fileName + ": " + e.getMessage());
            }
            index++;
        }

        if (foundFiles) {
            System.out.println(">>> Recovery Complete. Wallet restored.");
        } else{
            System.out.println(">>> No logs found for today. Starting fresh.");
        }

    }

    //________________________________________________________________________________________________________________________________________________________________________________________________


    //DELETE PART
    private static final String LOG_2_PATH = "D:\\WAL_Log_2.txt";

    public void WALCleanup(){

        try{
            
            boolean deleted = Files.deleteIfExists(Paths.get(LOG_2_PATH));
            if (deleted) {
                System.out.println("WAL Manager: Backup Log 2 deleted successfully.");
            }

            if (LOG_1_PATH != null) {
                File activeFile1 = new File(LOG_1_PATH);
                if (activeFile1.exists()) {

                    //Erase Log file 1
                    new FileWriter(LOG_1_PATH, false).close(); //***************
                    System.out.println("WAL Manager: Log 1 cleared and ready for next session.");
                }
            }
            

        } catch (IOException e) {
            System.err.println("WAL Manager Error: Could not clean up logs - " + e.getMessage());
        }
    }
    
}
