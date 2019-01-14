package slip.test;

import java.util.ArrayList;

import slip.blockchain.pos.SCBlockData_transaction;
import slip.blockchain.pos.SCNode;
import slip.network.buffers.NetBuffer;
import slip.network.tcp.TCPClient;
import slip.network.tcp.TCPServer;
import slip.security.common.RSA;

/**
 * Démonstration de la manière dont fonctionne le TCPServer et TCPClient
 *
 */



///!!\ Ici, je lance un nouveau thread pour simuler une application serveur, rien de plus
class ApplicationServeur implements Runnable {
	
	/** Ecrire un message sur la console (+ rapide à écrire !)
	 * @param infoMessage message à écrire
	 */
	public static void log(String infoMessage) {
		synchronized(WriteOnConsoleClass.LOCK) { System.out.println("ApplicationServeur : " + infoMessage); } //System.out.flush();
		//System.out.println("ApplicationServeur : " + infoMessage);
	}

	public static int nextClientID = 1;
	
	class CustomServerClient {
		public TCPClient tcpSock;
		private final int ID;
		private boolean estAuthentifie = false;
		private String nomDeCompte, motDePasse;
		
		public boolean estActuellementAuthentifie() {
			return estAuthentifie;
		}
		public void vientDEtreAuthentifie(String arg_compte, String arg_pass) {
			nomDeCompte = arg_compte;
			motDePasse = arg_pass;
			estAuthentifie = true;
		}
		
		public CustomServerClient(TCPClient arg_tcpSock) {
			tcpSock = arg_tcpSock;
			ID = ApplicationServeur.nextClientID;
			ApplicationServeur.nextClientID++;
		}
	}
	
	public ArrayList<CustomServerClient> serverClientList = new ArrayList<CustomServerClient>();
	
	public boolean checkUserCredentials(String userName, String userPass) {
		return true;
	}
	

