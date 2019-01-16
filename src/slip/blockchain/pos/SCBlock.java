package slip.blockchain.pos;

import java.util.ArrayList;

import slip.network.buffers.NetBuffer;
import slip.security.common.RSA;


// Pour une meilleure visibilité, signature des méthodes :
interface SCBlockInterafce {
	public String getPreviousBlockSignature();
	public String getBlockSignature();
	public String getAuthorPublicKey();
	public void readFromNetBuffer(NetBuffer readFromNetBuff, boolean hasBlockSignature);
	public NetBuffer writeToNetBuffer(boolean includeBlockSignature);
	public boolean signBlock(String authorPrivateKey);
	public String computeHash();
	public boolean checkWithBlockSignature();
	public double getWalletVariation(String ownerPublicKey);
	public boolean transactionIsAlreadyInBlock(SCBlockData_transaction transactionToCheck);
	
	
}

/** Un bloc de la chaine
 * 
 */
public class SCBlock implements SCBlockInterafce {
	
	public final double rewardPerBlockCreation = 0.1;
	
	private String previousBlockSignature; // l'ID du dernier bloc
	private String authorPublicKey; // L'identifiant de l'auteur du bloc = adresse de l'auteur du bloc
	private String blockSignature; // 
	// -> ajout d'un timeStamp ? pas forcément nécessaire
	private ArrayList<SCBlockData> myData = new ArrayList<SCBlockData>(); // Liste des données (entre autre : les transactions)
	// Le temps n'est pas utile ici (pour l'instant, du moins)
	
	
	//private String myHash; recalculé à chaque fois // Hash de ce bloc (garantit qu'il ne peut pas être modifié)
	//private boolean criticalErrorOccured = false;
	
	

	public String getPreviousBlockSignature() {
		return previousBlockSignature;
	}
	public String getBlockSignature() { // != signBlock()
		return blockSignature;
	}
	/*public String getMyHash() {
		return myHash; UNSAFE -> remplacée par computeHash(); toujours recalculer le hash
	}*/
	public String getAuthorPublicKey() {
		return authorPublicKey;
	}
	
	public ArrayList<SCBlockData> getData() {
		return this.myData;
	}
	
	/*public static ArrayList<SCBlockData> cloneDataList() {
		ArrayList<SCBlockData> copyList = new ArrayList<SCBlockData>();
		for (int iObj = 0; iObj < )
		return null;
	}*/
	
	public void readFromNetBuffer(NetBuffer readFromNetBuff, boolean hasBlockSignature) {
		
		previousBlockSignature = readFromNetBuff.readString();
		authorPublicKey = readFromNetBuff.readString();
		int dataListSize = readFromNetBuff.readInt();
		// Pour chaque donnée, je lis son NetBuffer en tant que tableau d'octets
		for (int iData = 0; iData < dataListSize; iData++) {
			byte[] scDataAsByteArray = readFromNetBuff.readByteArray(); //  tableau d'octets
			NetBuffer scDataAsNetBuffer = new NetBuffer(scDataAsByteArray); // buffer décodé
			int dataType = scDataAsNetBuffer.readInt(); // type de donnée
			if (dataType == SCBlockDataType.TRANSACTION.asInt()) { // transaction
				SCBlockData_transaction dataTransaction = SCBlockData_transaction.readFromNetBuffer(scDataAsNetBuffer, 0); // création de la transaction
				myData.add(dataTransaction); // ajout de la transaction au bloc
			} else {
				// il pourra y avoir, par la suite, d'autres types de données qu eles transaction simples
				//System.out.println("ERREUR : SCBlock.readFromNetBuffer() : donnée de type invalide dataType(" + dataType + ") != SCBlockDataType.TRANSACTION.asInt()(" + SCBlockDataType.TRANSACTION.asInt() + ")"); 
			}
			// Lecture de la signature du bloc
			if (hasBlockSignature) {
				blockSignature = readFromNetBuff.readString();
			}
		}
	}
	
	public NetBuffer writeToNetBuffer(boolean includeBlockSignature) {
		NetBuffer myBuffer = new NetBuffer();
		myBuffer.writeString(previousBlockSignature);
		myBuffer.writeString(authorPublicKey);
		int dataListSize = myData.size();
		myBuffer.writeInt(dataListSize);
		// Pour chaque donnée, j'écris son NetBuffer en tant que tableau d'octets
		for (int iData = 0; iData < dataListSize; iData++) {
			SCBlockData scData = myData.get(iData); // donnée
			NetBuffer scDataAsNetBuffer = scData.writeToNetBuffer(true); // écriture dans un NetBuffer (donnée avec la signature de l'auteur)
			byte[] scDataAsByteArray = scDataAsNetBuffer.convertToByteArray(); // conversion du buffer en tableau d'octets
			myBuffer.writeByteArray(scDataAsByteArray); // écriture dans le buffer du bloc
			//scData.getDataType().equals(SCBlockDataType.TRANSACTION)
		}
		if (includeBlockSignature)
			myBuffer.writeString(blockSignature);
		return myBuffer;
	}
	
	public boolean signBlock(String authorPrivateKey) {
		String blockHash = computeHash();
		blockSignature = RSA.sign(blockHash, authorPrivateKey);
		return (blockSignature != null);
	}
	
	/** Calcul du hash du bloc
	 * @return
	 */
	public String computeHash() {
		NetBuffer fullBlockDataAsBuffer = writeToNetBuffer(false); // sans inclure la signature (forcément)
		byte[] fullBlockDataAsByteArray = fullBlockDataAsBuffer.convertToByteArray();
		return RSA.sha256(fullBlockDataAsByteArray);
	}
	
