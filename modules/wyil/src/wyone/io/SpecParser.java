package wyone.io;

import java.math.BigInteger;
import java.util.*;

import wyone.core.Attribute;
import wyone.util.*;
import static wyone.core.SpecFile.*;
import static wyone.io.SpecLexer.*;
import wyone.core.*;

public class SpecParser {
	private String filename;
	private ArrayList<Token> tokens;
	private int index;

	public SpecParser(String filename, List<Token> tokens) {
		this.filename = filename;
		this.tokens = new ArrayList<Token>(tokens);
	}

	public SpecFile parse() {
		ArrayList<Decl> decls = new ArrayList<Decl>();

		while (index < tokens.size()) {
			Token t = tokens.get(index);
			if (t instanceof NewLine || t instanceof Comment) {
				matchEndLine();
			} else {
				Token lookahead = tokens.get(index);

				if (lookahead.text.equals("term")) {
					decls.add(parseTermDecl());
				} else if (lookahead.text.equals("class")) {
					decls.add(parseClassDecl());
				} else {
					decls.add(parseRewriteDecl());
				}
			}
		}

		return new SpecFile(filename, decls);
	}

	private Decl parseTermDecl() {
		int start = index;
		matchKeyword("term");
		Type.Term data = parseTermType();
		matchEndLine();
		return new TermDecl(data, sourceAttr(start, index - 1));
	}

	private Decl parseClassDecl() {
		int start = index;
		matchKeyword("class");
		String name = matchIdentifier().text;
		matchKeyword("as");
		ArrayList<String> children = new ArrayList<String>();
		boolean firstTime = true;
		do {
			if (!firstTime) {
				match(Bar.class);
			}
			firstTime = false;
			children.add(matchIdentifier().text);
		} while (index < tokens.size() && tokens.get(index) instanceof Bar);
		matchEndLine();
		return new ClassDecl(name, children, sourceAttr(start, index - 1));
	}

	private Decl parseRewriteDecl() {
		int start = index;
		matchKeyword("rewrite");
		List<Code> pattern = parsePatternTerm();
		match(Colon.class);
		matchEndLine();
		List<Code> codes = parseRuleBlock(1);
		return new FunDecl(null, codes, sourceAttr(start, index - 1));
	}

	public List<Code> parsePattern() {
		skipWhiteSpace(true);
		checkNotEof();
		Token token = tokens.get(index);

		if (token instanceof Star) {
			match(Star.class);
			return new Pattern.Leaf(Type.T_ANY);
		} else if (token instanceof LeftCurly || token instanceof LeftSquare) {
			return parsePatternCompound();
		} else if (token instanceof LeftBrace) {
			Pattern r = parsePattern();
			return r;
		} else if (token instanceof Identifier) {
			return parsePatternTerm();
		} else {
			return new Pattern.Leaf(parseType());
		}
	}

	public List<Code> parsePatternTerm() {
		int start = index;
		String name = matchIdentifier().text;
		Token token = tokens.get(index);
		String var = null;
		Pattern p;
		if (token instanceof LeftCurly || token instanceof LeftSquare) {
			p = parsePatternCompound();
		} else if (token instanceof LeftBrace) {
			match(LeftBrace.class);
			p = parsePattern();
			if (index < tokens.size()
					&& tokens.get(index) instanceof Identifier) {
				var = matchIdentifier().text;
			}
			match(RightBrace.class);
		} else {
			p = new Pattern.Leaf(Type.T_VOID);
		}

		return new Pattern.Term(name, p, var, sourceAttr(start, index - 1));
	}

	public List<Code> parsePatternCompound() {
		int start = index;
		Type.Compound.Kind kind;
		ArrayList<Pair<Pattern, String>> params = new ArrayList();
		if (index < tokens.size() && tokens.get(index) instanceof LeftSquare) {
			match(LeftSquare.class);
			kind = Type.Compound.Kind.LIST;
		} else {
			match(LeftCurly.class);
			kind = Type.Compound.Kind.SET;
		}
		boolean firstTime = true;
		boolean unbound = false;
		while (index < tokens.size()
				&& !(tokens.get(index) instanceof RightSquare)
				&& !(tokens.get(index) instanceof RightCurly)) {
			if (unbound) {
				syntaxError("... must be last match", tokens.get(index));
			}
			if (!firstTime) {
				match(Comma.class);
			}
			firstTime = false;
			Pattern p = parsePattern();
			if (index < tokens.size() && tokens.get(index) instanceof DotDotDot) {
				match(DotDotDot.class);
				unbound = true;
			}
			String n = null;
			if (index < tokens.size()
					&& tokens.get(index) instanceof Identifier) {
				n = matchIdentifier().text;
			}
			params.add(new Pair<Pattern, String>(p, n));
		}
		switch (kind) {
		case LIST:
			match(RightSquare.class);
			break;
		case SET:
			match(RightCurly.class);
		}

		return new Pattern.Compound(kind, unbound, params, sourceAttr(start,
				index - 1));
	}

