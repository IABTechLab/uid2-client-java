package com.uid2.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public class Site {
    private final int id;

    private final Set<String> domainOrAppNames;

    public Site(int id, List<String> domainOrAppNames) {
        this.id = id;
        this.domainOrAppNames = new HashSet<>(domainOrAppNames);
    }

    public boolean allowDomainOrAppName(String domainOrAppName) {
        // Using streams because HashSet's contains() is case sensitive
        return domainOrAppNames.stream().anyMatch(domainOrAppName::equalsIgnoreCase);
    }
}
