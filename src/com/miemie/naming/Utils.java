package com.miemie.naming;


public class Utils {
  private static int parse(char c) {
    if (c >= 'a') return (c - 'a' + 10) & 0x0f;
    if (c >= 'A') return (c - 'A' + 10) & 0x0f;
    return (c - '0') & 0x0f;
  }

  public static byte[] HexString2Bytes(String hexstr) {
    byte[] b = new byte[hexstr.length() / 2];
    int j = 0;
    for (int i = 0; i < b.length; i++) {
      char c0 = hexstr.charAt(j++);
      char c1 = hexstr.charAt(j++);
      b[i] = (byte) ((parse(c0) << 4) | parse(c1));
    }
    return b;
  }

  public static char HexString2Char(String hexstr) {
    byte[] buf = HexString2Bytes(hexstr);
    char c = 0;
    if (buf.length == 2) {
      c = (char) ((buf[1] & 0xff) | ((buf[0] & 0xff) << 8));
    }
    return c;
  }
}
