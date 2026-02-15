import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MobicardTokenGenerator {
    
    public static void main(String[] args) {
        try {
            // Configuration
            String mobicardVersion = "2.0";
            String mobicardMode = "LIVE"; // production
            String mobicardMerchantId = "";
            String mobicardApiKey = "";
            String mobicardSecretKey = "";
            
            Random random = new Random();
            String mobicardTokenId = String.valueOf(random.nextInt(900000000) + 1000000);
            String mobicardTxnReference = String.valueOf(random.nextInt(900000000) + 1000000);
            String mobicardServiceId = "20000"; // Card Services APIs
            String mobicardServiceType = "1"; // Use '1' for CARD SCAN METHOD 1
            String mobicardExtraData = "your_custom_data_here_will_be_returned_as_is";
            
            // Create JWT Header
            Map jwtHeader = new HashMap<>();
            jwtHeader.put("typ", "JWT");
            jwtHeader.put("alg", "HS256");
            
            String encodedHeader = base64UrlEncode(new Gson().toJson(jwtHeader));
            
            // Create JWT Payload
            Map jwtPayload = new HashMap<>();
            jwtPayload.put("mobicard_version", mobicardVersion);
            jwtPayload.put("mobicard_mode", mobicardMode);
            jwtPayload.put("mobicard_merchant_id", mobicardMerchantId);
            jwtPayload.put("mobicard_api_key", mobicardApiKey);
            jwtPayload.put("mobicard_service_id", mobicardServiceId);
            jwtPayload.put("mobicard_service_type", mobicardServiceType);
            jwtPayload.put("mobicard_token_id", mobicardTokenId);
            jwtPayload.put("mobicard_txn_reference", mobicardTxnReference);
            jwtPayload.put("mobicard_extra_data", mobicardExtraData);
            
            String encodedPayload = base64UrlEncode(new Gson().toJson(jwtPayload));
            
            // Generate Signature
            String headerPayload = encodedHeader + "." + encodedPayload;
            String signature = generateHMAC(headerPayload, mobicardSecretKey);
            
            // Create Final JWT
            String mobicardAuthJwt = encodedHeader + "." + encodedPayload + "." + signature;
            
            // Request Access Token
            String response = requestAccessToken(mobicardAuthJwt);
            
            // Parse response
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            
            if (jsonResponse.get("status_code").getAsString().equals("200")) {
                String mobicardTransactionAccessToken = jsonResponse.get("mobicard_transaction_access_token").getAsString();
                String mobicardTokenIdResponse = jsonResponse.get("mobicard_token_id").getAsString();
                String mobicardScanCardUrl = jsonResponse.get("mobicard_scan_card_url").getAsString();
                
                System.out.println("Access Token Generated Successfully!");
                System.out.println("Transaction Access Token: " + mobicardTransactionAccessToken);
                System.out.println("Token ID: " + mobicardTokenIdResponse);
                System.out.println("Scan Card URL: " + mobicardScanCardUrl);
            } else {
                System.err.println("Error: " + jsonResponse.get("status_message").getAsString());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static String base64UrlEncode(String data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data.getBytes());
    }
    
    private static String generateHMAC(String data, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hmacBytes = sha256Hmac.doFinal(data.getBytes());
        return base64UrlEncode(new String(hmacBytes));
    }
    
    private static String requestAccessToken(String jwt) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        Map requestBody = new HashMap<>();
        requestBody.put("mobicard_auth_jwt", jwt);
        
        String jsonBody = new Gson().toJson(requestBody);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://mobicardsystems.com/api/v1/card_scan"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        
        HttpResponse response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
