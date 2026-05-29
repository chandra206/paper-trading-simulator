import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import Market.PaperWallet;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;


public class S3Manager {

    //Bucket Name
    private static final String S3BucketName = "stock-market-user-data--chandrathatikonda";
    
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
}
