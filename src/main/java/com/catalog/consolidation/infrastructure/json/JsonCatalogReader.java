package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.domain.model.SellerProductInput;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonCatalogReader {

    private final ObjectMapper objectMapper;
    private final SellerProductInputFactory sellerProductInputFactory;

    public JsonCatalogReader() {
        this.objectMapper = new ObjectMapper();
        this.sellerProductInputFactory = new SellerProductInputFactory();
    }

    public List<SellerProductInput> read(Path inputPath) throws IOException {
        List<SellerProductInputJson> rawInputs = objectMapper.readValue(
                inputPath.toFile(),
                new TypeReference<List<SellerProductInputJson>>() {
                }
        );
        return rawInputs.stream()
                .map(sellerProductInputFactory::create)
                .toList();
    }
}
