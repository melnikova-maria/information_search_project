package org.example;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CVE {
    @JsonProperty("CVEid")
    private String CVEid = "";
    @JsonProperty("description")
    private String description = "";
    @JsonProperty("severity")
    private String severity = "";
    @JsonProperty("severityLevel")
    private String severityLevel = "";
    @JsonProperty("lastModified")
    private String lastModified = "";

    @JsonCreator
    CVE(@JsonProperty("CVEid") String CVEid, @JsonProperty("description") String description, @JsonProperty("severity") String severity, @JsonProperty("severityLevel") String severityLevel, @JsonProperty("lastModified") String lastModified) {
        super();
        this.CVEid = CVEid;
        this.description = description;
        this.severity = severity;
        this.severityLevel = severityLevel;
        this.lastModified = lastModified;
    }

//    public void setFields(String CVEid, String description, String severity, String severityLevel, String lastModified) {
//        _CVEid = CVEid;
//        _description = description;
//        _severity = severity;
//        _severityLevel = severityLevel;
//        _lastModified = lastModified;
//    }


    public Map<String, Object> getMap() {
        Map<String, Object> document = new HashMap<String, Object>();
        document.put("CVEid", this.CVEid);
        document.put("Description", this.description);
        document.put("Severity by NIST", this.severity);
        document.put("Severity Level", this.severityLevel);
        document.put("Last Modified", this.lastModified);
        return  document;
    }

    public boolean isNull() {
        return Objects.equals(CVEid, "") & Objects.equals(description, "") & Objects.equals(severity, "") & Objects.equals(severityLevel, "") & Objects.equals(lastModified, "");
    }
}
