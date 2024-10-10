package com.uid2.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashSet;

public class SiteTests {

    private static Site site;

    @BeforeAll
    public static void setup() {
        site = new Site(101, new HashSet<>(Arrays.asList(
         "example.com",
         "example.org",
         "com.123.Game.App.android",
         "123456789"
        )));
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "example.com",
            "example.org",
            "com.123.Game.App.android",
            "123456789",
            "EXAMPLE.COM",
            "com.123.game.app.android",
        })
    public void testAllowDomainOrAppNameSuccess(String domainOrAppName) {
        Assertions.assertTrue(site.allowDomainOrAppName(domainOrAppName));
    }

    @ParameterizedTest
    @CsvSource({
            "*",
            "example",
            "example*",
            "example.net"
    })
    public void testAllowDomainOrAppNameFailure(String domainOrAppName) {
        Assertions.assertFalse(site.allowDomainOrAppName(domainOrAppName));
    }
}
