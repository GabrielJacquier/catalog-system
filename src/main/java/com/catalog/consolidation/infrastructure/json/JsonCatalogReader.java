package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.application.dto.SellerProductInput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonCatalogReader {

    private final ObjectMapper objectMapper;

    public JsonCatalogReader() {
        this.objectMapper = new ObjectMapper();
    }

    public List<SellerProductInput> read(Path inputPath) throws IOException {
        return objectMapper.readValue(inputPath.toFile(), new TypeReference<List<SellerProductInput>>() {
        });
    }
}
