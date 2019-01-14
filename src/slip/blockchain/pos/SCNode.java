package slip.blockchain.pos;

import java.util.ArrayList;

/**
 * Une cellule faisant tourner la blockchain SlipCoin
 * 
 * Une cellule n'a pas forcément d'ID propre (à voir)
 * Elle a l'ID de celui qui la possède
 *
 */

public class SCNode {

	public static final double initialWalletAmount = 10;
	public static final boolean forceIdenticalTimestamp = false; // Pour faire des tests et bien vérifier que deux transactions identiques sont bien illégales
	public static final long forceTimestampValue = 12345;
	
	//public static mainNode = keurkeur
	
	// Liste des données en attente d'insertion dans la chaîne (trasations reçues d'autres cellules + mes transactions)
	private ArrayList<SCBlockData> bufferedDataList = new ArrayList<SCBlockData>();
	private ArrayList<SCBlock> blockChain = new ArrayList<SCBlock>(); // C'est plutôt une "blockArrayList" dans ce cas... J'aurais aussi pu utiliser une LinkedList pour rester dans le thème !
	
	// + Liste des transactions déjà broadcastées (pour ne pas les ré-émettre)
	
	private String nodeOwnerPublicKey;  // = identifiant de celui à qui appartient cette cellule
	private String nodeOwnerPrivateKey; // utile pour signer les données
	
	private ArrayList<SCNodeAddress> linkedToHosts = new ArrayList<SCNodeAddress>();
	
	public SCNode(String arg_ownerPublicKey, String arg_ownerPrivateKey) {
		nodeOwnerPublicKey = arg_ownerPublicKey;
		nodeOwnerPrivateKey = arg_ownerPrivateKey;
	}
	
	// + charger du disque ma chaîne
	// + tard : faire que l'intégralité de la chaîne ne soit pas chargée en mémoire vive mais reste sur le disque
	
	public static double getWalletAmountFromNodeChain(SCNode node, String ownerPublicKey) {
		
		return node.getWalletAmount(ownerPublicKey);
	}
	
	/** Récupérer l'argent dispo dans le compte d'une personne, depuis la blockchain
	 * Plus tard, il serait envisagable de faire des blocs signés par l'ensemble du réseau (51+%) pour faire un récapitulatif des wallets
	 * @param ownerPublicKey
	 * @return
	 */
	public double getWalletAmount(String ownerPublicKey) { // , boolean alsoUseBuffer)
		double ownedAmount = 0 + SCNode.initialWalletAmount; // montant initial (pour pouvoir faire des transactions dès le début
		// Montant dans la blockchain
		for (int iBlock = 0; iBlock < blockChain.size(); iBlock++) {
			SCBlock block = blockChain.get(iBlock);
			ownedAmount += block.getWalletVariation(ownerPublicKey);
		}
		
		// if (alsoUseBuffer) toujours utiliser son buffer des transactions pas encore dans la chaîne
		for (int iBufferedData = 0; iBufferedData < bufferedDataList.size(); iBufferedData++) {
			SCBlockData data = bufferedDataList.get(iBufferedData);
			if (data.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				SCBlockData_transaction transaction = (SCBlockData_transaction) data;
				ownedAmount += transaction.getWalletVariationForUser(ownerPublicKey);
			}
		}
		return ownedAmount;
	}
	
	/** Vérifier une transaction avec la blockchain et le buffer dont je dispose à l'heure actuelle
	 *  Je re-calcule le montant possédé par l'envoyeur de la transaction et je vérifie qu'il l'a bien
	 * @return
	 */
	public boolean checkTransactionAmountValidity(SCBlockData_transaction transaction) {
		if (transaction == null) return false;
		if (transaction.criticalErrorOccured) return false;
		if (transaction.checkSignatureValidity() == false) return false;
		if (transaction.amount <= 0) return false;
		String senderKey = transaction.senderPublicKey;
		double senderWalletAmount = getWalletAmount(senderKey);
		if (senderWalletAmount < transaction.amount) {
			return false; // fonds insuffisants
		}
		return true;
	}
	
	
	/** Regarde si la transaction est dans le buffer, non encore ajouté à la chaîne
	 * @param transactionToCheck
	 * @return 
	 */
	public boolean transactionIsAlreadyInDataBuffer(SCBlockData_transaction transactionToCheck) {
		if (transactionToCheck == null) return false;
		//System.out.println("transactionIsAlreadyInDataBuffer : bufferedDataList.size() = " + bufferedDataList.size());
		// Parcours de la liste de ce qu'il y a en buffer
		for (int iDataInBuffer = 0; iDataInBuffer < bufferedDataList.size(); iDataInBuffer++) {
			SCBlockData data = bufferedDataList.get(iDataInBuffer);
			if (data.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				// Si c'est une transaction
				SCBlockData_transaction compareToTransaction = (SCBlockData_transaction) data;
				if (compareToTransaction.equals(transactionToCheck)) {
					return true;
				}
				
			}
		}
		return false; // aucune correspondance
	}
	
