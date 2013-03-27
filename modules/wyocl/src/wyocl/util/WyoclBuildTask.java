package wyocl.util;

import wyjc.util.WyjcBuildTask;
import wyocl.builders.Wyil2JavaBuilder;

/**
 * Responsible for controlling the building of JVM Class files using the
 * WyoclBuilder. It pretty much just defers everything to the existing
 * <code>WyjcBuildTask</code>, but using a different builder.
 * 
 * @author David J. Pearce
 * 
 */
public class WyoclBuildTask extends WyjcBuildTask {

	@Override
	protected Wyil2JavaBuilder getBuilder() {
		return new Wyil2JavaBuilder();
	}
}
