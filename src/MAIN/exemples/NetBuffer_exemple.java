package MAIN.exemples;

import slip.network.buffers.NetBuffer;
import slip.network.buffers.NetBufferData;

public class NetBuffer_exemple {

	public static void main_ENLEVER_POUR_TESTER(String[] args) {
		
		System.out.println(" ----- Modèle : ----- ");
		
		NetBuffer netBuff = new NetBuffer();
		netBuff.writeInt(42);                             System.out.println(netBuff.readInt());
		netBuff.writeString("Salut je suis une string."); System.out.println(netBuff.readString());
		netBuff.writeInt(87);                             System.out.println(netBuff.readInt());
		netBuff.writeDouble(87.7562);                     System.out.println(netBuff.readDouble());
		netBuff.writeByteArray(NetBufferData.doubleToByteArray(12345.1234567)); System.out.println(NetBufferData.byteArrayToDouble(netBuff.readByteArray()));
		
		byte[] buffAsByteArray = netBuff.convertToByteArray();
		
		System.out.println(" ----- Vérification : ----- ");
		
		NetBuffer receivedBuffer = new NetBuffer(buffAsByteArray);
		System.out.println(receivedBuffer.readInt());
		System.out.println(receivedBuffer.readString());
		System.out.println(receivedBuffer.readInt());
		System.out.println(receivedBuffer.readDouble());
		System.out.println(NetBufferData.byteArrayToDouble(receivedBuffer.readByteArray()));
		
	}
}
