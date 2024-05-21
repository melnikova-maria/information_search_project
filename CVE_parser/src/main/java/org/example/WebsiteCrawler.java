package org.example;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebsiteCrawler {
    private final String _baseURL;
    private String _searchPath;
    private final JSONArray _CVEs = new JSONArray();
    private Logger _logger;

    public WebsiteCrawler(Logger logger, String baseURL, String searchPath) {
        _logger = logger;
        _baseURL = baseURL;
        _searchPath = searchPath;
    }

    public JSONArray getCVEs() {
        return this._CVEs;
    }

    public JSONArray getLinksToParse() {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(_baseURL + _searchPath)).build();
        HttpResponse<String> httpResponse;
        try {
            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }
        catch (IOException | InterruptedException e) {
            System.out.println("Oops! Unpredictable exception occurred while connecting to " + _baseURL + _searchPath);
            throw new RuntimeException(e);
        }
        if (httpResponse.statusCode() == 200) {
            _logger.info("Successfully conected and downloaded document from " + _baseURL + _searchPath);
            Document doc = Jsoup.parse(httpResponse.body());
            String cssSelector = ".table > tbody:nth-child(2)";
            Element vulnerabilities = doc.selectFirst(cssSelector); // ".table > tbody:nth-child(2)"
            JSONArray CVEs = new JSONArray();
            if (vulnerabilities != null) {
                for (var vulnerability : vulnerabilities.children()) {
                    var vulnerabilityLink = vulnerability.getElementsByTag("a");
                    JSONObject link = new JSONObject();
                    MessageDigest md5;
                    try {
                        md5 = MessageDigest.getInstance("MD5");
                        md5.update(vulnerabilityLink.get(0).text().getBytes(StandardCharsets.UTF_8));
                    }
                    catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                    link.put("hash", Hex.encodeHexString(md5.digest()));
                    link.put("link", vulnerabilityLink.get(0).attr("href"));
                    CVEs.put(link);
                }
            }
            String linkToNextPage = getLinkToNextPage(doc);
            if (!linkToNextPage.isEmpty()) {
                _searchPath = linkToNextPage;
            }
            return CVEs;
        }
        else {
//            System.out.println("Error while connecting to website " + _baseURL + _searchPath + "! Status code: " +
//                                httpResponse.statusCode());
            _logger.warn("Error while connecting to website " + _baseURL + _searchPath + "! Status code: " +
                    httpResponse.statusCode());
            return new JSONArray();
        }
    }

    private String getLinkToNextPage(Document doc) {
        String cssSelector = "div.col-sm-12:nth-child(3) > div:nth-child(1) > div:nth-child(1) > nav:nth-child(1) > ul:nth-child(1) > li:nth-child(11) > a:nth-child(1)";
        Element nextPageButton = doc.selectFirst(cssSelector);
        String link = null;
        if (nextPageButton != null) {
            return nextPageButton.attr("href");
        }
        else {
            return "";
        }
    }
}
