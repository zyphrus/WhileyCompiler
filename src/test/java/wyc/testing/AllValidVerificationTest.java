// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// This software may be modified and distributed under the terms
// of the BSD license.  See the LICENSE file for details.

package wyc.testing;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import wyc.commands.Compile;
import wyc.util.TestUtils;
import wycc.util.Pair;

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
public class AllValidVerificationTest {

	/**
	 * The directory containing the source files for each test case. Every test
	 * corresponds to a file in this directory.
	 */
	public final static String WHILEY_SRC_DIR = "tests/valid".replace('/', File.separatorChar);

	/**
	 * Ignored tests and a reason why we ignore them.
	 */
	public final static Map<String, String> IGNORED = new HashMap<String, String>();

	static {
		IGNORED.put("BoolAssign_Valid_2", "WyTP#19");
		IGNORED.put("BoolAssign_Valid_3", "WyTP#18");
		IGNORED.put("BoolList_Valid_3", "timeout");
		IGNORED.put("BoolRecord_Valid_1", "WyTP#5");
		IGNORED.put("Cast_Valid_5", "???");
		IGNORED.put("Coercion_Valid_8", "#681");
		IGNORED.put("Complex_Valid_1", "#666");
		IGNORED.put("Complex_Valid_3", "#339");
		IGNORED.put("Complex_Valid_4", "#664");
		IGNORED.put("Complex_Valid_7", "WyTP#18");
		IGNORED.put("ConstrainedIntersection_Valid_1", "???");
		IGNORED.put("ConstrainedList_Valid_18", "#666");
		IGNORED.put("ConstrainedList_Valid_2", "#666");
		IGNORED.put("ConstrainedList_Valid_20", "#666 WyTP#20");
		IGNORED.put("ConstrainedList_Valid_21", "#WyTP#20");
		IGNORED.put("ConstrainedList_Valid_22", "#666");
		IGNORED.put("ConstrainedList_Valid_23", "#666");
		IGNORED.put("ConstrainedList_Valid_25", "WyTP#20");
		IGNORED.put("ConstrainedList_Valid_26", "#WyTP21");
		IGNORED.put("ConstrainedList_Valid_27", "#663");
		IGNORED.put("ConstrainedList_Valid_28", "timeout");
		IGNORED.put("ConstrainedList_Valid_6", "#666");
		IGNORED.put("ConstrainedList_Valid_8", "WyTP#20");
		IGNORED.put("ConstrainedNegation_Valid_1", "#342");
		IGNORED.put("ConstrainedNegation_Valid_2", "#342");
		IGNORED.put("ConstrainedRecord_Valid_8", "Issue ???");
		IGNORED.put("ConstrainedRecord_Valid_9", "Issue ???");
		IGNORED.put("ConstrainedRecord_Valid_10", "Issue ???");
		IGNORED.put("ConstrainedReference_Valid_1", "#468");
		IGNORED.put("DoWhile_Valid_4", "#681");
		IGNORED.put("Ensures_Valid_3", "Issue ???");
		IGNORED.put("Ensures_Valid_6", "timeout");
		IGNORED.put("Fail_Valid_3", "Issue ???");
		IGNORED.put("FunctionRef_Valid_4", "Issue ???");
		IGNORED.put("FunctionRef_Valid_10", "#298");
		IGNORED.put("FunctionRef_Valid_11", "#298");
		IGNORED.put("IfElse_Valid_4", "#298");
		IGNORED.put("Import_Valid_4", "#492");
		IGNORED.put("Import_Valid_5", "#492");
		IGNORED.put("Intersection_Valid_2", "Known Issue");
		IGNORED.put("Lambda_Valid_3", "#344");
		IGNORED.put("Lambda_Valid_4", "#344");
		IGNORED.put("Lambda_Valid_7", "#344");
		IGNORED.put("Lifetime_Lambda_Valid_4", "#298");
		IGNORED.put("ListAccess_Valid_6", "Known Issue");
		IGNORED.put("ListAssign_Valid_1", "#233");
		IGNORED.put("ListAssign_Valid_6", "#233");
		IGNORED.put("ListAssign_Valid_5", "#661");
		IGNORED.put("ListAssign_Valid_12", "#661");
		IGNORED.put("OpenRecord_Valid_5", "unknown");
		IGNORED.put("OpenRecord_Valid_6", "#664");
		IGNORED.put("OpenRecord_Valid_11", "#585");
		IGNORED.put("Process_Valid_1", "#291");
		IGNORED.put("Process_Valid_10", "#291");
		IGNORED.put("Process_Valid_9", "#231");
		IGNORED.put("Quantifiers_Valid_1", "WyTP#20");
		IGNORED.put("RecordCoercion_Valid_1", "#564");
		IGNORED.put("RecordSubtype_Valid_1", "Known Issue");
		IGNORED.put("RecordSubtype_Valid_2", "Known Issue");
		IGNORED.put("RecursiveType_Valid_11", "#298");
		IGNORED.put("RecursiveType_Valid_12", "#298");
		IGNORED.put("RecursiveType_Valid_14", "#298");
		IGNORED.put("RecursiveType_Valid_2", "#298");
		IGNORED.put("RecursiveType_Valid_10", "#298");
		IGNORED.put("RecursiveType_Valid_6", "#298");
		IGNORED.put("RecursiveType_Valid_8", "#298");
		IGNORED.put("RecursiveType_Valid_9", "#298");
		IGNORED.put("RecursiveType_Valid_21", "#298");
		IGNORED.put("RecursiveType_Valid_22", "#298");
		IGNORED.put("RecursiveType_Valid_24", "#298");
		IGNORED.put("RecursiveType_Valid_3", "#298");
		IGNORED.put("RecursiveType_Valid_4", "#298");
		IGNORED.put("RecursiveType_Valid_5", "#18");
		IGNORED.put("RecursiveType_Valid_7", "#298");
		IGNORED.put("Reference_Valid_2", "Unclassified");
		IGNORED.put("Reference_Valid_3", "Unclassified");
		IGNORED.put("Reference_Valid_6", "#553");
		IGNORED.put("Subtype_Valid_1", "#522");
		IGNORED.put("Subtype_Valid_10", "#522");
		IGNORED.put("Subtype_Valid_11", "#522");
		IGNORED.put("Subtype_Valid_12", "#522");
		IGNORED.put("Subtype_Valid_13", "#522");
		IGNORED.put("Subtype_Valid_14", "#522");
		IGNORED.put("Subtype_Valid_7", "#522");
		IGNORED.put("TypeEquals_Valid_19", "#663");
		IGNORED.put("TypeEquals_Valid_23", "Issue ???");
		IGNORED.put("TypeEquals_Valid_25", "#298");
		IGNORED.put("TypeEquals_Valid_3", "Issue ???");
		IGNORED.put("TypeEquals_Valid_24", "#681");
		IGNORED.put("TypeEquals_Valid_29", "#681");
		IGNORED.put("TypeEquals_Valid_34", "#681");
		IGNORED.put("TypeEquals_Valid_36", "Known Issue");
		IGNORED.put("TypeEquals_Valid_37", "Known Issue");
		IGNORED.put("TypeEquals_Valid_38", "Known Issue");
		IGNORED.put("TypeEquals_Valid_41", "Known Issue");
		IGNORED.put("TypeEquals_Valid_42", "#681");
		IGNORED.put("TypeEquals_Valid_47", "#681");
		IGNORED.put("TypeEquals_Valid_55", "WyTP#17");
		IGNORED.put("UnionType_Valid_23", "?");
		IGNORED.put("While_Valid_11", "timeout");
		IGNORED.put("While_Valid_15", "#681");
		IGNORED.put("While_Valid_16", "timeout");
		IGNORED.put("While_Valid_2", "???");
		IGNORED.put("While_Valid_20", "#664");
		IGNORED.put("While_Valid_22", "timeout");
		IGNORED.put("While_Valid_23", "WyTP#11");
		IGNORED.put("While_Valid_24", "WyTP#17");
		IGNORED.put("While_Valid_26", "timeout");
		IGNORED.put("While_Valid_27", "WyTP#12");
		IGNORED.put("While_Valid_3", "#666");
		IGNORED.put("While_Valid_33", "WyTP#20");
		IGNORED.put("While_Valid_34", "???");
		IGNORED.put("While_Valid_35", "WyTP#17");
		IGNORED.put("While_Valid_42", "timeout");
		IGNORED.put("While_Valid_47", "WyTP#20");
		IGNORED.put("While_Valid_5", "timeout");
		IGNORED.put("While_Valid_52", "WyTP#20");

		// Fails and was not listed as test case before parameterizing
		IGNORED.put("RecursiveType_Valid_28", "unknown");
		IGNORED.put("Function_Valid_11", "unknown");
		IGNORED.put("Function_Valid_15", "unknown");
	}

