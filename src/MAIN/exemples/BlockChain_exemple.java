package MAIN.exemples;

import java.util.ArrayList;

import slip.blockchain.pos.SCBlockData_transaction;
import slip.blockchain.pos.SCCoinWallet;
import slip.blockchain.pos.SCNode;
import slip.network.buffers.NetBuffer;
import slip.security.common.RSA;

/**
 * Exemple de création et utilisation de la blockchain
 * Si les noeuds sont matériellement suffisamment fiables et sauf rare concours de circonstances,
 * cette chaîne garantit que toutes les transactions seront bien réparties dans le réseau.
 * Les noeuds/cellules du réseau ont une blockchain et un buffer de transactions pas encore dans la blockchain.
 * 
 * 
 * -> en cas de différence entre les blockchaines :
 * - Je vérifie si elle est valide
 * - Je prends la plus longue, et toutes les transactions qui ont été supprimées sont mises dans mon buffer "en attente"
 * 
 * 
 * Par manque de temps, nous n'avons pas implémenté de réel réseau entre les noeuds.
 * Néanmoins, les fonctionnalités de synchronisation de chaînes entre noeuds sont opérationnelles.
 * 
 */

public class BlockChain_exemple {
	
	
	
	
	public static void main(String[] args) {
		
		
		// Pour que des transactions soient possibles dans cette démo, tous les comptes ont 10 coins initialement.
		SCCoinWallet wallet_etienne = SCCoinWallet.createNewWallet("Etienne");
		SCCoinWallet wallet_sylvain = SCCoinWallet.createNewWallet("Sylvain");
		SCCoinWallet wallet_long = SCCoinWallet.createNewWallet("Long");
		SCCoinWallet wallet_antonin = SCCoinWallet.createNewWallet("Antonin");
		SCCoinWallet wallet_prof = SCCoinWallet.createNewWallet("Prof");
		// Création des noeuds du réseau
		SCNode node_etienne = new SCNode(wallet_etienne);
		SCNode node_sylvain = new SCNode(wallet_sylvain);
		SCNode node_sylvain2 = new SCNode(wallet_sylvain); // plusieurs nodes pour un portefeuille donné (possible et légal)
		SCNode node_sylvain3 = new SCNode(wallet_sylvain);
		SCNode node_long = new SCNode(wallet_long);
		SCNode node_antonin = new SCNode(wallet_antonin);
		SCNode node_prof = new SCNode(wallet_prof);
		// Liste des porte monnaie, pour les afficher simplement
		ArrayList<SCCoinWallet> a1Wallet = new ArrayList<SCCoinWallet>();
		a1Wallet.add(wallet_etienne);
		a1Wallet.add(wallet_sylvain);
		a1Wallet.add(wallet_long);
		a1Wallet.add(wallet_antonin);
		a1Wallet.add(wallet_prof);
		
		log("");
		log("TEST DE LA SECURITE DES TRANSACTIONS");
		log("");
		
		// Test de la sécurité d'une transaction
		// 1) Emission d'une nouvelle transaction
		SCBlockData_transaction transaction;
		double originalAmount = 2.4532486;
		transaction = SCBlockData_transaction.createTransaction(originalAmount, wallet_etienne, wallet_sylvain.getPublicKey());
		log("Nouvelle transaction (Etienne->Sylain)");
		log("  valide = " + transaction.checkSignatureValidity());
		
		// 2) Sylvain la modifie et essaie d'avoir plus d'argent
		log("Sylvain modifie le montant de la transaction");
		transaction.amount = 8;
		log("  valide = " + transaction.checkSignatureValidity());
		
		log("Sylvain remet la bonne valeur pour le montant");
		transaction.amount = originalAmount;
		log("  valide = " + transaction.checkSignatureValidity());
		// Fera le même chose si on modifie n'importe quel champ dans la transaction, sans avoir la clef privée du portefeuille qui l'a émis.
		// Modifier la transaction tout en la gardant valide nécessiterait une puissance de calcul immense (ou un ordinateur quantique, mais on en est pas encore là !)
		
		// 3) Sylvain essaie de prendre de l'argent à quelqu'un d'autre
		log("Sylvain modifie l'adresse d'émission de la transaction (Etienne -> Antonin)");
		transaction.senderPublicKey = wallet_antonin.getPublicKey();
		log("  valide = " + transaction.checkSignatureValidity());
		
		transaction.senderPublicKey = wallet_etienne.getPublicKey();
		log("Sylvain remet la bonne valeur");
		log("  valide = " + transaction.checkSignatureValidity());

		log("");
		log("TEST DE L'AJOUT A UN NOEUD");
		log("");
		
		log("Porte monnaies, dans le noeud d'Etienne :");
		showWallets(a1Wallet, node_etienne, true);
		log("");
		
		boolean sccessfullyAdded;
		sccessfullyAdded = node_etienne.addToDataBuffer(transaction);
		log("Tentative d'ajout d'une transaction valide");
		log("  ok = " + sccessfullyAdded);
		// Copie de la transaction, ajout en double :
		NetBuffer netTransaction = transaction.writeToNetBuffer(true);
		SCBlockData_transaction receivedTransaction = SCBlockData_transaction.readFromNetBuffer(netTransaction, 0);
		
		sccessfullyAdded = node_etienne.addToDataBuffer(receivedTransaction);
		log("Tentative d'ajout de la même transaction au même noeud, reçue (pas le même objet transaction, donc)");
		log("  ok = " + sccessfullyAdded);
		
		sccessfullyAdded = node_long.addToDataBuffer(receivedTransaction);
		log("Tentative d'ajout de la même transaction à un noeud différent, reçue (pas le même objet donc)");
		log("  ok = " + sccessfullyAdded);
		
		// Consenssus : les blocs des nodes sont synchronisés : c'est la chaine la plus longue qui est considérée comme valide,
		//   les transactions d'une chaîne plus petite et ne collant pas à la chaîne principale sont remis dans le buffer d'émisson
		//   du noeud qui vient d'adopter la chaîne principale.
		
		log("");
		log("Montant des porte-monnaie, après transaction (noeud d'Etienne) :");
		showWallets(a1Wallet, node_etienne, true);
		log("");
		boolean success;
		log("J'essaie de prendre plus d'argent qu'il n'en a à Antonin :");
		transaction = SCBlockData_transaction.createTransaction(54, wallet_antonin, wallet_sylvain.getPublicKey());
		success = node_etienne.addToDataBuffer(transaction);
		log("Réussite = " + success);
		
		// Plusieurs transactions
		transaction = SCBlockData_transaction.createTransaction(4.78965, wallet_antonin, wallet_sylvain.getPublicKey());
		node_etienne.addToDataBuffer(transaction);
		transaction = SCBlockData_transaction.createTransaction(8.7475, wallet_long, wallet_sylvain.getPublicKey());
		node_etienne.addToDataBuffer(transaction);
		transaction = SCBlockData_transaction.createTransaction(2.875422, wallet_etienne, wallet_sylvain.getPublicKey());
		node_etienne.addToDataBuffer(transaction);
		transaction = SCBlockData_transaction.createTransaction(19.75, wallet_sylvain, wallet_prof.getPublicKey());
		node_etienne.addToDataBuffer(transaction);
		
		log("");

		System.out.println("Node d'Etienne : nombre de transactions dans son buffer = " + node_etienne.get_bufferedDataList_size());
		System.out.println("Node d'Etienne : nombre de blocs dans sa blockChain = " + node_etienne.get_blockChain_size());
		log("");
		log("- ETIENNE CONSTITUE UN BLOC - ");
		node_etienne.assembleNewBlockWithBufferedData(wallet_etienne.getPublicKey(), wallet_etienne.getPrivateKey());
		System.out.println("Node d'Etienne : nombre de transactions dans son buffer = " + node_etienne.get_bufferedDataList_size());
		System.out.println("Node d'Etienne : nombre de blocs dans sa blockChain = " + node_etienne.get_blockChain_size());
		
		
		log("");
		log("Montant des porte-monnaie, après toutes les transactions + constutution du bloc (noeud d'Etienne) :");
		showWallets(a1Wallet, node_etienne, true);
		

		log("");
		log("Enfin, Etienne crée pleins de blocs (+récompenses)");
		for (int iBlock = 0; iBlock < 10; iBlock++)
			node_etienne.assembleNewBlockWithBufferedData(wallet_etienne.getPublicKey(), wallet_etienne.getPrivateKey());

		System.out.println("Node d'Etienne : nombre de transactions dans son buffer = " + node_etienne.get_bufferedDataList_size());
		System.out.println("Node d'Etienne : nombre de blocs dans sa blockChain = " + node_etienne.get_blockChain_size());
		
		log("");
		log("Montant des porte-monnaie (noeud d'Etienne) :");
		showWallets(a1Wallet, node_etienne, false);
		
		
		
	}
	
	public static void showWallets(ArrayList<SCCoinWallet> walletList, SCNode fromNode, boolean showFullWalletAddress) {
		for (SCCoinWallet wallet : walletList) {
			wallet.updateWallet(fromNode);
			if (showFullWalletAddress)
				System.out.println(wallet);
			else
				System.out.println(wallet.toSimpleString());
		}
	}
	
	/** Plus simple à écrire que System.out.println();
	 * @param message message à écrire sur la console
	 */
	public static void log(String message) {
		System.out.println(message);
	}
	
}
