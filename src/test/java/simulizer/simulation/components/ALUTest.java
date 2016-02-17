package simulizer.simulation.components;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import category.UnitTests;
import simulizer.assembler.representation.Instruction;
import simulizer.simulation.cpu.components.ALU;
import simulizer.simulation.data.representation.DataConverter;
import simulizer.simulation.data.representation.Word;
import simulizer.simulation.exceptions.InstructionException;


/**tests all ALU operations
 * 
 * @author Charlie Street
 * 
 */
@Category({UnitTests.class})
public class ALUTest {

	private ALU alu;
	
	/**initialising the alu
	 * 
	 */
	@Before
	public void ALUSetup()
	{
		this.alu = new ALU();
	}
	
	/**produces an optional signed word 
	 * this is the type required by the alu
	 * @param num the number to convert
	 * @return number in correct format
	 */
	public Optional<Word> signedW(long num)
	{
		return Optional.of(new Word(DataConverter.encodeAsSigned(num)));
	}
	
	/**produces an optional unsigned word 
	 * this is the type required by the alu
	 * @param num the number to convert
	 * @return number in correct format
	 */
	public Optional<Word> unsignedW(long num)
	{
		return Optional.of(new Word(DataConverter.encodeAsUnsigned(num)));
	}
	
	/**executes a given alu operation with data passed in 
	 * 
	 * @param instruction the instruction to execute
	 * @param word1 the first word of data
	 * @param word2 the second word to execute
	 * @return the long result
	 * @throws InstructionException
	 */
	public long executeS(Instruction instruction, Optional<Word> word1, Optional<Word> word2) throws InstructionException
	{
		return DataConverter.decodeAsSigned(this.alu.execute(instruction, word1, word2).getWord());
	}
	
	/**executes a given alu operation with data passed in 
	 * 
	 * @param instruction the instruction to execute
	 * @param word1 the first word of data
	 * @param word2 the second word to execute
	 * @return the long result
	 * @throws InstructionException
	 */
	public long executeU(Instruction instruction, Optional<Word> word1, Optional<Word> word2) throws InstructionException
	{
		return DataConverter.decodeAsUnsigned(this.alu.execute(instruction, word1, word2).getWord());
	}
	
