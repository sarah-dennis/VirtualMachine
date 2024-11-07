package parse;

import intermediateRepresentation.intermediateRepresentation;

public interface ParseInterface {
	
	ParseOutput parseSource(String source) throws ParseException;
	
	intermediateRepresentation parseTextLine(String code, int sourceLine) throws ParseException;
	
}
