package slip.blockchain.pos;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import slip.security.common.RSA;
import slip.security.common.RSAKey;

/**
 * Portefeuille sécurisé :
 * -> L'adresse du portefeuille est sa clef publique
 * -> La clef privée permet de signer des messages et elle est nécessaire pour créer une transaction (virement, de montant positif)
 *    Il est impossible de prendre de l'argent à un portefeuille sans la clef privée :
 *     toutes les cellules valides (non malicieuses/piratées) rejettent une transaction si elle est mal signée.
 */

public class SCCoinWallet {
	
	private String privateKeyAsString; // secrète, est la clef du coffre de l'utilisateur, aucun retrait d'argent n'est possible sans cette clef
	private String publicKeyAsString;
	private PrivateKey privateKey;
	private PublicKey publicKey;
	private String recognizableName;
	private double lastValidCoinAmount = 0; // actualisé via la blockchain
	
	/** Créer un nouveau portefeuille
	 * -> Attention, si la clef privée du portefeuille est perdue, plus aucun retrait d'argent n'est possible.
	 * @param recognizableName  nom d'utilisateur associé (non connu de la blockchain, seulement du portefeuille SCCoinWallet)
	 * @return un nouveau portefeuille avec un couple clef privée - clef publique.
	 */
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
	
	/** Constructeur du portefeuille, nécessite les clefs et un nom d'utilisateur (juste parce que c'est plus lisible qu'une clef publique RSA de 2048 caractères !)
	 * @param arg_privateKey  clef privée, clef du coffre sans laquelle aucune transaction n'est possible (utile pour les signatures des transactions et blocs)
	 * @param arg_publicKey   clef publique, adresse publique du portefeuille (public et connu de la blockchain)
	 * @param arg_recognizableName
	 */
	public SCCoinWallet(String arg_privateKey, String arg_publicKey, String arg_recognizableName) {
		privateKeyAsString = arg_privateKey;
		publicKeyAsString = arg_publicKey;
		recognizableName = arg_recognizableName;
		//privateKey = RSAKey.loadPrivateKey(privateKeyAsString);
		//publicKey = RSAKey.loadPublicKey(publicKeyAsString);
	}
	
	/**
	 * @return adresse publique du portefeuille
	 */
	public String getPublicKey() {
		return publicKeyAsString;
	}
	/**
	 * @return adresse privée du portefeuille, seul le possesseur du portefeuille doit avoir cette clef !
	 */
	public String getPrivateKey() {
		return privateKeyAsString;
	}
	/**
	 * @return nom (plus simple qu'une clef RSA 2048...) donné à ce portefeuille
	 */
	public String getRecognizableName() {
		return recognizableName;
	}
	/**
	 * Fonction toString pour afficher simplement le portefeuille
	 */
	@Override
	public String toString() {
		return "Wallet de " + recognizableName + ". Montant = " + lastValidCoinAmount + ". Clef publique = " + publicKeyAsString;
	}

	// toString mais sans la clef publique (+ lisible)
	public String toSimpleString() {
		return "Wallet de " + recognizableName + ". Montant = " + lastValidCoinAmount + ".";
	}
	/** Mise à jour du porte monnaie, plus tard réellement mis à jour du réseau (plusieurs confirmations de plusieurs noeuds)
	 * @param fromNode noeud duquel mettre à jour le portefeuille
	 */
	public void updateWallet(SCNode fromNode) {
		// Plus tard : requête réseau et non interrogation directe d'un noeud
		lastValidCoinAmount = SCNode.getWalletAmountFromNodeChain(fromNode, publicKeyAsString);
	}
	
	
	
}
