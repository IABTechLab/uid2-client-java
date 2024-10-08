package com.uid2.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class Site {
    private final int id;

    private final Set<String> domainOrAppNames;

    public boolean allowDomainOrAppName(String domainOrAppName) {
        // Using streams because HashSet's contains() is case sensitive
        return domainOrAppNames.stream().anyMatch(domainOrAppName::equalsIgnoreCase);
    }
}
