package parse;

import java.util.regex.Pattern;

public class ParseRegex {

	
//YAAL Syntax
	
	//High to Low YAAL translation regexes
	public static final Pattern FUNCTION =
			Pattern.compile("^function\\s*([a-zA-Z][a-zA-Z_]+)\\s*\\(\\s*(.*)\\s*\\)\\s*");
	public static final Pattern LABEL =
			Pattern.compile("\\s*(\\w+)\\s*:(.*)");
	public static final Pattern REG_VARIABLE =
			Pattern.compile("(\\${1}\\w+)");
	public static final Pattern SPEC_VARIABLE =
			Pattern.compile("(\\${2}\\w+)");
	public static final char COMMENT_SYMBOL = '#';
	public static final Pattern COMMENT =
			Pattern.compile(Character.toString(COMMENT_SYMBOL)); 
	
	//YAAL function format regexes
		//Parsing regexes for types of arguments
	public static final Pattern SYMBOL =
			Pattern.compile("([a-zA-Z_]+)");	
	public static final Pattern VARIABLE = 
			Pattern.compile("\\$?[a-zA-Z_]+|\\@[0-9]+");
	public static final Pattern CHAR =
			Pattern.compile("\\'(.{1}|\\n)\\'");
	public static final Pattern STRING =
			Pattern.compile("\"(.+)\"");
	
		//Parsing regexes for different instruction formats
	public static final Pattern GOTO =
			Pattern.compile("goto\\s+" + SYMBOL);
	public static final Pattern IF = 
			Pattern.compile("if\\s+(.+)\\s+(==|!=|>=|<=|>|<)\\s+(.+)\\s+" + GOTO);
	public static final Pattern STORE =
			Pattern.compile("store\\s+(.+)\\s+at\\s+(.*)");
	public static final Pattern LOAD =
			Pattern.compile("\\s*load\\s+from\\s+(.+)");
	public static final Pattern OPERATION =
			Pattern.compile("\\s*(@?-?\\w+)\\s*(\\+|-|\\*|\\/|%)\\s*(@?-?\\w+)");
	public static final Pattern ASSIGN=
			Pattern.compile("(.+)\\s*=\\s*(.+)");
	public static final Pattern CALL = 
			Pattern.compile("\\s*call\\s+([a-zA-Z_]+)\\s*\\(\\s*(.*)\\s*\\)");
	public static final Pattern RETURN =
			Pattern.compile("return\\s*(.*)");
	public static final Pattern SCHEDULE =
			Pattern.compile("_schedule\\s+(.+)\\s+at\\s+(.+)");
	public static final Pattern HANDLE =
			Pattern.compile("_handle\\s+(key|click)\\s+with\\s+(.+)");	
	public static final Pattern DOLLAR_SIGNS =
			Pattern.compile("\\${3,}");
	
	
	
//RISC Syntax
	
	public static final Pattern REG_REG_REG =
			Pattern.compile("(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(\\w+)");  // t0, t1, t2 OR t0, t1, loop
	public static final Pattern REG_REG_LIT =
			Pattern.compile("(\\w+)\\s*,\\s*(\\w+),\\s*(-?\\w+)");  // t0, t1, 37
	public static final Pattern REG_LIT_REG = 
			Pattern.compile("(\\w+)\\s*,\\s*(-?\\w+)\\s*\\(\\s*(\\w+)\\s*\\)");  // t0, -48(t1)
	public static final Pattern REG_SYMBOL =
			Pattern.compile("(\\w+)\\s*,\\s*(\\w+)");  // t0, loop_exit 
	public static final Pattern DIR = 
			Pattern.compile("\\s*\\.{1}(\\w+)\\s+(.*)");
	public static final Pattern RISC_SYMBOL =
			Pattern.compile("([a-zA-Z0-9_.]+)");
	public static final Pattern HEX =
			Pattern.compile("0x[0-9a-fA-F]+");
}