	/** Regarde si la transaction est dans la blockchain
	 * @param transactionToCheck
	 * @return
	 */
	public boolean transactionIsAlreadyInBlockchain(SCBlockData_transaction transactionToCheck) {
		if (transactionToCheck == null) return false;
		// Parcours de la totalité de la chaîne
		for (int iBlock = 0; iBlock < blockChain.size(); iBlock++) {
			SCBlock block = blockChain.get(iBlock);
			if (block.transactionIsAlreadyInBlock(transactionToCheck)) {
				return true;
			}
		}
		return false; // aucune correspondance
	}
	//(int arg_amount, String arg_senderKey, String arg_receiverKey, long arg_timeStamp, boolean hasToSignTransaction, String senderPrivateKey, String arg_senderSignature)
	
	public boolean newTransaction(double amount, String senderPrivateKey, String senderPublicKey, String receiverPublicKey) {
		long currentTimeStamp = System.currentTimeMillis();
		if (SCNode.forceIdenticalTimestamp) currentTimeStamp = SCNode.forceTimestampValue; // essentiellement pour faire du débug et montrer que ça marche bien !
		SCBlockData_transaction newTransaction = 
				new SCBlockData_transaction(amount, senderPublicKey, receiverPublicKey, currentTimeStamp, true, senderPrivateKey, null);
		return addToDataBuffer(newTransaction);
	}
	
	/** Ajout de donnée au buffer de la cellule (réception de donnée à ajouter, ou création)
	 * @param newData donnée à ajouter au buffer
	 * @return vrai si la donnée a été ajoutée au buffer et qu'elle n'était pas présente dans la 
	 */
	public boolean addToDataBuffer(SCBlockData newData) {
		
		try {
			// Cas de l'ajout d'une transaction
			if (newData.getDataType().equals(SCBlockDataType.TRANSACTION)) {
				
				// 1) Vérifier que la donnée est bien valide (avec la signature de l'auteur qui l'a émise)
				SCBlockData_transaction transaction = (SCBlockData_transaction) newData; // en cas d'attaque, ce cast pourrait mal se passer
				boolean authentifiedTransaction = transaction.checkSignatureValidity();
				if (! authentifiedTransaction) return false; // transaction invalide
				boolean validAmountTransaction = checkTransactionAmountValidity(transaction);
				if (! validAmountTransaction) return false;
				// 2) Regarder si la donnée est déjà dans le buffer
				if (transactionIsAlreadyInDataBuffer(transaction)) return false;
				// 3) Regarder si la donnée est déjà dans la blockchain
				if (transactionIsAlreadyInBlockchain(transaction)) return false;
				
				/*boolean scc = transactionIsAlreadyInDataBuffer(transaction);
				System.out.println("SCNode.addToDataBuffer : transactionIsAlreadyInDataBuffer 1 = " + scc);
				scc = transactionIsAlreadyInDataBuffer(transaction);
				System.out.println("SCNode.addToDataBuffer : transactionIsAlreadyInDataBuffer 2 = " + scc);*/
				
				// 4) Si la donnée a passé toutes les étapes précédentes, je l'ajoute à mon buffer
				bufferedDataList.add(transaction);
				return true;
			}
		} catch (Exception castException) {
			return false;
		}
		return false;
	}
	
