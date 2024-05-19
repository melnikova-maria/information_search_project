package org.example;
import com.rabbitmq.client.Connection;

import java.util.Objects;
import org.json.*;

public class ParserWorker extends Thread {
    private RabbitMQConnector _rabbitMQConnectorLinks;
    private RabbitMQConnector _rabbitMQConnectorContent;
    ParserWorker(Connection rabbitConnection, String queueLinksToParse, String queueContent) {
        _rabbitMQConnectorLinks = new RabbitMQConnector(rabbitConnection, queueLinksToParse);
        _rabbitMQConnectorContent = new RabbitMQConnector(rabbitConnection, queueContent);
    }

    public void run() {
        while (true) {
            String receivedMessage = _rabbitMQConnectorLinks.getMessageFromQueue();
            if (Objects.equals(receivedMessage, "")) {
                try {
                    System.out.println("Thread #" + this.getId() + ": Empty queue ...");
                    sleep(10000);
                    receivedMessage = _rabbitMQConnectorLinks.getMessageFromQueue();
                    if (Objects.equals(receivedMessage, "")) {
                        break;
                    }
                }
                catch (InterruptedException e) {
                    System.out.println("Error occured in ParserWorker trying to sleep: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            System.out.println("Thread #" + this.getId() + ": Basic.Get from links_to_parse queue: " + receivedMessage);
            JSONObject idObject;
            try {
                idObject = new JSONObject(receivedMessage);
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
            WebsitePageParser parser = new WebsitePageParser("https://nvd.nist.gov");
            JSONObject content = parser.parse_CVE((String)idObject.get("link"), (String)idObject.get("hash"));
            _rabbitMQConnectorContent.publishMessageToQueue(content.toString());
            System.out.println("Thread #" + this.getId() + ": Basic.Publish to content_to_put queue: " + content);
        }
    }
}
