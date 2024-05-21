package org.example;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.gson.*;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.CredentialsProvider;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.*;
import org.elasticsearch.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.text.html.parser.Parser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.sleep;

public class Main {
    protected static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws TimeoutException, InterruptedException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("PLease, enter number of pages to parse: ");
        String line = reader.readLine();
        int roundsQuantity = Integer.parseInt(line);
        if (roundsQuantity <= 0) {
            System.out.println("Oops! You entered number under 0! Please restart application and enter positive number ...");
            return;
        }

        WebsiteCrawler crawler = new WebsiteCrawler(logger,"https://nvd.nist.gov",
                "/vuln/search/results?form_type=Basic&results_type=overview&search_type=all&isCpeNameSearch=false");

        ConnectionFactory factory = new ConnectionFactory();
        Connection connection;
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("defaultuser");
        factory.setPassword("passw0rd");
        String queueLinksToParse = "links_to_parse";
        String queueContent = "content_to_put";
        try {
            connection = factory.newConnection();
            logger.info("Successfully created connection with RabbitMQ!");
            ParserWorker t1 = new ParserWorker(logger, connection, queueLinksToParse, queueContent);
            ParserWorker t2 = new ParserWorker(logger, connection, queueLinksToParse, queueContent);
            DatabaseWorker t3 = new DatabaseWorker(logger, connection, queueContent);
            t1.start();
            t2.start();
            t3.start();
            RabbitMQConnector mqConnector = new RabbitMQConnector(connection, queueLinksToParse);

            for (int i = 0; i < roundsQuantity; ++i) {
                for (Object link : crawler.getLinksToParse()) {
                    mqConnector.publishMessageToQueue(link.toString());
                }
            }

            t1.join();
            t2.join();
            t3.join();
            connection.close();
        }
        catch (IOException | TimeoutException e) {
//            System.out.println("Oops! Error occurred in Connection with RabbitMQ: " + e.getMessage());
            logger.error("Oops! Error occured in main thread: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String prettyPrintUsingGson(String uglyJson) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = JsonParser.parseString(uglyJson);
        return gson.toJson(jsonElement);
    }
}