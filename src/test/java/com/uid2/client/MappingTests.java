package com.uid2.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MappingTests {
    
    @Test
    public void mappingTest() {
        String apiKey = System.getenv("UID2_API_KEY");
        String secret = System.getenv("UID2_API_SECRET");

        UID2Client client = new UID2Client("https://prod.uidapi.com", apiKey, secret, IdentityScope.UID2);
        MappingResponse response;
        try {
            response = client.mapIdentity(new MappingRequest().email("fake.email@uid2implementation.com"));
        }catch(Exception e){
            fail(e.getMessage());
            return;
        }
        assertNotNull(response);
        assertTrue(response.getStatus().equals("success"));
    }
}
