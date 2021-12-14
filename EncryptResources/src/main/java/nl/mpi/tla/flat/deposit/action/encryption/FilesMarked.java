package nl.mpi.tla.flat.deposit.action.encryption;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.codec.digest.DigestUtils;

public class FilesMarked {

    private String[] marked = {};

    public String[] getMarked() {
        return marked;
    }

    public void setMarked(String[] marked) {
        this.marked = marked;
    }

    public boolean isMarked(File file) {

      String hex = this.generateMd5(file);
      return Arrays.asList(this.marked).contains(hex);
    }

    private String generateMd5(File file) {

      try {
        return DigestUtils.md5Hex(file.getName().getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        return "___INVALID___DIGEST_MD5_Hex___";
      }
    }
}
