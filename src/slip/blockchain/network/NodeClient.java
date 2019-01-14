package slip.blockchain.network;

import slip.network.tcp.*;

public class NodeClient {
	
	private final TCPClient tcpClient;
	private String nodeIp;
	private String nodePort;
	
	public NodeClient(TCPClient arg_tcpClient) {
		tcpClient = arg_tcpClient;
		//this.nodeIp = tcpClient.getRemoteIp();
	}

	public String getNodeIp() {
		return nodeIp;
	}

	public void setNodeIp(String nodeIp) {
		this.nodeIp = nodeIp;
	}

	public String getNodePort() {
		return nodePort;
	}

	public void setNodePort(String nodePort) {
		this.nodePort = nodePort;
	}

	public TCPClient getTcpClient() {
		return tcpClient;
	}
	
	
	
}
