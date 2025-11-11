package com.felipestanzani.jtoon.conformance.model;

import java.util.List;

public record ExpectedDecodeResult(List<String> tags,
                                   List<Object> items) {
}
