package org.example;

import com.rabbitmq.client.Connection;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.crypto.Data;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseWorker extends Thread {
    private RabbitMQConnector _rabbitMQConnectorContent;
    private ElasticSearchConnector _elasticSearchConnector;
    private Logger _logger;

    DatabaseWorker(Logger logger, Connection rabbitConnection, String queueContent) {
        _logger = logger;
        _rabbitMQConnectorContent = new RabbitMQConnector(rabbitConnection, queueContent);
        _elasticSearchConnector = new ElasticSearchConnector(logger);
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
            String id = (String) docObject.get("id");
            CVE documentCVE = new CVE((String) docObject.get("CVEid"),
                                  (String) docObject.get("Description"),
                                  (String) docObject.get("Severity by NIST"),
                                  (String) docObject.get("Severity Level"),
                                  (String) docObject.get("Last Modified"));
            _elasticSearchConnector.saveDocumentToDatabase(id, documentCVE);
        }
        _elasticSearchConnector.close();
    }
}