	public List<Code> parseRuleBlock(int indent) {
		Tabs tabs = getIndent();

		ArrayList<Code> codes = new ArrayList<Code>();
		while (tabs != null && tabs.ntabs == indent) {
			index = index + 1;
			codes.addAll(parseRule());
			tabs = getIndent();
		}

		return codes;
	}

	private Tabs getIndent() {
		// FIXME: there's still a bug here for empty lines with arbitrary tabs
		if (index < tokens.size() && tokens.get(index) instanceof Tabs) {
			return (Tabs) tokens.get(index);
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof Comment) {
			// This indicates a completely empty line. In which case, we just
			// ignore it.
			matchEndLine();
			return getIndent();
		} else {
			return null;
		}
	}

	public List<Code> parseRule() {
		match(Arrow.class);
		Environment environment = new Environment();
		ArrayList<Code> codes = new ArrayList<Code>();
		if (index < tokens.size() && tokens.get(index).text.equals("let")) {
			matchKeyword("let");
			boolean firstTime = true;
			do {
				if (!firstTime) {
					match(Comma.class);
					skipWhiteSpace(true);
				}
				firstTime = false;
				String id = matchIdentifier().text;
				match(Equals.class);
				int target = environment.allocate(Type.T_ANY, id);
				parseAddSubExpression(target, environment, codes);
				skipWhiteSpace(true);
			} while (index < tokens.size()
					&& tokens.get(index) instanceof Comma);
			match(ElemOf.class);
		}

		parseAddSubExpression(freeRegister, environment,
				codes);
		skipWhiteSpace(true);
		if (index < tokens.size() && tokens.get(index) instanceof Comma) {
			match(Comma.class);
			matchKeyword("if");
			int target = environment.allocate(Type.T_BOOL);
			parseCondition(target, environment, codes);
			// FIXME: need to do something with target!!
			matchEndLine();
		}

		return codes;
	}

