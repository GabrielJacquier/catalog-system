package com.catalog.consolidation.domain.ports.in;

import com.catalog.consolidation.application.dto.SellerProductInput;
import com.catalog.consolidation.domain.model.ImportCatalogResult;

import java.util.List;

public interface ImportCatalogUseCase {

    ImportCatalogResult execute(List<SellerProductInput> inputs);
}
