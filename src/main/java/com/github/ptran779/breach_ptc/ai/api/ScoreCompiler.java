package com.github.ptran779.breach_ptc.ai.api;

import java.util.ArrayList;
import java.util.Stack;

public class ScoreCompiler {
	// Instruction Opcodes (Expanded for Triple-State Architecture)
	private static final byte LOAD_VAR_A = 1, LOAD_VAR_B = 2, LOAD_VAR_C = 3, LOAD_CONST = 4;
	private static final byte ADD = 10, SUB = 11, MUL = 12, DIV = 13, POW = 14;
	private static final byte ABS = 20, MAX = 21, MIN = 22, SIGN = 23, STEP = 24, EXP = 25, LN = 30, LOG = 31, TANH
		= 32;

	private final byte[] instructions;
	private final float[] constants;

	// Pre-allocated stack for execution loop
	private final float[] stack = new float[64];

	// Triple Bitmask System (Max 64 variables per state)
	private long maskA = 0L; // $a0 to $a63 (State After / Post-Compute)
	private long maskB = 0L; // $b0 to $b63 (State Before / Pre-Compute)
	private long maskC = 0L; // $c0 to $c63 (Custom Functions)

	public ScoreCompiler(String formula) {
		ArrayList<Byte> code = new ArrayList<>();
		ArrayList<Float> consts = new ArrayList<>();
		Stack<String> operators = new Stack<>();

		Parser parser = new Parser(formula.replaceAll("\\s+", ""));

		while (parser.hasMore()) {
			String token = parser.nextToken();

			if (isNumber(token)) {
				code.add(LOAD_CONST);
				code.add((byte) consts.size());
				consts.add(Float.parseFloat(token));
			} else if (token.startsWith("$")) {
				char type = token.charAt(1);
				int idx = Integer.parseInt(token.substring(2));

				if (idx > 63) throw new RuntimeException("Index too large for 64-bit mask: $" + type + idx);

				if (type == 'a') {
					maskA |= (1L << idx);
					code.add(LOAD_VAR_A);
				} else if (type == 'b') {
					maskB |= (1L << idx);
					code.add(LOAD_VAR_B);
				} else if (type == 'c') {
					maskC |= (1L << idx);
					code.add(LOAD_VAR_C);
				} else {
					throw new RuntimeException("Invalid variable prefix: " + token);
				}

				code.add((byte) idx);

			} else if (isFunc(token)) {
				operators.push(token);
			} else if (token.equals(",")) {
				while (!operators.isEmpty() && !operators.peek().equals("(")) {
					code.add(opToByte(operators.pop()));
				}
			} else if (token.equals("(")) {
				operators.push(token);
			} else if (token.equals(")")) {
				while (!operators.peek().equals("(")) {
					code.add(opToByte(operators.pop()));
				}
				operators.pop(); // Pop '('
				if (!operators.isEmpty() && isFunc(operators.peek())) {
					code.add(opToByte(operators.pop()));
				}
			} else if (isOp(token)) {
				while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(token)) {
					code.add(opToByte(operators.pop()));
				}
				operators.push(token);
			}
		}

		while (!operators.isEmpty()) {
			code.add(opToByte(operators.pop()));
		}

		// Convert to primitive arrays
		this.instructions = new byte[code.size()];
		for (int i = 0; i < code.size(); i++) this.instructions[i] = code.get(i);

