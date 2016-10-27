// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyautl_old.io;

import java.io.IOException;

/**
 * An interface for reading specific values from a binary stream. This is used
 * by the Generator class so allow writing of Automata and/or values to the
 * stream.
 *
 * @author David J. Pearce
 *
 * @param <T>
 */
public interface GenericReader<T> {
	public T read() throws IOException;
	public void close() throws IOException;
}
