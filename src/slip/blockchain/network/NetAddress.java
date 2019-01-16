package slip.blockchain.network;

/**
 * Pas totalement commenté, ni totalement implémenté, par manque de temps
 */

public class NetAddress {
	private String ip;
	private int port;
	
	public NetAddress (String arg_ip, int arg_port) {
		ip = arg_ip;
		port = arg_port;
	}
	
	@Override
	public String toString() {
		return ip+":"+port;
	}

	public String getIp() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	
	
}
 	