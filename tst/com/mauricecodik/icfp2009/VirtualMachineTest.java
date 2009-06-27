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
	public void testReadInt() {
		assertEquals(0, vm.readInt(new byte[] { 0, 0, 0, 0 }, 0));
		assertEquals(1, vm.readInt(new byte[] { 1, 0, 0, 0 }, 0, true));
		assertEquals(0xFFFFFFFFl, vm.readInt(new byte[] { -1, -1, -1, -1 } , 0));
		assertEquals(0x00030901l, vm.readInt(new byte[] { 0x01, 0x09, 0x03, 0x00 }, 0));
	}

	@Test
	public void testReadDouble() {
		assertEquals(12.345, vm.readDouble(new byte[] { 0x71, 0x3D, 0x0A, -41, -93, -80, 0x28, 0x40 }, 0));
	}
	
	@Test
	public void testOpcode() {
		assertEquals(0, vm.opcode(0x00000000));
		assertEquals(1, vm.opcode(0x10000000));
		assertEquals(0, vm.opcode(0x01400005));
	}
	
	@Test
	public void load() throws Exception {
		vm.load("problems/bin1.obf");
		
		assertEquals(0, vm.getProgram(0));
		System.out.println(Long.toHexString(vm.getProgram(1)));
		assertEquals(0x00030901l, vm.getProgram(1));
		
		assertEquals(0, vm.getData(0));
		assertEquals(0, vm.getData(1));
	}
	
	
}
