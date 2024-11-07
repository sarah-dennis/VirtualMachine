package parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import util.Pair;
import virtual_machine.OperatingSystem;

public class HiToLowTranslate {

	private String CURRENT_FUNCTION;
	private Map<String, String[]> functToParams; 
	private Map<String, List<Pair<String, String>>> paramsToSpecRegs = new HashMap<String, List<Pair<String, String>>>();
	private boolean hasReturn = true; //must be true for initial function

	public HiToLowTranslate() {
		functToParams = new HashMap<String, String[]>();
		ArrayList<String> sysOps = OperatingSystem.getSystemOps();
		for (String op : sysOps) {
			if (op.equals("_print") || op.equals("_print_string") ||
					op.equals("_random") || op.equals("_allocate") || op.equals("_print_char")) {
				String[] items = new String[1];
				items[0] = op + "_0";
				functToParams.put(op, items);
			} else if (op.equals("_stop") || op.equals("_read") || 
					op.equals("_read_string") || op.equals("_read_char") ||
					op.equals("_clear_graphics") || op.equals("_clear_memory")) {
				String[] items = new String[0];
				functToParams.put(op, items);
			} else if (op.equals("_schedule")) {
				String[] items = new String[2];
				items[0] = op + "_funct";
				items[1] = op + "_time";
				functToParams.put(op, items);
			}
		}
	}

	public Map<String, String[]> getFunctMap() {
		return functToParams;
	}

	public Map<String, List<Pair<String, String>>> getParamsToSpecRegsMap() {
		return paramsToSpecRegs;
	}

