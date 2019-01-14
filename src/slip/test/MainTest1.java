package slip.test;

import slip.network.buffers.NetBuffer;
import slip.network.buffers.NetBufferData;
import slip.network.tcp.TCPClient;
import slip.network.tcp.TCPServer;

public class MainTest1 {

	public static void sleep(long millisec) {
		try { Thread.sleep(millisec); } catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void oldMain(String[] args) {
		// TODO Auto-generated method stub
		NetBuffer netBuff = new NetBuffer();
		netBuff.writeInt(42);                             System.out.println(netBuff.readInt());
		netBuff.writeString("Salut je suis une string."); System.out.println(netBuff.readString());
		netBuff.writeInt(87);                             System.out.println(netBuff.readInt());
		netBuff.writeDouble(87.7562);                     System.out.println(netBuff.readDouble());
		netBuff.writeByteArray(NetBufferData.doubleToByteArray(12345.1234567)); System.out.println(NetBufferData.byteArrayToDouble(netBuff.readByteArray()));
		
		
		byte[] buffAsByteArray = netBuff.convertToByteArray();
		
		NetBuffer receivedBuffer = new NetBuffer(buffAsByteArray);
		System.out.println(receivedBuffer.readInt());
		System.out.println(receivedBuffer.readString());
		System.out.println(receivedBuffer.readInt());
		System.out.println(receivedBuffer.readDouble());
		System.out.println(NetBufferData.byteArrayToDouble(receivedBuffer.readByteArray()));
		
		TCPServer server = new TCPServer(1234);
		System.out.println(server.isListening());
		
		TCPClient client = new TCPClient("localhost", 1234);
		/*
		try { Thread.sleep(200); } catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if (client.criticalErrorOccured()) {
			System.out.println("criticalErrorOccured : " + client.getCriticalErrorMessage());
		}
		*/
		boolean connexionReussie = false;
		for (int i = 0; i < 100; i++) {
			if (client.isConnected()) {
				System.out.println("Le client vient de se connecter !");
				connexionReussie = true;
				break;
			}
			if (!client.isStillActive()) {
				System.out.println("Le client n'est plus actif.");
				break;
			}
			sleep(4);
		}
		if (connexionReussie)
			client.sendMessage(netBuff);
		
		
		/*
		for (int i = 0; i < 100; i++) {
			NetBuffer clientMessage = server.getClientMessage(0);
			if (clientMessage != null) {
				System.out.println("Nouveau message reçu du client !");
				break;
			}
			if (!client.isStillActive()) {
				System.out.println("Le client n'est plus actif.");
				break;
			}
			sleep(4);
		}*/
		
		
		
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		// Plus tard TCPClient client = new TCPClient();
		// Plus tard client.connect("ip_a_laquelle_je_veux_me_connecter", 8080);
		/* plus tard !
		// Code à mettre dans le timer, à exécuter toutes les 0.01s (10ms)
			if (!client.isConnected()) {
				// Afficher "en attente de connexion au serveur"
			} else {
				// Changer l'état/apparence/foncions de l'application : on est connecté au serveur
			}
			
			NetBuffer nouveauMessage = client.getNewMessage();
			if (nouveauMessage != null) {
				// Traîter le nouveau message :
				int typeMessage = nouveauMessage.readInt(); // 1 pour un message de connexion, 2 pour message d'affichage de son compte...
				if (typeMessage == 1) {
					boolean aReusiASeConnecter = nouveauMessage.readBool();
					if (!aReusiASeConnecter)  {
						// Changer l'application : vider les champs et afficher "identifiant ou mot de passe invalide"
					} else {
						// On a réussi à se connecter, on va à la fenêtre suite, on est boen connecté
						// Pour envoyer des messages au serveur :
						NetBuffer messageEnvoyer = new NetBuffer();
						messageEnvoyer.writeInt(5); // message de type 5
						//messageEnvoyer.write...
					}
				}
			}
		//client.isConnected()
			
		
		
		// Ce que j'envoie au serveur
		NetBuffer demandeAuthentification = new NetBuffer();
		demandeAuthentification.writeInt(1);
		demandeAuthentification.writeString("Nom nom de compte");
		demandeAuthentification.writeString("Mot de passe");
		 plus tard -> client.sendMessage(demandeAuthentification);
		// Ce que j'attends en réponse :
		NetBuffer reponseDuServeur = new NetBuffer();//client.getNewMessage();
		reponseDuServeur.readInt(); // 1
		reponseDuServeur.readBool(); // true
		reponseDuServeur.readString(); // date de dernière connexion 
		
		NetBuffer reponseQueJeVeux = new NetBuffer();
		reponseQueJeVeux.writeInt(1);
		reponseQueJeVeux.writeBool(true);
		reponseQueJeVeux.writeString("2019-01-12");
		
		NetBuffer reponseUtilsiable = reponseQueJeVeux;
		reponseUtilsiable.readInt(); // = 1
		reponseUtilsiable.readBool();
		
		
		
		
		// réception du serveur
		NetBuffer recuDuClient /  recu du serveur * / = new NetBuffer();
		int messageType = recuDuClient.readInt();
		if (messageType == 1) { // type "demande de connexion"
			String dateDerniereConnexion = recuDuClient.readString();
			// ...
		}
		
		*/
		
		
		
		
		
	}

}