	/**
	 * The directory where compiler libraries are stored. This is necessary
	 * since it will contain the Whiley Runtime.
	 */
	public final static String WYC_LIB_DIR = "lib/".replace('/', File.separatorChar);

	//
	// Test Harness
	//

	/**
	 * Compile a syntactically invalid test case with verification enabled. The
	 * expectation is that compilation should fail with an error and, hence, the
	 * test fails if compilation does not.
	 *
	 * @param name
	 *            Name of the test to run. This must correspond to a whiley
	 *            source file in the <code>WHILEY_SRC_DIR</code> directory.
	 * @throws IOException
	 */
	protected void runTest(String name) throws IOException {
		// this will need to turn on verification at some point.
		name = WHILEY_SRC_DIR + File.separatorChar + name + ".whiley";

		Pair<Compile.Result,String> p = TestUtils.compile(
				WHILEY_SRC_DIR,      // location of source directory
				true,                // enable verification
				name);               // name of test to compile

		Compile.Result r = p.first();
		System.out.print(p.second());

		if (r != Compile.Result.SUCCESS) {
			fail("Test failed to compile!");
		} else if (r == Compile.Result.INTERNAL_FAILURE) {
			fail("Test caused internal failure!");
		}
	}

	// ======================================================================
	// Tests
	// ======================================================================

	// Parameter to test case is the name of the current test.
	// It will be passed to the constructor by JUnit.
	private final String testName;
	public AllValidVerificationTest(String testName) {
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
	public void validVerification() throws IOException {
		if (new File("../../running_on_travis").exists()) {
			System.out.println(".");
		}
		runTest(this.testName);
	}
}
