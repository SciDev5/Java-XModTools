package me.scidev5.xmodtools.data.file;

import java.nio.ByteBuffer;

public class DataUtils {

	public static String getString(ByteBuffer bb, int maxLength) {
		String str = "";
		for (int j = 0; j < maxLength; j++) {
			byte data = bb.get();
			if (data == 0) {
				bb.position(bb.position()+maxLength-j-1);
				break;
			}
			str += (char) data;
		}
		return str;
	}
	public static void setString(ByteBuffer bb, String str, int maxLength) {
		for (int j = 0; j < maxLength; j++) {
			bb.putChar(j < str.length() ? str.charAt(j) : 0);
		}
	}
	
}
