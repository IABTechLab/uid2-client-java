package com.uid2.client;

import java.util.Set;

public class Site {
    private final int id;

    private final Set<String> domainOrAppNames;

    public int getId() { return id;}

    public Site(int id, Set<String> domainOrAppNames) {
        this.id = id;
        this.domainOrAppNames = domainOrAppNames;
    }

    public boolean allowDomainOrAppName(String domainOrAppName) {
        // Using streams because HashSet's contains() is case sensitive
        return domainOrAppNames.stream().anyMatch(domainOrAppName::equalsIgnoreCase);
    }
}
