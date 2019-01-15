package slip.blockchain.network;

import slip.network.buffers.NetBuffer;
import slip.network.buffers.NetBufferData;
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
	//private ArrayList<NetAddress> AddressBook;
	
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
						if (! newMessage.currentData_isInt()) {
							throw new Error();
						}
						int senderPort = newMessage.readInteger();
						addAddressToBook(client.getNodeIp(), senderPort);
						switch (messageType) {
							case 1 : //message de type donnée
								NetBuffer toTransmit = receiveData(newMessage);
								//puisqu'on throw des erreurs, passe ici uniquement si toTransmit doit être transmis
								broadcastMessage(toTransmit);
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
	
	public void broadcastMessage(NetBuffer toTransmit) {
		/*for (NetAddress address : AddressBook) {
			TCPClient client = new TCPClient(address.getIp(), address.getPort());
			client.sendMessage(toTransmit);
			while 
		}*/
	}
	public NetBuffer receiveData(NetBuffer data) throws Error {
		if (!data.currentData_isInt()) {
			throw new Error();
		}
		int dataTypeAsInt = data.readInteger();
		SCBlockDataType dataType = SCBlockDataType.getFromInt(dataTypeAsInt);
		switch (dataType) {
			case TRANSACTION :
				SCBlockData_transaction transaction = SCBlockData_transaction.readFromNetBuffer(data, 2);
				if(this.node.addToDataBuffer(transaction)) {
					data.setPosition(data.getLastIndex()); //on se positionne sur le dernier int, qui contient le TTL
					if (!data.currentData_isInt()) {
						throw new Error();
					}
					int ttl = data.readInteger();
					if (ttl -1 <= 0) {
						throw new Error();
					} else {
						NetBuffer toSend = transaction.writeToNetBuffer(true);
						NetBufferData newData = new NetBufferData(1);
						//toSend.insertAtPos(0, newData);
						return toSend;
					}
				} else {
					throw new Error();
				}
			default:
				throw new Error();
		}
	}
	
	public void addAddressToBook(String ip, int port) {/*
		NetAddress toAdd = new NetAddress(ip, port);
		for(NetAddress netAddress : AddressBook) {
			if (toAdd.toString().equals(netAddress.toString())) {
				return;
			}
		}
		AddressBook.add(toAdd);*/
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
