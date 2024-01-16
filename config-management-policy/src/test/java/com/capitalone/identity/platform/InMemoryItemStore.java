package com.capitalone.identity.platform;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.repository.CommonItemStore;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryItemStore extends CommonItemStore {

    private final List<ConfigStoreItem> items = new ArrayList<>();

    public InMemoryItemStore(Entity... entities) {
        Arrays.stream(entities).forEach(entity -> items.addAll(entity.getItems()));
    }

    @Override
    public Set<ConfigStoreItemInfo> getAllItemInfo() {
        return items.stream().map(item -> item.info).collect(Collectors.toSet());
    }

    @Override
    public ConfigStoreItem getItem(ConfigStoreItemInfo info) {
        return items.stream().filter(entity -> entity.info.equals(info)).findFirst().get();
    }

    @Override
    public Flux<ConfigStoreItemInfo> getStoredItemInfo() {
        return Flux.fromStream(items.stream()).map(item -> item.info);
    }

    @Override
    public Optional<ConfigStoreItemInfo> getSingleStoredItemInfo(String key) {
        return items.stream()
                .map(item -> item.info)
                .filter(itemInfo -> itemInfo.getName().equals(key))
                .findFirst();
    }

    public void addEntity(Entity entity) {
        entity.getItems().forEach(this::addItem);
    }

    public void removeEntity(Entity entity) {
        entity.getItems().forEach(items::remove);
    }


    public void setItem(ConfigStoreItem item) {
        items.replaceAll(existing -> {
            if (existing.info.getName().equals(item.info.getName())) {
                return item;
            } else {
                return existing;
            }
        });
    }

    public void addItem(ConfigStoreItem... item) {
        items.addAll(Arrays.asList(item));
    }

    public void removeItem(String name) {
        items.removeIf(existing -> existing.info.getName().equals(name));
    }
}