		this.constants = new float[consts.size()];
		for (int i = 0; i < consts.size(); i++) this.constants[i] = consts.get(i);
	}

	public ScoreCompiler(byte[] instructions, float[] constants) {
		this.instructions = instructions;
		this.constants = constants;

		// Scan the raw bytes to reconstruct the triple bitmasks
		for (int i = 0; i < instructions.length; i++) {
			byte op = instructions[i];

			if (op == LOAD_VAR_A || op == LOAD_VAR_B || op == LOAD_VAR_C) {
				int idx = instructions[++i] & 0xFF;
				if (op == LOAD_VAR_A) maskA |= (1L << idx);
				else if (op == LOAD_VAR_B) maskB |= (1L << idx);
				else maskC |= (1L << idx);
			} else if (op == LOAD_CONST) {
				i++; // Skip constant index
			}
		}
	}

	// --- Getters for Network Sync & Lazy Loading ---
	public long getMaskA() { return maskA; }
	public long getMaskB() { return maskB; }
	public long getMaskC() { return maskC; }
	public byte[] getInstructions() { return instructions; }
	public float[] getConstants() { return constants; }

	public void validate(int maxStateVarIndex, int maxCustomVarIndex) {
		int sp = -1;
		for (int i = 0; i < instructions.length; i++) {
			byte op = instructions[i];

			if (op == LOAD_VAR_A || op == LOAD_VAR_B) {
				int idx = instructions[++i] & 0xFF;
				if (idx >= maxStateVarIndex) throw new RuntimeException("State Index out of bounds: $" + (op == LOAD_VAR_A ? "a" : "b") + idx);
				sp++;
			} else if (op == LOAD_VAR_C) {
				int idx = instructions[++i] & 0xFF;
				if (idx >= maxCustomVarIndex) throw new RuntimeException("Custom Index out of bounds: $c" + idx);
				sp++;
			} else if (op == LOAD_CONST) {
				i++; // Skip the index byte
				sp++;
			} else if (op >= 10 && op <= 14) { // Binary ops
				if (sp < 1) throw new RuntimeException("Incomplete formula: missing operands");
				sp--;
			} else if (op >= 20) { // Unary/Functions
				if (sp < 0) throw new RuntimeException("Function missing argument");
				if (op == MAX || op == MIN) {
					if (sp < 1) throw new RuntimeException("Max/Min missing second argument");
					sp--;
				}
			}
			if (sp >= 63) throw new RuntimeException("Formula too complex for stack (Depth 64+)");
		}
		if (sp != 0) throw new RuntimeException("Formula error: unbalanced operations left on stack");
	}

	// Evaluation now takes the 3 arrays directly. Zero offset math required.
	public float evaluate(float[] varsB, float[] varsA, float[] varsC) {
		int sp = -1, ip = 0;
		final byte[] inst = this.instructions;

		while (ip < inst.length) {
			byte op = inst[ip++];
			switch (op) {
				case LOAD_VAR_A: stack[++sp] = varsA[inst[ip++] & 0xFF]; break;
				case LOAD_VAR_B: stack[++sp] = varsB[inst[ip++] & 0xFF]; break;
				case LOAD_VAR_C: stack[++sp] = varsC[inst[ip++] & 0xFF]; break;
				case LOAD_CONST: stack[++sp] = constants[inst[ip++] & 0xFF]; break;
				case ADD: stack[--sp] += stack[sp + 1]; break;
				case SUB: stack[--sp] -= stack[sp + 1]; break;
				case MUL: stack[--sp] *= stack[sp + 1]; break;
				case DIV: stack[--sp] /= stack[sp + 1]; break;
				case POW: float b = stack[sp--]; stack[sp] = (float)Math.pow(stack[sp], b); break;
				case MAX: float v2 = stack[sp--]; stack[sp] = Math.max(stack[sp], v2); break;
				case MIN: float v3 = stack[sp--]; stack[sp] = Math.min(stack[sp], v3); break;
				case ABS: stack[sp] = Math.abs(stack[sp]); break;
				case SIGN: stack[sp] = Math.signum(stack[sp]); break;
				case STEP: stack[sp] = stack[sp] > 0 ? 1f : 0f; break;
				case EXP: stack[sp] = (float)Math.exp(stack[sp]); break;
				case LN:  stack[sp] = (float)Math.log(stack[sp]); break;
				case LOG: stack[sp] = (float)Math.log10(stack[sp]); break;
				case TANH: stack[sp] = (float) Math.tanh(stack[sp]); break;
			}
		}
		return stack[0];
	}

	// --- Helpers ---
	private byte opToByte(String op) {
		switch (op) {
			case "+": return ADD; case "-": return SUB; case "*": return MUL; case "/": return DIV; case "^": return POW;
			case "abs": return ABS; case "max": return MAX; case "min": return MIN;
			case "sign": return SIGN; case "step": return STEP; case "exp": return EXP;
			case "ln": return LN; case "log": return LOG; case "tanh" : return TANH;
			default: throw new RuntimeException("Unknown op: " + op);
		}
	}
	private boolean isNumber(String s) { return Character.isDigit(s.charAt(0)) || s.charAt(0) == '.'; }
	private boolean isFunc(String s) { return Character.isLetter(s.charAt(0)) && !s.startsWith("$"); }
	private boolean isOp(String s) { return "+-*/^".contains(s); }
	private int precedence(String op) {
		if (op.equals("^")) return 4;
		if (op.equals("*") || op.equals("/")) return 3;
		if (op.equals("+") || op.equals("-")) return 2;
		return 0; // func and (
	}

	private static class Parser {
		String s;
		int pos = 0;

		Parser(String s) { this.s = s; }

		boolean hasMore() { return pos < s.length(); }

		String nextToken() {
			char c = s.charAt(pos);
			if ("+-*/^(),".indexOf(c) != -1) {
				pos++;
				return String.valueOf(c);
			}

			int start = pos;
			if (c == '$') {
				pos++;
				if (pos < s.length() && (s.charAt(pos) == 'a' || s.charAt(pos) == 'b' || s.charAt(pos) == 'c')) {
					pos++;
				} else {
					throw new RuntimeException("Variables must start with $a, $b, or $c");
				}
				while (pos < s.length() && Character.isDigit(s.charAt(pos))) pos++;
			} else if (Character.isDigit(c) || c == '.') {
				while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
			} else if (Character.isLetter(c)) {
				while (pos < s.length() && Character.isLetter(s.charAt(pos))) pos++;
			} else {
				throw new RuntimeException("Unexpected character in formula: " + c);
			}
			return s.substring(start, pos);
		}
	}

	// --- for render ---
	public static String decompile(byte[] instructions, float[] constants) {
		if (instructions == null || instructions.length == 0) return "";

		Stack<String> stack = new Stack<>();
		int ip = 0;

		while (ip < instructions.length) {
			byte op = instructions[ip++];

			switch (op) {
				case LOAD_VAR_A: stack.push("$a" + (instructions[ip++] & 0xFF)); break;
				case LOAD_VAR_B: stack.push("$b" + (instructions[ip++] & 0xFF)); break;
				case LOAD_VAR_C: stack.push("$c" + (instructions[ip++] & 0xFF)); break;
				case LOAD_CONST:
					float val = constants[instructions[ip++] & 0xFF];
					if (val == (long) val) {
						stack.push(String.format("%d", (long) val));
					} else {
						stack.push(String.valueOf(val));
					}
					break;
				case ADD: String rAdd = stack.pop(); stack.push("(" + stack.pop() + " + " + rAdd + ")"); break;
				case SUB: String rSub = stack.pop(); stack.push("(" + stack.pop() + " - " + rSub + ")"); break;
				case MUL: String rMul = stack.pop(); stack.push("(" + stack.pop() + " * " + rMul + ")"); break;
				case DIV: String rDiv = stack.pop(); stack.push("(" + stack.pop() + " / " + rDiv + ")"); break;
				case POW: String rPow = stack.pop(); stack.push("(" + stack.pop() + " ^ " + rPow + ")"); break;
				case MAX: String rMax = stack.pop(); stack.push("max(" + stack.pop() + ", " + rMax + ")"); break;
				case MIN: String rMin = stack.pop(); stack.push("min(" + stack.pop() + ", " + rMin + ")"); break;
				case ABS: stack.push("abs(" + stack.pop() + ")"); break;
				case SIGN: stack.push("sign(" + stack.pop() + ")"); break;
				case STEP: stack.push("step(" + stack.pop() + ")"); break;
				case EXP: stack.push("exp(" + stack.pop() + ")"); break;
				case LN:  stack.push("ln(" + stack.pop() + ")"); break;
				case LOG: stack.push("log(" + stack.pop() + ")"); break;
				case TANH: stack.push("tanh(" + stack.pop() + ")"); break;
				default: throw new RuntimeException("Unknown opcode during decompilation: " + op);
			}
		}

		String result = stack.pop();
		if (result.startsWith("(") && result.endsWith(")")) {
			result = result.substring(1, result.length() - 1);
		}

		return result;
	}
}