	/** Vérifier la validité du bloc avec la signature et la clef publique de son créateur
	 * @return
	 */
	public boolean checkWithBlockSignature() {
		String blockHash = computeHash();
		return RSA.check(blockHash, blockSignature, authorPublicKey);
	}
	
	
	/** Créer un bloc depuis un NetBuffer
	 * @param readFromNetBuffer
	 * @param hasBlockSignature
	 */
	public SCBlock(NetBuffer readFromNetBuffer, boolean hasBlockSignature) {
		readFromNetBuffer(readFromNetBuffer, hasBlockSignature);
	}
	
	/** Créer un nouveau bloc vide
	 * @param arg_previousHash
	 * @param arg_authorPublicKey
	 */
	public SCBlock(String arg_previousHash, String arg_authorPublicKey) {
		previousBlockSignature = arg_previousHash; // l'ID du dernier bloc
		authorPublicKey = arg_authorPublicKey; // L'identifiant de l'auteur du bloc = adresse de l'auteur du bloc
		myData = new ArrayList<SCBlockData>(); // Liste des données (entre autre : les transactions)
	}
	
	/** Ajouter une donnée au bloc (une transaction par exemple)
	 * @param newDataToAdd
	 */
	public void addData(SCBlockData newDataToAdd) {
		myData.add(newDataToAdd);
	}
	
	/** Récupérer le montant du portefeuille d'un utilisateur de la chaine
	 *  
	 * @param ownerPublicKey
	 * @return
	 */
	public double getWalletVariation(String ownerPublicKey) {
		if (ownerPublicKey == null) return 0;
		double ownerVariation = 0;
		if (authorPublicKey.equals(ownerPublicKey)) { // créateur du bloc
			ownerVariation += rewardPerBlockCreation;
		}
		// Pour toutes les transactions, je regarde si l'utilisateur est intervenu
		for (int iData = 0; iData < myData.size(); iData++) {
			SCBlockData data = myData.get(iData);
			if (data.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				SCBlockData_transaction transaction = (SCBlockData_transaction) data;
				ownerVariation += transaction.getWalletVariationForUser(ownerPublicKey); // 0 si l'utilisateur n'est pas impliqué dans l'échange
			}
		}
		return ownerVariation;
	}
	
	public boolean transactionIsAlreadyInBlock(SCBlockData_transaction transactionToCheck) {
		if (transactionToCheck == null) return false;
		// Parcours de la liste des transactions du bloc
		for (int iData = 0; iData < myData.size(); iData++) {
			SCBlockData data = myData.get(iData);
			if (data.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				// Si c'est une transaction, je les compare
				SCBlockData_transaction compareToTransaction = (SCBlockData_transaction) data;
				if (compareToTransaction.equals(transactionToCheck)) {
					return true;
				}
				
			}
		}
		return false; // Aucune correspondance si je suis arrivé ici
	}
	
	//permet de check la présence d'une transaction dans un bloc passé en arg
	
	/*public SCBlock(String arg_previousHash, String arg_authorPublicKey, String arg_authorPrivateKey, ArrayList<SCBlockData> arg_myData) {
		previousBlockSignature = arg_previousHash;
		authorPublicKey = arg_authorPublicKey;
		myData = arg_myData;//(ArrayList<SCBlockData>) arg_myData.clone(); // -> s'assurer que la liste est bien clonée et les objets bien copiés
		
		// Calcul de la taille du buffer du bloc
		int rawBytesBufferSize = 0;
		for (int iData = 0; iData < myData.size(); iData++) {
			SCBlockData scData = myData.get(iData);
			if (scData.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				SCBlockData_transaction transaction = (SCBlockData_transaction) scData;
				int transactionSize = transaction.getRawByteArrayDataSize();
				if (transactionSize == 0) { // une transaction ne peut être vide
					criticalErrorOccured = true;
					return;
				}
				rawBytesBufferSize += transactionSize;
			}
		}
		
		dataAsByteArray = new byte[rawBytesBufferSize];
		int positionInBlockBuffer = 0;
		// Ecriture de toutes les données dans le buffer du bloc
		for (int iData = 0; iData < myData.size(); iData++) {
			SCBlockData scData = myData.get(iData);
			// Donnée de type transaction
			if (scData.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				SCBlockData_transaction transaction = (SCBlockData_transaction) scData;
				byte[] transactionBuffer = transaction.getRawByteArrayData();
				if (transactionBuffer == null) {
					criticalErrorOccured = true;
					return;
				}
				System.arraycopy(transactionBuffer, 0, dataAsByteArray, positionInBlockBuffer, transactionBuffer.length);
				positionInBlockBuffer += transactionBuffer.length;
			}// else { }
		}
		if (positionInBlockBuffer != dataAsByteArray.length) {
			System.out.println("ERREUR CRITUQUE constructeur SCBlock : positionInBlockBuffer(" + positionInBlockBuffer + ") != blockAsByteArray.length(" + blockAsByteArray.length + ")");
			criticalErrorOccured = true;
			return;
		}
		
		blockAsNetBuffer = new NetBuffer();
		blockAsNetBuffer.write();
		
		RSA.sha256(inputByteArray)
		
		RSA.sign(blockAsByteArray, arg_authorPrivateKey);
		
		
		// 1) Génération du nouveau bloc
		// 2) Calcul du hash
		// 3) Signature : je hash le tout (y compris le hash du bloc) et j'encrypte avec ma clef privée
		// cf. https://stackoverflow.com/questions/18257185/how-does-a-public-key-verify-a-signature
		// -> arg_authorPrivateKey est essentielle pour la signature du block
	}
	*/
	
	
	
}
