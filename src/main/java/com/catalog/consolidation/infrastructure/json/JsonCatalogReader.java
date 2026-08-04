package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.domain.model.SellerProduct;
import com.catalog.consolidation.infrastructure.factory.SellerProductFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonCatalogReader {

    private final ObjectMapper objectMapper;
    private final SellerProductFactory sellerProductFactory;

    public JsonCatalogReader() {
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.sellerProductFactory = new SellerProductFactory();
    }

    public List<SellerProduct> read(Path inputPath) throws IOException {
        List<SellerProductInputJson> rawInputs = objectMapper.readValue(
                inputPath.toFile(),
                new TypeReference<List<SellerProductInputJson>>() {
                }
        );
        return sellerProductFactory.createAll(rawInputs);
    }
}
