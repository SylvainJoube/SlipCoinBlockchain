package slip.blockchain.pos;

import java.util.ArrayList;

import slip.network.buffers.NetBuffer;

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
	public ArrayList<SCBlock> blockChain = new ArrayList<SCBlock>(); // C'est plutôt une "blockArrayList" dans ce cas... J'aurais aussi pu utiliser une LinkedList pour rester dans le thème !
	
	public ArrayList<SCBlock> debugGetBlockchain() {
		return blockChain;
	}
	
	// + Liste des transactions déjà broadcastées (pour ne pas les ré-émettre)
	
	private String nodeOwnerPublicKey;  // = identifiant de celui à qui appartient cette cellule
	private String nodeOwnerPrivateKey; // utile pour signer les données
	
	private ArrayList<SCNodeAddress> linkedToHosts = new ArrayList<SCNodeAddress>();
	
	/** Créer un noeud, en spéficiant les clefs de celui qui le possède (pour la singature d'une création de bloc)
	 * @param arg_ownerPublicKey   clef publique du portefeuille du possesseur du noeud
	 * @param arg_ownerPrivateKey  clef privée du portefeuille du possesseur du noeud
	 */
	public SCNode(String arg_ownerPublicKey, String arg_ownerPrivateKey) {
		nodeOwnerPublicKey = arg_ownerPublicKey;
		nodeOwnerPrivateKey = arg_ownerPrivateKey;
	}
	
	/** Créer un noeud en indiquand le portefeuille de celui qui le crée
	 * @param walletOfOwner  portefeuille du propriétaire du noeud (pour la singature lors des créations de bloc)
	 */
	public SCNode(SCCoinWallet walletOfOwner) {
		this(walletOfOwner.getPublicKey(), walletOfOwner.getPrivateKey());
	}
	
	public int get_bufferedDataList_size() {
		return bufferedDataList.size();
	}
	public int get_blockChain_size() {
		return blockChain.size();
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
	
	public ArrayList<SCBlockData> diffBlockChain(ArrayList<SCBlock> receivedBlockChain) {
		ArrayList<SCBlock> myBlockChain = this.blockChain;
		ArrayList<SCBlockData> dataToAdd= new ArrayList<SCBlockData>();
		int i = 0;
		boolean transactionInBlockChain = false;

		for (SCBlock currentBlock : myBlockChain) {
			for (SCBlockData data : currentBlock.getData()) {
				transactionInBlockChain = false;
				if(data.getDataType() == SCBlockDataType.TRANSACTION) {
					for (SCBlock receivedBlock : receivedBlockChain) {
						if (receivedBlock.transactionIsAlreadyInBlock((SCBlockData_transaction)data)) {
							transactionInBlockChain = true;
							break;
						}
					}
				}
				if(transactionInBlockChain) {
					dataToAdd.add(data);
				}
			}
		}
		return dataToAdd;
	}
	
	public ArrayList<byte[]> convertBlockChainToByteArray() {
		return convertBlockChainToByteArray(this.blockChain);
	}
	
	public static ArrayList<byte[]> convertBlockChainToByteArray(ArrayList<SCBlock> blockChain) {
		ArrayList<byte[]> myBlockchainAsByte = new ArrayList<byte[]>();
		for (SCBlock block : blockChain) {
			myBlockchainAsByte.add(block.writeToNetBuffer(true).convertToByteArray());
		}
		return myBlockchainAsByte;
	}
	
	/** Réception de la dernière partie blockchain concurrente à la mienne
	 * 
	 * -> ma blockchain va être réarrangée
	 */
	public void receiveNewBlockChainPart(ArrayList<SCBlock> receivedBlockChain) {
		
		// Plus tard, il serait bon de procéder ainsi : (pour essayer d'avoir une seule chaine identique partout)
		
		// 1) Vérifier la validité de la chaîne reçue (signature blocs et transactions)
		// 2) Je regarde si j'ai le premier bloc de la chaine (quelque part dans ma chaîne) 
		// 3) Si ce n'est qu'un ajout de blocs par rapport à la mienne, OK, je met à jour ma chaîne
		// 4) S'il y a conflit : si ma chaîne est la plus longue, je l'ignore, j'envoie ma chaîne à celui qui vient de m'envoyer sa chaîne
		// 5) Si ma chaine est plus courte mais qu'il y a des blocs en conflit : je reprends toutes les données dans mon buffer
		///    je supprime mes blocs de ma chaîne et j'accepte cette autre chaîne, je re-broadcast les données qui ne sont plus dans ma chaîne
		// 6) Si ma chaine est de même taille (blocs) : je conserve la chaîne de plus grande taille totale (octets) et je fais comme en 4) (broadcast des transactions)
		// 7) Si ma chaine a été changée (et donc validée), je relaye l'information : nouvelle chaîne
		
		// Il s'agit ici d'une implémentation partielle
		
		if (receivedBlockChain == null) return;
		SCNode_rcvBlockChainResult result = new SCNode_rcvBlockChainResult();
		// 1) Je vérifie la chaîne reçue : s'il y a la moindre erreur dans la signature des blocs, je l'ignore totalement
		if(!(checkBlockChainValidity(receivedBlockChain) && checkMyBlockChain())) {
			return;
		}
		//System.out.println("receiveNewBlockChainPart : 1");
		// 1.5) Je regarde si j'ai le premier bloc de la chaine (quelque part dans ma chaîne) 
		//-> Les blocs suivant doivent correspondre à ceux de ma chaîne, sinon, on se retrouve dans une des sitiations décrites en 4, 5 et 6
		//-> si je n'ai pas le premier bloc de la chaine envoyée dans ma chaine à moi, je demande les blocs précédents à l'autre
		ArrayList<SCBlock> myBlockChain = this.blockChain;
		String firstBlockSignature = receivedBlockChain.get(0).getBlockSignature();
		boolean isInBlockChain = false;
		int indexBlockBeggining = 0;
		for (SCBlock aBlock : myBlockChain) {
			if (aBlock.getBlockSignature().equals(firstBlockSignature)) {
				//System.out.println("receiveNewBlockChainPart : isInBlockChain = true");
				isInBlockChain = true;
				break;
			}
			indexBlockBeggining++;
		}
		//System.out.println("receiveNewBlockChainPart : 2");
		boolean chainMismatch = false;
		if (!isInBlockChain) {
			if (receivedBlockChain.get(0).getPreviousBlockSignature().equals("0")) {
				return; //si on a le genesis block, on ne demande pas de blocs...
			}
			//TODO implémenter la fonction qui demande plus de blocs
			return;
		} else {
			int indexToParse = indexBlockBeggining;
			for (SCBlock aBlock : receivedBlockChain) {
				if (indexToParse < myBlockChain.size()) {
					SCBlock myCurrentBlock = myBlockChain.get(indexToParse);
					if (!aBlock.getBlockSignature().equals(myCurrentBlock.getBlockSignature())) {
						chainMismatch = true;
						break;
					}
				}
				indexToParse++;
			}
		}
		//System.out.println("receiveNewBlockChainPart : 3");
		if (chainMismatch) {
			if (myBlockChain.size() > receivedBlockChain.size()) {
				if (receivedBlockChain.get(0).getPreviousBlockSignature().equals("0")) {
					return; //si on a le genesis block, on ne demande pas de blocs, je garde ma blockChain
					//TODO : envoyer mes blocs à l'autre
				}
				//on redemande des blocs, sauf si on a le genesis
				return;
			}
			if (myBlockChain.size() == receivedBlockChain.size()) {
				if (receivedBlockChain.get(0).getPreviousBlockSignature().equals("0")) {
					ArrayList<byte[]> myBlockchainAsByte = convertBlockChainToByteArray(myBlockChain);
					ArrayList<byte[]> receivedBlockChainAsByte = convertBlockChainToByteArray(receivedBlockChain);
					int myBlockChainSizeAsByte = 0;
					int receivedBlockChainSizeAsByte = 0;
					for (byte[] array : myBlockchainAsByte) {
						myBlockChainSizeAsByte += array.length;
					}
					for (byte[] array : receivedBlockChainAsByte) {
						receivedBlockChainSizeAsByte += array.length;
					}
					if (myBlockChainSizeAsByte > receivedBlockChainSizeAsByte) {
						return;
					} else if (myBlockChainSizeAsByte <= receivedBlockChainSizeAsByte) {
						this.blockChain = receivedBlockChain;
						ArrayList<SCBlockData> toAdd = diffBlockChain(receivedBlockChain); //contient les données non présentes dans l'autre blockChain
						for (SCBlockData data : toAdd) {
							addToDataBuffer(data);
						}
						//broadcast le toAdd
					}
				}
				
			}
			if (myBlockChain.size() < receivedBlockChain.size()) {
				if (receivedBlockChain.get(0).getPreviousBlockSignature().equals("0")) {
					ArrayList<SCBlockData> toAdd = diffBlockChain(receivedBlockChain); //contient les données non présentes dans l'autre blockChain
					this.blockChain = receivedBlockChain;
					for (SCBlockData data : toAdd) {
						addToDataBuffer(data);
					}
					//broadcast le toAdd
				}
				return;
			}
		} else {
			// si pas de chainMismatch, les blocs à ajouter sont ceux d'index maBlockChain.size() - indexBlockBeggining;
			int indexBlockToAdd = myBlockChain.size() - indexBlockBeggining;
			if (!(indexBlockToAdd >= receivedBlockChain.size())) {
				// on a pas de block à ajouter
				return;
			} else {
				ArrayList<SCBlock> newBlockChain = new ArrayList<SCBlock>(myBlockChain);
				for (int i = indexBlockToAdd; i < receivedBlockChain.size(); i++) {
					newBlockChain.add(receivedBlockChain.get(indexBlockToAdd));
				}
				if (checkBlockChainValidity(newBlockChain)) {
					this.blockChain = newBlockChain; //si la blockchain générée est valide, on l'utilise
				} else {
					return;
				}
			}
		}
		//System.out.println("receiveNewBlockChainPart : 4");
		// 2) Si ce n'est qu'un ajout de blocs par rapport à la mienne, OK, je met à jour ma chaîne
		// 3) S'il y a conflit : si ma chaîne est la plus longue, je l'ignore, j'envoie ma chaîne à celui qui vient de m'envoyer sa chaîne
		// 4) Si ma chaine est plus courte mais qu'il y a des blocs en conflit : je reprends toutes les données dans mon buffer
		///    je supprime mes blocs de ma chaîne et j'accepte cette autre chaîne, je re-broadcast les données qui ne sont plus dans ma chaîne
		// 5) Si ma chaine est de même taille (blocs) : je conserve la chaîne de plus grande taille totale (octets) et je fais comme en 4) (broadcast des transactions)
		// 6) Si ma chaine a été changée (et donc validée), je relaye l'information : nouvelle chaîne
		
		// 7) -> j'ai probablement oublié des cas ! ^^'
	}
	
	public void broadcastMyBlockChain(int nbDerniersBlocs) {
		// 1) Broadcast de ma chaîne à tous les intermédiaires que je connais
		// Pour ne pas faire de boucle, -> TTL + je hash cette blockchain et je garde une liste des hash que j'ai déjà transmis pour ne pas retransmettre indéfiniment
		
	}
	
	
	
	
	
	
	
}
