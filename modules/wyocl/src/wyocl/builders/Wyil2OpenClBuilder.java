// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyocl.builders;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import wybs.lang.Builder;
import wybs.lang.Logger;
import wybs.lang.NameSpace;
import wybs.lang.Path;
import wybs.util.Pair;
import wyil.lang.Block;
import wyil.lang.Code;
import wyil.lang.WyilFile;
import wyjvm.lang.ClassFile;
import wyocl.lang.ClFile;

public class Wyil2OpenClBuilder implements Builder {
	
	private Logger logger = Logger.NULL;
	
	/** 	
	 * The endLabel is used to determine when we're within a for loop being
	 * writed in OpenCL. If this is <code>null</code> then we're *not*
	 * within a for loop. Otherwise, we are.
	 */
	private String endLabel;
	
	public Wyil2OpenClBuilder() {
		
	}

	@Override
	public NameSpace namespace() {
		return null; // does this make sense to be in builder??
	}
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}	
		
	public void build(List<Pair<Path.Entry<?>,Path.Entry<?>>> delta) throws IOException {

		Runtime runtime = Runtime.getRuntime();
		long start = System.currentTimeMillis();
		long memory = runtime.freeMemory();

		// ========================================================================
		// write files
		// ========================================================================

		for(Pair<Path.Entry<?>,Path.Entry<?>> p : delta) {
			Path.Entry<?> f = p.second();
			if(f.contentType() == ClFile.ContentType) {
				//System.err.println("Processing .... ");
				Path.Entry<WyilFile> sf = (Path.Entry<WyilFile>) p.first();
				Path.Entry<ClFile> df = (Path.Entry<ClFile>) f;
				// build the C-File
				ClFile contents = build(sf.read());								
				// finally, write the file into its destination
				df.write(contents);
			} else {
				//System.err.println("Skipping .... " + f.contentType());
			}
		}

		// ========================================================================
		// Done
		// ========================================================================

		long endTime = System.currentTimeMillis();
		logger.logTimedMessage("Wyil => Open CL: compiled " + delta.size()
				+ " file(s)", endTime - start, memory - runtime.freeMemory());
	}	
	
	protected ClFile build(WyilFile module) {
		StringWriter writer = new StringWriter();
		PrintWriter pWriter = new PrintWriter(writer);
		for(WyilFile.MethodDeclaration method : module.methods()) {				
			build(method,pWriter);			
		}		
		return new ClFile(writer.toString());
	}
	
	protected void build(WyilFile.MethodDeclaration method, PrintWriter writer) {
		for(WyilFile.Case c : method.cases()) {
			write(c,method,writer);
		}
	}
		
	protected void write(WyilFile.Case c, WyilFile.MethodDeclaration method,
			PrintWriter writer) {
		write(c.body(),c,method,writer);
	}
	
	protected void write(Block b, WyilFile.Case c,
			WyilFile.MethodDeclaration method, PrintWriter writer) {
		for(Block.Entry e : b) {
			write(e,c,method,writer);
		}
	}
			
	protected void write(Block.Entry entry, WyilFile.Case c,
			WyilFile.MethodDeclaration method, PrintWriter writer) {
		try {
			Code code = entry.code;
			if(code instanceof Code.BinArithOp) {
				write((Code.BinArithOp)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.Convert) {
				write((Code.Convert)code);
			} else if(code instanceof Code.Const) {
				write((Code.Const) code, freeSlot, constants, bytecodes);
			} else if(code instanceof Code.Debug) {
				write((Code.Debug)code,freeSlot,bytecodes);
			} else if(code instanceof Code.LoopEnd) {
				write((Code.LoopEnd)code,freeSlot,bytecodes);
			} else if(code instanceof Code.AssertOrAssume) {
				write((Code.AssertOrAssume)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.FieldLoad) {
				write((Code.FieldLoad)code,freeSlot,bytecodes);
			} else if(code instanceof Code.ForAll) {
				freeSlot = write((Code.ForAll)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Goto) {
				write((Code.Goto)code,freeSlot,bytecodes);
			} else if(code instanceof Code.If) {				
				writeIfGoto((Code.If) code, entry, freeSlot, bytecodes);
			} else if(code instanceof Code.IfIs) {
				write((Code.IfIs) code, entry, freeSlot, constants, bytecodes);
			} else if(code instanceof Code.IndirectInvoke) {
				write((Code.IndirectInvoke)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Invoke) {
				write((Code.Invoke)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Invert) {
				write((Code.Invert)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Label) {
				write((Code.Label)code,freeSlot,bytecodes);
			} else if(code instanceof Code.BinListOp) {
				write((Code.BinListOp)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.Lambda) {
				write((Code.Lambda)code,freeSlot,bytecodes);
			} else if(code instanceof Code.LengthOf) {
				write((Code.LengthOf)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.SubList) {
				write((Code.SubList)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.IndexOf) {
				write((Code.IndexOf)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Assign) {
				write((Code.Assign)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Loop) {
				write((Code.Loop)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Move) {
				write((Code.Move)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Update) {
				write((Code.Update)code,freeSlot,bytecodes);
			} else if(code instanceof Code.NewMap) {
				write((Code.NewMap)code,freeSlot,bytecodes);
			} else if(code instanceof Code.NewList) {
				write((Code.NewList)code,freeSlot,bytecodes);
			} else if(code instanceof Code.NewRecord) {
				write((Code.NewRecord)code,freeSlot,bytecodes);
			} else if(code instanceof Code.NewSet) {
				write((Code.NewSet)code,freeSlot,bytecodes);
			} else if(code instanceof Code.NewTuple) {
				write((Code.NewTuple)code,freeSlot,bytecodes);
			} else if(code instanceof Code.UnArithOp) {
				write((Code.UnArithOp)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Dereference) {
				write((Code.Dereference)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Return) {
				write((Code.Return)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Nop) {
				// do nothing
			} else if(code instanceof Code.BinSetOp) {
				write((Code.BinSetOp)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.BinStringOp) {
				write((Code.BinStringOp)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.SubString) {
				write((Code.SubString)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.Switch) {
				write((Code.Switch)code,entry,freeSlot,bytecodes);
			} else if(code instanceof Code.TryCatch) {
				write((Code.TryCatch)code,entry,freeSlot,handlers,constants,bytecodes);
			} else if(code instanceof Code.NewObject) {
				write((Code.NewObject)code,freeSlot,bytecodes);
			} else if(code instanceof Code.Throw) {
				write((Code.Throw)code,freeSlot,bytecodes);
			} else if(code instanceof Code.TupleLoad) {
				write((Code.TupleLoad)code,freeSlot,bytecodes);
			} else {
				internalFailure("unknown wyil code encountered (" + code + ")", filename, entry);
			}

		} catch (SyntaxError ex) {
			throw ex;
		} catch (Exception ex) {		
			internalFailure(ex.getMessage(), filename, entry, ex);
		}
	}
}