	/** Assembler (= créer) un nouveau bloc avec les données du buffer
	 *  Le nouveau bloc sera
	 * @param useThisPublicKeyInstead  null si utiliser la clef du possesseur de la SCNode actuelle
	 * @return
	 */
	public boolean assembleNewBlockWithBufferedData(String useThisPublicKeyInstead, String useThisPrivateKeyInstead) { // SCBlock
		
		//if (bufferedDataList.size() == 0) return false; // ne pas créer inutilement de bloc vide !
		
		String previousBlockSignature = "0";
		if (blockChain.size() != 0) {
			SCBlock previousBlock = blockChain.get(blockChain.size() - 1);
			previousBlockSignature = previousBlock.getBlockSignature();
		}
		// Avant : bien vérifier que dans ma chaîne, il n'y a aucune donnée identique à celle que j'ai dans mon buffer

		if (useThisPublicKeyInstead == null) useThisPublicKeyInstead = nodeOwnerPublicKey;
		if (useThisPrivateKeyInstead == null) useThisPrivateKeyInstead = nodeOwnerPrivateKey;
		
		// Toutes les transactions du buffer sont supposées valides (je n'ajoute aucune transaction à mon buffer)
		SCBlock newBlock = new SCBlock(previousBlockSignature, useThisPublicKeyInstead);
		for (int iBuffData = 0; iBuffData < bufferedDataList.size(); iBuffData++) {
			newBlock.addData(bufferedDataList.get(iBuffData));
		}
		
		newBlock.signBlock(useThisPrivateKeyInstead); // c'est mon block à moi
		
		// J'ajoute mon bloc à ma chaîne
		blockChain.add(newBlock);
		// Je supprime les données de mon buffer
		bufferedDataList.clear();
		// Je broadcast ma chaine (les X derniers blocs)
		broadcastMyBlockChain(3);
		return true;
		
	}
	
	
	
	public static boolean checkBlockChainValidity(ArrayList<SCBlock> receivedBlockChain) {
		if (receivedBlockChain == null) return false;
		if (receivedBlockChain.size() == 0) return false; // test
		//String previousBlockSignature = receivedBlockChain.get(0).getBlockSinature();
		// Je vérifie la validité des blocs
		
		for (int iBlock = 0; iBlock < receivedBlockChain.size(); iBlock++) { // (SCBlock block : receivedBlockChain)
			SCBlock block = receivedBlockChain.get(iBlock);
			if (! block.checkWithBlockSignature()) return false; // bloc invalide
		}
		// Je vérifie que tous les blocs se suivent
		for (int iBlock = receivedBlockChain.size() - 1; iBlock >= 1; iBlock--) {
			SCBlock previousBlock = receivedBlockChain.get(iBlock - 1);
			SCBlock currentBlock = receivedBlockChain.get(iBlock);
			
			String previousBlockSignature = previousBlock.getBlockSignature();
			String previousBlockSignatureStoredInCurrentBlock = currentBlock.getPreviousBlockSignature();
			
			if (! previousBlockSignature.equals(previousBlockSignatureStoredInCurrentBlock)) {
				//System.out.println("ERREUR SCNode.checkBlockChainValidity : previousBlockSignature not equals previousBlockSignatureStoredInCurrentBlock");
				return false;
			}
		}
		return true;
	}
	
	public boolean checkMyBlockChain() {
		return checkBlockChainValidity(blockChain);
	}
	
	
	
	
	/** Réception de la dernière partie blockchain concurrente à la mienne
	 * 
	 * -> ma blockchain va être réarrangée
	 */
	public SCNode_rcvBlockChainResult receiveNewBlockChainPart(ArrayList<SCBlock> receivedBlockChain) {
		if (receivedBlockChain == null) return null;
		SCNode_rcvBlockChainResult result = new SCNode_rcvBlockChainResult();
		
		// Je vérifie la validité de cette chaîne
		
		
		
		// 1) Je vérifie la chaîne reçue : s'il y a la moindre erreur dans la signature des blocs, je l'ignore totalement
		// 1.5) Je regarde si j'ai le premier bloc de la chaine
		// 2) Si ce n'est qu'un ajout de blocs par rapport à la mienne, OK, je met à jour ma chaîne
		// 3) S'il y a conflit : si ma chaîne est la plus longue, je l'ignore, je l'envoie à celui qui vient de m'envoyer sa chaîne
		// 4) Si ma chaine est plus courte mais qu'il y a des blocs en conflit : je reprends toutes les données dans mon buffer
		///    je supprime mes blocs de ma chaîne et j'accepte cette autre chaîne, je re-broadcast les données qui ne sont plus dans ma chaîne
		// 5) Si ma chaine est de même taille (blocs) : je conserve la chaîne de plus grande taille totale (octets)
		// 6) Si ma chaine a été changée (et donc validée), je relaye l'information : nouvelle chaîne
		return result;
	}
	
	public void broadcastMyBlockChain(int nbDerniersBlocs) {
		// 1) Broadcast de ma chaîne à tous les intermédiaires que je connais
		// Pour ne pas faire de boucle, -> TTL + je hash cette blockchain et je garde une liste des hash que j'ai déjà transmis pour ne pas retransmettre indéfiniment
		
	}
	
	
	
	
	
	
	
}
