package backEndParsing;

import java.util.ArrayList;

import RISC.RiscOperations;
import RISC.RiscRegisters;
import intermediateRepresentation.Abs_Expression;
import intermediateRepresentation.intermediateRepresentation;
import intermediateRepresentation.BinOp;
import intermediateRepresentation.Branch;
import intermediateRepresentation.Jump;
import intermediateRepresentation.Literal;
import intermediateRepresentation.MemoryAddress;
import intermediateRepresentation.Move;
import intermediateRepresentation.Temporary;
import intermediateRepresentation.Symbol;
import intermediateRepresentation.SysCall;
import operation_syntax.Operation;
import parse.ParseException;

public class MachineToAbs {	

	RiscOperations ops = new RiscOperations();
	
	public ArrayList<intermediateRepresentation> machineToAbs(String source) throws ParseException {
		ArrayList<Integer> data = new ArrayList<Integer>();
		String[] s = source.split("\n");
		for (String str : s) { 
			int num = Integer.parseInt(str);
			data.add(num);
		}
		return constructCode(data);
	}
	
	public ArrayList<intermediateRepresentation> constructCode(ArrayList<Integer> raw) throws ParseException {
		ArrayList<intermediateRepresentation> instruct = new ArrayList<intermediateRepresentation>();
		for (int i : raw) {
			int op = i & MachineNumber.OP_MASK;
			switch (op) {
				case MachineNumber.R_OPCODE: //R instruction
					instruct.add(translateRLineToAbs(rParse(i)));
				    break;
				case MachineNumber.L_OPCODE: // lw, lb, lh
					instruct.add(translateLToAbs(iParse(i)));
					break;
				case MachineNumber.I_OPCODE: //I instruction
					instruct.add(translateImmToAbs(iParse(i)));
					break;
				case MachineNumber.S_OPCODE: //S instruction
					//since format of R and S exact same, can parse the same
					instruct.add(translateSLineToAbs(rParse(i))); 
					break;
				case MachineNumber.B_OPCODE: //B instruction
					instruct.add(translateBLineToAbs(bParse(i)));
					break;
				case MachineNumber.JAL_OPCODE: //J instructions 
				case MachineNumber.JALR_OPCODE:
					instruct.add(translateJLineToAbs(jParse(i)));
					break;
				case MachineNumber.SYSCALL_OPCODE:
					instruct.add(translateSysCallToAbs());
					break;
				default:
					throw new ParseException("Invalid opcode: " + (op) + " " + raw.indexOf(i), null);
			}
		}
		return instruct;	
	}
	
	public int[] rParse(int instruct) {
		int[] tokens = new int[5]; //only 6 tokens in an R format instruction, opcode unnecessary
		tokens[0] = (instruct & MachineNumber.FUNCT7_MASK) 
					>> MachineNumber.FUNCT7_SHAMT;
		tokens[1] = (instruct & MachineNumber.RS2_MASK) 
				     >> MachineNumber.RS2_SHAMT;
		tokens[2] = (instruct & MachineNumber.RS1_MASK)
				    >> MachineNumber.RS1_SHAMT;
		tokens[3] = ( instruct & MachineNumber.FUNCT3_MASK)
				     >> MachineNumber.FUNCT3_SHAMT;
		tokens[4] = (instruct & MachineNumber.RD_MASK)
				     >> MachineNumber.RD_SHAMT;
		return tokens;
	}
	
	public int[] iParse(int instruct) {
		int[] tokens = new int[6]; //only 5 tokens in an I format instruction, plus srai/srli bit
		tokens[0] = (instruct & MachineNumber.IMM_MASK)
				    >> MachineNumber.I_IMM_SHAMT;
		tokens[1] = (instruct & MachineNumber.RS1_MASK) 
					>> MachineNumber.RS1_SHAMT;
		tokens[2] = (instruct & MachineNumber.FUNCT3_MASK)
				 	>> MachineNumber.FUNCT3_SHAMT;
		tokens[3] = (instruct & MachineNumber.RD_MASK) 
					>> MachineNumber.RD_SHAMT; 
		tokens[4] = instruct & MachineNumber.OP_MASK; //opcode necessary to keep as it differs between I commands;
		tokens[5] = instruct & MachineNumber.SRAI_BIT;
		return tokens;
	}
	
	
	public int[] bParse(int instruct) {
		int[] tokens = new int[4]; //only 8 tokens in a B format instruction, immediates combine
		int upper_bit = (instruct & MachineNumber.BIT_12_MASK_MACHINE) 
					 	 >> MachineNumber.B_UPPER_BIT_SHAMT; //want highest bit to still represent accurate value
		int second_upper_bit = (instruct & MachineNumber.BIT_11_MASK_B)
								<< MachineNumber.SECOND_UPPER_BIT_SHAMT;
		int middle_bits = (instruct & MachineNumber.BITS_10TO5_MASK_MACHINE)
					  	  >> MachineNumber.MIDDLE_BITS_SHAMT;
		int lower_bits = (instruct & MachineNumber.BITS_4TO1_MASK_MACHINE)
					 	  >> MachineNumber.B_LOWER_BITS_SHAMT;
		tokens[0] =  (upper_bit + second_upper_bit + middle_bits + lower_bits) 
					 << MachineNumber.B_IMM_SHAMT; //since we discarded last bit during assembly, need to bring it back
		tokens[1] = (instruct & MachineNumber.RS2_MASK)
					 >> MachineNumber.RS2_SHAMT;
		tokens[2] = (instruct & MachineNumber.RS1_MASK)
					 >> MachineNumber.RS1_SHAMT;
		tokens[3] = (instruct & MachineNumber.FUNCT3_MASK) 
					>> MachineNumber.FUNCT3_SHAMT;
		return tokens;
	}
	
