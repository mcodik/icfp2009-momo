package com.mauricecodik.icfp2009;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class VirtualMachineTest {
	
	VirtualMachine vm;
	
	@Before
	public void setup() {
		vm = new VirtualMachine();
	}

	
	@Test
	public void testOpcode() {
		assertEquals(0, vm.opcode(0x00000000));
		assertEquals(1, vm.opcode(0x10000000));
		assertEquals(0, vm.opcode(0x01400005));
	}
	
	
	
	
}
