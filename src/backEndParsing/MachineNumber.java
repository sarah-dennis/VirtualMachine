package backEndParsing;

public class MachineNumber {
		
//--------RISC V opcodes---------------------------------------------
	public static final int R_OPCODE = 51;
	public static final int L_OPCODE = 3;
	public static final int I_OPCODE = 19;
	public static final int ECALL_OPCODE = 115;
	public static final int S_OPCODE = 35;
	public static final int B_OPCODE = 99;
	public static final int JAL_OPCODE = 111;
	public static final int JALR_OPCODE = 103;
	public static final int SYSCALL_OPCODE = 115;
	public static final int DATA_OPCODE = 1; //just chose a value for this...
//-------------------------------------------------------------------
	
//--------RISC V funct3 and funct7 codes-----------------------------
	public static final int ADD_FUNCT3 = 0;
	public static final int ADD_FUNCT7 = 0;
	
	public static final int SUB_FUNCT3 = 0;
	public static final int SUB_FUNCT7 = 32;
	
	public static final int AND_FUNCT3 = 7;
	public static final int AND_FUNCT7 = 0;
	
	public static final int OR_FUNCT3 = 6;
	public static final int OR_FUNCT7 = 0;
	
	public static final int XOR_FUNCT3 = 4;
	public static final int XOR_FUNCT7 = 0;
	
	public static final int MULT_DIV_FUNCT7 = 1;
	
	public static final int MUL_FUNCT3 = 0;
	public static final int DIV_FUNCT3 = 4;
	public static final int REM_FUNCT3 = 6;
	
	public static final int SRL_FUNCT3 = 5;
	public static final int SRL_FUNCT7 = 0;
	
	public static final int SRA_FUNCT3 = 5;
	public static final int SRA_FUNCT7 = 32;
	
	public static final int SLL_FUNCT3 = 1;
	public static final int SLL_FUNCT7 = 0;
	
	public static final int SLT_FUNCT3 = 2;
	public static final int SLT_FUNCT7 = 0;
	
	public static final int SLTU_FUNCT3 = 3;
	public static final int SLTU_FUNCT7 = 0;
	
	public static final int ADDI_FUNCT3 = 0;
	public static final int SLLI_FUNCT3 = 1;
	public static final int SLTI_FUNCT3 = 2;
	public static final int SLTIU_FUNCT3 = 3;
	public static final int XORI_FUNCT3 = 4;
	public static final int SRLI_FUNCT3 = 5;
	public static final int SRAI_FUNCT3 = 5;
	public static final int ORI_FUNCT3 = 6;
	public static final int ANDI_FUNCT3 = 7;
	
	public static final int LB_FUNCT3 = 0;
	public static final int LH_FUNCT3 = 1;
	public static final int LW_FUNCT3 = 2;
	
	public static final int SB_FUNCT3 = 0;
	public static final int SH_FUNCT3 = 1;
	public static final int SW_FUNCT3 = 2;
	
	public static final int BEQ_FUNCT3 = 0;
	public static final int BNE_FUNCT3 = 1;
	public static final int BLT_FUNCT3 = 4;
	public static final int BGE_FUNCT3 = 5;
	public static final int BLTU_FUNCT3 = 6;
	public static final int BGEU_FUNCT3 = 7;
//-------------------------------------------------------------------
	
//--------Byte amounts for load/store instructions-------------------
	public static final int W_BYTES = 4;
	public static final int H_BYTES = 2;
	public static final int B_BYTES = 1;
//-------------------------------------------------------------------	
	
//--------Abstract instruction to machine code masks-----------------	
	//For S type instructions...
	public static final int BITS_11TO5_MASK = 0xFE0;
	public static final int BITS_4TO0_MASK = 0x1F;
	
	//For B type instructions...
	public static final int BIT_12_MASK_INT = 0x1000;
	public static final int BITS_10TO5_MASK_INT = 0x7E0;
	public static final int BITS_4TO1_MASK_INT = 0x1E;
	public static final int BIT_11_MASK = 0x800;
	
	//For J type instructions...
	public static final int BIT_20_MASK = 0x100000;
	public static final int BITS_10TO1_MASK_INT = 0x7FE;
	public static final int BITS_19TO12_MASK_INT = 0xFF000;
//-------------------------------------------------------------------
	
//--------Machine code to abstract instruction masks-----------------
	//For ALL instructions...
	public static final int OP_MASK = 0x7F; //7 least significant bits are where opcode is stored
	public static final int RD_MASK = 0xF80;
	public static final int RS1_MASK = 0xF8000;
	public static final int RS2_MASK = 0x1F00000;
	public static final int FUNCT3_MASK = 0x7000;
	
	//For R type instructions...
	public static final int FUNCT7_MASK = 0xFE000000;
	
	//For I type instructions...
	public static final int IMM_MASK = 0xFFF00000;
	
	//For B type instructions...
	public static final int BIT_12_MASK_MACHINE = 0x80000000;
	public static final int BITS_10TO5_MASK_MACHINE = 0x7E000000;
	public static final int BITS_4TO1_MASK_MACHINE = 0xF00;
	public static final int BIT_11_MASK_B = 0x80;
	
	//For J type instructions...
	public static final int BITS_10TO1_MASK_MACHINE = 0x7FE00000;
	public static final int BIT_11_MASK_J = 0x100000;
	public static final int BITS_19TO12_MASK_MACHINE = 0xFF000;
	
	//For U type instructions...
	public static final int BITS_31TO12_MASK = 0xFFFFF000;	
//-------------------------------------------------------------------	

//--------Shift amounts (machine code to abstract)-------------------
	//For all instructions...
	public static final int FUNCT3_SHAMT = 12;
	public static final int RD_SHAMT = 7;
	public static final int RS1_SHAMT = 15;
	public static final int RS2_SHAMT = 20;
	
	//For R type instructions...
	public static final int FUNCT7_SHAMT = 25;	
	
	//For I type instructions...
    public static final int I_IMM_SHAMT = 20; 

    //For B type instructions...
    public static final int B_UPPER_BIT_SHAMT = 25;
    public static final int SECOND_UPPER_BIT_SHAMT = 3;
    public static final int MIDDLE_BITS_SHAMT = 20;
    public static final int B_LOWER_BITS_SHAMT = 8;
    public static final int B_IMM_SHAMT = 1;
    
    //For J type instructions...
    public static final int J_UPPER_BIT_SHAMT = 31;
    public static final int UPPER_MIDDLE_BITS_SHAMT = 12;
    public static final int BIT_11_SHAMT_MACHINE = 20;
    public static final int J_LOWER_BITS_SHAMT = 21;
    
    //For U type instructions...
    public static final int U_IMM_SHAMT = 12;
//-------------------------------------------------------------------
    
//--------Shift amounts (abstract to machine code)-------------------
    public static final int BITS_4TO1_SHAMT = 7;
    public static final int BITS_11TO5_SHAMT = 25;
    public static final int BIT_12_SHAMT = 31;
    public static final int BITS_10TO5_SHAMT = 25;
    public static final int B_BIT_11_SHAMT = 7;
    public static final int BITS_10TO1_SHAMT = 21;
    public static final int J_BIT_11_SHAMT = 20;

//--------Miscellaneous----------------------------------------------s
    public static final int SRAI_BIT = 0x40000000; 
    public static final int RA_SHAMT = 7;
	public static final int WORD_SIZE = 4;
	public static final int ENTER_ASCII = 13;
	public static final int SPACE_ASCII = 32;
}
