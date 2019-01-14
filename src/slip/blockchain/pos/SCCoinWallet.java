package slip.blockchain.pos;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import slip.security.common.RSA;
import slip.security.common.RSAKey;

public class SCCoinWallet {
	
	private String privateKeyAsString; // secrète, est la clef du coffre de l'utilisateur
	private String publicKeyAsString;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private String recognizableName;
	private double lastValidCoinAmount = 0; // actualisé via la blockchain
	
	public static SCCoinWallet createNewWallet(String recognizableName)  {
		KeyPair keys = null;
		try {
			keys = RSA.generateRSAKeyPair();
		} catch (Exception e) { e.printStackTrace(); }
		if (keys == null) return null;

		String privateKeyAsString = RSAKey.saveKey(keys.getPrivate());
		String prublicKeyAsString = RSAKey.saveKey(keys.getPublic());
		return new SCCoinWallet(privateKeyAsString, prublicKeyAsString, recognizableName);
	}
	
	public SCCoinWallet(String arg_privateKey, String arg_publicKey, String arg_recognizableName) {
		privateKeyAsString = arg_privateKey;
		publicKeyAsString = arg_publicKey;
		recognizableName = arg_recognizableName;
		privateKey = RSAKey.loadPrivateKey(privateKeyAsString);
		publicKey = RSAKey.loadPublicKey(publicKeyAsString);
	}
	
	public String getPublicKey() {
		return publicKeyAsString;
	}
	public String getPrivateKey() {
		return privateKeyAsString;
	}
	public String getRecognizableName() {
		return recognizableName;
	}
	@Override
	public String toString() {
		return "Wallet de " + recognizableName + ". Montant = " + lastValidCoinAmount + ". Clef publique = " + publicKeyAsString;
	}
	/** Mise à jour du porte monnaie, plus tard réellement mis à jour du réseau (plusieurs confirmations de plusieurs noeuds)
	 * @param fromNode 
	 */
	public void updateWallet(SCNode fromNode) {
		lastValidCoinAmount = SCNode.getWalletAmountFromNodeChain(fromNode, publicKeyAsString);
	}
	
	
	
}
