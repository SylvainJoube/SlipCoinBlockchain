package slip.blockchain.pos;

import java.util.ArrayList;

import slip.network.buffers.NetBuffer;

/**
 * Une donnée contenue dans un bloc
 * Une transaction par exemple.
 * 
 * La clef publique d'un utilisateur de la chaine est son identifiant.
 * Tout les messages et données d'un utilisateur doivent être signées via la clef privée associée à la clef publique.
 * 
 */
public interface SCBlockData {
	
	public boolean checkValidity();
	public String getAuthorPublicKey(); // L'identifiant de celui qui a émis cette donnée. Utile pour valider la donnée.
	public SCBlockDataType getDataType();
	public byte[] getRawByteArrayData(boolean withSignature);
	//public int getRawByteArrayDataSize(boolean withSignature);
	public NetBuffer writeToNetBuffer(boolean withSignature);
	public void signData(String authorPrivateKey);
	public String computeHash();
	//public static SCBlockData_transaction readFromNetBuffer(NetBuffer readFrom) { return null; }
	
	//public boolean signData(); // false si ça s'est mal passé
	// public SCBlockData copy();

}
