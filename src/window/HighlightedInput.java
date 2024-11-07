package window;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Class manages the syntax highlighting of the CodeAreas.
 *
 */
public class HighlightedInput {
	
	/** List of Risc operations for highlighting **/
	private static final String[] RISC_OPS = {
			"add", "sub", "sll", "srl", "sra", "xor", "and", "or", "slt", "sltu", 
			"addi", "slli", "srli", "srai", "slti", "ori", "xori", "andi", "sltiu",
			"nop", "li", "mv", "seqz", "snez", "not", "neg", "sltz", "sgtz",
			"ecall", "lw", "lb", "lh", "sw", "sb", "sh", "la", "mul", "div", "rem",
			"jal", "jalr", "jr", "j", "beq", "bne", "blt", "bge", "bgeu", "bltu",
			"beqz", "bnez", "blez", "bgez", "bltz", "bgtz", "bgt", "ble", "ret"
	};
	
	/** List of YAAL function words for highlighting **/
	private static final String[] FUNCT_WORDS = {
			"function", "call", "return", "_stop", "_print", "_read", "_clear_graphics",
			"_print_string", "_read_string", "_random", "_allocate", "_print_char", "_read_char",
			"schedule", "handle", "with",
	};
	
	/** List of memory words to highlight **/
	private static final String[] MEM_WORDS = {
			"store", "at", "load", "from", "\\;", "key", "click"
	};
	
	/** List of control related words to highlight **/
	private static final String[] CONTROL_WORDS = {
			"if", "goto"
	};
	
	/** 
	 * List of RISC-V register names to highlight
	 * (since yaal variables are defined with preceding $,
	 * no list is necessary for those) 
	 */
	private static final String[] RISC_REGS = {
			"zero", "ra", "sp", "gp", "tp","t0", "t1", "t2", 
			"s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7",
			"s2", "s3", "s4", "s5", "s6", "s7", "s8","s9","s10","s11",
			"t3", "t4", "t5", "t6"
	};
	
	/** Strings to make into patterns for the RegEx **/
	private static final String FUNCT_PATTERN = "\\b(" + String.join("|", FUNCT_WORDS) + ")\\b";
	private static final String MEM_PATTERN = "\\b(" + String.join("|", MEM_WORDS) + ")\\b";
	private static final String CONTROL_PATTERN = "\\b(" + String.join("|", CONTROL_WORDS) + ")\\b";
	private static final String COMMENT_PATTERN = "#[^\n]*";
	private static final String VAR_PATTERN = "\\$+(\\w+)";
	private static final String REG_PATTERN = "\\b(" + String.join("|", RISC_REGS) + ")\\b";
	private static final String ROP_PATTERN = "\\b(" + String.join("|", RISC_OPS) + ")\\b";
	private static final String LABEL_PATTERN = "(\\w+)\\s*:";
	private static final String DIRECTIVE_PATTERN = "(\\.\\w+)";

	/** the actual pattern made of all the above strings **/
	private static final Pattern PATTERN = Pattern.compile(
					"(?<FUNCTION>" + FUNCT_PATTERN + ")"
					+ "|(?<LABEL>" + LABEL_PATTERN + ")"
					+ "|(?<DIRECTIVE>" + DIRECTIVE_PATTERN + ")"
					+ "|(?<MEMORY>" + MEM_PATTERN + ")"
					+ "|(?<CONTROL>" + CONTROL_PATTERN + ")"
					+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
					+ "|(?<VARIABLE>" + VAR_PATTERN + ")"
					+ "|(?<REGISTER>" + REG_PATTERN + ")"
					+ "|(?<OPERATIONR>" + ROP_PATTERN + ")"
			);
	
	/**
	 * Given a String of words to highlight, matches patterns and computes highlight colors as needed.
	 * @param text - the input that will be highlighted
	 * @return the style to apply to the CodeArea that will provide highlighting
	 */
	public static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		
		while (matcher.find()) {
			String styleClass = 
					matcher.group("FUNCTION") != null ? "function-y" :
					matcher.group("MEMORY") != null ? "memory" :
					matcher.group("CONTROL") != null ? "control" :
					matcher.group("COMMENT") != null ? "comment" :
					matcher.group("VARIABLE") != null ? "variable" :
					matcher.group("REGISTER") != null ? "variable" :
					matcher.group("OPERATIONR") != null ? "operation-r" :
					matcher.group("LABEL") != null ? "label" :
					matcher.group("DIRECTIVE") != null ? "directive" :
						null; assert styleClass != null;
						
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastKwEnd = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}
	
	/**
	 * Gives the name of the .css where the StyleClass is held.
	 * @return the String name of the style document.
	 */
	public static String getStyleClassName() {
		return HighlightedInput.class.getResource("/resources/syntax-keywords.css").toExternalForm();
	}
}
