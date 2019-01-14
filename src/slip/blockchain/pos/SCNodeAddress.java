package slip.blockchain.pos;

import slip.network.tcp.TCPClient;

/** Stocke l'adresse d'une autre cellule du réseau
 * 
 *
 */
public class SCNodeAddress {
	
	public long lastResponseDate = 0; // dernière fois que l'ai communiqué avec cette cellule
	public int port;
	public String host;
	public TCPClient tcpConnection = null; // peut être initialisé ou rester à null
	private String ownerPublicKey;  // = identifiant de celui à qui appartient cette cellule
	
	public SCNodeAddress(String arg_host, int arg_port, TCPClient arg_tcpConnection, String arg_ownerPublicKey) {
		lastResponseDate = System.currentTimeMillis();
		tcpConnection = arg_tcpConnection;
		host = arg_host;
		port = arg_port;
		ownerPublicKey = arg_ownerPublicKey;
	}
	
}