	public static void sleep(long millisec) {
		try { Thread.sleep(millisec); } catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	TCPServer server;
	
	@Override
	// Equivalent de la méthode :
	// public static void main(String[] args)
	// sur une réelle appication serveur.
	public void run() {
		
		
		int tcpPort = 12345;
		server = new TCPServer(tcpPort);
		if (server.isListening()) {
			log("Le serveur écoute sur le port " + tcpPort);
		} else {
			server.stop();
			return;
		}
		
		// Boucle du serveur
		while (server.isListening()) {
			
			// Accepter de nouveaux clients (asynchrone)
			TCPClient newTCPClient = server.accept(); // non bloquant
			if (newTCPClient != null) {
				// Nouveau client accepté !
				// Je crée le client du serveur
				CustomServerClient servClient = new CustomServerClient(newTCPClient);
				serverClientList.add(servClient);

				log("Nouveau client ! IP client = " + newTCPClient.getRemoteIP());
				/*
				System.out.println("Serveur : nouveau client - Liste des clients :");
				for (int i = 0; i < serverClientList.size(); i++) {
					System.out.println(serverClientList.get(i).ID);
				}*/
			}
			
			// Suppression des clients qui ne sont plus connectés
			int clientIndex = 0;
			while (clientIndex < serverClientList.size()) {
				CustomServerClient servClient = serverClientList.get(clientIndex);
				if ( ! servClient.tcpSock.isConnected() )  {
					boolean criticalErrorOccured = servClient.tcpSock.criticalErrorOccured();
					if (criticalErrorOccured) {
						log("Erreur critique sur un client, déconnexion : " + servClient.tcpSock.getCriticalErrorMessage());
					}
					servClient.tcpSock.stop(); // facultatif
					serverClientList.remove(clientIndex);
					System.out.println("Serveur : Déconnexion du client : " + servClient.ID);
					
				} else
					clientIndex++;
			}
			
			// Ecouter ce que les clients demandent
			for (clientIndex = 0; clientIndex < serverClientList.size(); clientIndex++) {
				CustomServerClient servClient = serverClientList.get(clientIndex);
				NetBuffer newMessage = servClient.tcpSock.getNewMessage();
				if (newMessage != null) {
					log("Nouveau message reçu de " + servClient.ID);
					if (! newMessage.currentData_isInt()) {
						log("ERREUR : message mal formatté.");
						// Je ne réponds rien
						//servClient.tcpSock.sendMessage(replyMessage);
					} else {
						int messageType = newMessage.readInteger();
						
						// Authentification
						if (messageType == 1) {
							String nomCompte = newMessage.readString();
							String motDePasse = newMessage.readString();
							if (checkUserCredentials(nomCompte, motDePasse)) {
								servClient.vientDEtreAuthentifie(nomCompte, motDePasse);
								// Réuss
								NetBuffer reply = new NetBuffer();
								reply.writeInt(1);
								reply.writeBool(true);
								reply.writeString("Bienvenue " + nomCompte);
								servClient.tcpSock.sendMessage(reply);
							} else {
								NetBuffer reply = new NetBuffer();
								reply.writeInt(1);
								reply.writeBool(false);
								reply.writeString("Echec de la connexion : mot de passe ou nom de compte invalide.");
								servClient.tcpSock.sendMessage(reply);
							}
						}

						// Demander son ID
						if (messageType == 2) {
							NetBuffer reply = new NetBuffer();
							reply.writeInt(2);
							reply.writeBool(servClient.estActuellementAuthentifie());
							if (servClient.estActuellementAuthentifie()) {
								reply.writeInt(servClient.ID);
							}
							servClient.tcpSock.sendMessage(reply);
						}

						// Demander son nom (amnésie power...)
						if (messageType == 3) {
							NetBuffer reply = new NetBuffer();
							reply.writeInt(3);
							reply.writeBool(servClient.estActuellementAuthentifie());
							if (servClient.estActuellementAuthentifie()) {
								reply.writeString(servClient.nomDeCompte);
							}
							servClient.tcpSock.sendMessage(reply);
						}
						
						
					}
				}
			}
			sleep(1); // 1ms entre chaque itération, minimum
		}
		
	}
	
	public void forceStop() {
		if (server == null) return;
		server.stop();
	}
	
}

///!!\ Ici, je lance un nouveau thread pour simuler une application serveur, rien de plus
class ApplicationClient implements Runnable {
	
	
	public static void sleep(long millisec) {
		try { Thread.sleep(millisec); } catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	TCPClient client;
	
	/** Ecrire un message sur la console (+ rapide à écrire !)
	 * @param infoMessage message à écrire
	 */
	public static void log(String infoMessage) {
		synchronized(WriteOnConsoleClass.LOCK) { System.out.println("ApplicationClient : " + infoMessage); } //System.out.flush();
		//System.out.println("ApplicationClient : " + infoMessage);
	}
	
	public boolean rcvMessageLoop() {
		boolean receivedSomething = false;
		boolean continueLoop = true;
		while (continueLoop) {
			if (rcvMessageLoop_iteration()) {
				receivedSomething = true;
			} else continueLoop = false; // plus de message à recevoir
		}
		return receivedSomething;
	}
	
	private boolean rcvMessageLoop_iteration() {
		
		if (! client.isConnected()) {
			return false;
		}
		
		NetBuffer rcvMessage = client.getNewMessage();
		if (rcvMessage == null)  return false;
		
		int typeMessage = rcvMessage.readInt();
		log("Reçu message de type = " + typeMessage);
		
		if (typeMessage == 1) {
			boolean ressiteAuthentification = rcvMessage.readBool();
			String strMessage = rcvMessage.readStr();
			log("Réussite authentification : " + ressiteAuthentification + " message = " + strMessage);
		}

		if (typeMessage == 2) {
			boolean estAuthentifie = rcvMessage.readBool();
			log("Est authentifie = " + estAuthentifie);
			if (estAuthentifie) {
				int monID = rcvMessage.readInt();
				log("Mon ID reçu : " + monID);
			}
		}

		if (typeMessage == 3) {
			boolean estAuthentifie = rcvMessage.readBool();
			log("Est authentifie = " + estAuthentifie);
			if (estAuthentifie) {
				String nomDeCompte = rcvMessage.readString();
				log("Mon nom de compte : " + nomDeCompte);
			}
		}
		
		return true;
	}
	
	@Override
	// Equivalent de la méthode :
	// public static void main(String[] args)
	// sur une réelle appication client.
	public void run() {
		
		int tcpPort = 12345;
		client = new TCPClient("localhost", tcpPort);
		
		for (int iWait = 0; iWait < 1000; iWait++) {
			if (client.isConnected()) {
				break;
			}
			if (iWait >= 990) {
				log("ERREUR : impossible de se connecter, temps d'attente dépassé");
				client.stop();
				break;
			}
			if (client.criticalErrorOccured()) {
				log("ERREUR critique dans le client : " + client.getCriticalErrorMessage());
				break;
			}
			sleep(1);
		}
		if (! client.isConnected()) return;
		
		
		NetBuffer demandeConnexionMessage = new NetBuffer();
		demandeConnexionMessage.writeInt(1);
		demandeConnexionMessage.writeString("Sylvie");
		demandeConnexionMessage.writeString("**Lalouette**");
		client.sendMessage(demandeConnexionMessage);
		
		for (int iWait = 0; iWait < 1000; iWait++) {
			
			/*NetBuffer rcvMessage = client.getNewMessage();
			if (rcvMessage == null)  {
				sleep(1);
				continue;
			}
			int typeMessage = rcvMessage.readInt();
			if (typeMessage == 1) {
				boolean ressiteAuthentification = rcvMessage.readBool();
				String strMessage = rcvMessage.readStr();
				log("Réussite authentification : " + ressiteAuthentification + " message = " + strMessage);
			}*/
			if (rcvMessageLoop()) break; // message reçu !
			sleep(1);
		}
		

		NetBuffer demandeIDMessage = new NetBuffer();
		demandeIDMessage.writeInt(2);
		client.sendMessage(demandeIDMessage);
		
		NetBuffer demandeNomMessage = new NetBuffer();
		demandeNomMessage.writeInt(3);
		client.sendMessage(demandeNomMessage);
		
		// Je reçois ce que je peux pendant 200ms
		for (int iWait = 0; iWait < 200; iWait++) {
			
			rcvMessageLoop();
			sleep(1);
			
		}
		
		client.stop();
		
		
	}
	
}

public class FullTest {
	
