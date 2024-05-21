package org.example;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

public class ElasticSearchConnector {
    private Logger _logger;
    private RestClient _restClient;
    private ElasticsearchTransport _transport;
    private ElasticsearchClient _client;
    private String _indexName = "cve_db";

    ElasticSearchConnector(Logger logger) {
        _logger = logger;
        _restClient = RestClient
                .builder(HttpHost.create("http://localhost:9200"))
                .build();
        _transport = new RestClientTransport(_restClient, new JacksonJsonpMapper());
        _client = new ElasticsearchClient(_transport);
    }

    public void saveDocumentToDatabase(String id, CVE document) {
        try {
            IndexResponse response = _client.index(i -> i
                    .index(_indexName)
                    .id(id)
                    .document(document));
            if (response.result().toString().equals("Created") || response.result().toString().equals("Updated")) {
                _logger.info("[Result: " + response.result() + " id=" + id + "] Successfully saved document " + document.getMap() + " to database");
            }
        }
        catch (IOException e) {
            _logger.error("Oops! Error occured while indexing document " + document + "to database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public CVE getDocumentFromDatabase(String id) {
        try {
            GetResponse<CVE> response = _client.get(g -> g
                    .index(_indexName)
                    .id(id), CVE.class);
            if (response.found()) {
                CVE document = response.source();
                _logger.info("[Result: Found id=" + id + "] Successfully got document " + document.getMap() + " from database");
                return document;
            }
            else {
                _logger.info("[Result: NotFound] Unable to find document with id=" + id);
                return new CVE("", "", "", "", "");
            }
        }
        catch (IOException e) {
            _logger.error("Oops! Error occured while getting document with id=" + id + "from database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleteDocumentFromDatabase(String id) {
        try {
            DeleteResponse response = _client.delete(i -> i
                    .index(_indexName)
                    .id(id));
            if (response.result().toString().equals("Deleted")) {
                _logger.info("[Result: " + response.result() + " id=" + id + "] Successfully deleted document from database");
            }
        } catch (IOException e) {
            _logger.error("Oops! Error occured while deleting document with id=" + id + "from database: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            _transport.close();
            _restClient.close();
        }
        catch (IOException e) {
            _logger.error("Oops! Error occured while trying to close ElasticSearchClient: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
