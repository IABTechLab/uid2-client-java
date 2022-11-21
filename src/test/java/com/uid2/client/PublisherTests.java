package com.uid2.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class PublisherTests {
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private final String UID2_SECRET_KEY = "ioG3wKxAokmp+rERx6A4kM/13qhyolUXIu14WN16Spo=";
    private final PublisherUid2Helper publisherUid2Helper = new PublisherUid2Helper(UID2_SECRET_KEY);
    private final byte[] nonce = hexStringToByteArray("312fe5aa08b2a049");

    private EnvelopeV2 createEnvelopeFromIdentityInput(TokenGenerateInput tokenGenerateInput) {
        final Instant instant = Instant.ofEpochMilli(1667885597644L);
        final byte[] iv = hexStringToByteArray("cc3ccaca9889eab3800e787e");

        return publisherUid2Helper.createEnvelopeImpl(tokenGenerateInput, nonce, instant, iv);
    }

    private EnvelopeV2 createEnvelopeForTokenGenerateRequest(IdentityType identityType, String value, String tcString, boolean hash) {
        TokenGenerateInput tokenGenerateInput;
        if (identityType == IdentityType.Email) {
            tokenGenerateInput = TokenGenerateInput.fromEmail(value).withTransparencyAndConsentString(tcString);
        }
        else {
            tokenGenerateInput = TokenGenerateInput.fromPhone(value).withTransparencyAndConsentString(tcString);
        }

        if (!hash)
            tokenGenerateInput.doNotHash();

        final EnvelopeV2 envelope = createEnvelopeFromIdentityInput(tokenGenerateInput);
        assertEquals(nonce, envelope.getNonce());
        return envelope;
    }

    private String createEnvelopeString(IdentityType identityType, String value, String tcString, boolean hash) {
        return createEnvelopeForTokenGenerateRequest(identityType, value, tcString, hash).getEnvelope();
    }

    @Test
    public void unhashedEmail() {
        final String envelope = createEnvelopeString(IdentityType.Email, "test@example.com", null, false);

        //See encrypt_request.py to see how all the expected envelopes were derived
        assertEquals("Acw8ysqYieqzgA54fifdV1TB6V+da8p/AFc8Ju/IYrD77pL7JYMJj8YqD9EsrG3d2d2j0H7kZjH41YsNLpFVCH+oce28z9L9ug==", envelope);
    }

    @Test
    public void unhashedPhone() {
        final String envelope = createEnvelopeString(IdentityType.Phone, "+12345678901", null, false);
        assertEquals("Acw8ysqYieqzgA54fifdV1TB6V+da8p/AFc8Ju/IYqX+4JXyJYMJ0JJrSKV84juIkIH33GCmx+7RM2pSX9P4nAN0pn2g", envelope);
    }

    @Test
    public void hashedEmail() { //https://github.com/UnifiedID2/uid2docs/blob/main/api/README.md#email-address-hash-encoding
        final String envelope = createEnvelopeString(IdentityType.Email, "user@example.com", null, true);
        final String expectedEnvelope = "Acw8ysqYieqzgA54fifdV1TB6V+da8p/AFc8Ju/IYrD77pL7WNFKiMt7QbM9mWHZwOWPyVTqSnDVNzhXRdcmIkV0ODOdll2inDo9iIZQ5oV2NvSUSWG5j6ACiXKH0JxiTPzs716eIH7P1Q==";

        assertEquals(expectedEnvelope, envelope);
        final String envelopeWhenUsingUnnormalizedEmail = createEnvelopeString(IdentityType.Email, "   USER@EXAMPLE.COM   ", null, true);
        assertEquals(expectedEnvelope, envelopeWhenUsingUnnormalizedEmail);
    }

    @Test
    public void hashedGmail() { //https://github.com/UnifiedID2/uid2docs/blob/main/api/README.md#email-address-hash-encoding
        final String envelope = createEnvelopeString(IdentityType.Email, "janedoe@gmail.com", null, true);
        final String expectedEnvelope = "Acw8ysqYieqzgA54fifdV1TB6V+da8p/AFc8Ju/IYrD77pL7WNFKiMt7QbN4vErK69qumy3EXliuNSZ0a/4mTht7DyqMjF20oUECkYNZvtJhGo+wfXSDj6ACEfrViY4lnt1KEUjC4JlDaQ==";

        assertEquals(expectedEnvelope, envelope);
        final String envelopeWhenUsingUnnormalizedEmail = createEnvelopeString(IdentityType.Email, "   jane.doe+home@gmail.com   ", null, true);
        assertEquals(expectedEnvelope, envelopeWhenUsingUnnormalizedEmail);
    }


    @Test
    public void hashedEmailWithTcString() {
        final String tcString = "CPhJRpMPhJRpMABAMBFRACBoALAAAEJAAIYgAKwAQAKgArABAAqAAA";
        final String envelope = createEnvelopeString(IdentityType.Email, "user@example.com", tcString, true);
        final String expectedEnvelope = "Acw8ysqYieqzgA54fifdV1TB6V+da8p/AFc8Ju/IYrD77pL7WNFKiMt7QbM9mWHZwOWPyVTqSnDVNzhXRdcmIkV0ODOdll2inDo9iIZQ5oV2NvSUSWG5j6BTr3kl2OMGYCCQWOw+fx5/D8+7+J2+xJVUXN8K2yBbjpYqiOpOVVTktDh7TxkPwEGuYR5onLsXKFzjg7rdh0Mfu+emB6JrjFw/en304ydJgpPefDK/4/cohZQdgpjaqQ==";
        assertEquals(expectedEnvelope, envelope);
    }

    @Test
    public void hashedPhone() { //https://github.com/UnifiedID2/uid2docs/blob/main/api/README.md#phone-number-hash-encoding
        final String envelope = createEnvelopeString(IdentityType.Phone, "+12345678901", null, true);
        assertEquals("Acw8ysqYieqzgA54fifdV1TB6V+da8p/AFc8Ju/IYqX+4JXyWNFKiMt7QbMMm27H3fmEq2zPRUnXdFpLTdVdc0RNWxOruBaarHYQksdqpbkVFa+walbcj6AClILBJjbDMl1IoeqJVG2fyw==", envelope);
    }

    @Test
    public void decryptGenerateResponse() {
        final EnvelopeV2 envelope = createEnvelopeForTokenGenerateRequest(IdentityType.Email, "test@example.com", null, true);
        final String response = "RUlLWcXlSdRhpf82J8iDqeVFbZQx2MCcVaTjhf1gVPpSS3SfOMzfCB8tW8VHzY15b5ZN6FKlZ/3FjHi54H7uyyVp2BS7puWgzGiv5jxhp0hpP2fL1KdmRs8UpX2XgmdEkfeYjTzFHFhq8SfocV2MIK8Gu4QtCtQdMg1Vk9fyeIOwgRUODkHnFZN8rcZIPDdqkZRS6Rftgdizss1FSkInYNpT2Cq07iIHVREmuJdZIinK0qDjIXcXUI3oE8WzdEtd/e4/8xEEZ4HRYuhNF640hX/O+ivBgygW/nqctAr6r9zYu65zs45UYClSUNaWsvE34IWq4q61T99z8etf81lS8E/PWrskOnH968azv0/FVFlLiKks0a1W1iDqq0/pps8LBq/Eo8ByadXQITDFwT6LN9qQE1lW4bGQrY3ZgALZQHsW1eAdaedaYwGOhp81xLlrq9aRaKXdiLuil/lgUNQX51yW8U8izYIq8cue0cUMg/TDV+5YtcC5eZiQskTjMFkInjfDI8k5F5KGnVRfsj8w+mcXv0O9E7djTy6Mmt4OQ1MxDFYuzptf3GnC7GK8VHpxHc84rOZADzBcgVOfYz4x8jA3fPVMkE08fMMpzH8OpH0riQHjk95rAF3KcOss5IvmPsUy+ZgCwOloWH+dwgYy8CDTR3RYPA+zOmEk4bGwvy6hQ5JJwah43ba73IrWZpiANfyCFMr7PY1HFvp7fC+i8w8zLTxMcLgH0m8zXMJXo6IVJDhuQKM/ViSkS/ONatlQvbtFehZfMwzPeD/mDmy1JAcvAAqmiAl13ULdNrIET9zyqmJVEaDEawVckomeIJoznbBS7WEFelcueYcXBi+Tl7qTlPL8UI4AjrzYp/sfMCvCoBtQnFYQAmChmE5OCpnE0X/uCa9oUK7HrVEqgBLft25w4d1mYtMV02gZ0SB/T2MA3BeSEp6RUKrYO63+/8eYUF4pEuQEjDcdRX6rxs9lpPPLKglnn9VteJ61BjsLphwYxqwgdlHZhS6xSvqYveHwwe94QyOYeyiJiEqJ+QooUciWSyRXBfJdcE4inuzO4T/wZauocgw8ZiKJMAfPzXjSLdVNM/sosd0vIhgXvlMpj+a+B4lbsfcVjRGrwU1VPZvHlubVCxJGieW7hQ8ugzpSYibMxxvhFBCyzjKHYodllp/umuq2KmoMdbB2lOsVFtZ3ufYs7cqSTcZIgPFUXlEJs/hUFvTnUCfZ1vC+YNF4d7ngVvCdMYsOCLQNAZeMaVoSG8m/cvY2FT0MGAgLhuu+IGWMcGVYPUrjP6Hy9gpq/wEkRnnfDAo+ENmMQsYcdzg+VNREZ4p4";

        IdentityTokens identity = publisherUid2Helper.createIdentityfromTokenGenerateResponse(response, envelope);

        /* The expected IdentityTokens fields were derived using decrypt_response.py, which had the following output:
        {
            "body": {
            "advertising_token": "AgAAAAN6QZRCFTau+sfOlMMUY2ftElFMq2TCrcu1EAaD9WmEfoT2BWm2ZKz1tumbT00tWLffRDQ/9POXfA0O/Ljszn7FLtG5EzTBM3HYs4f5irkqeEvu38DhVCxUEpI+gZZZkynRap1oYx6AmC/ip3rk+7pmqa3r3saDs1mPRSSTm+Nh6A==",
                    "user_token": "AgAAAAL6aleYI4BubI5ZXMBshqmMEfCkbCJF4fLeg1sdI0BTLzj9sXsSISjkG0lMC743diC2NVy3ElkbO1lLysd+Lm6alkqevPrcuWDisQ1939YdoH6LqpwBH3FNSE4/xa3Q+94=",
                    "refresh_token": "AAAAAARomrP3NjjH+8mt5djfTHbmRZXjOMnAN8WpjJoe30AhUCvYksO/xoDSj77GzWv4M99DhnPl2cVco8CZFTcE10nauXI4Barr890ILnH0IIacOei5Zjwh6DycFkoXkAAuHY1zjmxb7niGLfSP2RctWkZdRVGWQv/UW/grw6+paU9bnKEWPzVvLwwdW2NgjDKu+szE6A+b5hkY+I3voKoaz8/kLDmX8ddJGLy/YOh/LIveBspSAvEg+v89OuUCwAqm8L3Rt8PxDzDnt0U4Na+AUawvvfsIhmsn/zMpRRks6GHhIAB/EQUHID8TedU8Hv1WFRsiraG9Dfn1Kc5/uYnDJhEagWc+7RgTGT+U5GqI6+afrAl5091eBLbmvXnXn9ts",
                    "identity_expires": 1668059799628,
                    "refresh_expires": 1668142599628,
                    "refresh_from": 1668056202628,
                    "refresh_response_key": "P941vVeuyjaDRVnFQ8DPd0AZnW4bPeiJPXER2K9QXcU="
        },
            "status": "success"
        } */

        assertEquals("AgAAAAN6QZRCFTau+sfOlMMUY2ftElFMq2TCrcu1EAaD9WmEfoT2BWm2ZKz1tumbT00tWLffRDQ/9POXfA0O/Ljszn7FLtG5EzTBM3HYs4f5irkqeEvu38DhVCxUEpI+gZZZkynRap1oYx6AmC/ip3rk+7pmqa3r3saDs1mPRSSTm+Nh6A==",
                identity.getAdvertisingToken());

        assertEquals("AAAAAARomrP3NjjH+8mt5djfTHbmRZXjOMnAN8WpjJoe30AhUCvYksO/xoDSj77GzWv4M99DhnPl2cVco8CZFTcE10nauXI4Barr890ILnH0IIacOei5Zjwh6DycFkoXkAAuHY1zjmxb7niGLfSP2RctWkZdRVGWQv/UW/grw6+paU9bnKEWPzVvLwwdW2NgjDKu+szE6A+b5hkY+I3voKoaz8/kLDmX8ddJGLy/YOh/LIveBspSAvEg+v89OuUCwAqm8L3Rt8PxDzDnt0U4Na+AUawvvfsIhmsn/zMpRRks6GHhIAB/EQUHID8TedU8Hv1WFRsiraG9Dfn1Kc5/uYnDJhEagWc+7RgTGT+U5GqI6+afrAl5091eBLbmvXnXn9ts",
                identity.getRefreshToken());

        assertEquals("P941vVeuyjaDRVnFQ8DPd0AZnW4bPeiJPXER2K9QXcU=", identity.getRefreshResponseKey());

        assertEquals(Instant.ofEpochMilli(1668059799628L), identity.getIdentityExpires());

        assertEquals(Instant.ofEpochMilli(1668142599628L), identity.getRefreshExpires());

        assertEquals(Instant.ofEpochMilli(1668056202628L), identity.getRefreshFrom());

        assertEquals(expectedDecryptedJsonForTokenGenerateResponse, identity.getJsonString());

        assertFalse(identity.isRefreshable());
        assertTrue(identity.isDueForRefresh());
    }

    private final static String expectedDecryptedJsonForTokenGenerateResponse = "{\"advertising_token\":\"AgAAAAN6QZRCFTau+sfOlMMUY2ftElFMq2TCrcu1EAaD9WmEfoT2BWm2ZKz1tumbT00tWLffRDQ/9POXfA0O/Ljszn7FLtG5EzTBM3HYs4f5irkqeEvu38DhVCxUEpI+gZZZkynRap1oYx6AmC/ip3rk+7pmqa3r3saDs1mPRSSTm+Nh6A==\",\"user_token\":\"AgAAAAL6aleYI4BubI5ZXMBshqmMEfCkbCJF4fLeg1sdI0BTLzj9sXsSISjkG0lMC743diC2NVy3ElkbO1lLysd+Lm6alkqevPrcuWDisQ1939YdoH6LqpwBH3FNSE4/xa3Q+94=\",\"refresh_token\":\"AAAAAARomrP3NjjH+8mt5djfTHbmRZXjOMnAN8WpjJoe30AhUCvYksO/xoDSj77GzWv4M99DhnPl2cVco8CZFTcE10nauXI4Barr890ILnH0IIacOei5Zjwh6DycFkoXkAAuHY1zjmxb7niGLfSP2RctWkZdRVGWQv/UW/grw6+paU9bnKEWPzVvLwwdW2NgjDKu+szE6A+b5hkY+I3voKoaz8/kLDmX8ddJGLy/YOh/LIveBspSAvEg+v89OuUCwAqm8L3Rt8PxDzDnt0U4Na+AUawvvfsIhmsn/zMpRRks6GHhIAB/EQUHID8TedU8Hv1WFRsiraG9Dfn1Kc5/uYnDJhEagWc+7RgTGT+U5GqI6+afrAl5091eBLbmvXnXn9ts\",\"identity_expires\":1668059799628,\"refresh_expires\":1668142599628,\"refresh_from\":1668056202628,\"refresh_response_key\":\"P941vVeuyjaDRVnFQ8DPd0AZnW4bPeiJPXER2K9QXcU=\"}";


    @Test
    public void decryptRefreshResponse() {
        IdentityTokens currentIdentity = IdentityTokens.fromJsonString(expectedDecryptedJsonForTokenGenerateResponse);

        // encryptedResponse was derived from the following command line:
        // echo {refresh token from decryptGenerateResponse's identity.getRefreshToken()}| curl -X POST "http://localhost:8180/v2/token/refresh" -H "Authorization: Bearer trusted-partner-key" -d @- | .\decrypt_response.py {getRefreshResponseKey above} --is-refresh
        final String encryptedResponse = "suJBe05BsUlmulBse8Kuq6haiirs5O8wokoXQz8rxK047Q60vZQrWGy3+ufTwpIKpoMGLFGvVXpE8ifoN4Z34qPrGaJaVuCmcVTbQ8kkou/RwLp+hjoXaaTBmlqVGGbVrBzFg14KMlZLKKEh7EfABSQeUvbi3TtOzbluHZn0jCri3qJEDnSFP4tXhGjJw5ufB4GiSGOzfuw6rvRrWh+FjpNein8ijVNzA4MQAYMGrZEeKd639hCu5Q4tdkuOZKrkLTUPoeKNmt3moyQtKxztpPa/UCDJ/bOpIiLXI+vLScHG0ekhRq/5eQ7nFk3wUd9mlrA3f/+MyqkaPSjIVYvmcpzC55qh0PXLXkOzhIrLUjaIP8UzU30us+pHL2exdUZrrh4w8Pp7MZkdYzEBvc7ZyeXObNzBJNED62T7GI5UxKAndMoSPxsewttZ2MpAepfRQ9RH97eIX+Bg2KJa5WOOLK5KfVS01oKxXbwcCsGpw5sDAIiobrYdWWn9UMUhbFpbuqsmSWW4vFZR02RMHzif8N/vJkBLyvxSAlP363QULrEVMOueG91MI1W9BICYvTM06ZHYt5dz2l1GkKu+OSKAaTLvCJAWsCDgowtJcHtl0kYmZBJn/yfcc3a8SQC1dv2el1l3DWG6J8Udrci6rmOzox0MqIWkBfOqEOHFOPpV8RqYTJVYuKEigYAiAvw7RFA7ZSCwJ3nhc6GUD7XoCK96Y/TrLud3gagxRUz4D+OZUidQP01LeVW9XfBi/UGxQC1WcZ/AS7MF3byjXDJRR6MZRbTm4hkLJm1hQzGsiVNBbBR/qCwoaAI9PwvBKoZpujMzH3+HdUOubCw6sOCWfgO4cwn7TTyQAf9KJ1B6Tj+RNWe3HyImcCpfBMl8suMRh8cZX48r/Z3um4w0bNLog7L1VVA4J6G8EXvpDdkE/78mIdL7dNraZdioF/X/vP7efkDTHTLiJsGq7fZxGqqrFt/ENqhsgpq7punMq8k96sDQ7ujVc4LN0M6qPqIYiC7lT4haXtdugFaKBZp80bbV5ZM1s0VH9nw7SgJUA9RZcxYkU5x77kY+i6fUK45bDC0x0j4lPpC027CgzJhGZffZyvRCPzXYDTa9s40JswKLnKf27HwwYCTJNsnIMRBJlUSnU/JqLvzYFIBSEa2SNsD4qlvdoWqo0bZvpDquqz2mH36w3u7j5L/CoTy7dcjORPdC9/nJN/b+b/G5vjE5bcDtfek3Kc6n6ABjRnXYVstK43ghUeczUHzRwUjdX616pphGliP+bTZHtcfr2DBp2N5vMgxf4bMmhseDlzc=";
        final Instant refreshFrom = Instant.ofEpochMilli(1668062281329L);
        IdentityTokens refreshedIdentity = PublisherUid2Helper.createIdentityFromTokenRefreshResponseImpl(encryptedResponse, currentIdentity, refreshFrom); //returns null if user has opted out

        /* the command line above also resulted in the following output:
        {
            "body": {
                "advertising_token": "AgAAAANVKWUKoXWL9OdHdxoEhRlHC9BEJucY44HF3RmJHlXzjEJ/nvySkVFeRbo3W/2Mpe9qi4haYz6psljI6ZwUq8n6jPyAR0WK5LDrTxUPibeaV9aZJoa6gNCKfJ/Z8tZtG/iWUbZkkDPmJiF7LTxqB1TQX0UvtLDbDbewsa+b+PJgEw==",
                "user_token": "AgAAAAIlFxyzWNTuZYFdPbKb6p+Gy+kyYDaa7j8s60uKIWO2WqQuHIdFxviaDjSkweooyYuJ5muZY5o/2vJRUT6OsnYD46WNNVZQxQTsRApV7/VjMOyOLd8gAXLBMQvJvyNG++w=",
                "refresh_token": "AAAAAARS93G4B29nG5kXD5C97S5wGBJcGU3NXBYME+vMqlvvgXG4RuRt+UHG6fe7hiGoK7QtQVL1qj1SdpyuiJQHeM1PfRzk5vjXEv39wedDxQNWAnJsF6I6W6HPFCJADcUd/IEk2FKTq5+al5KwL9woU82WNCCt+n6ut29Bb/F3S/blW2G7/Q2i+tImcwuTTiAZ2uYzgg/t4HfB880rpa28E/cfqJ3vsJHFGiDOUqU9JpNPFByOnxay5PQgkRlMjmea6ZfAwOCyFC69E51B3NZb3+mdx9zaHB4e6hCmk3BLPhSQzV3zOM1IRS4Rq2xO5EuEGEaSU1K3iluXTXBAOoalbjj1qln+BCu0ohA9sqfEOC/hZcmYL0YEkPHLXW2C094g",
                "identity_expires": 1668065878329,
                "refresh_expires": 1668148678329,
                "refresh_from": 1668062281329,
                "refresh_response_key": "3df27Us3KXIy4LhebjNt0DDfF0N5zGXEQ1MmJ7jIU8o="
            },
            "status": "success"
        }
         */

        assertEquals("AgAAAANVKWUKoXWL9OdHdxoEhRlHC9BEJucY44HF3RmJHlXzjEJ/nvySkVFeRbo3W/2Mpe9qi4haYz6psljI6ZwUq8n6jPyAR0WK5LDrTxUPibeaV9aZJoa6gNCKfJ/Z8tZtG/iWUbZkkDPmJiF7LTxqB1TQX0UvtLDbDbewsa+b+PJgEw==",
                refreshedIdentity.getAdvertisingToken());
        assertEquals("AAAAAARS93G4B29nG5kXD5C97S5wGBJcGU3NXBYME+vMqlvvgXG4RuRt+UHG6fe7hiGoK7QtQVL1qj1SdpyuiJQHeM1PfRzk5vjXEv39wedDxQNWAnJsF6I6W6HPFCJADcUd/IEk2FKTq5+al5KwL9woU82WNCCt+n6ut29Bb/F3S/blW2G7/Q2i+tImcwuTTiAZ2uYzgg/t4HfB880rpa28E/cfqJ3vsJHFGiDOUqU9JpNPFByOnxay5PQgkRlMjmea6ZfAwOCyFC69E51B3NZb3+mdx9zaHB4e6hCmk3BLPhSQzV3zOM1IRS4Rq2xO5EuEGEaSU1K3iluXTXBAOoalbjj1qln+BCu0ohA9sqfEOC/hZcmYL0YEkPHLXW2C094g",
                refreshedIdentity.getRefreshToken());
        assertEquals(Instant.ofEpochMilli(1668065878329L), refreshedIdentity.getIdentityExpires()); //November 10, 2022 7:37:58 AM
        assertEquals(Instant.ofEpochMilli(1668148678329L), refreshedIdentity.getRefreshExpires()); //November 11, 2022 6:37:58 AM
        assertEquals(refreshFrom, refreshedIdentity.getRefreshFrom()); //November 10, 2022 6:38:01 AM
    }

    @Test
    public void unencryptedRefreshResponseForV1Session() {
        JsonObject json = new Gson().fromJson(expectedDecryptedJsonForTokenGenerateResponse, JsonObject.class);
        json.remove("refresh_response_key"); //V1 sessions did not include refresh_response_key

        IdentityTokens currentIdentity = IdentityTokens.fromJsonString(json.toString());
        assertNotNull(currentIdentity); //missing fields normally throw an exception. refresh_response_key does not, in order to support saved V1 sessions

        final String expectedRefreshResponseForV1Session = "{\"body\":{\"advertising_token\":\"AgAAAANsVKE9wmkKhBhOucQ26SXHAAr02EnIkXxJQP2kalEEov3h18AP7nGIhQUQQVAPKaL6y8iyyA65YtZhYHLKW+eJhXUFqm4vl+F2Gg3Q7bBu+GZ6DzY5rpDfukw3vztJccEqdkKaEdbJIIKi/jwRazQWbAJNXF4V4HUd9SExMJ+QKQ==\",\"user_token\":\"AgAAAAJd/kXzNcUdHRt4iyZIzYYD+DegEYk8brfvjFFtjmJZysI82h01mo4J8mkdt8vZ6e30Zbor6rcLAJ3MeDOPG0eRD5agLn0/bgP5kAFF0HCLOtEgmaTQ0mC0NIAcQUxznKQ=\",\"refresh_token\":\"AAAAAAT7HUXdgdgMjz5isYkZTlgWtgiW5YesZEZIIfFqvtqCeXGYZUvfzO896LhNeL6jlJZRfNMl8Pr63NRG0zS5atNKKU3OzVLLqd+W8xgtQd3VUndq7l6eHtVMckiQ5Z/XyuAcYP+vCe1JXzM/67SQ9VMgPV8UFMyo3UZQDuOyKlPQaav91gq/L4yTA/6EnOTUxs/sc2zegIQeYRasSMuQqXAHLOGy4v86/fLVxdjj+eEtIvrBDVAdubrITHgFmLALSHx0sdAgq+yRiEvOM+i7vfqTxcNI6zfL8y6dypI243Pm+D4n145CURT243n7ZrmF6bWUayGOZTkLdRJe4E3Mf+QewkAUZsShrofUB4ylehe1U1wedh5648+zLaTmBwC1\",\"identity_expires\":1668398943517,\"refresh_expires\":1668481743517,\"refresh_from\":1668395346517,\"refresh_response_key\":\"7Cm2fFFbM2klAzTHEqbDnE8BFJKm02doDxI5uIcCl3Y=\"},\"status\":\"success\"}";
        IdentityTokens refreshedIdentity = PublisherUid2Helper.createIdentityFromTokenRefreshResponseImpl(expectedRefreshResponseForV1Session, currentIdentity, currentIdentity.getRefreshFrom());
        assertNotNull(refreshedIdentity);
    }

    @Test
    public void handleInvalidJsonString() {
        assertThrows(Uid2Exception.class,
                () -> IdentityTokens.fromJsonString("this is not a json string"));
    }

    @Test
    public void handleMissingIdentityFields() {
        //todo - change to parameterized test after we migrate to JUnit 5
        String[] expectedFields = {"advertising_token", "refresh_token", "identity_expires", "refresh_expires", "refresh_from"};
        for (String field : expectedFields) {
            handleMissingIdentityField(field);
        }
    }

    private void handleMissingIdentityField(String field) {
        JsonObject json = new Gson().fromJson(expectedDecryptedJsonForTokenGenerateResponse, JsonObject.class);
        json.remove(field);

        assertThrows(Uid2Exception.class,
                () -> IdentityTokens.fromJsonString(json.toString()));
    }

    @Test
    public void nullJsonStringThrowsNullException() {
        assertThrows(NullPointerException.class,
                () -> IdentityTokens.fromJsonString(null));
    }

    @Test
    public void optOutRefreshResponse() {
        final String refreshResponseKey = "WL0AYr41Db9m6dCIPtQlTz2kE4NMx3nLmloGHkQPGSQ="; //derived by sending "optout@email.com" in a token generate request
        final String encryptedRefreshOptOutResponse = "d9I6qCdlJ6mkWYLyw2iGnCnJ5OocyQDtbbvW6YXAYKpxTXaOePQzPPJIrHnI7Io="; //derived by sending a refresh request using the refresh token from the generate request above

        JsonObject json = new Gson().fromJson(expectedDecryptedJsonForTokenGenerateResponse, JsonObject.class);
        json.addProperty("refresh_response_key", refreshResponseKey); //replaces the existing property

        IdentityTokens currentIdentity = IdentityTokens.fromJsonString(json.toString());

        IdentityTokens refreshedIdentity = PublisherUid2Helper.createIdentityFromTokenRefreshResponse(encryptedRefreshOptOutResponse, currentIdentity);

        assertNull(refreshedIdentity);
    }

    @Test
    public void identityTimeRelated() {
        IdentityTokens identity = IdentityTokens.fromJsonString(expectedDecryptedJsonForTokenGenerateResponse);

        Instant beforeRefreshExpires = identity.getRefreshExpires().minusSeconds(1);
        Instant afterRefreshExpires = identity.getRefreshExpires().plusSeconds(1);
        assertTrue(identity.isRefreshableImpl(beforeRefreshExpires));
        assertFalse(identity.isRefreshableImpl(afterRefreshExpires));

        Instant beforeRefreshFrom = identity.getRefreshFrom().minusSeconds(1);
        Instant afterRefreshFrom = identity.getRefreshFrom().plusSeconds(1);
        Instant afterIdentityExpires = identity.getIdentityExpires().plusSeconds(1);
        assertFalse(identity.isDueForRefreshImpl(beforeRefreshFrom));
        assertTrue(identity.isDueForRefreshImpl(afterRefreshFrom));
        assertTrue(identity.isDueForRefreshImpl(afterIdentityExpires));
    }

    @Test
    public void invalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> createEnvelopeString(IdentityType.Email, "this is not an email address", null, true));

        assertThrows(IllegalArgumentException.class,
                () -> createEnvelopeString(IdentityType.Email, "neither.is@this@", null, true));
    }

    @Test
    public void unnormalizedOrInvalidPhone() {
        assertThrows(IllegalArgumentException.class,
                () -> createEnvelopeString(IdentityType.Phone, "+123 44 555-66-77", null, true));

        assertThrows(IllegalArgumentException.class,
                () -> createEnvelopeString(IdentityType.Phone, "1 (123) 456-7890", null, true));

        assertThrows(IllegalArgumentException.class,
                () -> createEnvelopeString(IdentityType.Phone, "this is not a phone number", null, true));
    }

    @Test
    public void invalidEncryptedString() {
        final EnvelopeV2 envelope = publisherUid2Helper.createEnvelopeForTokenGenerateRequest(TokenGenerateInput.fromEmail("test@example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> publisherUid2Helper.createIdentityfromTokenGenerateResponse("this is not an encrypted response", envelope));

        assertThrows(RuntimeException.class, //using a request envelope where a response is expected:
                () -> publisherUid2Helper.createIdentityfromTokenGenerateResponse(envelope.getEnvelope(), envelope));

        IdentityTokens identity = IdentityTokens.fromJsonString(expectedDecryptedJsonForTokenGenerateResponse);
        assertThrows(IllegalArgumentException.class,
            () -> PublisherUid2Helper.createIdentityFromTokenRefreshResponse("this is not an encrypted response", identity));
    }
}
