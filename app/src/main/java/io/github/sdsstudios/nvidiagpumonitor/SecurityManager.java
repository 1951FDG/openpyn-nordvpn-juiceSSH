package io.github.sdsstudios.nvidiagpumonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecurityManager {

    private static final int IV_LENGTH = 16;
    private static volatile SecurityManager sInstance;
    private SecretKey mKey;

    private SecurityManager() {
        if (sInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    @SuppressLint("HardwareIds")
    private SecurityManager(Context context) {
        String androidId = Settings
                .Secure
                .getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        try {
            byte[] key = androidId.getBytes("UTF8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            mKey = new SecretKeySpec(key, "AES");
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static SecurityManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (SecurityManager.class) {
                if (sInstance == null) {
                    sInstance = new SecurityManager(context);
                }
            }
        }
        return sInstance;
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, pos, array.length);
            pos += array.length;
        }
        return result;
    }

    public String encryptString(String stringToEncrypt) {
        String output = stringToEncrypt;
        try {
            byte[] clearText = stringToEncrypt.getBytes("UTF8");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, mKey, ivSpec);
            byte[] cipherBytes = cipher.doFinal(clearText);
            output = new String(Base64.encode(concat(iv, cipherBytes),
                    Base64.NO_WRAP), "UTF8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | UnsupportedEncodingException
                | IllegalBlockSizeException | BadPaddingException
                | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return output;
    }

    public String decryptString(String stringToDecrypt) {
        String output = stringToDecrypt;
        try {
            byte[] encryptedBytes = Base64.decode(stringToDecrypt, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            IvParameterSpec ivSpec = new IvParameterSpec(encryptedBytes, 0, IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, mKey, ivSpec);

            byte[] cipherBytes = cipher.doFinal(encryptedBytes, IV_LENGTH, encryptedBytes.length - IV_LENGTH);

            output = new String(cipherBytes, "UTF8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException
                | InvalidKeyException | UnsupportedEncodingException
                | IllegalBlockSizeException | BadPaddingException
                | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return output;
    }
}
