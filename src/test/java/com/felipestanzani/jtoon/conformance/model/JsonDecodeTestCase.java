package com.felipestanzani.jtoon.conformance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.JsonNode;

public record JsonDecodeTestCase(String name,
                                 JsonNode input,
                                 ExpectedDecodeResult expected,
                                 String specSection,
                                 @JsonIgnore String note,
                                 JsonDecodeTestOptions options) {
}
