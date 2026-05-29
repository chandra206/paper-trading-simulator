import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.regions.Region;
/*
This import statement give a list of all AWS data centers like US EAST1, 2, ...
This import statement is very important because it tells the code where exactly 
the secret information is stored, otherwise it will search the wrong building 
and return an error*/

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
/*
This does the connection between the EC2 instance the AWS Secret Manager Serivce.
It handles the "under-the-hood" work of using your IAM Role to prove your identity. */

import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
/*
This is a request we send to Amazon, before we get a secret, we have to address 
the envelope by saying secretId (which you named AlpacaKeys). This class is sort 
of a templete for the request.*/

import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
/*
This represents a package Amazon sends back to us, after they approve our request. */

/*
Region: Where to go
Client: How to talk
Request: What to ask
Response: What we get back */

public class SecretRetriever {


    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public static String getSecretJson() { 

        
        Region region = Region.US_EAST_2; //It is in US East 1
        //Telling the code where the secret is.

        SecretsManagerClient client = SecretsManagerClient.builder().region(region).build();
        //Telling the code to start talking 

        String SecretName = "AlpacaKeyz";

        try{

            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder().secretId(SecretName).build();
            //I am looking for the label SecretName = "AlpacaKeyz"

            GetSecretValueResponse valueResponse = client.getSecretValue(valueRequest);
            /*
            The client hands the request to AWS. The code pauses here for a millisecond 
            while AWS checks your IAM Role to see if you are allowed to see this secret.*/

            System.out.println("--- CONNECTION SUCCESS ---");
            return valueResponse.secretString();
        } catch (Exception e){ 
            /*
            If there are any exceptions in that occur when performing the try block 
            we will send an error message */

            System.err.println("--- CONNECTION ERROR ---");
            System.err.println("Error: " + e.getMessage());
            System.err.println("Note: Ensure your EC2 IAM Role has SecretsManager permissions.");
            return null;
        } finally{
            client.close();
        }

    }

    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    // Helper method to get the specific Key
    public static String getAlpacaKey() {
        try {
            String fullJson = getSecretJson();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode keysJson = mapper.readTree(fullJson);
            return keysJson.get("ALPACAKEY").asText();
        } catch (Exception e) {
            return null;
        }
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    // Helper method to get the specific Secret
    public static String getAlpacaSecret() {
        try {
            String fullJson = getSecretJson();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode keysJson = mapper.readTree(fullJson);
            return keysJson.get("ALPACASECRET").asText();
        } catch (Exception e) {
            return null;
        }
    }

    
}
