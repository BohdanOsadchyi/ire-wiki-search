package com.ire.commons;

public class VBEncoding {
	
	public static byte[] encode(int n) {
		//Encodes a integer to stream of bytes.
        if (n == 0) return new byte[]{0};
        int i = (int) (Math.log(n) / Math.log(128)) + 1;
        final byte[] rv = new byte[i];
        int j = i - 1;
        do {
            rv[j--] = (byte) (n % 128);
            n /= 128;
        } while (j >= 0);
        rv[i - 1] += 128;
        return rv;
    }
	
	public static int decode(final byte[] arr){
		//Decodes a stream of bytes to integer.
		int n = 0;
        for (byte b : arr) {
            if ((b & 0xff) < 128) {
                n = 128 * n + b;
            } else {
                return (128 * n + ((b - 128) & 0xff));
            }
        }
        return n;
	}
	 
	public static String encodeInStr(int n) {
		//Encodes a integer to stream of bytes (which is converted to string).
        if (n == 0) {        	
        	throw new IllegalArgumentException("0 passed as input to the encoding algorithm.");
        }
        int i = (int) (Math.log(n) / Math.log(128)) + 1;
        final byte[] res = new byte[i];
        int j = i - 1;
        do {
            res[j--] = (byte) (n % 128);
            n /= 128;
        } while (j >= 0);
        res[i - 1] += 128;
        //Write to a string
        final StringBuffer buff=new StringBuffer();
        for(j=0;j<i;j++)
        	buff.append((char)res[j]);
        
        return buff.toString();
    }
	
	public static String encodeInStr(String str) {
		//Encodes a number in string format.
		return encodeInStr(Integer.parseInt(str));
	}
	
	public static int decode(final String str){
		int n = 0;
        for (int i=0;i<str.length();i++) {
        	byte b=(byte)str.charAt(i);
            if ((b & 0xff) < 128) {
                n = 128 * n + b;
            } else {
                return (128 * n + ((b - 128) & 0xff));
            }
        }
        return n;
	}

}
