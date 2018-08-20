package com.stackimpact.agent;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.eclipsesource.json.*;

public class APIRequest {
    private Agent agent;


    public APIRequest(Agent agent) {
        this.agent = agent;
    }


    public Object post(String endpoint, Object payload) throws Exception {
        HashMap dataObject = new HashMap();
        dataObject.put("runtime_type", "java");
        dataObject.put("runtime_version", System.getProperty("java.version"));
        dataObject.put("agent_version", Agent.VERSION);
        dataObject.put("app_name", agent.getAppName());
        dataObject.put("app_version", agent.getAppVersion());
        dataObject.put("app_environment", agent.getAppEnvironment());
        dataObject.put("host_name", agent.getHostName());
        dataObject.put("process_id", AgentUtils.getPID());
        dataObject.put("run_id", agent.getRunID());
        dataObject.put("run_ts", agent.getRunTS());
        dataObject.put("sent_at", AgentUtils.timestamp());
        dataObject.put("payload", payload);

        String data = null;
        if(dataObject != null) {
            data = toJSON(dataObject);
        }

        agent.logInfo("APIRequest: request:");
        if(data != null) {
            agent.logInfo(data);
        }

        HttpURLConnection connection = null;

        try {
            // create connection
            URL url = new URL(agent.getDashboardAddress() + "/agent/v1/" + endpoint);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            if(data != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Content-Encoding", "gzip");
                connection.setRequestProperty("Content-Length", Integer.toString(data.length()));
            }
            connection.setRequestProperty("Authorization", "Basic " +
                    toBase64((agent.getAgentKey() + ":").getBytes()));

            connection.setUseCaches(false);

            if(data != null) {
                connection.setDoOutput(true);

                // send compressed request
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GZIPOutputStream gzos = null;

                gzos = new GZIPOutputStream(baos);
                gzos.write(data.getBytes("UTF-8"));
                gzos.close();

                byte[] compressedPayload = baos.toByteArray();
                baos.close();

                dos.write(compressedPayload, 0, compressedPayload.length);
                dos.flush();
                dos.close();

                agent.logInfo("APIRequest: sent " + compressedPayload.length + " bytes of compressed payload data");
            }

            // get response
            if(connection.getResponseCode() != 200) {
                throw new Exception(
                        "APIRequest: response code: " + connection.getResponseCode() +
                                ", response message: " + connection.getResponseMessage());
            }

            InputStream is = connection.getInputStream();

            // uncompress response if needed
            String contentEncoding = connection.getContentEncoding();
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                GZIPInputStream gzis = new GZIPInputStream(is);
                is = gzis;
            }

            // read response
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                builder.append(line);
                builder.append('\r');
            }
            br.close();

            agent.logInfo("APIRequest: payload response:");
            agent.logInfo(builder.toString());

            return fromJSON(builder.toString());
        }
        catch (Exception ex) {
            throw ex;
        }
        finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }



    public static String toBase64(byte[] data) {
        char[] tbl = {
                'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
                'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
                'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
                'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/' };

        StringBuilder buffer = new StringBuilder();
        int pad = 0;
        for (int i = 0; i < data.length; i += 3) {

            int b = ((data[i] & 0xFF) << 16) & 0xFFFFFF;
            if (i + 1 < data.length) {
                b |= (data[i+1] & 0xFF) << 8;
            } else {
                pad++;
            }
            if (i + 2 < data.length) {
                b |= (data[i+2] & 0xFF);
            } else {
                pad++;
            }

            for (int j = 0; j < 4 - pad; j++) {
                int c = (b & 0xFC0000) >> 18;
                buffer.append(tbl[c]);
                b <<= 6;
            }
        }
        for (int j = 0; j < pad; j++) {
            buffer.append("=");
        }

        return buffer.toString();
    }



    public static String toJSON(Object obj) {
        return toJSONValue(obj).toString();
    }


    public static JsonValue toJSONValue(Object obj) {
        if (obj instanceof HashMap) {
            JsonObject jsonObject = Json.object();

            Iterator it = ((HashMap)obj).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                jsonObject.add((String)pair.getKey(), toJSONValue(pair.getValue()));
            }

            return jsonObject;
        }
        else if (obj instanceof ArrayList) {
            JsonArray jsonArray = Json.array();

            Iterator<String> iterator = ((ArrayList)obj).iterator();
            while (iterator.hasNext()) {
                jsonArray.add(toJSONValue(iterator.next()));
            }

            return jsonArray;
        }
        else if(obj instanceof String) {
            return Json.value((String)obj);
        }
        else if(obj instanceof Integer) {
            return Json.value((Integer)obj);
        }
        else if(obj instanceof Long) {
            return Json.value((Long)obj);
        }
        else if(obj instanceof Float) {
            return Json.value((Float)obj);
        }
        else if(obj instanceof Double) {
            return Json.value((Double)obj);
        }
        else if(obj instanceof Boolean) {
            return Json.value((Boolean)obj);
        }
        else if(obj == null) {
            return Json.value(null);
        }

        return null;
    }


    public static Object fromJSON(String json) {
        return fromJSONValue(Json.parse(json).asObject());
    }


    public static Object fromJSONValue(JsonValue jsonValue) {
        if (jsonValue.isObject()) {
            HashMap obj = new HashMap();

            JsonObject jsonObject = jsonValue.asObject();
            Iterator it = jsonObject.names().iterator();
            while (it.hasNext()) {
                String name = (String)it.next();
                obj.put(name, fromJSONValue(jsonObject.get(name)));
            }

            return obj;
        }
        else if (jsonValue.isArray()) {
            ArrayList arr = new ArrayList();

            JsonArray jsonArray = jsonValue.asArray();
            Iterator it = jsonArray.iterator();
            while (it.hasNext()) {
                JsonValue elem = (JsonValue)it.next();
                arr.add(fromJSONValue(elem));
            }

            return arr;
        }
        else if(jsonValue.isString()) {
            return jsonValue.asString();
        }
        else if(jsonValue.isNumber()) {
            return jsonValue.asDouble();
        }
        else if(jsonValue.isBoolean()) {
            return jsonValue.asBoolean();
        }
        else if(jsonValue.isNull()) {
            return null;
        }

        return null;
    }

}
