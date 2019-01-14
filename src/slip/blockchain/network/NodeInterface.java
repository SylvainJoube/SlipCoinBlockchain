package slip.blockchain.network;

import slip.network.buffers.NetBuffer;
import slip.network.tcp.*;

import java.util.ArrayList;

import slip.blockchain.pos.*;

class WriteOnConsoleClass {
	static public final Object LOCK = new Object();
}

public class NodeInterface {

	private SCNode node;
	private ArrayList<NodeClient> nodeClients;
	private TCPServer nodeServer;
	
	public NodeInterface(int tcpPort) {
		nodeServer = new TCPServer(tcpPort);
		
		if (nodeServer.isListening()) {
			log("Le serveur écoute sur le port " + tcpPort);
		} else {
			nodeServer.stop();
			return;
		}
		
		while (nodeServer.isListening()) {		
			// Accepter de nouveaux clients (asynchrone)
			TCPClient newTCPClient = nodeServer.accept(); // non bloquant
			if (newTCPClient != null) {
				// Nouveau client accepté !
				// Je crée le client du serveur
				nodeClients.add(new NodeClient(newTCPClient));
			}
			
			// Suppression des clients qui ne sont plus connectés
			for (NodeClient client : nodeClients) {
				if (!client.getTcpClient().isConnected()) {
					if (client.getTcpClient().criticalErrorOccured()) {
						log("Erreur critique sur un client, déconnexion : " + client.getTcpClient().getCriticalErrorMessage());
					}
					client.getTcpClient().stop();
					nodeClients.remove(client);
				}
			}
			
			for (NodeClient client : nodeClients) {
				NetBuffer newMessage = client.getTcpClient().getNewMessage();
				if (newMessage != null) {
					try {
						if (! newMessage.currentData_isInt()) {
							throw new Error();
						}
						int messageType = newMessage.readInteger();
						switch (messageType) {
							case 1 : //message de type donnée
								
								NetBuffer toTransmit = receiveData(newMessage);
								// TODO broadcast message if(transmitData)
								break;
						}
					} catch (Exception e) {
						log("Erreur, message mal formaté");
					}
					sleep(1); // 1ms entre chaque itération, minimum
				}
			}
		}
	}
	
	public NetBuffer receiveData(NetBuffer data) throws Error {
		if (!data.currentData_isInt()) {
			throw new Error();
		}
		int dataType = data.readInteger();
		switch (dataType) {
			case SCBlockDataType.TRANSACTION.asInt() :
				SCBlockData_transaction transa
			default:
				throw new Error();
		}
	}
	
	
	public static void sleep(long millisec) {
		try { Thread.sleep(millisec); } catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void log(String infoMessage) {
		synchronized(WriteOnConsoleClass.LOCK) { System.out.println("ApplicationServeur : " + infoMessage); } //System.out.flush();
		//System.out.println("ApplicationServeur : " + infoMessage);
	}
}
