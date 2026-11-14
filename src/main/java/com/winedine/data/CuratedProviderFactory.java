package com.winedine.data;

import java.nio.file.Path;

/** Concrete Creator: builds a provider backed by the curated JSON dataset. */
public class CuratedProviderFactory extends ProviderFactory {
    private final Path datasetFile;

    public CuratedProviderFactory(Path datasetFile) {
        this.datasetFile = datasetFile;
    }

    @Override
    public RestaurantDataProvider createProvider() {
        return CuratedTorontoDataProvider.fromFile(datasetFile);
    }
}
