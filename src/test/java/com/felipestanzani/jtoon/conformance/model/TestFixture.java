package com.felipestanzani.jtoon.conformance.model;

import java.util.List;

public record TestFixture(String version,
                          String category,
                          String description,
                          List<JsonTestCase> tests) {
}
