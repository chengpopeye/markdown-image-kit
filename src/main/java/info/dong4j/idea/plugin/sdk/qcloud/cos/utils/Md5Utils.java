package info.dong4j.idea.plugin.sdk.qcloud.cos.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;

public class Md5Utils {
    public static byte[] computeMD5Hash(InputStream is) throws IOException {
        return DigestUtils.md5(is);
    }

    /**
     * Returns the MD5 in base64 for the data from the given input stream. Note this method closes
     * the given input stream upon completion.
     */
    public static String md5AsBase64(InputStream is) throws IOException {
        return Base64.encodeAsString(computeMD5Hash(is));
    }

    /**
     * Computes the MD5 hash of the given data and returns it as an array of bytes.
     */
    public static byte[] computeMD5Hash(byte[] input) {
        return DigestUtils.md5(input);
    }

    /**
     * Returns the MD5 in base64 for the given byte array.
     */
    public static String md5AsBase64(byte[] input) {
        return Base64.encodeAsString(computeMD5Hash(input));
    }

    public static String md5Hex(File file) throws IOException {
        return Hex.encodeHexString(computeMD5Hash(file));
    }

    public static String md5Hex(String utf8Content) {
        return Hex.encodeHexString(computeMD5Hash(utf8Content.getBytes(StringUtils.UTF8)));
    }


    public static String md5Hex(byte[] input) {
        return Hex.encodeHexString(computeMD5Hash(input));
    }

    /**
     * Computes the MD5 of the given file.
     */
    public static byte[] computeMD5Hash(File file) throws IOException {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            return computeMD5Hash(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Returns the MD5 in base64 for the given file.
     */
    public static String md5AsBase64(File file) throws IOException {
        return Base64.encodeAsString(computeMD5Hash(file));
    }

}
