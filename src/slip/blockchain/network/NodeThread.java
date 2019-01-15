package slip.blockchain.network;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import slip.blockchain.pos.SCBlockData_transaction;
import slip.blockchain.pos.SCNode;
import slip.network.buffers.NetBuffer;
import slip.network.tcp.TCPServer;

/**
 * Classe gérant la node dans son environnement.
 * Fil gérant la node en réseau.
 * 
 * Ouverture du serveur
 * 
 */
public class NodeThread implements Runnable {
	
	// Liste des hôtes connus
	public ArrayList<NodeActiveHost> activeHostList = new ArrayList<NodeActiveHost>();
	public ArrayList<NodeHostAddress> hostAddressList = new ArrayList<NodeHostAddress>();
	
	public SCNode myNode;
	public NodeServer myServer;
	private int listenOnPort;
	public AtomicBoolean hasToStop = new AtomicBoolean(false);
	public AtomicBoolean isStopped = new AtomicBoolean(false);
	
	/** Ajouter un hôte, mettre à jour sa date de dernière activité
	 * @param arg_hostIP
	 * @param arg_hostPort
	 * @param updateLastResponseTime vrai si doit mettre à jour le dernier temps de réponse
	 */
	public void updateHostAddressList(String arg_hostIP, int arg_hostPort, boolean updateLastResponseTime) {
		int hostIndex = findHostAddressIndex(arg_hostIP, arg_hostPort);
		NodeHostAddress hostAddress = null;
		if (hostIndex == -1) {
			hostAddress = new NodeHostAddress(arg_hostIP, arg_hostPort);
			hostAddressList.add(hostAddress);
		} else {
			hostAddress = hostAddressList.get(hostIndex);
		}
		if (hostAddress == null) return; // ne doit pas arriver
		if (updateLastResponseTime) {
			hostAddress.lastResponseDateMs = System.currentTimeMillis(); 
		}
	}
	public void deleteFromHostAddressList(String arg_hostIP, int arg_hostPort) {
		int hostIndex = findHostAddressIndex(arg_hostIP, arg_hostPort);
		if (hostIndex != -1) {
			hostAddressList.remove(hostIndex);
		}
	}
	public int findHostAddressIndex(String arg_hostIP, int arg_hostPort) {
		for (int iHost = 0; iHost < hostAddressList.size(); iHost++) {
			NodeHostAddress hostAddress = hostAddressList.get(iHost);
			if (hostAddress.hostIP.equals(arg_hostIP) && hostAddress.hostPort == arg_hostPort) {
				return iHost;
			}
		}
		return -1;
	}
	
	/** Recevoir une nouvelle transaction du réseau
	 * @param transactionAsNetBuffer transaction reçue
	 */
	public void receiveTransaction(NetBuffer transactionAsNetBuffer) {
		SCBlockData_transaction transaction = SCBlockData_transaction.readFromNetBuffer(transactionAsNetBuffer, 0);
		myNode.addToDataBuffer(transaction);
	}
	public void receiveTransaction(byte[] transactionAsByteArray) {
		NetBuffer transactionAsBuff = new NetBuffer(transactionAsByteArray);
		receiveTransaction(transactionAsBuff);
	}
	
	/** Constructeur, une seule SCNode par NodeThread.
	 * @param arg_myNode node à gérer.
	 */
	public NodeThread(SCNode arg_myNode, int arg_portToUse) {
		myNode = arg_myNode;
		listenOnPort = arg_portToUse;
	}
	
	@Override
	public void run() {
		// TODO
		updateHostAddressList("127.0.0.1", 3334, false);
		updateHostAddressList("127.0.0.1", 3335, false);
		updateHostAddressList("127.0.0.1", 3336, false);
		updateHostAddressList("127.0.0.1", 3337, false);
		updateHostAddressList("127.0.0.1", 3338, false);
		updateHostAddressList("127.0.0.1", 3339, false);
		
		// Se connecter à tous les hôtes connus
		myServer = new NodeServer(this);
		myServer.startServer(listenOnPort);
		for (int iHost = 0; iHost < hostAddressList.size(); iHost++) {
			NodeHostAddress hostAddr = hostAddressList.get(iHost);
			NodeActiveHost activeHost = new NodeActiveHost(hostAddr.hostIP, hostAddr.hostPort, null, this);
			activeHost.connect();
		}
		
		while ( ! hasToStop.get() ) {
			
			myServer.loop(); // gère aussi les clients
			
			try {
				Thread.sleep(1);
			} catch (Exception e) { break; }
		}
		
		myServer.forceStop();
		isStopped.set(true);
	}
	
	
	
	
	
}
