package slip.blockchain.pos;

public enum SCBlockDataType {
	UNKNOWN(0),
	TRANSACTION(1),
	SYSTEM_MESSAGE(2),
	SYSTEM_ERROR(3);
	
	private final int typeAsInt;
	
	private SCBlockDataType(int arg_typeAsInt) {
		typeAsInt = arg_typeAsInt;
	}

	public int getInt() {
		return asInt();
	}
	public int asInt() {
		return typeAsInt;
	}
	
	
}
