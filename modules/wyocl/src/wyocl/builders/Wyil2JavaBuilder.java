package wyocl.builders;

import java.util.ArrayList;
import java.util.HashMap;

import wyil.lang.Code;
import wyil.lang.Block.Entry;
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
public class Wyil2JavaBuilder extends wyjc.Wyil2JavaBuilder {
	/**
	 * The endLabel is used to determine when we're within a for loop being
	 * translated in OpenCL. If this is <code>null</code> then we're *not*
	 * within a for loop. Otherwise, we are.
	 */
	private String endLabel;
	
	protected int translate(Entry entry, int freeSlot,
			HashMap<JvmConstant, Integer> constants,
			ArrayList<UnresolvedHandler> handlers, ArrayList<Bytecode> bytecodes) {
		Code code = entry.code;
		
		// Check to see whether we're at the end of our for loop.
		if(code instanceof Code.Label) {
			Code.Label lab = (Code.Label) code;
			if(lab.label.equals(endLabel)) {
				// hit
				endLabel = null;
				return freeSlot; // skip it
			}
		}
		
		if (endLabel != null) {
			// skip this bytecode.
			
			// TODO: add bytecodes which call the required method to invoke the
			// corresponding kernel.
			
			return freeSlot;
		} else {
			return super.translate(entry, freeSlot, constants, handlers,
					bytecodes);
		}
	}

	@Override
	protected int translate(Code.ForAll c, int freeSlot,
			ArrayList<Bytecode> bytecodes) {
		endLabel = c.target;
		// return super.translate(c, freeSlot, bytecodes);
		return freeSlot;
	}
}
