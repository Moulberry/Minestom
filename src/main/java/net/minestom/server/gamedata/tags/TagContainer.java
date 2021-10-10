package net.minestom.server.gamedata.tags;

import net.minestom.server.utils.NamespaceID;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TagContainer {

    private static final Map<Tag.BasicType, Map<NamespaceID, TagContainer>> CONTAINER_MAP = new ConcurrentHashMap<>();

    public static TagContainer byID(Tag.BasicType type, NamespaceID id) {
        return CONTAINER_MAP.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).computeIfAbsent(id, k -> new TagContainer());
    }

    private final Set<NamespaceID> tags = new HashSet<>();

    public boolean contains(NamespaceID value) {
        return tags.contains(value);
    }

    /*package-private*/ void add(NamespaceID value) {
        tags.add(value);
    }

}
