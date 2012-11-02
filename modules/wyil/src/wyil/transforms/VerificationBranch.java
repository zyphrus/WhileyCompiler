// Copyright (c) 2012, David J. Pearce (djp@ecs.vuw.ac.nz)
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

package wyil.transforms;

import static wyil.util.ConstraintSolver.And;
import static wyil.util.ConstraintSolver.Equals;
import static wyil.util.ConstraintSolver.Not;
import static wyil.util.ConstraintSolver.Or;
import static wyil.util.ConstraintSolver.SCHEMA;
import static wyil.util.ConstraintSolver.Var;
import static wyil.util.ConstraintSolver.infer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import wyil.lang.Attribute;
import wyil.lang.Block;
import wyil.lang.Code;
import wyil.util.ConstraintSolver;
import wyone.core.Automaton;
import wyone.io.PrettyAutomataWriter;

/**
 * <p>
 * Represents a path through the body of a Wyil method or function. A branch
 * accumulates the constraints known to hold through that particular execution
 * path. These constraints can then be checked for satisfiability at various
 * critical points in the function.
 * </p>
 * <p>
 * When verifying a given function or method, the verifier starts with a single
 * branch at the beginning of the method. When split points in the control-flow
 * graph are encountered, branches are accordingly forked off to represent the
 * alternate control-flow path. At control-flow meet points, branches may also
 * be joined back together (although this is not always strictly necessary). A
 * diagrammatic view might be:
 * </p>
 * 
 * <pre>
 *  entry
 *   ||
 *   ||
 *   ##\
 *   ||\\
 *   || \\
 *   || || 
 *   || ##\
 *   || ||\\
 *   ||//  \\
 *   ##/   ||
 *   ||    ||
 *   \/    \/
 *   B1    B3
 * </pre>
 * <p>
 * In the above example, we initially start with one branch <code>B1</code>.
 * This is then forked to give branch <code>B2</code> which, in turn, is forked
 * again to give <code>B3</code>. Subsequently, branch <code>B2</code> is joined
 * back with <code>B1</code>. However, <code>B3</code> is never joined and
 * terminates separately.
 * </p>
 * <p>
 * Every branch (except the first) has a <i>parent</i> branch which it was
 * forked from. Given any two branches there is always a <i>Least Common
 * Ancestor (LCA)</i> --- that is, the latest point which is common to both
 * branches. Finding the LCA can be useful, for example, to identify constraints
 * common to both branches.
 * </p>
 * 
 * @author David J. Pearce
 * 
 */
public class VerificationBranch {
	/**
	 * The parent branch which this branch was forked from, or <code>null</code>
	 * if it is the initial "master" branch for the function or method in
	 * question.
	 */
	private final VerificationBranch parent;

	/**
	 * Maintains the master map of variables to unique identifiers for
	 * allocating new SSA variable indices. This is shared amongst all branches
	 * which originate from a given master (hence
	 * <code>parent == null || registry == parent.registry</code> must hold).
	 */
	private final int[] registry;

	/**
	 * Maintains the current static single assignment index for each variable in
	 * the method or function. Note,
	 * <code>environment.length == registry.length</code> must hold.
	 */
	private final int[] environment;

	/**
	 * Provides a prefix for all variables written or read on this branch. This
	 * is primarily useful for ensuring that constraints generated from external
	 * blocks (e.g. post-conditions, loop invariants, etc) can be safely merged
	 * into the outermost block.
	 */
	private final String prefix;

	/**
	 * The automaton into which all constraints are constructed and which will
	 * eventually be used to check for (un)satisfiability.
	 */
	private final Automaton automaton;

	/**
	 * The list of constraints which are known to be true at this point on the
	 * branch. Note that these are indexes into the automaton above.
	 */
	private final ArrayList<Integer> constraints;

	/**
	 * The block of Wyil bytecode instructions which this branch is traversing
	 * (note: <code>parent == null || block == parent.block</code> must hold).
	 */
	private final Block block;

	/**
	 * The origin determines the bytecode offset in block where this branch was
	 * forked from. For the master branch, this will be <code>0</code>.
	 */
	private final int origin;

	/**
	 * The bytecode index into the above block that this branch is currently at.
	 */
	private int pc;

	/**
	 * Construct the master verification branch for a given code block. The
	 * master for a block has an origin <code>0</code> and an (initial) PC of
	 * <code>0</code> (i.e. the branch begins at the entry of the block).
	 * 
	 * @param prefix
	 *            --- the prefix string to use for variable names within the
	 *            generated constraints. For the master branch of a
	 *            method/function block, this can be the empty string. However,
	 *            for an block external to the main block for a method/function,
	 *            we will need a suitable prefix to avoid name clashes with
	 *            existing variables.
	 * @param automaton
	 *            --- the automaton to which constraints generated for this
	 *            block are stored.
	 * @param block
	 *            --- the block of code on which this branch is operating.
	 */
	public VerificationBranch(String prefix, Automaton automaton, Block block) {
		this.parent = null;
		this.registry = new int[block.numSlots()];
		this.environment = new int[block.numSlots()];
		this.prefix = prefix;
		this.automaton = automaton;
		this.constraints = new ArrayList<Integer>();
		this.block = block;
		this.origin = 0;
		this.pc = 0;
	}

