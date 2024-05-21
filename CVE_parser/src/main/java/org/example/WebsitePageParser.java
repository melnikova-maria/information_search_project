package org.example;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;

public class WebsitePageParser {
    private final String _baseURL;
    private final JSONArray CVEs = new JSONArray();
    private Logger _logger;
    private ElasticSearchConnector _elasticSearchConnector;


    // constructor
    public WebsitePageParser(Logger logger, String baseURL) {
        _logger = logger;
        _baseURL = baseURL;
        _elasticSearchConnector = new ElasticSearchConnector(logger);
    }


    // methods
    public JSONArray getCVEs() {
        return this.CVEs;
    }

    public JSONObject parse_CVE(String cveURL, String hash) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(_baseURL + cveURL)).build();
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
//            System.out.println("Oops! Unpredictable exception occurred while connecting to " + _baseURL + cveURL);
            _logger.error("Oops! Error occured while connecting to " + _baseURL + cveURL + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
        if (httpResponse.statusCode() == 200) {
            _logger.info("Successfully conected and downloaded document from " + _baseURL + cveURL);
            Document doc = Jsoup.parse(httpResponse.body());


            String lastModified = "";
            try {
                lastModified = doc.selectFirst("#vulnDetailTableView > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(2) > div:nth-child(2) > div:nth-child(1) > span:nth-child(12)").text();
            }
            catch (NullPointerException e) {
                _logger.warn("Oops! Error while parsing page (only lastModified) of CVE " + hash);
            }
            if (!lastModified.isEmpty()) {
                CVE tmp = _elasticSearchConnector.getDocumentFromDatabase(hash);
                _elasticSearchConnector.close();
                if (Objects.equals((String) tmp.getMap().get("Last Modified"), lastModified)) {
                    return new JSONObject();
                }
            }


            JSONObject cve = new JSONObject();
            String cveID = "";
            String description = "";
            String severityCVSS3 = "";
            String severityLevel = "";
            cve.put("id", hash);
            try {
                cveID = doc.selectFirst("#vulnDetailTableView > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > h2:nth-child(1) > span:nth-child(2)").text();
                description = doc.selectFirst("div.col-lg-9:nth-child(1) > p:nth-child(3)").text();
                severityCVSS3 = doc.selectFirst("#Cvss3CnaCalculatorAnchor").text();
                // #Cvss3CnaCalculatorAnchor
//                lastModified = doc.selectFirst("#vulnDetailTableView > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(2) > div:nth-child(2) > div:nth-child(1) > span:nth-child(12)").text();
            }
            catch (NullPointerException e) {
//                System.out.println("Oops! Error while parsing page of CVE " + hash);
                _logger.warn("Oops! Error while parsing page of CVE " + hash);
            }
            cve.put("CVEid", cveID);
            cve.put("Description", description);
            if (!severityCVSS3.isEmpty()) {
                String[] tmpString = severityCVSS3.split(" ");
                severityCVSS3 = tmpString[0];
                severityLevel = tmpString[1];
            }
            cve.put("Severity by NIST", severityCVSS3);
            cve.put("Severity Level", severityLevel);
            cve.put("Last Modified", lastModified);
            return cve;
        }
        else {
            return new JSONObject();
        }
    }
}