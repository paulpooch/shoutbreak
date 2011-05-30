package com.shoutbreak.service;

import java.security.MessageDigest;

import com.shoutbreak.ErrorManager;


public class Hash {

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}

		return buf.toString();
	}

	public static String sha1(String text) {
		MessageDigest md = null;
		byte[] sha1hash = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
			sha1hash = new byte[40];
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			sha1hash = md.digest();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		}
		return convertToHex(sha1hash);
	}
	
	public static String sha512(String text) {
		MessageDigest md = null;
		byte[] sha512hash = null;
		try {
			md = MessageDigest.getInstance("SHA-512");
			sha512hash = new byte[40];
			md.update(text.getBytes("iso-8859-1"), 0, text.length());
			sha512hash = md.digest();
		} catch (Exception ex) {
			ErrorManager.manage(ex);
		}
		return convertToHex(sha512hash);
	}

}