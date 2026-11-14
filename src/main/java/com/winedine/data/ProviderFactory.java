package com.winedine.data;

/** Creator of the Factory Method pattern: subclasses decide which provider to build. */
public abstract class ProviderFactory {
    public abstract RestaurantDataProvider createProvider();
}
