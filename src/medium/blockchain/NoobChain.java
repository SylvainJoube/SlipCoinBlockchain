package medium.blockchain;

//import com.google.gson.*;
import java.util.ArrayList;

public class NoobChain {
	
	public static ArrayList<Block> blockchain = new ArrayList<Block>(); 
	
	public static void mainnne(String[] args) {
		
		Block genesisBlock = new Block("Hi im the first block", "0");
		System.out.println("Hash for block 1 : " + genesisBlock.hash);
		
		Block secondBlock = new Block("Yo im the second block", genesisBlock.hash);
		System.out.println("Hash for block 2 : " + secondBlock.hash);
		
		Block thirdBlock = new Block("Hey im the third block", secondBlock.hash);
		System.out.println("Hash for block 3 : " + thirdBlock.hash);
		
	}
}