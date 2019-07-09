package info.dong4j.idea.plugin.sdk.qcloud.cos.internal.crypto;

import info.dong4j.idea.plugin.sdk.qcloud.cos.exception.CosClientException;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * Cryptographic scheme for content encrypt/decryption.
 *
 */
abstract class ContentCryptoScheme {
    /**
     * The maximum number of 16-byte blocks that can be encrypted with a
     * GCM cipher.  Note the maximum bit-length of the plaintext is (2^39 - 256),
     * which translates to a maximum byte-length of (2^36 - 32), which in turn
     * translates to a maximum block-length of (2^32 - 2).
     * <p>
     */
    static final long MAX_GCM_BLOCKS = (1L << 32) - 2; // 2^32 - 2
    /**
     * The maximum number of bytes that can be encrypted with a
     * GCM cipher.
     */
    static final long MAX_GCM_BYTES = MAX_GCM_BLOCKS << 4;
    /**
     * The maximum number of bytes that can be securely encrypted per a single key using AES/CBC.
     */
    static final long MAX_CBC_BYTES = (1L << 48) << 4;  // 2^48 blocks, assuming an adversary advantage of at most 1/2^32 per Prof. Dan Boneh
    /**
     * The maximum number of bytes that can be securely encrypted per a single key using AES/CTR.
     */
    static final long MAX_CTR_BYTES = -1;   // 2^64 blocks, or effectively no limits, assuming an adversary advantage of at most 1/2^32 per Prof. Dan Boneh

    /**
     * Authenticated Encryption (AE) scheme.
     */
    static final ContentCryptoScheme AES_GCM = new AesGcm();

    /**
     * This is an auxiliary scheme used for range retrieval when object is
     * encrypted via AES/GCM.
     */
    static final ContentCryptoScheme AES_CTR = new AesCtr();

    abstract String getKeyGeneratorAlgorithm();
    abstract String getCipherAlgorithm();

    /**
     * Returns the only security provider that is known to work with the
     * cipher algorithm in the current implementation; or null if there is
     * no specific limitation.
     */
    String getSpecificCipherProvider() { return null; }
    abstract int getKeyLengthInBits();
    abstract int getBlockSizeInBytes();
    abstract int getIVLengthInBytes();

    int getTagLengthInBits() { return 0; } // default to zero ie no tag

    byte[] adjustIV(byte[] iv, long startingBytePos) {
        return iv;
    }

    @Override
    public String toString() {
        return "cipherAlgo=" + getCipherAlgorithm() + ", blockSizeInBytes="
                + getBlockSizeInBytes() + ", ivLengthInBytes="
                + getIVLengthInBytes() + ", keyGenAlgo="
                + getKeyGeneratorAlgorithm() + ", keyLengthInBits="
                + getKeyLengthInBits() + ", specificProvider="
                + getSpecificCipherProvider() + ", tagLengthInBits="
                + getTagLengthInBits();
    }

    /**
     * Increment the rightmost 32 bits of a 16-byte counter by the specified
     * delta. Both the specified delta and the resultant value must stay within
     * the capacity of 32 bits.
     * (Package private for testing purposes.)
     *
     * @param counter
     *            a 16-byte counter used in AES/CTR
     * @param blockDelta
     *            the number of blocks (16-byte) to increment
     */
    static byte[] incrementBlocks(byte[] counter, long blockDelta) {
        if (blockDelta == 0)
            return counter;
        if (counter == null || counter.length != 16)
            throw new IllegalArgumentException();
        // Can optimize this later.  KISS for now.
        if (blockDelta > MAX_GCM_BLOCKS)
            throw new IllegalStateException();
        // Allocate 8 bytes for a long
        ByteBuffer bb = ByteBuffer.allocate(8);
        // Copy the right-most 32 bits from the counter
        for (int i=12; i <= 15; i++)
            bb.put(i-8, counter[i]);
        long val = bb.getLong() + blockDelta;    // increment by delta
        if (val > MAX_GCM_BLOCKS)
            throw new IllegalStateException(); // overflow 2^32-2
        bb.rewind();
        // Get the incremented value (result) as an 8-byte array
        byte[] result = bb.putLong(val).array();
        // Copy the rightmost 32 bits from the resultant array to the input counter;
        for (int i=12; i <= 15; i++)
            counter[i] = result[i-8];
        return counter;
    }