	public int[] jParse(int instruct) {
		int[] tokens = new int[2]; //only 6 tokens in a J format instruction, immediates combine
		
		int upper_bit = (instruct & MachineNumber.BIT_12_MASK_MACHINE) 
						>> MachineNumber.J_UPPER_BIT_SHAMT;
		int upper_middle_bits = (instruct & MachineNumber.BITS_19TO12_MASK_MACHINE) 
								 >> MachineNumber.UPPER_MIDDLE_BITS_SHAMT;
		int bit_11 = (instruct & MachineNumber.BIT_11_MASK_J) 
					  >> MachineNumber.BIT_11_SHAMT_MACHINE;
		int lower_bits = ( instruct & MachineNumber.BITS_10TO1_MASK_MACHINE)
						 >> MachineNumber.J_LOWER_BITS_SHAMT;
		tokens[0] = upper_bit + upper_middle_bits + bit_11 + lower_bits;
		tokens[1] = (instruct & MachineNumber.RD_MASK) 
					>> MachineNumber.RD_SHAMT;
		return tokens;
	}
	
	//Currently unused, but will keep in case we add U type instructions
	public int[] uParse(int instruct) {
		int[] tokens = new int[1]; //we only really care about the immediate here, assume rd always 0
		tokens[0] = (instruct & MachineNumber.BITS_31TO12_MASK)
					 >> MachineNumber.U_IMM_SHAMT;
		return tokens;
	}
	
	public intermediateRepresentation translateRLineToAbs(int[] token) throws ParseException {
		Abs_Expression rd = new Temporary( RiscRegisters.REGISTERS.get(token[4]));
		String command = null;
		if (token[0] == MachineNumber.ADD_FUNCT7 && 
		    token[3] == MachineNumber.ADD_FUNCT3) {
			command = "add";
		} else if (token[0] == MachineNumber.AND_FUNCT7 && 
				   token[3] == MachineNumber.AND_FUNCT3) {
			command = "and";
		} else if (token[0] == MachineNumber.SRL_FUNCT7 && 
				   token[3] == MachineNumber.SRL_FUNCT3) {
			command = "srl";
		} else if (token[0] == MachineNumber.SLL_FUNCT7 && 
				   token[3] == MachineNumber.SLL_FUNCT3) {
			command = "sll";
		} else if (token[0] == MachineNumber.SUB_FUNCT7 && 
				   token[3] == MachineNumber.SUB_FUNCT3) {
			command = "sub";
		} else if (token[0] == MachineNumber.SRA_FUNCT7 && 
				   token[3] == MachineNumber.SRA_FUNCT3) {
			command = "sra";
		} else if (token[0] == MachineNumber.XOR_FUNCT7 && 
				   token[3] == MachineNumber.XOR_FUNCT3) {
			command = "xor";
		} else if (token[0] == MachineNumber.OR_FUNCT7 && 
				   token[3] == MachineNumber.OR_FUNCT3) {
			command = "or";
		} else if (token[0] == MachineNumber.SLT_FUNCT7 && 
				   token[3] == MachineNumber.SLT_FUNCT3) {
			command = "slt";
		} else if (token[0] == MachineNumber.SLTU_FUNCT7 && 
				   token[3] == MachineNumber.SLTU_FUNCT3) {
			command = "sltu";
		} else if (token[0] == MachineNumber.MULT_DIV_FUNCT7 && 
				   token[3] == MachineNumber.MUL_FUNCT3) {
			command = "mul";
		} else if (token[0] == MachineNumber.MULT_DIV_FUNCT7 && 
				   token[3] == MachineNumber.DIV_FUNCT3) {
			command = "div";
		} else if (token[0] == MachineNumber.MULT_DIV_FUNCT7 && 
				   token[3] == MachineNumber.REM_FUNCT3) {
			command = "rem";
		} else {
			throw new ParseException("Invalid R type command", null);
		}
		
		Abs_Expression rs1 = new Temporary(RiscRegisters.REGISTERS.get(token[2]));
		Abs_Expression rs2 = new Temporary(RiscRegisters.REGISTERS.get(token[1]));
		Abs_Expression source = new BinOp(parseOp(command), rs1, rs2);
		intermediateRepresentation a = new Move(rd, source);
		return a;
	}
	
