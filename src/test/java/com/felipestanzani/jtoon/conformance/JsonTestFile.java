package com.felipestanzani.jtoon.conformance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

import java.util.List;

public class JsonTestFile {

    public record TestCase (

            String name,
            JsonNode input,
            String expected,
            @JsonProperty("specSection") String spec,
            @JsonIgnore String note,
            @JsonIgnore String options
    ) {}

    public record TestSuite (

            String version,
            String category,
            String description,
            List <TestCase> tests

    ) {}

}
