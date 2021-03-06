// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyil.util.type;

import static wyil.util.TypeSystem.*;
import wyautl_old.lang.*;
import wyil.lang.Type;
import wyil.util.TypeSystem;

/**
 * <p>
 * The explicit coercion operator extends the implicit coercion operator to
 * include coercions which must be specified with an explicit cast operation.
 * This is necessary because such coercions correspond to a loss of precision
 * and, hence, may fail at runtime. An example is the following
 * </p>
 *
 * <pre>
 * char f(int x):
 *     return (char) x
 * </pre>
 *
 * <p>
 * The above will only compile if the explicit <code>(char)</code> cast is
 * provided. This is required because a <code>char</code> corresponds only to a
 * subset of the possible integers (i.e. those codepoints defined by the Unicode
 * standard). The semantics of the Whiley language dictate that, should the
 * integer lie outside the range of permissible code points, then a runtime
 * fault is raised.
 * </p>
 *
 * <b>NOTE:</b> as for the subtype operator, both types must have been
 * normalised beforehand to guarantee correct results from this operator. </p>
 *
 * @author David J. Pearce
 *
 */
public class ExplicitCoercionOperator extends SubtypeOperator {

	public ExplicitCoercionOperator(Automaton fromAutomata, Automaton toAutomata, LifetimeRelation lr) {
		super(fromAutomata,toAutomata,lr);
	}

	@Override
	public boolean isIntersectionInner(int fromIndex, boolean fromSign,
			int toIndex, boolean toSign) {
		Automaton.State fromState = from.states[fromIndex];
		Automaton.State toState = to.states[toIndex];
		int fromKind = fromState.kind;
		int toKind = toState.kind;

		if(fromKind == K_RECORD && toKind == K_RECORD) {
			return intersectRecords(fromIndex,fromSign,toIndex,toSign);
		} else {
			return super.isIntersectionInner(fromIndex, fromSign, toIndex,
					toSign);
		}
	}

	/**
	 * <p>
	 * Check for intersection between two states with kind K_RECORD. The
	 * distinction between open and closed records adds complexity here.
	 * </p>
	 *
	 * <p>
	 * Intersection between <b>closed</b> records is the easiest case. The main
	 * examples are:
	 * </p>
	 * <ul>
	 * <li><code>{T1 f, T2 g} & {T3 f, T4 g} = if T1&T3 and T2&T4</code>.</li>
	 * <li><code>{T1 f, T2 g} & {T3 f, T4 h} = false</code>.</li>
	 * <li><code>{T1 f, T2 g} & !{T3 f, T4 g} = if T1&!T3 or T2&!T4</code>.</li>
	 * <li><code>{T1 f, T2 g} & !{T3 f} = false</code>.</li>
	 * <li><code>!{T1 f} & !{T2 f} = true</code>.</li>
	 * </ul>
	 * <p>
	 * Intersection between a <b>closed</b> and <b>open</b> record is similar. The main examples are:
	 * </p>
	 * <ul>
	 * <li><code>{T1 f, T2 g, ...} & {T3 f, T4 g} = if T1&T3 and T2&T4</code>.</li>
	 * <li><code>{T1 f, ...} & {T2 f, T3 g} = if T1&T2</code>.</li>
	 * <li><code>{T1 f, T2 g, ...} & {T3 f, T4 h} = false</code>.</li>
	 * <li><code>{T1 f, T2 g, ...} & !{T3 f, T4 g} = if T1&!T3 or T2&!T4</code>.</li>	 *
	 * <li><code>!{T1 f, T2 g, ...} & {T3 f, T4 g} = if T1&!T3 or T2&!T4</code>.</li>	 *
	 * <li><code>{T1 f, ...} & !{T2 f, T3 g} = true</code>.</li>
	 * <li><code>{T1 f, T2 g, ...} & !{T3 f, T4 h} = false</code>.</li>
     * <li><code>!{T1 f,...} & !{T2 f} = true</code>.</li>
	 * </ul>
	 *
	 * @param fromIndex
	 *            --- index of from state
	 * @param fromSign
	 *            --- sign of from state (true = normal, false = inverted).
	 * @param toIndex
	 *            --- index of to state
	 * @param toSign
	 *            --- sign of from state (true = normal, false = inverted).
	 * @return --- true if such an intersection exists, false otherwise.
	 */
	@Override
	protected boolean intersectRecords(int fromIndex, boolean fromSign, int toIndex, boolean toSign) {
		Automaton.State fromState = from.states[fromIndex];
		Automaton.State toState = to.states[toIndex];
		if(fromSign || toSign) {
			int[] fromChildren = fromState.children;
			int[] toChildren = toState.children;
			TypeSystem.RecordState fromFields = (TypeSystem.RecordState) fromState.data;
			TypeSystem.RecordState toFields = (TypeSystem.RecordState) toState.data;

			boolean fromOpen = true; // to force explicit coercions
			boolean toOpen = true;   // to force explicit coercions

			if (fromChildren.length < toChildren.length && !fromOpen) {
				return !fromSign || !toSign;
			} else if (fromChildren.length > toChildren.length  && !toOpen) {
				return !fromSign || !toSign;
			} else if (!fromSign && !fromOpen && toOpen) {
				return true; // guaranteed true!
			} else if (!toSign && !toOpen && fromOpen) {
				return true; // guaranteed true!
			}

			boolean andChildren = true;
			boolean orChildren = false;

			int fi=0;
			int ti=0;
			while(fi != fromFields.size() && ti != toFields.size()) {
				boolean v;
				String fn = fromFields.get(fi);
				String tn = toFields.get(ti);
				int c = fn.compareTo(tn);
				if(c == 0) {
					int fromChild = fromChildren[fi++];
					int toChild = toChildren[ti++];
					v = isIntersection(fromChild, fromSign, toChild,
							toSign);
				} else if(c < 0 && toOpen) {
					fi++;
					v = toSign;
				} else if(c > 0 && fromOpen) {
					ti++;
					v = fromSign;
				} else {
					return !fromSign || !toSign;
				}
				andChildren &= v;
				orChildren |= v;
			}

			if(fi < fromFields.size()) {
				if(toOpen) {
					// assert fromSign || fromOpen
					orChildren |= toSign;
					andChildren &= toSign;
				} else {
					return !fromSign || !toSign;
				}
			} else if(ti < toFields.size()) {
				if(fromOpen) {
					// assert toSign || toOpen
					orChildren |= fromSign;
					andChildren &= fromSign;
				} else {
					return !fromSign || !toSign;
				}
			}

			if(!fromSign || !toSign) {
				return orChildren;
			} else {
				return andChildren;
			}
		}
		return true;
	}
}