	/**
	 * Private constructor used for forking a child-branch from a parent branch.
	 * 
	 * @param parent
	 *            --- parent branch being forked from.
	 */
	private VerificationBranch(VerificationBranch parent) {
		this.parent = parent;
		this.registry = parent.registry;
		this.environment = parent.environment.clone();
		this.prefix = parent.prefix;
		this.automaton = parent.automaton;
		// TODO: investigate alternatives to this?
		this.constraints = new ArrayList<Integer>(parent.constraints);
		this.block = parent.block;
		this.origin = parent.pc;
		this.pc = parent.pc;
	}

	/**
	 * Return the current Program Counter (PC) value for this branch. This must
	 * be a valid index into the code block this branch is operating over.
	 * 
	 * @return
	 */
	public int pc() {
		return pc;
	}

	/**
	 * Get the block entry at the current PC position.
	 * 
	 * @return
	 */
	public Block.Entry entry() {
		return block.get(pc);
	}
	
	/**
	 * Return the automaton associated with this branch.
	 */
	public Automaton automaton() {
		return automaton;
	}

	/**
	 * Move the Program Counter (PC) to the next logical instruction in the
	 * branch.
	 * 
	 * @return -- true if there is a next instruction, or false otherwise.
	 */
	public boolean next() {
		this.pc++;
		return this.pc < block.size();
	}
		
	/**
	 * Reposition the Program Counter (PC) for this branch to a given label in
	 * the block.
	 * 
	 * @param label
	 *            --- label to look for, which is assumed to occupy an index
	 *            greater than the current PC (this follows the Wyil requirement
	 *            that branches always go forward).
	 */
	public void goTo(String label) {
		for (int i = pc; i != block.size(); ++i) {
			Code code = block.get(i).code;
			if (code instanceof Code.Label) {
				Code.Label l = (Code.Label) code;
				if (l.label.equals(label)) {
					pc = i;
					return;
				}
			}
		}
	}
	
	/**
	 * Get the constraint variable which corresponds to the given Wyil bytecode
	 * register at this point on this branch.
	 * 
	 * @param register
	 * @return
	 */
	public int read(int register) {		
		return Var(automaton, prefix + register + "$"
				+ environment[register]);
	}

	/**
	 * Assign a given expression stored in the automaton to a given Wyil
	 * bytecode register.
	 * 
	 * @param register
	 * @param expr
	 */
	public void write(int register, int expr) {
		int nval = allocateNewIndex(register);
		environment[register] = nval;
		constraints.add(Equals(automaton,
				Var(automaton, prefix + register + "$" + nval), expr));
	}
	
	/**
	 * Terminate the current flow for a given register and begin a new one. In
	 * terms of static-single assignment, this means simply change the index of
	 * the register in question.
	 * 
	 * @param register
	 */
	public void invalidate(int register) {
		environment[register] = allocateNewIndex(register);
	}
	
	/**
	 * Return a reference into the automaton which represents all of the
	 * constraints that hold at this position in the branch.
	 * 
	 * @return
	 */
	public int constraints() {
		return And(automaton, constraints);
	}

	/**
	 * Add a given constraint to the list of constraints which are assumed to
	 * hold.
	 * 
	 * @param constraint
	 */
	public void assume(int constraint) {
		constraints.add(constraint);
	}
	
