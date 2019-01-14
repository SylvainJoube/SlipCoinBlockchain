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
	
	/** Récupérer l'argent dispo dans le compte d'une personne, depuis la blockchain
	 * @param ownerPublicKey
	 * @return
	 */
	public int getWalletAmount(String ownerPublicKey) {
		int ownedAmount = 0;
		for (int iBlock = 0; iBlock < blockChain.size(); iBlock++) {
			SCBlock block = blockChain.get(iBlock);
			
		}
		
	}
	
	/** Vérifier une transaction avec la blockchain et le buffer dont je dispose à l'heure actuelle
	 *  Je re-calcule le montant possédé par l'envoyeur de la transaction et je vérifie qu'il l'a bien
	 * @return
	 */
	public boolean checkTransactionAmount(SCBlockData_transaction transaction) {
		if (transaction == null) return false;
		if (transaction.criticalErrorOccured) return false;
		if (transaction.checkValidity() == false) return false;
		return true;// TODO
	}
	
	/** Ajout de donnée au buffer de la cellule (réception de donnée à ajouter, ou création)
	 * @param newData donnée à ajouter au buffer
	 */
	public void addToDataBuffer(SCBlockData newData) {
		// 1) Vérifier que la donnée est bien valide (avec la signature de l'auteur qui l'a émise)
		// 2) Regarder si la donnée est déjà dans le buffer
		// 3) Regarder si la donnée est déjà dans la blockchain
		// 4) Si la donnée a passé toutes les étapes précédentes, je l'ajoute à mon buffer
	}
	
	/** Assembler (= créer) un nouveau bloc avec les données du buffer
	 *  Le nouveau bloc sera
	 * @return
	 */
	public boolean assembleNewBlockWithBufferedData() { // SCBlock
		
		//if (bufferedDataList.size() == 0) return false; // ne pas créer inutilement de bloc vide !
		
		String previousBlockHash = "0";
		if (blockChain.size() != 0) {
			SCBlock previousBlock = blockChain.get(blockChain.size() - 1);
			previousBlockHash = previousBlock.getMyHash();
		}
		// Avant : bien vérifier que dans ma chaîne, il n'y a aucune donnée identique à celle que j'ai dans mon buffer
		
		// Toutes les transactions du buffer sont supposées valides (je n'ajoute aucune transaction à mon buffer)
		SCBlock newBlock = new SCBlock(previousBlockHash, nodeOwnerPublicKey);
		for (int iBuffData = 0; iBuffData < bufferedDataList.size(); iBuffData++) {
			newBlock.addData(bufferedDataList.get(iBuffData));
		}
		
		newBlock.signBlock(nodeOwnerPrivateKey); // c'est mon block à moi
		
		// J'ajoute mon bloc à ma chaîne
		blockChain.add(newBlock);
		// Je supprime les données de mon buffer
		bufferedDataList.clear();
		// Je broadcast ma chaine (les X derniers blocs)
		broadcastMyBlockChain();
		return true;
		
	}
	
	/** Réception d'une blockchain concurrente à la mienne
	 * 
	 */
	public void receiveNewBlockChain() {
		// 1) Jé vérifie la chaîne reçue : s'il y a la moindre erreur dans la signature des blocs, je l'ignore totalement
		// 2) Si ce n'est qu'un ajout de blocs par rapport à la mienne, OK, je met à jour ma chaîne
		// 3) S'il y a conflit : si ma chaîne est la plus longue, je l'ignore
		// 4) Si ma chaine est plus courte mais qu'il y a des blocs en conflit : je reprends toutes les données dans mon buffer
		///    je supprime mes blocs de ma chaîne et j'accepte cette autre chaîne, je re-broadcast les données qui ne sont plus dans ma chaîne
		// 5) Si ma chaine est de même taille (blocs) : je conserve la chaîne de plus grande taille totale (octets)
		// 6) Si ma chaine a été changée (et donc validée), je relaye l'information : nouvelle chaîne
		
	}
	
	public void broadcastMyBlockChain() {
		// 1) Broadcast de ma chaîne à tous les intermédiaires que je connais
		// Pour ne pas faire de boucle, -> TTL + je hash cette blockchain et je garde une liste des hash que j'ai déjà transmis pour ne pas retransmettre indéfiniment
	}
	
	
	
}
