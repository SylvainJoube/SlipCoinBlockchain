package slip.blockchain.network;

import slip.blockchain.pos.SCBlockData_transaction;
import slip.network.buffers.NetBuffer;
import slip.network.tcp.TCPClient;

public class NodeActiveHost {
	
	public static final long maxInactivityTime = 2 * 60 * 1000; // en millisecondes
	
	public boolean alreadyTalkedTo = false; // si j'ai déjà parlé à cet hôte
	public int hostPort;
	public String hostIP;
	public TCPClient connection = null;
	public long lastResponseMs = 0;
	public final NodeThread  myNodeThread;
	
	public static final int hostTimerValue = 4 * 1000; // 4 secondes pour répondre correctement à toute requête
	public int networkingStep = 0; // 0 signifie aucune activité réseau, 1 connexion en cours, 2 connecté, 
	public boolean isConnectedToHost = false;
	private long firstConnectionRequestTime;
	
	public void connect() {
		if (connection != null) {
			connection.stop();
			connection = null;
		}
		connection = new TCPClient();
		connection.connect(hostIP, hostPort);
		networkingStep = 1;
		firstConnectionRequestTime = System.currentTimeMillis();
	}
	
	public boolean isStillActive() {
		if (connection == null) return false;
		return connection.isConnected();
	}
	
	public void loopConnection() {
		if (myNodeThread == null) return; // jamais réalisé, en théorie
		if (connection == null) {
			networkingStep = 0;
			isConnectedToHost = false;
			return;
		}
		if (networkingStep == 1) { // connexion en cours
			if (System.currentTimeMillis() - firstConnectionRequestTime > hostTimerValue) {
				connection.stop();
				connection = null;
				// timeout de la connexion : impossible de joindre cet hôte
			}
		}
		NetBuffer message = connection.getNewMessage();
		if (message == null) return;
		myNodeThread.updateHostAddressList(hostIP, hostPort, true); // actualisation du temps
		try {
			int messageType = message.readInt();
			
			if (messageType == 1) { // nouvelle transaction
				byte[] transactionAsByteArray = message.readByteArray();
				myNodeThread.receiveTransaction(transactionAsByteArray);
			}

			if (messageType == 2) { // nouveaux hôtes communiqués
				//int hostNb = 
			}
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public NodeActiveHost(String arg_hostIP, int arg_hostPort, TCPClient arg_tcpConnection, NodeThread arg_myNodeThread) {
		hostIP = arg_hostIP;
		hostPort = arg_hostPort; // port inutile s'il s'agit d'un client du serveur
		if (arg_tcpConnection != null) {
			connection = arg_tcpConnection;
		}
		myNodeThread = arg_myNodeThread;
	}
	
	public void sendTransaction(SCBlockData_transaction transaction) {
		if (connection == null) return;
		if (! connection.isConnected() ) return; // en théorie, il faudrait que je stocke ces données
		NetBuffer message = new NetBuffer();
		message.writeInt(1);
		NetBuffer transactionAsNetBuffer = transaction.writeToNetBuffer(true);
		byte[] transactionAsByteArray = transactionAsNetBuffer.convertToByteArray();
		message.writeByteArray(transactionAsByteArray);
		connection.sendMessage(message);
	}
	
	public void sendBlockChain() {
		
	}
}
