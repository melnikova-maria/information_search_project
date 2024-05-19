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

public class WebsitePageParser {
    private final String _baseURL;
    private final JSONArray CVEs = new JSONArray();


    // constructor
    public WebsitePageParser(String baseURL) {
        _baseURL = baseURL;
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
            System.out.println("Oops! Unpredictable exception occurred while connecting to " + _baseURL + cveURL);
            throw new RuntimeException(e);
        }
        if (httpResponse.statusCode() == 200) {
            Document doc = Jsoup.parse(httpResponse.body());
            JSONObject cve = new JSONObject();
            String cveID = "";
            String description = "";
            String severityCVSS3 = "";
            String lastModified = "";
            cve.put("id", hash);
            try {
                cveID = doc.selectFirst("#vulnDetailTableView > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > h2:nth-child(1) > span:nth-child(2)").text();
                description = doc.selectFirst("div.col-lg-9:nth-child(1) > p:nth-child(3)").text();
                severityCVSS3 = doc.selectFirst("#Cvss3CnaCalculatorAnchor").text();
                // #Cvss3CnaCalculatorAnchor
                lastModified = doc.selectFirst("#vulnDetailTableView > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(2) > div:nth-child(2) > div:nth-child(1) > span:nth-child(12)").text();
            }
            catch (NullPointerException e) {
                System.out.println("Oops! Error while parsing page of CVE " + hash);
            }
            cve.put("CVEid", cveID);
            cve.put("Description", description);
            cve.put("Severity by NIST", severityCVSS3);
            cve.put("Last Modified", lastModified);
            return cve;
        }
        else {
            return new JSONObject();
        }
    }
}