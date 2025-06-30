package com.uid2.client;


public class EnvelopeV2 {
  EnvelopeV2(byte[] envelope, byte[] nonce) {
    this.binaryEnvelope = envelope;
    this.nonce = nonce;
  }

  /**
   * @return an encrypted request envelope which can be used in the POST body of a <a href="https://unifiedid.com/docs/endpoints/summary-endpoints">UID2 endpoint</a>.
   * See <a href="https://unifiedid.com/docs/getting-started/gs-encryption-decryption#encrypted-request-envelope">Encrypted Request Envelope</a>
   */
  public String getEnvelope() { return InputUtil.byteArrayToBase64(binaryEnvelope); }
  byte[] getNonce() { return nonce;}

  public byte[] getBinaryEnvelope() {
    return binaryEnvelope;
  }

  private final byte[] binaryEnvelope;
  private final byte[] nonce;
}


