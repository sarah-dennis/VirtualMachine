package backEndParsing;

import java.util.ArrayList;
import java.util.List;

import RISC.RiscRegisters;
import intermediateRepresentation.Abs_Expression;
import intermediateRepresentation.intermediateRepresentation;
import intermediateRepresentation.BinOp;
import intermediateRepresentation.Branch;
import intermediateRepresentation.DataLabel;
import intermediateRepresentation.Jump;
import intermediateRepresentation.Literal;
import intermediateRepresentation.MemoryAddress;
import intermediateRepresentation.Move;
import intermediateRepresentation.Temporary;
import intermediateRepresentation.Symbol;
import intermediateRepresentation.SysCall;
import operation_syntax.Operation;
import parse.Data;
import parse.ParseException;
import parse.SymbolTable;

/**
 * A class that takes abstract instructions and translates them to
 * RISC-V machine code. The method parseAbstracts takes an ArrayList
 * of abstract instructions and returns an ArrayList of corresponding
 * integers representing machine code, with a one-to-one relationship
 * between each ArrayList.
 */
public class AbsToMachine {
	//Temporary until we get the ra register from user
	public final static Temporary RA = new Temporary("ra");
	private static SymbolTable _st;
	private static Data _data;

	public static ArrayList<Integer> parseAbstracts
	(List<intermediateRepresentation> abs, SymbolTable st, Data d) throws ParseException {
		ArrayList<Integer> mc = new ArrayList<Integer>();
		_st = st;
		_data = d;
		for (intermediateRepresentation a : abs) {
			int i = parseInstruction(a);
			mc.add(i);
		}
		return mc;
	}

	public static int parseInstruction(intermediateRepresentation i) throws ParseException {

		if (i instanceof Move) {
			return moveCode((Move) i);
		} else if (i instanceof Branch) {
			return branchCode((Branch) i);
		} else if (i instanceof Jump) {
			return jumpCode((Jump) i);
		} else if (i instanceof SysCall) {
			return MachineNumber.SYSCALL_OPCODE;
		} else {
			throw new ParseException
			("Disassembly not implemented for instruction", null);
		}
	}

