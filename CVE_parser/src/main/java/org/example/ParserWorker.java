package org.example;
import com.rabbitmq.client.Connection;

import java.util.Objects;
import org.json.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParserWorker extends Thread {
    private RabbitMQConnector _rabbitMQConnectorLinks;
    private RabbitMQConnector _rabbitMQConnectorContent;
    private Logger _logger;
    ParserWorker(Logger logger, Connection rabbitConnection, String queueLinksToParse, String queueContent) {
        _logger = logger;
        _rabbitMQConnectorLinks = new RabbitMQConnector(rabbitConnection, queueLinksToParse);
        _rabbitMQConnectorContent = new RabbitMQConnector(rabbitConnection, queueContent);
    }

    public void run() {
        while (true) {
            String receivedMessage = _rabbitMQConnectorLinks.getMessageFromQueue();
            if (Objects.equals(receivedMessage, "")) {
                try {
//                    System.out.println("Thread #" + this.getId() + ": Empty queue ...");
                    _logger.info("Thread #" + this.getId() + ": Empty queue ...");
                    sleep(10000);
                    receivedMessage = _rabbitMQConnectorLinks.getMessageFromQueue();
                    if (Objects.equals(receivedMessage, "")) {
                        break;
                    }
                }
                catch (InterruptedException e) {
//                    System.out.println("Error occured in ParserWorker trying to sleep: " + e.getMessage());
                    _logger.error("Error occured in ParserWorker trying to sleep: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

//            System.out.println("Thread #" + this.getId() + ": Basic.Get from links_to_parse queue: " + receivedMessage);
            _logger.info("Thread #" + this.getId() + ": Basic.Get from links_to_parse queue: " + receivedMessage);
            JSONObject idObject;
            try {
                idObject = new JSONObject(receivedMessage);
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
            WebsitePageParser parser = new WebsitePageParser(_logger, "https://nvd.nist.gov");
            JSONObject content = parser.parse_CVE((String)idObject.get("link"), (String)idObject.get("hash"));
            if (!content.isEmpty()) {
                _rabbitMQConnectorContent.publishMessageToQueue(content.toString());
                _logger.info("Thread #" + this.getId() + ": Basic.Publish to content_to_put queue: " + content);
            }
        }
    }
}
