package io.github.getsixtyfour.openpyn.security;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("Singleton")
public final class SecurityCypher {

    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    private static final int IV_LENGTH = 16;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    @Nullable
    private static volatile SecurityCypher sInstance = null;

    @Nullable
    private SecretKey mSecretKey = null;

    @SuppressLint("HardwareIds")
    @SuppressWarnings("HardCodedStringLiteral")
    private SecurityCypher(Context context) {
        String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        try {
            byte[] key = androidId.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            key = md.digest(key);
            key = Arrays.copyOf(key, IV_LENGTH);
            mSecretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException ignored) {
        }
    }

    @SuppressWarnings("SynchronizeOnThis")
    @NonNull
    public static SecurityCypher getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (SecurityCypher.class) {
                if (sInstance == null) {
                    sInstance = new SecurityCypher(context.getApplicationContext());
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

    @SuppressWarnings("MagicCharacter")
    private static char[] toChars(byte[] array) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(array, 0, array.length);
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(byteBuffer);
        char[] chars = Arrays.copyOf(charBuffer.array(), charBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000');
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return chars;
    }

    @SuppressWarnings({ "WeakerAccess", "TryWithIdenticalCatches", "MethodWithMultipleReturnPoints" })
    @Nullable
    public char[] decrypt(@NonNull String stringToDecrypt) {
        if (stringToDecrypt.isEmpty()) {
            return EMPTY_CHAR_ARRAY;
        }
        try {
            byte[] encryptedBytes = Base64.decode(stringToDecrypt, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(encryptedBytes, 0, IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, mSecretKey, ivSpec);
            byte[] cipherBytes = cipher.doFinal(encryptedBytes, IV_LENGTH, encryptedBytes.length - IV_LENGTH);
            return toChars(cipherBytes);
        } catch (NoSuchAlgorithmException ignored) {
        } catch (NoSuchPaddingException ignored) {
        } catch (InvalidKeyException ignored) {
        } catch (IllegalArgumentException ignored) {
        } catch (IllegalBlockSizeException ignored) {
        } catch (BadPaddingException ignored) {
        } catch (InvalidAlgorithmParameterException ignored) {
        }
        return null;
    }

    @Nullable
    public String decryptString(@NonNull String stringToDecrypt) {
        char[] chars = decrypt(stringToDecrypt);
        return (chars != null) ? new String(chars) : null;
    }

    @SuppressWarnings({ "WeakerAccess", "TryWithIdenticalCatches", "MethodWithMultipleReturnPoints" })
    @Nullable
    public char[] encrypt(@NonNull String stringToEncrypt) {
        if (stringToEncrypt.isEmpty()) {
            return EMPTY_CHAR_ARRAY;
        }
        try {
            byte[] clearText = stringToEncrypt.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom sr = new SecureRandom();
            sr.nextBytes(iv);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, mSecretKey, ivSpec);
            byte[] cipherBytes = cipher.doFinal(clearText);
            byte[] encryptedBytes = Base64.encode(concat(iv, cipherBytes), Base64.NO_WRAP);
            return toChars(encryptedBytes);
        } catch (NoSuchAlgorithmException ignored) {
        } catch (NoSuchPaddingException ignored) {
        } catch (InvalidKeyException ignored) {
        } catch (IllegalArgumentException ignored) {
        } catch (IllegalBlockSizeException ignored) {
        } catch (BadPaddingException ignored) {
        } catch (InvalidAlgorithmParameterException ignored) {
        }
        return null;
    }

    @Nullable
    public String encryptString(@NonNull String stringToEncrypt) {
        char[] chars = encrypt(stringToEncrypt);
        return (chars != null) ? new String(chars) : null;
    }
}
