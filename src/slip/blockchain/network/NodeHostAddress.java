package slip.blockchain.network;


/** Garde en mémoire les adresses des hôtes et la dernière fois qu'ils ont répondu
 *  Pas totalement commenté, par manque de temps
 */
public class NodeHostAddress {

	public int hostPort;
	public String hostIP;
	public long lastResponseDateMs = 0;
	
	public NodeHostAddress(String arg_hostIP, int arg_hostPort) {
		hostIP = arg_hostIP;
		hostPort = arg_hostPort;
	}
	
	
}
