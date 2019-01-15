package slip.blockchain.network;

import java.util.ArrayList;

import slip.network.tcp.TCPClient;
import slip.network.tcp.TCPServer;

/** Serveur de la cellule, s'occupe d'accepter de nouvelles connexions
 * 
 */

public class NodeServer {
	private int localServerPort = 3334;
	private TCPServer localServer;
	private NodeThread myNodeThread;
	
	// Liste des hôtes connus
	// Ils sont périodiquement
	//ArrayList<NodeActiveHost> knownHostList = new ArrayList<NodeActiveHost>();
	//ArrayList<NodeHost> connectedHosts = 
	//ArrayList<TCPClient> clientList = new ArrayList<TCPClient>();
	
	public NodeServer(NodeThread arg_myNodeThread) {
		myNodeThread = arg_myNodeThread;
	}
	
	private void log(String message) {
		System.out.println("NodeThread : " + message);
	}
	
	public void forceStop() {
		if (localServer == null) return;
		localServer.stop();
	}
	
	// Seul processus bloquant, le démarrage du serveur
	public boolean startServer(int arg_localServerPort) {
		if (arg_localServerPort != 0)
			localServerPort = arg_localServerPort;
		log("Démarrage du serveur sur le port " + localServerPort);
		localServer = new TCPServer(localServerPort);
		if (! localServer.isListening()) {
			log("ERREUR : impossible de démarrer le serveur sur le port " + localServerPort);
			localServer.stop(); /// facultatif
			return false;
		}
		log("Serveur démarré sur le port " + localServerPort + " !");
		return true;
	}
	
	/** Boucle du serveur
	 *  
	 */
	public void loop() {
		if (myNodeThread == null) return;
		if (localServer == null) return;
		if (! localServer.isListening() ) return;
		
		// Accepter de nouveaux clients
		boolean continueAcceptLoop = true;
		while (continueAcceptLoop) { // juste pour ne pas faire de while(true), moins clair
			TCPClient netClient = localServer.accept();
			if (netClient == null) {
				continueAcceptLoop = false;
				break;
			}
			
			NodeActiveHost newActiveHost = new NodeActiveHost(netClient.getRemoteIP(), localServerPort, netClient, myNodeThread);
			myNodeThread.activeHostList.add(newActiveHost);
			System.out.println("NodeServer : Nouveau client ! localServerPort = " + localServerPort);
			//myNodeThread.updateHostAddressList(arg_hostIP, arg_hostPort, updateLastResponseTime);
		}
		
		// Ecouter les clients et traiter les demandes
		int iClient = 0;
		while (iClient < myNodeThread.activeHostList.size()) {
			NodeActiveHost activeHost = myNodeThread.activeHostList.get(iClient);
			activeHost.loopConnection();
			if (! activeHost.isStillActive() ) {
				myNodeThread.activeHostList.remove(iClient);
				continue;
			}
			iClient++;
		}
		
	}
}
