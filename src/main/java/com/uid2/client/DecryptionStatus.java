 package com.uid2.client;

 /**
  * DecryptionStatus: Indicates the result of a decrypt() call.
  */
public enum DecryptionStatus {

    SUCCESS,
<<<<<<< HEAD
     /**
      * NOT_AUTHORIZED_FOR_MASTER_KEY can occur for old tokens, when the master key used to encrypt it has been deleted (eg after 30 days).
      * It can also occur for participants that have been removed from the UID platform, and therefore no longer have access to master keys.
      */
    NOT_AUTHORIZED_FOR_MASTER_KEY,
     /**
      * NOT_AUTHORIZED_FOR_KEY can occur when decrypting a token from a participant that has not granted sharing access to the receiver.
      */
=======
    NOT_AUTHORIZED_FOR_MASTER_KEY, //note this error can occur for old tokens also, since old encryption keys are regularly deleted
>>>>>>> 842be6ab81cc3d9ef81e20a737315ddae2b6b354
    NOT_AUTHORIZED_FOR_KEY,
     /**
      * NOT_INITIALIZED: IUID2Client.refresh() has not been called or did not succeed.
      */
    NOT_INITIALIZED,
     /**
      * INVALID_PAYLOAD: The UID token passed to decrypt() was invalid.
      */
    INVALID_PAYLOAD,
     /**
      * EXPIRED_TOKEN: The token has expired and so must not be used. See also NOT_AUTHORIZED_FOR_MASTER_KEY.
      */
    EXPIRED_TOKEN,
     /**
      * KEYS_NOT_SYNCED: The encryption keys have expired. This can happen if IUID2Client.refresh() has not been called for a long time.
      */
    KEYS_NOT_SYNCED,
     /**
      * VERSION_NOT_SUPPORTED: The token version being decrypted is too new for this SDK. Ensure you have the latest version of the SDK.
      */
     VERSION_NOT_SUPPORTED,
     /**
      * INVALID_PAYLOAD_TYPE: This is a value returned by the legacy decryptData() method. Use decrypt() instead.
      */
    INVALID_PAYLOAD_TYPE,
     /**
      * INVALID_IDENTITY_SCOPE: Either UID2ClientFactory.create() was used to decrypt an EUID token, or EUIDClientFactory.create() was used
      * to decrypt a UID2 token. Ensure the factory class matches the token type you are decrypting.
      */
    INVALID_IDENTITY_SCOPE,
}
