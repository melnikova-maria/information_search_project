package org.example;

import com.rabbitmq.client.Connection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DatabaseWorker extends Thread {
    private RabbitMQConnector _rabbitMQConnectorContent;

    DatabaseWorker(Connection rabbitConnection, String queueContent) {
        _rabbitMQConnectorContent = new RabbitMQConnector(rabbitConnection, queueContent);
    }

    public void run() {
        while (true) {
            String receivedMessage = _rabbitMQConnectorContent.getMessageFromQueue();
            if (Objects.equals(receivedMessage, "")) {
                try {
                    System.out.println("Thread #" + this.getId() + ": Empty queue ...");
                    sleep(20000);
                    receivedMessage = _rabbitMQConnectorContent.getMessageFromQueue();
                    if (Objects.equals(receivedMessage, "")) {
                        break;
                    }
                }
                catch (InterruptedException e) {
                    System.out.println("Error occured in ParserWorker trying to sleep: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
            JSONObject docObject;
            try {
                docObject = new JSONObject(receivedMessage);
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
            Map<String, Object> document = new HashMap<String, Object>();
            document.put("id", docObject.get("id"));
            document.put("CVEid", docObject.get("CVEid"));
            document.put("Severity by NIST", docObject.get("Severity by NIST"));
            document.put("Last Modified", docObject.get("Last Modified"));

        }
    }
}
