package com.uid2.client;


public class EnvelopeV2 {
  EnvelopeV2(String envelope, byte[] nonce) {
    this.envelope = envelope;
    this.nonce = nonce;
  }

  /**
   * @return an encrypted request envelope which can be used in the POST body of a <a href="https://github.com/UnifiedID2/uid2docs/tree/main/api/v2/endpoints">UID2 endpoint</a>.
   * See <a href="https://github.com/UnifiedID2/uid2docs/blob/main/api/v2/encryption-decryption.md#encrypted-request-envelope">Encrypted Request Envelope</a>
   */
  public String getEnvelope() { return envelope; }
  byte[] getNonce() { return nonce;}

  private final String envelope;
  private final byte[] nonce;
}


