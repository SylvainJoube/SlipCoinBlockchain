package slip.network.tcp;

import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import slip.network.buffers.NetBuffer;


public class TCPClient {
	
	//private Socket mySocket;
	private TCPClientThread myThread = null; // Réception des nouveaux octets à lire
	private ArrayList<NetBuffer> readyBufferList = new ArrayList<NetBuffer>(); // Liste des buffers accessibles 
	private Object bufferListLock = new Object();
	// Utilisation d'un AtomicBoolean pour ne pas bloquer le thread principal de l'applications lors de la vérification des nouveaux messages
	private AtomicBoolean canAccessReadyBufferList = new AtomicBoolean(true); // Un booléen atomique donc thread-safe, pour savoir si
	//private boolean serverSide_clientIsAccepted = false;
	
	public TCPClient() {
		//-> fonction vérification données
	}
	public TCPClient(String hostIp, int hostPort) {
		connect(hostIp, hostPort);
	}
	
	public TCPClient(Socket workingSocket) {
		myThread = new TCPClientThread(this, workingSocket, false);
		new Thread(myThread).start();
	}
	
	
	public void connect(String hostIp, int hostPort) {
		if (myThread != null) return; // impossible de lancer 2+ fois connect()
		myThread = new TCPClientThread(this, hostIp, hostPort, false);
		new Thread(myThread).start();
	}
	
	public boolean isConnected() {
		if (myThread == null) return false;
		if (!isStillActive()) return false;
		return myThread.isConnectedToHost();
	}
	public boolean isStillActive() {
		if (myThread == null) return false;
		return myThread.isStillActive();
	}
	
	public boolean sendMessage(NetBuffer messageToSend) {
		if (myThread != null) {
			myThread.addMessageToSendList(messageToSend);
			return true;
		}
		return false;
	}

	public boolean criticalErrorOccured() {
		if (myThread != null) {
			return myThread.criticalErrorOccured();
		}
		return false;
	}
	public String getCriticalErrorMessage() {
		if (myThread != null) {
			return myThread.getCriticalErrorMessage();
		}
		return "";
	}
	
	/** Ajout d'un nouveau buffer prêt à être lu
	 * @param newBuffer
	 */
	protected void addReadyBufferFromThread(NetBuffer newBuffer) {
		synchronized (bufferListLock) {
			canAccessReadyBufferList.set(false);
			readyBufferList.add(newBuffer);
			canAccessReadyBufferList.set(true);
		}
	}

	public NetBuffer getNewMessage() {
		// Le booléen atomique canAccessReadyBufferList est là pour ne pas faire attendre le thread principal avec un synchronized
		// si la liste des buffers est en cours de modification par le thread de réception.
		if (canAccessReadyBufferList.get() == false)
			return null;
		
		synchronized(bufferListLock) {
			if (readyBufferList.size() == 0) return null;
			NetBuffer result = readyBufferList.get(0);
			
			//System.out.println("TCPClient.getNewMessage() : result.dataList.size() = " + result.dataList.size()); 
			readyBufferList.remove(0);
			return result;
		}
	}
	
	/** Comme getNewMessage() mais ne supprime pas le message de la liste des messages reçus
	 * @return
	 */
	public NetBuffer peekNewMessage() {
		// Le booléen atomique canAccessReadyBufferList est là pour ne pas faire attendre le thread principal avec un synchronized
		// si la liste des buffers est en cours de modification par le thread de réception.
		if (canAccessReadyBufferList.get() == false)
			return null;
		
		synchronized(bufferListLock) {
			if (readyBufferList.size() == 0) return null;
			NetBuffer result = readyBufferList.get(0);
			return result;
		}
	}
	
	public boolean hasNewMessage() {
		// Le booléen atomique canAccessReadyBufferList est là pour ne pas faire attendre le thread principal avec un synchronized
		// si la liste des buffers est en cours de modification par le thread de réception.
		if (canAccessReadyBufferList.get() == false)
			return false;
		
		synchronized(bufferListLock) {
			return (readyBufferList.size() != 0);
		}
	}
	
	public void stop() {
		if (myThread != null)
			myThread.stop();
		// TODO
	}
	
	
	
}
