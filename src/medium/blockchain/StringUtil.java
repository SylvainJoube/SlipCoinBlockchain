package medium.blockchain;

import java.security.MessageDigest;

public class StringUtil {
	//Applies Sha256 to a string and returns the result. 
	public static String applySha256(String input) throws RuntimeException {
		try {
			return applySha256(input.getBytes("UTF-8"));
		} catch (Exception e) {
			throw new RuntimeException(e); // Erreur critique
		}
	}	

	public static String applySha256(byte[] inputByteArray){		
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");	        
			//Applies sha256 to our input, 
			byte[] hash = digest.digest(inputByteArray);	        
			StringBuffer hexString = new StringBuffer(); // Contiendra le hash en hexidecimal
			for (int i = 0; i < hash.length; i++) {
				String hex = Integer.toHexString(0xff & hash[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		}
		catch(Exception e) {
			throw new RuntimeException(e); // Erreur critique
		}
	}
}