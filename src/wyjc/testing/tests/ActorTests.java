package wyjc.testing.tests;

import org.junit.Test;

import wyjc.testing.TestHarness;

public class ActorTests extends TestHarness {
	
	public ActorTests() {
		super("tests/actors/valid", "tests/actors/valid", "sysout");
	}
	
	@Test
	public void PrintAsync1() {
		runTest("PrintAsync1");
	}
	
	@Test
	public void PrintAsync2() {
		runTest("PrintAsync2");
	}
	
	@Test
	public void PrintSync1() {
		runTest("PrintSync1");
	}
	
	@Test
	public void PrintSync2() {
		runTest("PrintSync2");
	}
	
	@Test
	public void LocalMethod() {
	  runTest("LocalMethod");
	}
	
	@Test
	public void SelfMethod1() {
		runTest("SelfMethod1");
	}
	
	@Test
	public void SelfMethod2() {
		runTest("SelfMethod2");
	}
	
	@Test
	public void Variable1() {
		runTest("Variable1");
	}
	
	@Test
	public void Variable2() {
		runTest("Variable2");
	}
	
	@Test
	public void Parameter() {
		runTest("Parameter");
	}
	
	@Test
	public void While() {
		runTest("While");
	}
	
	@Test
	public void For() {
		runTest("For");
	}
	
	@Test
	public void TryCatch() {
		runTest("TryCatch");
	}
	
	@Test
	public void SyncFailure() {
		runTest("SyncFailure");
	}
	
	@Test
	public void BoolOp() {
		runTest("BoolOp");
	}
	
}