	public static void sleep(long millisec) {
		try { Thread.sleep(millisec); } catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	/** Ecrire un message sur la console (+ rapide à écrire !)
	 * @param infoMessage message à écrire
	 */
	public static void log(String infoMessage) {
		System.out.println(infoMessage);
	}
	
	public static void main(String[] args) {
		
		SCNode node = new SCNode(RSA.STR_PUBLIC_KEY, RSA.STR_PRIVATE_KEY);
		//SCBlockData_transaction(int arg_amount, String arg_senderKey, String arg_receiverKey, boolean hasToSignTransaction, String senderPrivateKey, String arg_senderSignature);
		SCBlockData_transaction transaction
		  = new SCBlockData_transaction(78, RSA.STR_PUBLIC_KEY, "123456789", System.currentTimeMillis(), true, RSA.STR_PRIVATE_KEY, null);
		System.out.println(transaction.toString());
		System.out.println("Transaction valide : " + transaction.checkValidity());
		transaction.receiverPublicKey = "123456779"; // petite erreur
		System.out.println("Transaction valide : " + transaction.checkValidity());
		transaction.receiverPublicKey = "méchant_voleur"; // vilain, vilain
		System.out.println("Transaction valide : " + transaction.checkValidity());
		transaction.receiverPublicKey = "123456789"; // retour à la normale
		System.out.println("Transaction valide : " + transaction.checkValidity());
		
		
		/*
		
		ApplicationServeur applicationServ = new ApplicationServeur();
		ApplicationClient applicationClient = new ApplicationClient();
		
		new Thread(applicationServ).start();
		new Thread(applicationClient).start();
		
		// Attendre 200ms, histoire que tout le monde ait bien fini de tout faire ce qu'il avait à faire (toussa toussa)
		sleep(200);
		// Maintenant arrêt en bourrin
		applicationServ.forceStop();
		*/
		//applicationClient.forceStop();
		
		
		
		/* ça marche bien, même si on en lance 5000 en 5 secondes ! (1 par ms)
		for (int i = 1; i <= 5000; i++) {
			new Thread(new ApplicationClient()).start();
			sleep(1);
		}*/
		
	}
}

//Juste pour avoir un println thread-safe
class WriteOnConsoleClass {
	static public final Object LOCK = new Object();
}
