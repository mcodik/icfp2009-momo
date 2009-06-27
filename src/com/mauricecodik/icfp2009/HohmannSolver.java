package com.mauricecodik.icfp2009;

import clojure.lang.AFn;

public class HohmannSolver {
	
	public static void main(String[] args) throws Exception {
		String filename = "problems/bin1.obf";
		int configuration = 1001;
		
		final Visualizer vz = new Visualizer(500, 1.0/100000.0);
		final VirtualMachine vm = new VirtualMachine();

		final HohmannSolver hs = new HohmannSolver();
		
		AFn callback = new AFn() {
			@Override
			public Object invoke(Object arg1) throws Exception {
				double target = vm.getOutput(4);
				double mex = vm.getOutput(2);
				double mey = vm.getOutput(3);

				vz.addCircle(0, 0, target);
				vz.addCircle(0, 0, 6357000.0);
				
				if (vm.getCurrentIteration() % 50 == 0) {
					vz.addPoint("me", mex, mey);
					vz.repaint();
				}
				
				hs.computeStep(vm);
				
				return null;
			}
		};
		
		vm.load(filename);
		vm.run(configuration, 10000, callback);
	}

	protected void computeStep(VirtualMachine vm) {
		
		double mex = vm.getOutput(2);
		double mey = vm.getOutput(3);
		double target = vm.getOutput(4);
		
		if (vm.getCurrentIteration() == 3) {
			//vm.setInput(2, 10.21);
			vm.setInput(3, 2466);
		}
		else if (vm.getCurrentIteration() == 4) {
			vm.setInput(2, 0);
			vm.setInput(3, 0);
		}
	}

}
