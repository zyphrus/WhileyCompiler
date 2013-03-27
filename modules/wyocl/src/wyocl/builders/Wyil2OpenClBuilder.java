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
		pWriter.println("// Automatically generated from " + module.filename());
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
	
	protected static int kid = 0;
	
	protected void write(Block.Entry entry, WyilFile.Case c,
			WyilFile.MethodDeclaration method, PrintWriter writer) {
		Code code = entry.code;
		if(code instanceof Code.Label) {
			Code.Label lab = (Code.Label) code;
			if(lab.label.equals(endLabel)) {
				// hit
				writer.println("}");
				endLabel = null;
			}
		} else if(code instanceof Code.ForAll) {
			Code.ForAll fall = (Code.ForAll) code;
			endLabel = fall.target;
			writer.println("void kernel_" + kid + "() {");
			kid = kid + 1;
		} else if (endLabel != null) {
			writer.println("\t// " + code);
		}
	}
}
