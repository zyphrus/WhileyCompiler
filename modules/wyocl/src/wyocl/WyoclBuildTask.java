package wyocl;

import wyjc.Wyil2JavaBuilder;
import wyjc.util.WyjcBuildTask;

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
		return new Wyil2JavaOpenCLBuilder();
	}
}
