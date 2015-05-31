package com.mailinator;

import com.mailinator.Email;
import com.mailinator.Email.EmailPart;
import com.mailinator.InboxMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * @author Adam Boulton
 */
public class Mailinator {

    //{"error":"rate limiter reached"}
    private static final String MAILINATOR_API_ENDPOINT = "https://api.mailinator.com/api";
    private static final String MAILINATOR_INBOX_TEMPLATE_URL = MAILINATOR_API_ENDPOINT + "/inbox?token=%s&to=%s";
    private static final String MAILINATOR_EMAIL_TEMPLATE_URL = MAILINATOR_API_ENDPOINT + "/email?token=%s&msgid=%s";

    private Mailinator() {
    }

    /**
     * Retrieves all messages from an inbox
     *
     * @param apikey - Mailinator API key
     * @param emailAddress - Email address of the account
     * @return Array of messages from the inbox
     * @throws IOException
     */
    public static ArrayList<InboxMessage> getInboxMessages(String apikey, String emailAddress) throws IOException {

        Reader reader = getInboxStream(apikey, emailAddress);

        JSONObject obj = (JSONObject) JSONValue.parse(reader);
        JSONArray jsonMessages = (JSONArray) obj.get("messages"); //messages array

        //Parse the messages, make the POJOs and add them to our custom list.
        ArrayList<InboxMessage> messages = new ArrayList<>();
        for (Object jsonMsg : jsonMessages) {
            InboxMessage message = createInboxMessageFrom((JSONObject) jsonMsg);
            messages.add(message);
        }

        return messages;
    }

    /**
     * Invokes the Mailnator API endpoint for the Inbox.
     *
     * @param apikey
     * @param emailAddress
     * @return The stream ready for reading the response..
     * @throws IOException
     */
    private static Reader getInboxStream(String apikey, String emailAddress) throws IOException {
        String inboxUrl = String.format(MAILINATOR_INBOX_TEMPLATE_URL, apikey, emailAddress);
        Reader reader = getResponse(inboxUrl);
        return reader;
    }

    /**
     * Once you have the email id's from a given inbox query, you can retrieve
     * the full email.
     *
     * @param apikey
     * @param emailId
     * @throws java.io.IOException
     */
    public static Email getEmail(String apikey, String emailId) throws IOException {

        String emailUrl = String.format(MAILINATOR_EMAIL_TEMPLATE_URL, apikey, emailId);
        Reader reader = getResponse(emailUrl);

        JSONObject obj = (JSONObject) JSONValue.parse(reader);
        return createEmailFrom(obj);
    }

    private static Reader getResponse(String url) throws MalformedURLException, IOException {
        URLConnection connection = new URL(url).openConnection();

        InputStream response = connection.getInputStream();

        return new InputStreamReader(response);
    }

    /**
     * Creates a message object based on the JSON representing a message from
     * the inbox response.
     *
     * @param jsonInboxMsg
     * @return
     */
    private static InboxMessage createInboxMessageFrom(JSONObject jsonInboxMsg) {
        //  JSONObject jsonMsg = (JSONObject) jsonMsg;

        InboxMessage message = new InboxMessage();
        message.setTo(jsonInboxMsg.get("to").toString());
        message.setId(jsonInboxMsg.get("id").toString());
        message.setSeconds_ago(Long.parseLong(jsonInboxMsg.get("seconds_ago").toString()));
        message.setTime(Long.parseLong(jsonInboxMsg.get("time").toString()));
        message.setSubject(jsonInboxMsg.get("subject").toString());
        message.setFromfull(jsonInboxMsg.get("fromfull").toString());
        message.setFrom(jsonInboxMsg.get("from").toString());
        message.setBeen_read((Boolean) jsonInboxMsg.get("been_read"));
        message.setIp(jsonInboxMsg.get("ip").toString());

        return message;
    }

    private static Email createEmailFrom(JSONObject jsonEmail) {
        Email emailMsg = new Email();
//        System.out.println("START RAW JSON");
//        System.out.println(jsonEmail.toJSONString());
//        System.out.println("END JSON");
        emailMsg.setApiInboxFetchesLeft(Integer.valueOf(jsonEmail.get("apiInboxFetchesLeft").toString()));
        emailMsg.setApiEmailFetchesLeft(Integer.valueOf(jsonEmail.get("apiEmailFetchesLeft").toString()));
        emailMsg.setForwardsLeft(Integer.valueOf(jsonEmail.get("forwardsLeft").toString()));

        JSONObject jsonDataSection = (JSONObject) jsonEmail.get("data");
        emailMsg.setId(jsonDataSection.get("id").toString());
        emailMsg.setSecondsAgo(Long.valueOf(jsonDataSection.get("seconds_ago").toString()));
        emailMsg.setTo(jsonDataSection.get("to").toString());
        emailMsg.setTime(Long.valueOf(jsonDataSection.get("time").toString()));
        emailMsg.setSubject(jsonDataSection.get("subject").toString());
        emailMsg.setFromFull(jsonDataSection.get("fromfull").toString());

        //headers
        JSONObject jsonHeaders = (JSONObject) jsonDataSection.get("headers");      
        emailMsg.setHeaders(builderHeaders(jsonHeaders.entrySet()));

        //Parts / content
        HashMap<String, String> parts = new HashMap<>();
        JSONArray jsonParts = (JSONArray) jsonDataSection.get("parts");

        for (Object jsonPart1 : jsonParts) {
            EmailPart emailPart = emailMsg.new EmailPart();
            
            JSONObject jsonPart = (JSONObject) jsonPart1;
            JSONObject jsonPartHeaders = (JSONObject)jsonPart.get("headers");
            emailPart.setHeaders(builderHeaders(jsonPartHeaders.entrySet()));
             
            emailPart.setBody(jsonPart.get("body").toString());
            
            emailMsg.getEmailParts().add(emailPart);
        }

        return emailMsg;
    }

    private static HashMap<String, String> builderHeaders(Set<Entry> jsonHeaderEntries) {
        HashMap<String, String> headers = new HashMap<>();

        for (Entry header : jsonHeaderEntries) {
            headers.put(header.getKey().toString().trim(), header.getValue().toString().trim());
        }
        
        return headers;
    }

}
