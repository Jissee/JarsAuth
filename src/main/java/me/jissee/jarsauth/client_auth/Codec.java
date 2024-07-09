/*
 * This file is part of the JarsAuth, licensed under the
 * GNU General Public License v3.0. <https://www.gnu.org/licenses/>
 *
 * Copyright (C) 2024 Jissee and contributors
 */
package me.jissee.jarsauth.client_auth;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

public class Codec {
    private static final String RSA = "RSA";

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(2048); // 可以根据需要调整密钥长度
        return generator.generateKeyPair();
    }

    public static PublicKey byteArr2PublicKey(byte[] key) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        return keyFactory.generatePublic(spec);
    }

    public static PrivateKey byteArr2PrivateKey(byte[] key) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        return keyFactory.generatePrivate(spec);
    }

    public static byte[] encrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }
    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    public static UUID bytesToUUID(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Invalid byte array length for UUID: " + bytes.length);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long mostSigBits = byteBuffer.getLong();
        long leastSigBits = byteBuffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }


    /*
    public static byte[] encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data.getBytes());
    }

    public static byte[] decrypt(String data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(Base64.getDecoder().decode(data));
    }

    private static void f(){
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);

        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(data));
        return new String(decryptedBytes);
    }*/

}
