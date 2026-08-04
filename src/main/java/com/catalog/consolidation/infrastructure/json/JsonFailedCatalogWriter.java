package com.catalog.consolidation.infrastructure.json;

import com.catalog.consolidation.domain.model.ProductInsertionResult;
import com.catalog.consolidation.domain.model.SellerProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonFailedCatalogWriter {

    private final ObjectMapper objectMapper;

    public JsonFailedCatalogWriter() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void write(Path outputPath, List<ProductInsertionResult> failures) throws IOException {
        List<FailedSellerProductOutputJson> outputs = new ArrayList<>();
        for (ProductInsertionResult failure : failures) {
            outputs.add(toOutput(failure));
        }
        objectMapper.writeValue(outputPath.toFile(), outputs);
    }

    private FailedSellerProductOutputJson toOutput(ProductInsertionResult failure) {
        SellerProduct source = failure.failedSellerProduct();
        FailedSellerProductOutputJson json = new FailedSellerProductOutputJson();
        json.setId(source.sellerProductId());
        json.setSellerName(source.seller().getName());
        json.setName(source.sellerProductName());
        json.setBrand(source.sellerBrand());
        json.setCategory(source.sellerCategory());
        json.setErrorMessage(failure.errorMessage());
        return json;
    }
}
