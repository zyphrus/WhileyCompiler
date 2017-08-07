// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyc.testing;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import wybs.lang.SyntaxError;
import wyc.commands.Compile;
import wyc.io.WhileyFileLexer;
import wyc.io.WhileyFilePrinter;
import wyc.lang.Stmt;
import wyc.lang.WhileyFile;
import wyc.util.TestUtils;
import wycc.util.Logger;
import wycc.util.Pair;
import wyfs.lang.Content;
import wyfs.lang.Path;
import wyfs.util.Trie;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Run through all valid test cases with verification enabled. Since every test
 * file is valid, a successful test occurs when the compiler succeeds and, when
 * executed, the compiled file produces the expected output. Note that an
 * internal failure does not count as a valid pass, and indicates the test
 * exposed some kind of compiler bug.
 *
 * @author David J. Pearce
 *
 */
@RunWith(Parameterized.class)
public class PrettyPrinterTest {

	/**
	 * The directory containing the source files for each test case. Every test
	 * corresponds to a file in this directory.
	 */
	public final static String WHILEY_SRC_DIR = "tests/valid".replace('/', File.separatorChar);
	public final static String WHILEY_PRETTY_DIR = "tests/pretty".replace('/', File.separatorChar);

	/**
	 * Ignored tests and a reason why we ignore them.
	 */
	public final static Map<String, String> IGNORED = new HashMap<>();

	static {
	}

	/**
	 * The directory where compiler libraries are stored. This is necessary
	 * since it will contain the Whiley Runtime.
	 */
	public final static String WYC_LIB_DIR = "../../lib/".replace('/', File.separatorChar);

	static {

		// The purpose of this is to figure out what the proper name for the
		// wyrt file is. Since there can be multiple versions of this file,
		// we're not sure which one to pick.

		File file = new File(WYC_LIB_DIR);
//		for(String f : file.list()) {
//			if(f.startsWith("wyrt-v")) {
//				WYRT_PATH = WYC_LIB_DIR + f;
//			}
//		}
	}

	// ======================================================================
	// Test Harness
	// ======================================================================

	/**
	 * Compile a syntactically invalid test case with verification enabled. The
	 * expectation is that compilation should fail with an error and, hence, the
	 * test fails if compilation does not.
	 *
	 * @param name
	 *            Name of the test to run. This must correspond to a whiley
	 *            source file in the <code>WHILEY_SRC_DIR</code> directory.
	 */
	protected void runTest(String testName) throws IOException {
		File whileySrcDir = new File(WHILEY_SRC_DIR);
		File whileyPrettyDir = new File(WHILEY_PRETTY_DIR);

		if (!whileyPrettyDir.exists()) {
            whileyPrettyDir.mkdir();
        }

		// this will need to turn on verification at some point.
		String whileyFilename = WHILEY_SRC_DIR + File.separatorChar + testName
				+ ".whiley";

		String whileyPrettyFilename = WHILEY_PRETTY_DIR + File.separatorChar + testName
				+ ".whiley";

		File whileyPrettyFile = new File(whileyPrettyFilename);

        List<WhileyFile> whileyFiles = TestUtils.parse(whileySrcDir, whileyFilename);

        for (WhileyFile file : whileyFiles) {
			FileOutputStream fileOut = new FileOutputStream(whileyPrettyFile);
			WhileyFilePrinter printer = new WhileyFilePrinter(fileOut);

			printer.print(file);

			try {
				TestUtils.parse(whileyPrettyDir, whileyPrettyFilename);
				whileyPrettyFile.delete();
			} catch (wybs.lang.SyntaxError ex) {
			    ex.outputSourceError(System.err, false);
                new WhileyFilePrinter(System.err).print(file);
				fail("Outputs did not parse");
			}
		}
	}

	// ======================================================================
	// Tests
	// ======================================================================

	// Parameter to test case is the name of the current test.
	// It will be passed to the constructor by JUnit.
	private final String testName;
	public PrettyPrinterTest(String testName) {
		this.testName = testName;
	}

	// Here we enumerate all available test cases.
	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return TestUtils.findTestNames(WHILEY_SRC_DIR);
	}

	// Skip ignored tests
	@Before
	public void beforeMethod() {
		String ignored = IGNORED.get(this.testName);
		Assume.assumeTrue("Test " + this.testName + " skipped: " + ignored, ignored == null);
	}

	@Test
	public void valid() throws IOException {
		if (new File("../../running_on_travis").exists()) {
			System.out.println(".");
		}
		runTest(this.testName);
	}
}
