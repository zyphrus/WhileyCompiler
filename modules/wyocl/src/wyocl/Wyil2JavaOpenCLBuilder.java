package wyocl;

import java.util.ArrayList;

import wyil.lang.Code;
import wyjc.*;
import wyjvm.lang.Bytecode;

/**
 * A builder for compiling Wyil files into a combination of Java and OpenCL.
 * Essentially, certain loops marked for execution on the GPU are compiled into
 * OpenCL, whilst the remainder is left in Java with appropriate hooks being
 * installed to interact with the GPU.
 * 
 * 
 */
public class Wyil2JavaOpenCLBuilder extends Wyil2JavaBuilder {
	
	@Override
	protected int translate(Code.ForAll c, int freeSlot,
			ArrayList<Bytecode> bytecodes) {
		System.out.println("WYOCL: spotted a for loop!");
		return super.translate(c, freeSlot, bytecodes);
	}
}
