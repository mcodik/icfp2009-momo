package com.mauricecodik.icfp2009;


import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VisualizerTest {

	Visualizer v;
	
	@Before
	public void setUp() throws Exception {
		v = new Visualizer(500, 0.5);
	}
	
	@After
	public void close() throws Exception {
		v.dispose();
	}
	
	@Test
	public void toPx() {
		assertEquals(0, v.toPx(-500));
		assertEquals(250, v.toPx(0));
		assertEquals(500, v.toPx(500));
	}

	@Test
	public void circles() throws Exception {
		v.addCircle(0, 0, 500);
		Thread.sleep(5000);
	}
}
