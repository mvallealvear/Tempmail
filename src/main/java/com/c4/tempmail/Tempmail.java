package com.c4.tempmail;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Tempmail {

    // type de UUID defined for you in Mashape
    private static final String XMashapeKey = "iboVF12LNEmshIRNA8rlKpgrmqEap12OmTcjsn6rMFaa2xnWRc";

    private static final String _EMAIL  = "EMAIL";
    private static final String _KEY  = "KEY";

    private static final String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
    private static final String appStorePath = rootPath + "tempmail.dat";
    private static final Logger LOGGER = Logger.getLogger(Tempmail.class.getName());
    private static FileHandler fh = null;


    private static void init() {
        // for log info
        try {
            // choose to append data to existing: true or false.
            fh = new FileHandler("logger.log", false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger l = Logger.getLogger("");
        fh.setFormatter(new SimpleFormatter());
        l.addHandler(fh);
        l.setLevel(Level.INFO);
    }

    public static void main(String[] args) {
        init();

        System.out.println(appStorePath);

        System.out.printf("Test run in Date-Time: %s%n", getDateTimeNow());

        // Create a new random email, with 10 minutes of life (random: true)
        //or
        // Create with a specific email name, with 60 minutes of life (random: true)
        newEmail(false);
        // Email and email-key, is stored in properties file (tempmail.dat)

        // get the list of received emails. The first execution will return an error: "the_list_is_empty".
        // To test the email created above, comment on the previous line (newEmail();),
        // the application will use the data stored in the properties file.
        getEmails();
    }

    // Create a new email, with random name. 10 minutes life.
    private static HttpResponse<JsonNode> createRandom10minEmail(){
        try {
            String sURI = "https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=new&type=json";
            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);

            return response;

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Create a new email, specifying its name. 60 minutes life.
    private static HttpResponse<JsonNode> createEmailWithName(String sEmailName){
        try {
            String sURI = String.format("https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=new&name=%s&type=json", sEmailName);
            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);
            System.out.printf("Response status: %s(%d)%n%n", response.getStatusText(), response.getStatus());

            return response;

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Get a list of emails of inbox
    private static HttpResponse<JsonNode> getEmailList(String sKey){
        try {
            String sURI = String.format("https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=getlist&key=%s&type=json", sKey);

            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);
            System.out.printf("Response status: %s(%d)%n%n", response.getStatusText(), response.getStatus());

            return response;

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return null;
    }


    // Get a specific email through the ID in the inbox
    private static HttpResponse<JsonNode> getEmail(String key, String id){
        try {
            String sURI = String.format("https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=getmail&forced=1&id=%s&key=%s&type=json", id, key);

            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);
            System.out.printf("Response status: %s(%d)%n%n", response.getStatusText(), response.getStatus());

            return response;

        } catch (UnirestException e) {
            e.printStackTrace();
        }

        return null;
    }

    // wrapper to create new email and store the name and key(UUID)
    private static void newEmail(Boolean bRandom){

        HttpResponse<JsonNode> responseCreate;

        if (bRandom) {
            // 10 minutes life
            responseCreate = createRandom10minEmail();
        } else {
            // 60 minutes life. String of email name limited to 10 characters.
            // This generate emails like: "test_58741@post-shift.ru"
            String sEmailPseudoRandom = String.format("test_%d", (new Random().nextInt(89999)+10000));
            responseCreate = createEmailWithName(sEmailPseudoRandom);
        }

        if (null != responseCreate){
            System.out.printf("Headers: %s%n", responseCreate.getHeaders().toString());
            System.out.printf("Body string: %s%n", responseCreate.getBody().toString());

            JSONObject myResponseObj1 = responseCreate.getBody().getObject();

            // if has key with name "error"
            if(myResponseObj1.has("error")){

                String sGetListError = myResponseObj1.getString("error");
                System.out.printf("Error message: %s%n", sGetListError);

            } else {

                String sNewEmail = myResponseObj1.getString("email");
                String sNewKeyEmail = myResponseObj1.getString("key");

                HashMap<String, String> hmDatos = new HashMap<String, String>();
                hmDatos.put(_EMAIL, sNewEmail);
                hmDatos.put(_KEY, sNewKeyEmail);

                setDataInProperties(hmDatos);

                // for traceability purposes
                LOGGER.log(Level.INFO, String.format("Email: %s", sNewEmail));
                LOGGER.log(Level.INFO, String.format("Email key: %s", sNewKeyEmail));
            }
        }
    }


    // get all the emails of inbox
    private static void getEmails() {
        HttpResponse<JsonNode> responseGetList;

        String sNewKeyEmail = getDataInProperties(_KEY);

        if(!"".equals(sNewKeyEmail)) {
            // Is mandatory a email UUID
            responseGetList = getEmailList(sNewKeyEmail);
        }else {
            return;
        }

        if (null != responseGetList) {
            System.out.printf("Headers: %s%n", responseGetList.getHeaders().toString());
            System.out.printf("Body: %s%n", responseGetList.getBody().toString());

            JSONObject myResponseObj2 = responseGetList.getBody().getObject();

            if (null != myResponseObj2) {
                if(myResponseObj2.has("error")){
                    String sGetListError = myResponseObj2.getString("error");
                    System.out.printf("Error message:: %s%n", sGetListError);
                    LOGGER.log(Level.INFO, String.format("Error message: %s", sGetListError));
                }
            }else{
                // recuperamos la lista de emails
                JsonNode node = responseGetList.getBody();

                Integer iEmails = node.getArray().length();
                System.out.printf("%nEmails count: %d%n%n", iEmails);

                for (int i = iEmails-1; i > -1 ; i--) {
                    JSONObject jsonObj = node.getArray().getJSONObject(i);
                    System.out.printf("Email ID: %s, subject: %s%n", jsonObj.get("id"), jsonObj.get("subject"));
                    LOGGER.log(Level.INFO, String.format("Email ID: %s, subject: %s%n", jsonObj.get("id"), jsonObj.get("subject")));

                    HttpResponse<JsonNode> responseGetEmail = getEmail(sNewKeyEmail, String.valueOf(i+1));
                    if (null != responseGetEmail) {
                        System.out.printf("Headers: %s%n", responseGetEmail.getHeaders().toString());
                        System.out.printf("Body: %s%n", responseGetEmail.getBody().toString());

                        JSONObject jsonObjBody = responseGetEmail.getBody().getObject();

                        String sHtml = jsonObjBody.get("message").toString();

                        System.out.printf("Message: %s%n", sHtml);
//                        LOGGER.log(Level.INFO, String.format("Email body: %s", jsonObjBody.get("message")));
                    }
                }
            }
        }
    }


    // Clean Inbox
    private static void clearEmails(String sKey){
        try {
            String sURI = String.format("https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=clear&key=%s", sKey);

            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);
            System.out.printf("Response status: %s(%d)%n%n", response.getStatusText(), response.getStatus());

        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }


    // Delete the email account
    private static void deleteEmailAccount(String sKey){
        try {
            String sURI = String.format("https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=delete&key=%s", sKey);

            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);
            System.out.printf("Response status: %s(%d)%n%n", response.getStatusText(), response.getStatus());

        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }


    // Restart the 10 minutes counter. Beware!! you can setting an email from 1 hour to 10 minutes
    private static void extend10MinutesEmail(String sKey){
        try {
            String sURI = String.format("https://reuleaux-post-shift-v1.p.mashape.com/api.php?action=update&key=%s", sKey);

            HttpResponse<JsonNode> response = Unirest.get(sURI)
                    .header("X-Mashape-Key", XMashapeKey)
                    .header("Accept", "application/json")
                    .asJson();

            System.out.printf("%nRequest %s%n", sURI);
            System.out.printf("Response status: %s(%d)%n%n", response.getStatusText(), response.getStatus());

        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }


    // utilities

    // Return a string with date-time format
    private static String getDateTimeNow(){
        SimpleDateFormat formatter;
        formatter = new SimpleDateFormat("dd.MM.yy H:mm:ss", new Locale("es_ES"));
        Date now = new Date();
        return formatter.format(now);
    }

    // Store a hashmap in  aproperties file
    private static void setDataInProperties(HashMap<String, String> hmDatos) {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream(appStorePath);

            for (Map.Entry<String, String> entry : hmDatos.entrySet()) {
                // set the properties value
                prop.setProperty(entry.getKey(), entry.getValue());
            }

            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Return de value of a key, in the properties file
    private static String getDataInProperties(String sKey) {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(appStorePath);

            // load a properties file
            prop.load(input);

            // set the properties value
            return prop.getProperty(sKey);

        } catch (IOException io) {
            io.printStackTrace();
            return "";
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // TODO:
    public static void sendEmail(String sEmailTo){

        String sEmailFrom = "test_eamil@yopmail.com";

        try {
            HttpResponse<String> response = Unirest.post("https://mailgun.p.mashape.com/mysite.com/messages")
                    .header("Authorization", "<required>")
                    .header("X-Mashape-Key", "<required>")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "text/plain")
                    // .field("bcc", "another@email.com")
                    // .field("cc", "him@email.com")
                    .field("from", sEmailFrom)
                    .field("h:", "h:X-My-Header")
                    .field("html", "<b>Mailgun rocks</b>")
                    .field("inline", "@files/awesome.gif")
                    .field("o:campaign", "some_campaign_id")
                    .field("o:deliverytime", "'Thu, 12 April 2018 15:11:30 GMT'")
                    .field("o:dkim", "yes")
                    .field("o:tag", "newsletter")
                    .field("subject", "My subject line")
                    .field("text", "Plain text lines are pretty")
                    .field("to", sEmailTo)
                    .field("v:", "v:my-var")
                    .asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
    }


}
