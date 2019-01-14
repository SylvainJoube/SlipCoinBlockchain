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
	
	public static SCBlockDataType getFromInt(int dataType) {
		switch (dataType) {
		case 0 : return UNKNOWN;
		case 1 : return TRANSACTION;
		case 2 : return SYSTEM_MESSAGE;
		case 3 : return SYSTEM_ERROR;
		default : return UNKNOWN;
		}
		
	}
	
	
}
