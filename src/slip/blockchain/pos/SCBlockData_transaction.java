package slip.blockchain.pos;

import java.security.PublicKey;

import slip.network.buffers.NetBuffer;
import slip.network.buffers.NetBufferData;
import slip.security.common.RSA;
import slip.security.common.RSAKey;

/**
 * Un cas particulier de donnée : une transaction
 *
 */
public class SCBlockData_transaction implements SCBlockData {
	
	// Mis en public pour gagner un peu de temps et ne pas faire les getters et setters
	public double amount; // strictement positif
	public String senderPublicKey; // adresse de l'émetteur
	public String receiverPublicKey; // adresse du récepteur
	public long timeStamp;
	// transactionHash : retrouvée en hashant le message via computeHash()
	private String senderSignature; // hash du message 
	
	/*public byte[] asByteArray_withSignature = null;
	public NetBuffer asBuffer_withSignature = null;
	public byte[] asByteArray_noSignature = null;
	public NetBuffer asBuffer_noSignature = null;*/
	
	public boolean criticalErrorOccured = false;
	
	
	// -> signer la transaction avec la clef privée du créateur (avec senderPrivateKey dans le constructeur)
	// -> charger la signature si la transaction est une transaction déjà chargée
	
	public String getSignature() {
		return senderSignature;
	}
	

	public static SCBlockData_transaction readFromByteArray(byte[] readByteArray) {
		NetBuffer readNetBuffer = new NetBuffer(readByteArray);
		return readFromNetBuffer(readNetBuffer);	
	}
	
	/** Avec la signature (lecture du réseau)
	 * @return 
	 */
	public static SCBlockData_transaction readFromNetBuffer(NetBuffer readFrom) {
		if (readFrom == null) return null;
		
		readFrom.resetPosition();
		readFrom.readInt(); // type de la donnée (ici SCBlockDataType.TRANSACTION.asInt())
		int amount = readFrom.readInt();
		String senderPublicKey = readFrom.readString();
		String receiverPublicKey = readFrom.readString();
		int timeStamp = readFrom.readInt();
		String senderSignature = readFrom.readString();
		
		SCBlockData_transaction transaction = new SCBlockData_transaction(amount, senderPublicKey, receiverPublicKey, timeStamp, false, null, senderSignature);
		//if (transaction.checkValidity() == false) return null;
		return transaction;
	}
	
	
	/** Transaction -> NetBuffer
	 */
	@Override
	public NetBuffer writeToNetBuffer(boolean withSignature) {
		NetBuffer toNetBuffer = new NetBuffer();
		toNetBuffer.writeInt(SCBlockDataType.TRANSACTION.asInt());
		toNetBuffer.writeDouble(amount);
		toNetBuffer.writeString(senderPublicKey);
		toNetBuffer.writeString(receiverPublicKey);
		toNetBuffer.writeLong(timeStamp);
		if (withSignature)
			toNetBuffer.writeString(senderSignature);
		
		return toNetBuffer;
	}
	/** Signer la transaction
	 * @param senderPrivateKey
	 */
	public void signData(String authorPrivateKey) {
		String transactionHash = computeHash();
		senderSignature = RSA.sign(transactionHash, authorPrivateKey); // authorPrivateKey = senderPrivateKey
		if (senderSignature == null) {
			criticalErrorOccured = true;
			return;
		}
	}
	
	// Ici, je pars du principe que la transaction n'est pas encore signée
	public SCBlockData_transaction(int arg_amount, String arg_senderKey, String arg_receiverKey, long arg_timeStamp, boolean hasToSignTransaction, String senderPrivateKey, String arg_senderSignature) { // , String arg_senderSignature
		// Utilisation pour une transaction non signée
		senderPublicKey = arg_senderKey;
		receiverPublicKey = arg_receiverKey;
		amount = arg_amount;
		timeStamp = arg_timeStamp;
		if (hasToSignTransaction)
			signData(senderPrivateKey);
		else {
			senderSignature = arg_senderSignature;
		}
	}
	
	@Override
	public String toString() {
		String asString = "SCBlockData_transaction : amount("+amount+") timeStamp(" + timeStamp + ") senderPublicKey("+senderPublicKey+") " + "receiverPublicKey("+receiverPublicKey+") " + "senderSignature("+senderSignature+") ";
		return asString;
	}
	
	/** Avec signature
	 * 
	 */
	@Override
	public byte[] getRawByteArrayData(boolean withSignature) { // _withSignature
		if (criticalErrorOccured) return null;
		NetBuffer asNetBuffer = writeToNetBuffer(withSignature);
		return asNetBuffer.convertToByteArray();
	}
	
	@Override
	public String computeHash() {
		byte[] transactionAsByteArray = getRawByteArrayData(false); // sans la signature
		String transactionHash = RSA.sha256(transactionAsByteArray);
		return transactionHash;
	}
	
	@Override
	public boolean checkSignatureValidity() {
		if (amount <= 0) return false;
		//boolean successLoadingKey = RSA.setRSAPublicKey(senderPublicKey);
		//if (!successLoadingKey) return false; // clef probablement invalide => bloc invalide
		String transactionHash = computeHash();
		return RSA.check(transactionHash, senderSignature, senderPublicKey);
	}
	
	/*@Override
	public boolean signData() {
		déjà signé dans le constructeur
	}*/
	
	/** Regarder si un utilisateur est impliqué dans cet échange, retourner la modification de son solde
	 *  @param ownerPublicKey
	 *  @return
	 */
	public double getWalletVariationForUser(String ownerPublicKey) {
		if (ownerPublicKey == null) return 0;
		if (ownerPublicKey.equals(senderPublicKey)) { // envoyeur - le montant
			return -amount;
		}
		if (ownerPublicKey.equals(receiverPublicKey)) { // receveur + le montant
			return amount;
		}
		return 0;
	}
	
	@Override
	public String getAuthorPublicKey() {
		return senderPublicKey;
	}
	
	@Override
	public SCBlockDataType getDataType() {
		return SCBlockDataType.TRANSACTION;
	}
	
}
