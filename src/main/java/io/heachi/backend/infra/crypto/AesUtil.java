package io.heachi.backend.infra.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AesUtil {

  private final String IV = "ShVmYq3t6w9y$B&E";
  private final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

  public String encrypt(String secretKey, String str) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
      IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

      return Base64.getEncoder()
          .encodeToString(
              cipher.doFinal(str.getBytes(StandardCharsets.UTF_8))
          );
    } catch (NoSuchPaddingException | NoSuchAlgorithmException |
        InvalidAlgorithmParameterException | InvalidKeyException |
        IllegalBlockSizeException | BadPaddingException e) {
      log.error("aesUtil >> encrypt >> fail >> {}", e.getMessage());
      return null;
    }
  }

  public String decrypt(String secretKey, String str) {
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), "AES");
      IvParameterSpec ivParameterSpec = new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8));

      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

      return new String(
          cipher.doFinal(Base64.getDecoder().decode(str)), StandardCharsets.UTF_8
      );
    } catch (NoSuchPaddingException | NoSuchAlgorithmException |
        InvalidAlgorithmParameterException | InvalidKeyException |
        IllegalBlockSizeException | BadPaddingException e) {
      return null;
    }
  }
}