    /**
     * Returns the content crypto scheme of the given content encryption algorithm.
     */
    static ContentCryptoScheme fromCEKAlgo(String cekAlgo) {
        return fromCEKAlgo(cekAlgo, false);
    }

    static ContentCryptoScheme fromCEKAlgo(String cekAlgo, boolean isRangeGet) {
        if (AES_GCM.getCipherAlgorithm().equals(cekAlgo)) {
            return isRangeGet ? AES_CTR : AES_GCM;
        }
        throw new UnsupportedOperationException("Unsupported content encryption scheme: " + cekAlgo);
    }

    /**
     * Creates and initializes a {@link CipherLite} for content
     * encrypt/decryption.
     *
     * @param cek
     *            content encrypting key
     * @param iv
     *            initialization vector
     * @param cipherMode
     *            such as {@link Cipher#ENCRYPT_MODE}
     * @param securityProvider
     *            optional security provider to be used but only if there is no
     *            specific provider defined for the specified scheme.
     * @return the cipher lite created and initialized.
     */
    CipherLite createCipherLite(SecretKey cek, byte[] iv, int cipherMode,
            Provider securityProvider) {
        String specificProvider = getSpecificCipherProvider();
        Cipher cipher;
        try {
            if (specificProvider != null) { // use the specific provider if defined
                    cipher = Cipher.getInstance(getCipherAlgorithm(), specificProvider);
            } else if (securityProvider != null) { // use the one optionally specified in the input
                cipher = Cipher.getInstance(getCipherAlgorithm(), securityProvider);
            } else { // use the default provider
                cipher = Cipher.getInstance(getCipherAlgorithm());
            }
            cipher.init(cipherMode, cek, new IvParameterSpec(iv));
            return newCipherLite(cipher, cek, cipherMode);
        } catch (Exception e) {
            throw e instanceof RuntimeException
                ? (RuntimeException) e
                : new CosClientException(
                    "Unable to build cipher: "
                        + e.getMessage()
                        + "\nMake sure you have the JCE unlimited strength policy files installed and "
                        + "configured for your JVM",
                    e);
        }
    }

    /**
     * This is a factory method intended to be overridden by sublcasses to
     * return the appropriate instance of cipher lite.
     */
    protected CipherLite newCipherLite(Cipher cipher,  SecretKey cek, int cipherMode) {
        return new CipherLite(cipher, this, cek, cipherMode);
    }

    CipherLite createAuxillaryCipher(SecretKey cek, byte[] iv, int cipherMode,
            Provider securityProvider, long startingBytePos) throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        return null;
    }
    /**
     * Creates and initializes a cipher lite for content encrypt/decryption.
     *
     * @param cek
     *            content encrypting key
     * @param iv
     *            initialization vector
     * @param cipherMode
     *            such as {@link Cipher#ENCRYPT_MODE}
     * @return the cipher lite created and initialized.
     */
    CipherLite createCipherLite(SecretKey cek, byte[] iv, int cipherMode)
            throws InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException,
            InvalidAlgorithmParameterException {
        return createCipherLite(cek, iv, cipherMode, null);
    }

    /**
     * Returns the maximum size of the plaintext that can be encrypted using
     * the current scheme per a single secret key; or -1 if there is effectively
     * no limit.
     */
    abstract long getMaxPlaintextSize();

    /**
     * A convenient method motivated by KMS.
     */
    final String getKeySpec() {
        return getKeyGeneratorAlgorithm() + "_" + getKeyLengthInBits();
    }
}