	private void parseCondition(int target, Environment environment,
			ArrayList<Code> codes) {
		checkNotEof();
		int start = index;
		parseConditionExpression(target, environment, codes);

		if (index < tokens.size() && tokens.get(index) instanceof LogicalAnd) {
			match(LogicalAnd.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseCondition(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.AND, target, target, operand,
					sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof LogicalOr) {
			match(LogicalOr.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseCondition(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.OR, target, target, operand,
					sourceAttr(start, index - 1)));
		}
	}

	private void parseConditionExpression(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;

		parseAddSubExpression(target, environment, codes);

		if (index < tokens.size() && tokens.get(index) instanceof LessEquals) {
			match(LessEquals.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseConditionExpression(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.LTEQ, target, target, operand,
					sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof LeftAngle) {
			match(LeftAngle.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseConditionExpression(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.LT, target, target, operand,
					sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof GreaterEquals) {
			match(GreaterEquals.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseConditionExpression(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.GTEQ, target, target, operand,
					sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof RightAngle) {
			match(RightAngle.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseConditionExpression(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.GT, target, target, operand,
					sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof EqualsEquals) {
			match(EqualsEquals.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseConditionExpression(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.EQ, target, target, operand,
					sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof NotEquals) {
			match(NotEquals.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_BOOL);
			parseConditionExpression(operand, environment, codes);
			codes.add(new Code.BinOp(Code.BOp.NEQ, target, target, operand,
					sourceAttr(start, index - 1)));
		}
	}

	private void parseAddSubExpression(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		parseMulDivExpression(target, environment, codes);

		if (index < tokens.size() && tokens.get(index) instanceof Plus) {
			match(Plus.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_ANY);
			parseAddSubExpression(operand, environment,codes);
			codes.add(new Code.BinOp(Code.BOp.ADD, target, target,
					operand, sourceAttr(start, index - 1)));
		} else if (index < tokens.size() && tokens.get(index) instanceof Minus) {
			match(Minus.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_ANY);
			parseAddSubExpression(operand, environment,codes);
			codes.add(new Code.BinOp(Code.BOp.SUB, target, target,
					operand, sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof PlusPlus) {
			// wrong precidence
			match(PlusPlus.class);
			skipWhiteSpace(true);
			int operand = environment.allocate(Type.T_ANY);
			parseAddSubExpression(operand, environment,codes);
			codes.add(new Code.BinOp(Code.BOp.APPEND, target, target,
					operand, sourceAttr(start, index - 1)));
		}
	}

	private void parseMulDivExpression(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		parseIndexTerm(target, freeRegister, environment, codes);

		if (index < tokens.size() && tokens.get(index) instanceof Star) {
			match(Star.class);
			skipWhiteSpace(true);
			parseMulDivExpression(freeRegister, freeRegister + 1, environment,
					codes);
			codes.add(new Code.BinOp(Code.BOp.MUL, target, target,
					freeRegister, sourceAttr(start, index - 1)));
		} else if (index < tokens.size()
				&& tokens.get(index) instanceof RightSlash) {
			match(RightSlash.class);
			skipWhiteSpace(true);
			parseMulDivExpression(freeRegister, freeRegister + 1, environment,
					codes);
			codes.add(new Code.BinOp(Code.BOp.DIV, target, target,
					freeRegister, sourceAttr(start, index - 1)));
		}
	}

	private void parseIndexTerm(int target, Environment environment,
			ArrayList<Code> codes) {
		checkNotEof();
		int start = index;
		parseTerm(target, freeRegister, environment, codes);

		Token lookahead = tokens.get(index);

		while (lookahead instanceof LeftSquare
				|| lookahead instanceof LeftBrace || lookahead instanceof Hash) {
			start = index;
			if (lookahead instanceof LeftSquare) {
				match(LeftSquare.class);
				skipWhiteSpace(true);

				lookahead = tokens.get(index);

				parseAddSubExpression(freeRegister, freeRegister + 1,
						environment, codes);

				match(RightSquare.class);
				codes.add(new Code.ListAccess(target, target, freeRegister,
						sourceAttr(start, index - 1)));
			}
			if (index < tokens.size()) {
				lookahead = tokens.get(index);
			} else {
				lookahead = null;
			}
		}
	}

	private void parseTerm(int target, Environment environment,
			ArrayList<Code> codes) {
		checkNotEof();

		int start = index;
		Token token = tokens.get(index);

		if (token instanceof LeftBrace) {
			match(LeftBrace.class);
			skipWhiteSpace(true);
			checkNotEof();
			parseCondition(target, freeRegister, environment, codes);
			skipWhiteSpace(true);
			checkNotEof();
			token = tokens.get(index);
			match(RightBrace.class);
		} else if ((index + 1) < tokens.size()
				&& token instanceof Identifier
				&& (tokens.get(index + 1) instanceof LeftBrace
						|| tokens.get(index + 1) instanceof LeftSquare || tokens
							.get(index + 1) instanceof LeftCurly)) {
			parseConstructor(target, freeRegister, environment, codes);
		} else if (token.text.equals("true")) {
			matchKeyword("true");
			codes.add(new Code.Constant(target, true, sourceAttr(start,
					index - 1)));
		} else if (token.text.equals("false")) {
			matchKeyword("false");
			codes.add(new Code.Constant(target, false, sourceAttr(start,
					index - 1)));
		} else if (token instanceof Identifier) {
			String var = matchIdentifier().text;
			Integer source = environment.get(var);
			if (source == null) {
				syntaxError("unknown variable", token);
			}
			codes.add(new Code.Assign(target, source, sourceAttr(start,
					index - 1)));
		} else if (token instanceof Int) {
			parseInteger(target, freeRegister, environment, codes);
		} else if (token instanceof Real) {
			parseReal(target, freeRegister, environment, codes);
		} else if (token instanceof Strung) {
			parseString(target, freeRegister, environment, codes);
		} else if (token instanceof Minus) {
			parseNegation(target, freeRegister, environment, codes);
		} else if (token instanceof Bar) {
			parseLengthOf(target, freeRegister, environment, codes);
		} else if (token instanceof LeftSquare || token instanceof LeftCurly) {
			parseCompoundVal(target, freeRegister, environment, codes);
		} else if (token instanceof Shreak) {
			parseNot(target, freeRegister, environment, codes);
		}
		syntaxError("unrecognised term.", token);
	}

	private void parseCompoundVal(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		match(LeftSquare.class);
		skipWhiteSpace(true);
		boolean firstTime = true;
		checkNotEof();
		Token token = tokens.get(index);
		ArrayList<Integer> operands = new ArrayList<Integer>();
		while (!(token instanceof RightSquare)) {
			if (!firstTime) {
				match(Comma.class);
				skipWhiteSpace(true);
			}
			firstTime = false;
			parseCondition(freeRegister, freeRegister + 1, environment, codes);
			operands.add(freeRegister);
			skipWhiteSpace(true);
			checkNotEof();
			token = tokens.get(index);
			freeRegister = freeRegister + 1;
		}
		match(RightSquare.class);
		codes.add(new Code.NaryOp(Code.NOp.LISTGEN, target, operands,
				sourceAttr(start, index - 1)));
	}

	private void parseLengthOf(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		match(Bar.class);
		skipWhiteSpace(true);
		parseIndexTerm(target, freeRegister, environment, codes);
		skipWhiteSpace(true);
		match(Bar.class);
		codes.add(new Code.UnOp(Code.UOp.LENGTHOF, target, target, sourceAttr(
				start, index - 1)));
	}

	private void parseNegation(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		match(Minus.class);
		skipWhiteSpace(true);
		parseIndexTerm(target, freeRegister, environment, codes);
		codes.add(new Code.UnOp(Code.UOp.NEG, target, target, sourceAttr(start,
				index - 1)));
	}

	private void parseNot(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		match(Shreak.class);
		skipWhiteSpace(true);
		parseIndexTerm(target, freeRegister, environment, codes);
		codes.add(new Code.UnOp(Code.UOp.NOT, target, target, sourceAttr(start,
				index - 1)));
	}

	private void parseConstructor(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		Identifier name = matchIdentifier();
		skipWhiteSpace(true);
		checkNotEof();
		Token token = tokens.get(index);
		if (token instanceof LeftBrace) {
			match(LeftBrace.class);
			parseConditionExpression(target, freeRegister, environment, codes);
			match(RightBrace.class);
		} else {
			parseCompoundVal(target, freeRegister, environment, codes);
		}

		codes.add(new Code.Constructor(target, target, name.text, sourceAttr(
				start, index - 1)));
	}

	private void parseInteger(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		BigInteger i = match(Int.class).value;
		codes.add(new Code.Constant(target, i, sourceAttr(start, index - 1)));
	}

	private void parseReal(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		BigRational i = match(Real.class).value;
		codes.add(new Code.Constant(target, i, sourceAttr(start, index - 1)));
	}

	private void parseString(int target, Environment environment,
			ArrayList<Code> codes) {
		int start = index;
		String s = match(Strung.class).string;
		codes.add(new Code.Constant(target, s, sourceAttr(start, index - 1)));
	}

	private Type parseType() {
		skipWhiteSpace(true);
		checkNotEof();
		int start = index;
		Token token = tokens.get(index);
		Type t;

		if (token instanceof Star) {
			match(Star.class);
			t = Type.T_ANY;
		} else if (token.text.equals("int")) {
			matchKeyword("int");
			t = Type.T_INT;
		} else if (token.text.equals("real")) {
			matchKeyword("real");
			t = Type.T_REAL;
		} else if (token.text.equals("void")) {
			matchKeyword("void");
			t = Type.T_VOID;
		} else if (token.text.equals("bool")) {
			matchKeyword("bool");
			t = Type.T_BOOL;
		} else if (token.text.equals("string")) {
			matchKeyword("string");
			t = Type.T_STRING;
		} else if (token instanceof LeftBrace) {
			match(LeftBrace.class);
			t = parseType();
			match(RightBrace.class);
		} else if (token instanceof LeftCurly || token instanceof LeftSquare) {
			return parseCompoundType();
		} else {
			return parseTermType();
		}

		return t;
	}

	private Type.Term parseTermType() {
		skipWhiteSpace(false);
		checkNotEof();
		Identifier id = matchIdentifier();
		Type data = Type.T_VOID;
		if (index < tokens.size()) {
			Token token = tokens.get(index);
			if (token instanceof LeftBrace || token instanceof LeftCurly
					|| token instanceof LeftSquare) {
				data = parseType();
			}
		}
		return Type.T_TERM(id.text, Type.T_REF(data));
	}

	private Type.Compound parseCompoundType() {
		Type.Compound.Kind kind;
		if (index < tokens.size() && tokens.get(index) instanceof LeftSquare) {
			match(LeftSquare.class);
			kind = Type.Compound.Kind.LIST;
		} else {
			match(LeftCurly.class);
			kind = Type.Compound.Kind.SET;
		}
		ArrayList<Type> types = new ArrayList<Type>();
		boolean firstTime = true;
		while (index < tokens.size()
				&& !(tokens.get(index) instanceof RightSquare
						|| tokens.get(index) instanceof RightCurly || tokens
							.get(index) instanceof DotDotDot)) {
			if (!firstTime) {
				match(Comma.class);
			}
			firstTime = false;
			types.add(parseType());
		}
		boolean unbounded = false;
		if (index < tokens.size() && tokens.get(index) instanceof DotDotDot) {
			match(DotDotDot.class);
			unbounded = true;
		}
		switch (kind) {
		case LIST:
			match(RightSquare.class);
			break;
		case SET:
			match(RightCurly.class);
		}

		return Type.T_COMPOUND(kind, unbounded,
				types.toArray(new Type[types.size()]));
	}

	private boolean isTypeStart() {
		checkNotEof();
		Token token = tokens.get(index);
		if (token instanceof Keyword) {
			return token.text.equals("int") || token.text.equals("void")
					|| token.text.equals("bool") || token.text.equals("real")
					|| token.text.equals("?") || token.text.equals("*")
					|| token.text.equals("process");
		} else if (token instanceof LeftBrace) {
			// Left brace is a difficult situation, since it can represent the
			// start of a tuple expression or the start of a typle lval.
			int tmp = index;
			match(LeftBrace.class);
			boolean r = isTypeStart();
			index = tmp;
			return r;
		} else {
			return token instanceof LeftCurly || token instanceof LeftSquare;
		}
	}

	private void skipWhiteSpace(boolean includeNewLine) {
		while (index < tokens.size()
				&& isWhiteSpace(includeNewLine, tokens.get(index))) {
			index++;
		}
	}

	private boolean isWhiteSpace(boolean includeNewLine, Token t) {
		return (includeNewLine && t instanceof SpecLexer.NewLine)
				|| t instanceof SpecLexer.Comment
				|| t instanceof SpecLexer.Tabs;
	}

	private void checkNotEof() {
		if (index >= tokens.size()) {
			throw new SyntaxError("unexpected end-of-file", filename,
					index - 1, index - 1);
		}
		return;
	}

	private <T extends Token> T match(Class<T> c) {
		checkNotEof();
		Token t = tokens.get(index);
		if (!c.isInstance(t)) {
			syntaxError("syntax error", t);
		}
		index = index + 1;
		return (T) t;
	}

	private Token matchAll(Class<? extends Token>... cs) {
		checkNotEof();
		Token t = tokens.get(index);
		for (Class<? extends Token> c : cs) {
			if (c.isInstance(t)) {
				index = index + 1;
				return t;
			}
		}
		syntaxError("syntax error", t);
		return null;
	}

	private Identifier matchIdentifier() {
		checkNotEof();
		Token t = tokens.get(index);
		if (t instanceof Identifier) {
			Identifier i = (Identifier) t;
			index = index + 1;
			return i;
		}
		syntaxError("identifier expected", t);
		return null; // unreachable.
	}

	private Keyword matchKeyword(String keyword) {
		checkNotEof();
		Token t = tokens.get(index);
		if (t instanceof Keyword) {
			if (t.text.equals(keyword)) {
				index = index + 1;
				return (Keyword) t;
			}
		}
		syntaxError("keyword " + keyword + " expected.", t);
		return null;
	}

	private void matchEndLine() {
		while (index < tokens.size()) {
			Token t = tokens.get(index++);
			if (t instanceof NewLine) {
				break;
			} else if (!(t instanceof Comment) && !(t instanceof Tabs)) {
				syntaxError("syntax error", t);
			}
		}
	}

	private Attribute.Source sourceAttr(int start, int end) {
		Token t1 = tokens.get(start);
		Token t2 = tokens.get(end);
		return new Attribute.Source(t1.start, t2.end());
	}

	private void syntaxError(String msg, Token t) {
		throw new SyntaxError(msg, filename, t.start, t.start + t.text.length()
				- 1);
	}

	private class Environment {
		private final HashMap<String, Integer> var2idx = new HashMap<String, Integer>();
		private final ArrayList<Type> idx2type = new ArrayList<Type>();

		public int allocate(Type t) {
			int idx = idx2type.size();
			idx2type.add(t);
			return idx;
		}

		public int allocate(Type t, String v) {
			int r = allocate(t);
			var2idx.put(v, r);
			return r;
		}

		public Integer get(String v) {
			return var2idx.get(v);
		}

		public ArrayList<Type> asList() {
			return idx2type;
		}
	}
}