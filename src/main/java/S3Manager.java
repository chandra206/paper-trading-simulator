import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;

import Market.PaperWallet;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;


public class S3Manager {

    //Bucket Name
    private static final String S3BucketName = "stock-market-user-data--chandrathatikonda";
    private static final String S3BucketKey = "Alpca-top100-prices--per-minute/live_prices.json";
    
    //Region the Bucket is in
    private static final Region S3BucketRegion = Region.US_EAST_2;

    private static final S3Client s3Client = S3Client.builder().region(S3BucketRegion).build();


    //________________________________________________________________________________________________________________________________________________________________________________________________________________

    //Convert Account Summary to JSON
    public static void SummarytoJSON(PaperWallet paperWallet, ConcurrentHashMap<String, Double> priceCache, String currentDate, String previousDate){
        try{
            
            /*
            What we basically did was we called the netStatus() method in PaperWallet.java.
            We got the status of our account put into a map and returned to us.
            the jsonString, what it does is, it creates a new ObjectMapper, it takes
            the returned value, status Java Map, converts it into a String human-readable JSON text*/
            String jsonString = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(paperWallet.netStatus(priceCache));

            //Calls the S3 upload method to send this newly created file up to the S3 Bucket.
            uploadAccountSummary(currentDate, jsonString);

            //Checks if there is a previous date.
            //If there is no perivous date, that means we are we are not here to erase the data or the file
            //If there is that means that we have created our Second S3 file and deleteing the old S3 file
            if (previousDate != null && !previousDate.isEmpty()) {

                //Creates old fileName
                String oldS3FileName = "Account_Summary_" + previousDate + ".json";
                
                //Delete file request
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder().bucket(S3BucketName).key(oldS3FileName).build();
                
                //Passes the delete file request
                s3Client.deleteObject(deleteRequest);
                
                //Confirms the deletion
                System.out.println("Successfully deleted old summary from S3: " + oldS3FileName);

            }

        }catch( Exception e){

            e.printStackTrace();

        }
    }

    //________________________________________________________________________________________________________________________________________________________________________________________________________________

    //Upload account summary
    public static void uploadAccountSummary(String date, String jsonString){

        //File name
        String s3FileName = "Account_Summary_" + date + ".json";

        try{

            //Build the file in S3 Bucket, S3Bucket'sName, with name: s3FileName
            PutObjectRequest putFile = PutObjectRequest.builder().bucket(S3BucketName).key(s3FileName).build();

            //It takes the jsonString, we created in the SummaryToJSON, and puts it the in the file location we created,  PutObjectRequest.builder().bucket(S3BucketName).key(s3FileName).build(); 
            s3Client.putObject(putFile, RequestBody.fromString(jsonString));

            //Prints Success Message
            System.out.println("Successfully uploaded to S3: " + s3FileName );

        } catch(Exception e){
            
            //Throws exception if try{} fails
            System.err.println("Failed to upload to S3: " + e.getMessage());
        }

        
    }

    //________________________________________________________________________________________________________________________________________________________________________________________________________________

    /*
    This method extracts the stock data. Later in the StockStreamer.java file
    we will create a 60sec loop that calls this method every 60 seconds to get 
    the most up to date stock prices. We will take the extracted data, every 60sec, 
    and dump it into the priceCache. [This is really good bc it does not create 
    conflict when the user BUY/SELL stock and the stock price is being updated 
    at the same time.]*/
    public static ConcurrentHashMap<String, Double> extractStockData(){
        try {

        /*This part is writing a letter saying I want to access the file in this bucket in with this key. 
        The .build is building the request.*/
        GetObjectRequest request = GetObjectRequest.builder().bucket(S3BucketName)
        .key(S3BucketKey).build();

        // Download directly to RAM as a byte array
        ResponseBytes<GetObjectResponse> infoAsBytes = s3Client.getObjectAsBytes(request);
        
        // Parse the JSON bytes instantly into a ConcurrentHashMap
        ObjectMapper mapper = new ObjectMapper();

        // Convert the byte array directly into a ConcurrentHashMap<String, Double>
        return mapper.readValue(infoAsBytes.asByteArray(), 
        mapper.getTypeFactory().constructMapType(ConcurrentHashMap.class, String.class, Double.class));
        
        } catch (Exception e) {

            System.err.println("Failed to fetch live prices from S3: " + e.getMessage());
            return new ConcurrentHashMap<>(); //Return an empty map if there was an error
        }
    }
}