	//First pass finds and renames functions, creates function map and parameter to special register map
	public String pass1(String source) throws ParseException {
		if (source.isEmpty()) {
			throw new ParseException("no code!", 0);
		}
		String result = "";
		String[] lines = source.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String newLine = pass1Line(lines[i], i);
			result += newLine + "\n";
		}
		return result;
	}

	public String pass1Line(String line, int sourceLine) throws ParseException {

		//First, we remove comments
		String[] commentSplit = ParseRegex.COMMENT.split(line);
		if (commentSplit.length > 1) {
			line = commentSplit[0];
		}

		//Next, we identify functions
		Matcher functMatcher = ParseRegex.FUNCTION.matcher(line);
		if (functMatcher.matches()) {
			if (!hasReturn) {
				if (CURRENT_FUNCTION != null) {
					throw new ParseException(CURRENT_FUNCTION + " missing return statement", sourceLine-1);
				} else {
					throw new ParseException("main missing return statement", sourceLine-1);
				}
			} else {
				if (line.contains("$$")) {
					throw new ParseException("invalid global variable declaration as argument", sourceLine);
				}
			}
			hasReturn = false;
			String functName = functMatcher.group(1);
			line = line.replace(functName, functName.trim());

			String rest = functMatcher.group(2);
			String[] args;

			//If the function takes no arguments, make sure the list length is 0
			if (rest.isEmpty()) {
				args = new String[0];

			} else {
				args = rest.split(",");
				if (hasRepeats(args)) {
					throw new ParseException("Duplicate parameter name", sourceLine);
				} else {
					Integer counter = 0;
					for (String arg : args) {
						if (arg.length() == 0 || arg.equals(null)) {
							continue;
						}
						arg = arg.trim();
						args[counter] = arg;

						Pair<String, String> p = new Pair<String, String>(arg, "@" + counter.toString());
						if (paramsToSpecRegs.containsKey(functName)) {
							List<Pair<String, String>> val = paramsToSpecRegs.get(functName);
							val.add(p);
							paramsToSpecRegs.put(functName, val);
						} else {
							List<Pair<String, String>> val = new ArrayList<Pair<String, String>>();
							val.add(p);
							paramsToSpecRegs.put(functName, val);
						}
						counter++;
					}
				}
			}

			if (functToParams.containsKey(functName)) {
				throw new ParseException("Duplicate function name " + functName, sourceLine);
			} else {
				functToParams.put(functName, args);
			}

			//Also must identify returns to make sure each function is closed
		} else if (line.matches("\\s*return.*")) {
			hasReturn = true;
			CURRENT_FUNCTION = null;
		}
		return line;
	}	

	//Second pass changes syntax from HI YAAL to YAAL
	public String hiToLowSyntax(String source) throws ParseException {
		String result = "";
		source = pass1(source);
		String[] lines = source.split("\n");
		int counter = 0;
		for (String line : lines) {
			line = hiToLowSyntaxLine(line, counter);
			result += line + "\n";
			counter++;
		}
		return result;
	}

	public String hiToLowSyntaxLine(String line, int sourceLine) throws ParseException {

		//First step: replace all parameter names for functions 
		//with their special register names (@0, @1, etc)
		if (CURRENT_FUNCTION != null) {	
			//If the length is 0, then no arguments are given for the function
			if (functToParams.get(CURRENT_FUNCTION).length > 0) {
				for (String param : functToParams.get(CURRENT_FUNCTION)) {
					List<Pair<String, String>> args = paramsToSpecRegs.get(CURRENT_FUNCTION);
					//Now, find parameters of current function within paramsToSpecRegs
					for (Pair<String, String> p : args) {
						if (p.first().equals(param)) {
							String regex = "\\" + param + "\\b"; //find literal $ and not within another word
							line = line.replaceAll(regex, p.second());
						}
					}
				}
			}
		}


		//Then, make sure the right number of dollar signs are present
		Matcher dollarSigns = ParseRegex.DOLLAR_SIGNS.matcher(line);
		if (dollarSigns.find()) {
			throw new ParseException("too many dollar signs", sourceLine);
		}

		Matcher functMatcher = ParseRegex.FUNCTION.matcher(line);
		Matcher callMatcher = ParseRegex.CALL.matcher(line);
		Matcher labelMatcher = ParseRegex.LABEL.matcher(line);
		Matcher gotoMatcher = ParseRegex.GOTO.matcher(line);

		//Next, rename functions
		if (functMatcher.matches()) {
			String functName = functMatcher.group(1);
			CURRENT_FUNCTION = functName;
			line = functName + ":";
			//catch code declared outside of a function
		} else {
			if (!line.isEmpty() && CURRENT_FUNCTION == null) {
				throw new ParseException("invalid code outside of function at line " + sourceLine, sourceLine);
			}
		}

		//Next, parse calls
		if (callMatcher.matches()) {
			String funct = callMatcher.group(1);
			String rest = callMatcher.group(2);

			line.replace(funct, "_" + funct.toLowerCase());

			String[] args = rest.split(",");
			int lenOfArgs;
			if (args[0].isEmpty()) {
				lenOfArgs = 0;
			} else {
				lenOfArgs = args.length;
			}
			if (lenOfArgs != numOfParams(funct)) {
				throw new ParseException("Invalid number of arguments " + funct, sourceLine);
			}

			//Then, parse labels
		} else if (labelMatcher.matches() || gotoMatcher.find()) {
			String label;
			if (labelMatcher.matches()) {
				label = labelMatcher.group(1);
			} else {
				label = gotoMatcher.group(1);
			}
			line = line.replace(label, CURRENT_FUNCTION + "_" + label.toLowerCase());
		}

		//Then, Change special functions 
		if (line.contains("schedule")) {
			line = line.replace("schedule", "_schedule");
		}
		if (line.contains("handle")) {
			line = line.replaceAll("handle", "_handle");
		}

		//Finally, rename variables
		if (line.contains("$")) {
			line = parseVariables(line);
		}
		return line;
	}

	public String parseVariables(String line) {
		Matcher rvMatcher = ParseRegex.REG_VARIABLE.matcher(line);
		Matcher svMatcher = ParseRegex.SPEC_VARIABLE.matcher(line);
		while (svMatcher.find()) {
			String var = svMatcher.group(1);
			String newVar = var.replace("$$", "_");
			newVar = newVar.toLowerCase();
			line = line.replace(var, newVar);
		}
		while (rvMatcher.find()) { 
			String var = rvMatcher.group(1);
			String newVar = var.replace("$", CURRENT_FUNCTION + "_");
			newVar = newVar.toLowerCase();
			line = line.replace(var, newVar);	
		} 
		return line;
	}


	public int numOfFuncts() {
		return functToParams.size();
	}

	public int numOfParams(String function) {
		if (functToParams.containsKey(function)) {
			return functToParams.get(function).length;
		} else {
			return 0;
		}
	}

	public boolean hasRepeats(String[] elements) {
		for (int i = 0; i < elements.length-1; i++) {
			for (int j = i + 1; j < elements.length; j++) {
				if (elements[i].equals(elements[j])) {
					return true;
				}
			}
		}
		return false;
	}
}