	public intermediateRepresentation translateImmToAbs(int[] token) throws ParseException {
		String command = null;
		if (token[4] == MachineNumber.I_OPCODE && 
			token[2] == MachineNumber.ADDI_FUNCT3) {
			command = "addi";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.SLLI_FUNCT3) {
			command = "slli";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.SRLI_FUNCT3 &&
				   token[5] == 0) {
			command = "srli";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.SRAI_FUNCT3 &&
				   token[5] == 1) {
			command = "srai";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.SLTI_FUNCT3) {
			command = "slti";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.ORI_FUNCT3) {
			command = "ori";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.XORI_FUNCT3) {
			command = "xori";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.ANDI_FUNCT3) {
			command = "andi";
		} else if (token[4] == MachineNumber.I_OPCODE && 
				   token[2] == MachineNumber.SLTIU_FUNCT3) {
			command = "sltiu";
		} else {
			throw new ParseException("Invalid I type command", null);
		}
		
		Abs_Expression rd = new Temporary(RiscRegisters.REGISTERS.get(token[3]));
		Abs_Expression rs1 = new Temporary(RiscRegisters.REGISTERS.get(token[1]));
		Abs_Expression imm = new Literal(token[0]);
		Abs_Expression source = new BinOp(parseOp(command), rs1, imm);
		intermediateRepresentation a = new Move(rd, source);
		return a;
	}
	
	public intermediateRepresentation translateLToAbs(int[] token) throws ParseException {
		int bytes;
		if (token[2] == MachineNumber.LW_FUNCT3) {
			bytes = MachineNumber.W_BYTES;
		} else if (token[2] == MachineNumber.LB_FUNCT3) {
			bytes = MachineNumber.B_BYTES;
		} else if (token[2] == MachineNumber.LH_FUNCT3) {
			bytes = MachineNumber.H_BYTES;
		} else {
			throw new ParseException("Invalid L type command", null);
		}
		
		Abs_Expression rd = new Temporary(RiscRegisters.REGISTERS.get(token[3]));
		Abs_Expression imm = new Literal(token[0]);
		Abs_Expression rs1 = new Temporary(RiscRegisters.REGISTERS.get(token[1]));
		Abs_Expression binop = new BinOp(parseOp("add"), imm, rs1);
		Abs_Expression source = new MemoryAddress(binop, bytes);
		intermediateRepresentation a = new Move(rd, source);
		return a;
	}
	
	public intermediateRepresentation translateSLineToAbs(int[] token) throws ParseException {
		int bytes;
		if (token[3] == MachineNumber.SW_FUNCT3) {
			bytes = MachineNumber.W_BYTES;
		} else if (token[3] == MachineNumber.SB_FUNCT3) {
			bytes = MachineNumber.B_BYTES;
		} else if (token[3] == MachineNumber.SH_FUNCT3) {
			bytes = MachineNumber.H_BYTES;
		}else {
			throw new ParseException("Invalid S type command", null);
		}
		
		Abs_Expression rs2 = new Temporary(RiscRegisters.REGISTERS.get(token[1]));
		Abs_Expression imm = new Literal(token[4] + token[0]);
	    Abs_Expression rs1 = new Temporary(RiscRegisters.REGISTERS.get(token[2]));
		Abs_Expression binop = new BinOp(parseOp("add"), imm, rs1);
		Abs_Expression source = new MemoryAddress(binop, bytes);
		intermediateRepresentation a = new Move(rs2, source);
		return a;
	}
	
	public intermediateRepresentation translateBLineToAbs(int[] token) throws ParseException {
		String command = null;
		if (token[3] == MachineNumber.BEQ_FUNCT3) {
			command = "beq";
		} else if (token[3] == MachineNumber.BNE_FUNCT3) {
			command = "bne";
		} else if (token[3] == MachineNumber.BLT_FUNCT3) {
			command = "blt";
		} else if (token[3] == MachineNumber.BGE_FUNCT3) {
			command = "bge";
		} else if (token[3] == MachineNumber.BGEU_FUNCT3) {
			command = "bgeu";
		} else if (token[3] == MachineNumber.BLTU_FUNCT3) {
			command = "bltu";
		} else {
			throw new ParseException("Invalid B type command " +  token[3], null);
		}
		
		Abs_Expression rs1 = new Temporary(RiscRegisters.REGISTERS.get(token[2]));
		Abs_Expression rs2 = new Temporary(RiscRegisters.REGISTERS.get(token[1])); 
		Abs_Expression imm = new Literal(token[0]); 
		Abs_Expression symbol = new Symbol("line" + imm);
		Abs_Expression binop = new BinOp(parseOp(command), rs1, rs2);
		intermediateRepresentation branch = new Branch(binop, symbol);
		return branch;
	}
	
	public intermediateRepresentation translateJLineToAbs(int[] token) {
		//"j" instruction
		Abs_Expression rd = new Temporary(RiscRegisters.REGISTERS.get(token[1]));
		
		Abs_Expression imm = new Literal(token[0]);
		Abs_Expression newLabel = new Symbol("line" + imm);
		intermediateRepresentation jump = new Jump(rd, newLabel);
		return jump;
	}
	
	public intermediateRepresentation translateSysCallToAbs() {	
		return new SysCall();
	}	
	

	private Operation parseOp(String op) {
		return ops.opOf(op);
	}
}