	/**
	 * Assert that the given constraint holds.
	 * 
	 * @return
	 */
	public boolean assertTrue(int test, boolean debug) {
		try {
			Automaton tmp = new Automaton(automaton);
			int root = And(tmp, constraints);
			root = And(tmp, root, Not(tmp, test));
			int mark = tmp.mark(root);

			if (debug) {
				Attribute.Source src = block.get(pc).attribute(
						Attribute.Source.class);
				System.err
						.println("============================================");
				if (src != null) {
					System.err.print(src.line + ":");
				}
				new PrettyAutomataWriter(System.err, SCHEMA, "And", "Or")
						.write(tmp);
			}

			infer(tmp);

			if (debug) {
				System.err.println("\n\n=> (" + ConstraintSolver.numSteps
						+ " steps, " + ConstraintSolver.numInferences
						+ " reductions, " + ConstraintSolver.numInferences
						+ " inferences)\n");
				new PrettyAutomataWriter(System.err, SCHEMA, "And", "Or")
						.write(tmp);
				System.err.println();
			}

			// assertion holds if a contradiction is shown.
			return tmp.get(tmp.root(mark)).equals(ConstraintSolver.False);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * <p>
	 * Fork a child-branch from this branch. The child branch is (initially)
	 * identical in every way to the parent, however the expectation is that
	 * they will diverge.
	 * </p>
	 * 
	 * <pre>
	 *    B1
	 *    ||
	 *    ||
	 *    ##    <- origin
	 *    | \ 
	 *    ||\\
	 *    || \\
	 *    \/  \/
	 *    B1  B2
	 * </pre>
	 * <p>
	 * The origin for the forked branch is the <code>PC</code value at the split
	 * point. Initially, the <code>PC</code> value for the forked branch is
	 * identical to that of the parent, however it is expected that a
	 * <code>goTo</code> will be used immediately after the fork to jump the
	 * child branch to its logical starting point.
	 * </p>
	 * <p>
	 * A new environment is created for the child branch which, initially, is
	 * identical to that of the parent. As assignments to variables are made on
	 * either branch, these environments will move apart.
	 * </p>
	 * 
	 * @return --- The child branch which is forked off this branch.
	 */
	public VerificationBranch fork() {
		return new VerificationBranch(this);
	}

	/**
	 * <p>
	 * Merge descendant (i.e. a child or child-of-child, etc) branch back into
	 * this branch. The constraints for this branch must now correctly capture
	 * those constraints that hold coming from either branch (i.e. this
	 * represents a meet-point in the control-flow graph).
	 * </p>
	 * <p>
	 * To generate the constraints which hold after the meet, we take the
	 * logical OR of those constraints holding on this branch prior to the meet
	 * and those which hold on the incoming branch. For example, support we
	 * have:
	 * </p>
	 * 
	 * <pre>
	 * 	 y$0 != 0    y$0 != 0
	 *   && x$1 < 1  && x$2 >= 1   
	 *        ||      ||
	 *         \\    //
	 *          \\  //
	 *           \\//
	 *            ##
	 *   y$0 != 0 &&
	 * ((x$1 < 1 && x$3 == x$1) || 
	 *  (x$2 >= 1 && x$3 == x$2))
	 * </pre>
	 * <p>
	 * Here, we see that <code>y$0 != 0</code> is constant to both branches and
	 * is ommitted from the disjunction. Furthermore, we've added and assignment
	 * <code>x$3 == </code> onto both sides of the disjunction to capture the
	 * flow of variable <code>x</code> from both sides (since it was modified on
	 * atleast one of the branches).
	 * </p>
	 * <p>
	 * One challenge is to determine constraints which are constant to both
	 * sides. Eliminating such constraints from the disjunction reduces the
	 * overall work of the constraint solver. This is done by comparing the
	 * automaton references for each constraint since the automaton guarantees
	 * that identical terms have the same reference.
	 * </p>
	 * 
	 * @param incoming
	 *            --- The descendant branch which is being merged into this one.
	 */
	public void join(VerificationBranch incoming) {
		// FIRST: determine new constraint sequence
		ArrayList<Integer> common = new ArrayList<Integer>();
		ArrayList<Integer> lhsConstraints = new ArrayList<Integer>();
		ArrayList<Integer> rhsConstraints = new ArrayList<Integer>();
		splitConstraints(incoming,common,lhsConstraints,rhsConstraints);
		
		int l = And(automaton, lhsConstraints);
		int r = And(automaton, rhsConstraints);
		
		// can now compute the logical OR of both branches
		int join = Or(automaton, l, r);

		// now, clear our sequential constraints since we can only have one
		// which holds now: namely, the or of the two branches.
		constraints.clear();
		constraints.addAll(common);
		constraints.add(join);
		
		// SECOND: update environment
		for(int i=0;i!=environment.length;++i) {
			if(environment[i] != incoming.environment[i]) {
				System.err.println("DIFFERING ENVIRONMENTS!!");
			}
		}
	}

	/**
	 * Determine a fresh index for the given variable.
	 * 
	 * @param var
	 * @return
	 */
	private int allocateNewIndex(int var) {
		return ++registry[var];
	}
	
	/**
	 * Split the constraints for this branch and the incoming branch into three
	 * sets: those common to both; those unique to this branch; and, those
	 * unique to the incoming branch.
	 * 
	 * @param incoming
	 * @param common
	 * @param myRemainder
	 * @param incomingRemainder
	 */
	private void splitConstraints(VerificationBranch incoming,
			ArrayList<Integer> common, ArrayList<Integer> myRemainder,
			ArrayList<Integer> incomingRemainder) {
		BitSet lhs = new BitSet(automaton.nStates());
		BitSet rhs = new BitSet(automaton.nStates());
		for (int i : constraints) {
			lhs.set(i);
		}
		for (int i : incoming.constraints) {
			rhs.set(i);
		}
		lhs.and(rhs);

		for (int i : constraints) {
			if (lhs.get(i)) {
				common.add(i);
			} else {
				myRemainder.add(i);
			}
		}

		for (int i : incoming.constraints) {
			if (lhs.get(i)) {
				common.add(i);
			} else {
				incomingRemainder.add(i);
			}
		}
	}
}
