package examples;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.Writer;
import java.util.Random;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.Context; 


import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

import com.stackimpact.agent.StackImpact;
import com.stackimpact.agent.ProfileSpan;


public class Hello implements RequestStreamHandler {
    JSONParser parser = new JSONParser();


    // initialize StackImpact agent
    static {
        StackImpact.setAutoProfilingMode(false);
        StackImpact.start("agent key here", "LambdaJavaApp");
    }

    
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        ProfileSpan span = StackImpact.profile();

        // simulate CPU work
        try {
            Random rand = new Random();
            for (int i = 0; i < 20 * 1000000; i++) {
                rand.nextInt(100000);
            }
        }
        catch(Exception ex) {
        }
        // end CPU work

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        String responseCode = "200";

        try {
            JSONObject event = (JSONObject)parser.parse(reader);

            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            responseBody.put("message", "Hello");

            JSONObject headerJson = new JSONObject();
            headerJson.put("x-custom-header", "my custom header value");

            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", responseCode);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());  

        } catch(ParseException pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toJSONString());  
        writer.close();

        span.stop();
    }
}