	public static int moveCode(Move i) throws ParseException {
		int machine = 0;


		//////////// DESTINATION PARSE \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\		
		Abs_Expression dest = ((Move) i).getDestination();

		if (dest instanceof Temporary) {
			int rd = RiscRegisters.REGISTERS.indexOf(((Temporary) dest).get());
			machine += rd << MachineNumber.RD_SHAMT;


		} else if (dest instanceof DataLabel) {
			String s = ((DataLabel) dest).getLabel().get();
			if (_data.containsLabel(s)) {
				Integer val = _data.getVal(s);
				machine += val << MachineNumber.RD_SHAMT;
			} else {
				throw new ParseException("undefined data label " + s, null);
			}

		} else if (dest instanceof MemoryAddress) { //Store instruction
			Abs_Expression address = ((MemoryAddress) dest).getAddress();
			int size = ((MemoryAddress) dest).getSize();
			int funct3 = 0; //temp

			if (address instanceof BinOp) {

				Operation op = ((BinOp) address).getOp();

				//SW, SB, SH INSTRUCTIONS
				if (op.equals(Operation.ADD)) {
					machine += MachineNumber.S_OPCODE;

					if(size == MachineNumber.W_BYTES) { //sw
						funct3 = MachineNumber.SW_FUNCT3;
					}else if(size == MachineNumber.H_BYTES) { //sh
						funct3 = MachineNumber.SH_FUNCT3;
					}else if(size == MachineNumber.B_BYTES) {//sb
						funct3 = MachineNumber.SB_FUNCT3;
					}
				} else {
					throw new ParseException
					("Incorrect operand in store instruction " + op, null);
				}

				Abs_Expression arg1 = ((BinOp) address).getArg1();
				if (arg1 instanceof Literal) {
					int imm = ((Literal) arg1).get();

					machine += (imm & MachineNumber.BITS_4TO0_MASK) 
							<< MachineNumber.BITS_4TO1_SHAMT;

					machine += (imm & MachineNumber.BITS_11TO5_MASK)
							<< MachineNumber.BITS_11TO5_SHAMT;

					machine += funct3 << MachineNumber.FUNCT3_SHAMT;
				} else {
					throw new ParseException
					("Incorrect arg type for store instruction " + 
							arg1.toString(), null);
				}

				Abs_Expression arg2 = ((BinOp) address).getArg2();
				if (arg2 instanceof Temporary) {
					int rs1 =  RiscRegisters.REGISTERS.indexOf(((Temporary) arg2).get());
					machine += rs1 << MachineNumber.RS1_SHAMT;
				} else {
					throw new ParseException
					("Incorrect arg type for store instruction " + 
							arg2.toString(), null);
				}
			} else {
				throw new ParseException
				("Incorrect address type in MemAddress " + 
						address.toString(), null);
			}
		} else {
			throw new ParseException
			("Unknown destination type in Move code " + 
					dest.toString(), null);
		}

		//////////// SOURCE PARSE \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\	
		Abs_Expression src = ((Move) i).getSource();
		if (src instanceof BinOp) {
			Operation op = ((BinOp) src).getOp();
			Abs_Expression arg1 = ((BinOp) src).getArg1();
			Abs_Expression arg2 = ((BinOp) src).getArg2();

			//add, sub, sll, srl, sra, or, xor, and, slt
			if (arg1 instanceof Temporary) { 
				int rs1 =  RiscRegisters.REGISTERS.indexOf(((Temporary) arg1).get());
				machine += rs1 << MachineNumber.RS1_SHAMT;

				//addi, slli, srli, srai, slti
			} else {
				throw new ParseException
				("Unknown argument in BinOp in Move " + 
						arg1.toString(), null);
			}

			if(arg2 instanceof Temporary) {
				machine += MachineNumber.R_OPCODE;
				int rs2 =  RiscRegisters.REGISTERS.indexOf(((Temporary) arg2).get());
				rs2 = rs2 << MachineNumber.RS2_SHAMT;
				machine += rs2;
				//ADD INSTRUCTION
				if (op.equals(Operation.ADD)) {
					machine += MachineNumber.ADD_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.ADD_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//SUB INSTRUCTION
				else if (op.equals(Operation.SUB)) {
					machine += MachineNumber.SUB_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.SUB_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//AND INSTRUCTION
				else if (op.equals(Operation.AND)) {
					machine += MachineNumber.AND_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.AND_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//OR INSTRUCTION
				else if (op.equals(Operation.OR)) {
					machine += MachineNumber.OR_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.OR_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//XOR INSTRUCTION 
				else if (op.equals(Operation.X_OR)) {
					machine += MachineNumber.XOR_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.XOR_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//SLT INSTRUCTION
				else if (op.equals(Operation.LESS)) {
					machine += MachineNumber.SLT_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.SLT_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//SLTU INSTRUCTION
				else if(op.equals(Operation.LESS_U)) {
					machine += MachineNumber.SLTU_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.SLTU_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//SRL INSTRUCTION
				else if (op.equals(Operation.SHIFT_RIGHT_L)) {
					machine += MachineNumber.SRL_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.SRL_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//SRA INSTRUCTION
				else if (op.equals(Operation.SHIFT_RIGHT_A)) {
					machine += MachineNumber.SRA_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.SRA_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//SLL INSTRUCTION
				else if (op.equals(Operation.SHIFT_LEFT)) {
					machine += MachineNumber.SLL_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.SLL_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}
				
				//MUL INSTRUCTION
				else if (op.equals(Operation.MULT)) {
					machine += MachineNumber.MUL_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.MULT_DIV_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}
				
				//DIV INSTRUCTION
				else if (op.equals(Operation.DIV)) {
					machine += MachineNumber.DIV_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.MULT_DIV_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}

				//REM INSTRUCTION
				else if (op.equals(Operation.REM)) {
					machine += MachineNumber.REM_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					machine += MachineNumber.MULT_DIV_FUNCT7 << MachineNumber.FUNCT7_SHAMT;
				}


			}else if (arg2 instanceof Literal) {
				int imm = ((Literal) arg2).get();
				machine += imm << MachineNumber.I_IMM_SHAMT;
				//ADDI INSTRUCTION
				if (op.equals(Operation.ADD)) { 
					machine += MachineNumber.I_OPCODE; //opcode for addi instruction
					machine += MachineNumber.ADDI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//SLLI INSTRUCTION
				else if (op.equals(Operation.SHIFT_LEFT)) {
					machine += MachineNumber.I_OPCODE; //opcode for slli instruction
					machine += MachineNumber.SLLI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//SRLI INSTRUCTION
				else if (op.equals(Operation.SHIFT_RIGHT_L)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.SRLI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//SRAI INSTRUCTION
				else if (op.equals(Operation.SHIFT_RIGHT_A)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.SRAI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
					//turn bit on in high immediate to differentiate b/w srli and srai
					machine += MachineNumber.SRAI_BIT;
				}

				//ANDI INSTRUCTION
				else if (op.equals(Operation.AND)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.ANDI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//ORI INSTRUCTION
				else if (op.equals(Operation.OR)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.ORI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//XORI INSTRUCTION
				else if (op.equals(Operation.X_OR)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.XORI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//SLTI INSTRUCTION
				else if (op.equals(Operation.LESS)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.SLTI_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				}

				//SLTIU INSTRUCTION
				else if(op.equals(Operation.LESS_U)) {
					machine += MachineNumber.I_OPCODE; //opcode for srli instruction
					machine += MachineNumber.SLTIU_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
				} else {
					throw new ParseException("Unknown arg in BinOp in Move " + arg2.toString(), null);
				}
			} else {
				throw new ParseException("Unknown op in BinOp in Move " + op, null);
			}


		} else if (src instanceof Temporary) { //Store instruction
			int rs2 =  RiscRegisters.REGISTERS.indexOf(((Temporary) src).get());
			machine += rs2 << MachineNumber.RS2_SHAMT;

		} else if (src instanceof DataLabel) {
			String s = ((DataLabel) src).getLabel().get();
			if (_data.containsLabel(s)) {
				Integer val = _data.getVal(s);
				machine += val;
			} else {
				throw new ParseException("undefined data label " + s, null);
			}



		} else if (src instanceof MemoryAddress) { //Load instruction
			Abs_Expression address = ((MemoryAddress) src).getAddress();
			int size = ((MemoryAddress) src).getSize();
			int funct3 = 0; //temp

			if (address instanceof BinOp) {

				Operation op = ((BinOp) address).getOp();

				//LW, LB, LH INSTRUCTIONS
				if (op.equals(Operation.ADD)) {

					machine += MachineNumber.L_OPCODE;
					if (size == MachineNumber.W_BYTES) { //lw
						funct3 = MachineNumber.LW_FUNCT3;
					} else if (size == MachineNumber.B_BYTES) { //lb
						funct3 = MachineNumber.LB_FUNCT3;
					} else if (size == MachineNumber.H_BYTES) {//lh
						funct3 = MachineNumber.LH_FUNCT3;
					}

				} else {
					throw new ParseException
					("Invalid rel op in BinOp in Memory Address " + op, null);
				}

				Abs_Expression arg1 = ((BinOp) address).getArg1();
				if (arg1 instanceof Literal) {
					int imm = ((Literal) arg1).get();
					machine += imm << MachineNumber.I_IMM_SHAMT;
					machine += funct3 << MachineNumber.FUNCT3_SHAMT;
				} else {
					throw new ParseException
					("Incorrect arg type for load instruction " +
							arg1.toString(), null);
				}

				Abs_Expression arg2 = ((BinOp) address).getArg2();
				if (arg2 instanceof Temporary) {
					int rs1 =  RiscRegisters.REGISTERS.indexOf(((Temporary) arg2).get());
					machine += rs1 << MachineNumber.RS1_SHAMT;
				}
				else {
					throw new ParseException
					("Incorrect arg type for load instruction " + 
							arg2.toString(), null);
				}
			} else {
				throw new ParseException
				("Unknown kind of address in MemAdd " + 
						address.toString(), null);
			}
		} else {
			throw new ParseException
			("Unknown abstract expression in Move " + 
					src.toString(), null);
		}

		return machine;
	}

	public static int branchCode(Branch i) throws ParseException {
		int machine = 0;
		Abs_Expression relOp = ((Branch) i).getRelOp();
		if (relOp instanceof BinOp) {

			Operation op = ((BinOp) relOp).getOp();

			//BEQ INSTRUCTION
			if (op.equals(Operation.EQUAL)) {
				machine += MachineNumber.B_OPCODE;
				machine += MachineNumber.BEQ_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
			}

			//BNE INSTRUCTION
			else if (op.equals(Operation.NOT_EQUAL)) {
				machine += MachineNumber.B_OPCODE;
				machine += MachineNumber.BNE_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
			}

			//BLT INSTRUCTION
			else if (op.equals(Operation.LESS)) {
				machine += MachineNumber.B_OPCODE;
				machine += MachineNumber.BLT_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
			}

			//BLTU INSTRUCTION
			else if (op.equals(Operation.LESS_U)) {
				machine += MachineNumber.B_OPCODE;
				machine += MachineNumber.BLTU_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
			}


			//BGE INSTRUCTION
			else if (op.equals(Operation.GREATER_EQUAL)) {
				machine += MachineNumber.B_OPCODE;
				machine += MachineNumber.BGE_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
			}

			//BGEU INSTRUCTION
			else if (op.equals(Operation.GREATER_EQUAL_U)) {
				machine += MachineNumber.B_OPCODE;
				machine += MachineNumber.BGEU_FUNCT3 << MachineNumber.FUNCT3_SHAMT;
			} else {
				throw new ParseException("Invalid RelOp in Branch " + op, null);
			}

			Abs_Expression arg1 = ((BinOp) relOp).getArg1();
			if (arg1 instanceof Temporary) {
				int rs1 =  RiscRegisters.REGISTERS.indexOf(((Temporary) arg1).get());            
				machine += rs1 << MachineNumber.RS1_SHAMT;
			} else {
				throw new ParseException
				("Invalid argument type in BinOp in Branch " + 
						arg1.toString(), null);
			}

			Abs_Expression arg2 = ((BinOp) relOp).getArg2();
			if (arg2 instanceof Temporary) {
				int rs2 =  RiscRegisters.REGISTERS.indexOf(((Temporary) arg2).get());
				machine +=  rs2 << MachineNumber.RS2_SHAMT;
			} else {
				throw new ParseException
				("Invalid argument type in BinOp in Branch " + 
						arg2.toString(), null);
			}
		}

		Abs_Expression dest = ((Branch) i).getDestination();
		if (dest instanceof Symbol) {
			String s = ((Symbol) dest).get();
			int linePos = _st.getCodeLine(s);

			machine += (linePos & MachineNumber.BIT_12_MASK_INT)
					<< MachineNumber.BIT_12_SHAMT;

			machine += (linePos & MachineNumber.BITS_10TO5_MASK_INT)
					<< MachineNumber.BITS_10TO5_SHAMT;

			machine += (linePos & MachineNumber.BITS_4TO1_MASK_INT)
					<< MachineNumber.BITS_4TO1_SHAMT;

			machine += (linePos & MachineNumber.BIT_11_MASK)
					<< MachineNumber.B_BIT_11_SHAMT;

		} else {
			throw new ParseException
			("Invalid destination type in Branch " +
					dest.toString(), null);
		}
		return machine;
	}

	public static int jumpCode(Jump i) throws ParseException {
		int machine = 0;
		Abs_Expression dest = i.getTarget();

		if (dest instanceof Symbol) {
			machine += MachineNumber.JAL_OPCODE; //jal opcode
			String s = ((Symbol) dest).get();
			int linePos = _st.getCodeLine(s);

			machine += (linePos & MachineNumber.BIT_12_MASK_INT)
					<< MachineNumber.BIT_12_SHAMT;

			machine += (linePos & MachineNumber.BITS_19TO12_MASK_INT)
					<< MachineNumber.UPPER_MIDDLE_BITS_SHAMT;

			machine += (linePos & MachineNumber.BITS_10TO1_MASK_INT)
					<< MachineNumber.BITS_10TO1_SHAMT;

			machine += (linePos & MachineNumber.BIT_11_MASK)
					<< MachineNumber.J_BIT_11_SHAMT;
		} else if (dest instanceof Temporary) { 
			machine += MachineNumber.JAL_OPCODE; //jalr opcode
			int val =  RiscRegisters.REGISTERS.indexOf(((Temporary) dest).get());

			machine += (val & MachineNumber.BIT_12_MASK_INT)
					<< MachineNumber.BIT_12_SHAMT;

			machine += (val & MachineNumber.BITS_19TO12_MASK_INT)
					<< MachineNumber.UPPER_MIDDLE_BITS_SHAMT;

			machine += (val & MachineNumber.BITS_10TO1_MASK_INT)
					<< MachineNumber.BITS_10TO1_SHAMT;

			machine += (val & MachineNumber.BIT_11_MASK)
					<< MachineNumber.J_BIT_11_SHAMT;
		} else {
			throw new ParseException
			("Invalid destination type in Jump " + 
					dest.toString(), null);
		}

		return machine;
	}
}