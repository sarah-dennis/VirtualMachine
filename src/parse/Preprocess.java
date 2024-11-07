package parse;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dataTypes.MemAllocDir;
import dataTypes.StringDir;
import dataTypes.WordDir;
import util.Pair;


public class Preprocess {

	private static Pattern labelMarkerPattern = Pattern.compile(":");
	private static Pattern labelPattern = Pattern.compile("\\s*([a-zA-Z_]+)");

	public static PreParseOutput preprocess(String source, String parseType) 
			throws ParseException {
		SymbolTable symbolTable = new SymbolTable();
		List<Pair<String, Integer>> codeLines = new ArrayList<Pair<String, Integer>>();
		int dataLines = 0;
		boolean dataLabel = false;
		Data data = new Data();
		String currentLabel = null;
		String[] lines = source.split("\n");
		for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
			String currentLine = lines[lineNumber];
			if (currentLine.isEmpty() || currentLine.matches("\\s*")) {
				continue;
			}

			if (parseType.equals("risc")) {
				if (currentLine.matches("\\s*.data\\s*")) {
					dataLabel = true;
					continue;
				} else if (currentLine.matches("\\s*.text\\s*")) {
					dataLabel = false;
					continue;
				}
			}

			String[] commentLine = ParseRegex.COMMENT.split(currentLine, -1);
			if (commentLine.length > 1) {
				currentLine = commentLine[0];
			}

			String[] str = currentLine.split("\"");
			String[] labelSplit;
			if (str.length == 1) { //no strings present, continue
				labelSplit = labelMarkerPattern.split(currentLine, -1);
			} else {
				labelSplit = labelMarkerPattern.split(str[0]); //Only look for labels that are NOT in strings
			}

			if (labelSplit.length == 1) {
				if (!currentLine.isEmpty() && !currentLine.matches("\\s*")) {
					//no label present, do RISC data parsing if applicable
					if (parseType.equals("risc") && dataLabel) {
						parseDataLine(currentLine, lineNumber, data);
						dataLines++;
					} else {
						currentLine = currentLine.trim();
						codeLines.add(new Pair<String, Integer>(currentLine, lineNumber));
					}
				}

			} else if (labelSplit.length == 2) {
				currentLabel = labelSplit[0];
				if (labelPattern.matcher(currentLabel).matches()) {
					if (currentLabel.charAt(0) == ' ' || currentLabel.charAt(0) == '\t') {
						throw new ParseException("label must be aligned left", lineNumber);
					} else if (symbolTable.getLabels().contains(currentLabel)) {
						throw new ParseException("Duplicate label at ", lineNumber);
					} else {
						Pair<Integer, Integer> labelLines =
								new Pair<Integer, Integer>(lineNumber, codeLines.size());
						currentLabel = currentLabel.trim();

						//RISC Data processing
						if (parseType.equals("risc") && dataLabel) {
							data.assignData(currentLabel, dataLines);
							currentLabel = null;
						} else {
							symbolTable.add(currentLabel, labelLines);
							currentLabel = null;
						}
						currentLine = labelSplit[1];

						currentLine = currentLine.trim();
						if (!currentLine.isEmpty() && !currentLine.matches("\\s*")) {
							if (parseType.equals("risc") && dataLabel) {
								System.out.println("parsing data " + currentLine);
								parseDataLine(currentLine, lineNumber, data);
								dataLines++;
							} else {
								currentLine = currentLine.trim();
								codeLines.add(new Pair<String, Integer>(currentLine, lineNumber));
							}
						}
					}
				} else {
					throw new ParseException("invalid label", lineNumber);
				}
			} else {
				throw new ParseException("more than one : found", lineNumber);
			}
		}

		if (parseType.equals("risc")) {
			return new PreParseOutput(symbolTable, codeLines, data);
		} else {
			return new PreParseOutput(symbolTable, codeLines);
		}
	}

	public static void parseDataLine(String line, int lineNumber, Data d) throws ParseException {
		Matcher directive = ParseRegex.DIR.matcher(line);
		if (directive.matches()) {
			String command = directive.group(1);
			String rest = directive.group(2);
			rest = rest.trim();

			if (command.equals("word")) {
				if (rest.matches("-?\\d+(?:,\\s*-?\\d+)?")) {
					String[] nums = rest.split(","); //catch multiple pieces of data if given
					for (String n : nums) {
						d.addData(new WordDir(Integer.parseInt(n)));
					}
				} else if (rest.matches("0x[0-9a-fA-F]+")) {
					d.addData(new WordDir(Integer.parseInt(rest, 16)));
				} else {
					throw new ParseException("invalid type in .word " + rest, lineNumber);
				}
			} else if (command.equals("string")) {
				Matcher string = ParseRegex.STRING.matcher(rest);
				if (string.matches()) {
					String s = string.group(1);
					d.addData(new StringDir(s));
				} else {
					throw new ParseException("invalid type in .string " + rest, lineNumber);
				}
			} else if (command.equals("skip")) {
				if (rest.matches("-?\\d+")) {
					d.addData(new MemAllocDir(Integer.parseInt(rest)));
				} else {
					throw new ParseException("invalid type in .skip " + rest, lineNumber);
				}
			} else {
				throw new ParseException("directive not yet implemented " + command, lineNumber);
			}
		} else {
			throw new ParseException("invalid format in .data", lineNumber);
		}
	}
}