	/**testing all alu operations with 3 cases each
	 * @throws InstructionException when a bad instruction is used
	 * 
	 */
	@Test
	public void testALUOps() throws InstructionException
	{
		{//abs
			assertEquals(10,executeS(Instruction.abs,signedW(-10),Optional.empty()));
			assertEquals(27,executeS(Instruction.abs,signedW(27),Optional.empty()));
			assertEquals(0,executeS(Instruction.abs,signedW(0),Optional.empty()));
		}
		
		{//and
			assertEquals(0,executeS(Instruction.and,signedW(0),signedW(17)));
			assertEquals(16,executeS(Instruction.and,signedW(17),signedW(24)));
			assertEquals(-6,executeS(Instruction.and,signedW(-5),signedW(-2)));
		}
		
		{//add
			
			assertEquals(11,executeS(Instruction.add,signedW(4),signedW(7)));
			assertEquals(-14,executeS(Instruction.add,signedW(-4),signedW(-10)));
			assertEquals(-1,executeS(Instruction.add,signedW(-7),signedW(6)));
		}
		
		{//addu
			
			assertEquals((long)(Math.pow(2, 31) + Math.pow(2, 31) - 1),executeU(Instruction.addu,unsignedW((long)Math.pow(2,31)),unsignedW((long)Math.pow(2,31)-1)));
			assertEquals((long)(Math.pow(2,31)),executeU(Instruction.addu,unsignedW((long)Math.pow(2,30)),unsignedW((long)Math.pow(2,30))));
			assertEquals(4,executeU(Instruction.addu,unsignedW(0),unsignedW(4)));
		}
		
		{//addi (same tests as for add)
			assertEquals(11,executeS(Instruction.addi,signedW(4),signedW(7)));
			assertEquals(-14,executeS(Instruction.addi,signedW(-4),signedW(-10)));
			assertEquals(-1,executeS(Instruction.addi,signedW(-7),signedW(6)));
		}
		
		{//addiu
			assertEquals((long)(Math.pow(2, 31) + Math.pow(2, 31) - 1),executeU(Instruction.addiu,unsignedW((long)Math.pow(2,31)),unsignedW((long)Math.pow(2,31)-1)));
			assertEquals((long)(Math.pow(2,31)),executeU(Instruction.addiu,unsignedW((long)Math.pow(2,30)),unsignedW((long)Math.pow(2,30))));
			assertEquals(4,executeU(Instruction.addiu,unsignedW(0),unsignedW(4)));
		}
		
		{//sub
			assertEquals(-3,executeS(Instruction.sub,signedW(4),signedW(7)));
			assertEquals(3,executeS(Instruction.sub,signedW(7),signedW(4)));
			assertEquals(6,executeS(Instruction.sub,signedW(-4),signedW(-10)));
		}
		
		{//subu
			assertEquals(0,executeU(Instruction.subu,unsignedW((long)Math.pow(2,31)),unsignedW((long)Math.pow(2,31))));
			assertEquals((long)Math.pow(2,31),executeU(Instruction.subu,unsignedW((long)Math.pow(2,31)),unsignedW(0)));
			assertEquals((long)Math.pow(2,32)-2,executeU(Instruction.subu,unsignedW((long)Math.pow(2,32)-1),unsignedW(1)));
		}
		
		{//subi
			assertEquals(-3,executeS(Instruction.subi,signedW(4),signedW(7)));
			assertEquals(3,executeS(Instruction.subi,signedW(7),signedW(4)));
			assertEquals(6,executeS(Instruction.subi,signedW(-4),signedW(-10)));
		}
		
		{//subiu
			assertEquals(0,executeU(Instruction.subu,unsignedW((long)Math.pow(2,31)),unsignedW((long)Math.pow(2,31))));
			assertEquals((long)Math.pow(2,31),executeU(Instruction.subu,unsignedW((long)Math.pow(2,31)),unsignedW(0)));
			assertEquals((long)Math.pow(2,32)-2,executeU(Instruction.subu,unsignedW((long)Math.pow(2,32)-1),unsignedW(1)));
		}
		
		{//mul
			
			assertEquals(1073709056L,executeS(Instruction.mul,signedW((long)(Math.pow(2,15)-1)),signedW((long)Math.pow(2,15))));
			assertEquals(0,executeS(Instruction.mul,signedW(0),signedW((long)Math.pow(2,15))));
			assertEquals(12,executeS(Instruction.mul,signedW(-4),signedW(-3)));
		}
		
		{//mulo (check this one)
			assertEquals(1073709056L,executeS(Instruction.mulo,signedW((long)(Math.pow(2,15)-1)),signedW((long)Math.pow(2,15))));
			assertEquals(0,executeS(Instruction.mulo,signedW(0),signedW((long)Math.pow(2,15))));
			assertEquals(12,executeS(Instruction.mulo,signedW(-4),signedW(-3)));
		}
		
		{//mulou
			
			assertEquals(4294901760L,executeU(Instruction.mulou,unsignedW((long)(Math.pow(2,16)-1)),unsignedW((long)Math.pow(2,16))));
			assertEquals(0,executeU(Instruction.mulou,unsignedW(0),unsignedW((long)Math.pow(2,16))));
			assertEquals(12,executeU(Instruction.mulou,unsignedW(4),unsignedW(3)));
		}
		
		{//div
			assertEquals(0,executeS(Instruction.div,signedW(0),signedW(4)));
			assertEquals(2,executeS(Instruction.div,signedW(4),signedW(2)));
			assertEquals(-2,executeS(Instruction.div,signedW(4),signedW(-2)));
		}
		
		{//divu
			assertEquals(0,executeU(Instruction.divu,unsignedW(0),unsignedW(4)));
			assertEquals(1,executeU(Instruction.divu,unsignedW((long)(Math.pow(2, 32)-1)),unsignedW((long)(Math.pow(2, 32)-1))));
			assertEquals(2,executeU(Instruction.divu,unsignedW(4),unsignedW(2)));
		}
		
		{//neg
			assertEquals(0,executeS(Instruction.neg,signedW(0),Optional.empty()));
			assertEquals(-1,executeS(Instruction.neg,signedW(1),Optional.empty()));
			assertEquals(1,executeS(Instruction.neg,signedW(-1),Optional.empty()));
		}
		
		{//negu
			assertEquals(0,executeU(Instruction.negu,signedW(0),Optional.empty()));
			assertEquals((long)Math.pow(2, 30),executeU(Instruction.negu,signedW(-1*(long)Math.pow(2, 30)),Optional.empty()));
			assertEquals(1,executeU(Instruction.negu,signedW(-1),Optional.empty()));
		}
		
		{//nor
			assertEquals(-1,executeS(Instruction.nor,signedW(0),signedW(0)));
			assertEquals(-2,executeS(Instruction.nor,signedW(0),signedW(1)));
			assertEquals(0,executeS(Instruction.nor,signedW(-1),signedW(1)));
		}
		 
		{//not
			assertEquals(-1,executeS(Instruction.not,signedW(0),Optional.empty()));
			assertEquals(0,executeS(Instruction.not,signedW(-1),Optional.empty()));
			assertEquals(-2,executeS(Instruction.not,signedW(1),Optional.empty()));
		}
		
		{//or			
			assertEquals(1,executeS(Instruction.or,signedW(0),signedW(1)));
			assertEquals(5,executeS(Instruction.or,signedW(1),signedW(4)));
			assertEquals(20,executeS(Instruction.or,signedW(16),signedW(4)));
		}
		
		{//ori
			assertEquals(1,executeS(Instruction.ori,signedW(0),signedW(1)));
			assertEquals(5,executeS(Instruction.ori,signedW(1),signedW(4)));
			assertEquals(20,executeS(Instruction.ori,signedW(16),signedW(4)));
		}
		
		{//xor
			assertEquals(10,executeS(Instruction.xor,signedW(4),signedW(14)));
			assertEquals(2,executeS(Instruction.xor,signedW(3),signedW(1)));
			assertEquals(3,executeS(Instruction.xor,signedW(3),signedW(0)));
		}
		
		{//xori
			assertEquals(10,executeS(Instruction.xor,signedW(4),signedW(14)));
			assertEquals(2,executeS(Instruction.xor,signedW(3),signedW(1)));
			assertEquals(3,executeS(Instruction.xor,signedW(3),signedW(0)));
		}
		
		{//b
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.b,signedW(0),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.b,signedW(-20),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.b,signedW(20),Optional.empty()));
		}
		
		{//beq
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.beq,signedW(0),signedW(1)));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.beq,signedW(0),signedW(0)));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.beq,signedW(0),signedW(-1)));
		}
		
		{//bne
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.bne,signedW(0),signedW(1)));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.bne,signedW(0),signedW(0)));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.bne,signedW(0),signedW(-1)));
		}
		
		{//bgez (entering random items in second slot to test that it isn't used by the ALU)
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.bgez,signedW(0),signedW(1)));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.bgez,signedW(-1),signedW(0)));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.bgez,signedW(1),signedW(-1)));
		}
		
		{//bgtz (now with Optional.empty() as second item)
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.bgtz,signedW(0),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.bgtz,signedW(-1),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.bgtz,signedW(1),Optional.empty()));
		}
		
		{//blez 
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.blez,signedW(0),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.blez,signedW(-1),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.blez,signedW(1),Optional.empty()));
		}
		
		{//bltz
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.bltz,signedW(0),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.bltz,signedW(-1),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.bltz,signedW(1),Optional.empty()));
		}
		
		{//beqz
			assertEquals(DataConverter.decodeAsSigned(ALU.branchTrue),executeS(Instruction.beqz,signedW(0),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.beqz,signedW(-1),Optional.empty()));
			assertEquals(DataConverter.decodeAsSigned(ALU.branchFalse),executeS(Instruction.beqz,signedW(1),Optional.empty()));
		}
		
		{//move
			assertEquals(0, executeS(Instruction.move, signedW(0),signedW(17)));
			assertEquals(-6, executeS(Instruction.move, signedW(-6),signedW(0)));
			assertEquals(5, executeS(Instruction.move, signedW(5),Optional.empty()));
		}	
	}
}