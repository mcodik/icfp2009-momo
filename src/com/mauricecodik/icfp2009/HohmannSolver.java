package com.mauricecodik.icfp2009;

import clojure.lang.AFn;

public class HohmannSolver {
	
	static VirtualMachine vm = new VirtualMachine();
	
	public static void main(String[] args) throws Exception {
		String filename = "problems/bin1.obf";
		int configuration = 1001;
		
		final HohmannSolver hs = new HohmannSolver();
		
		AFn callback = new AFn() {
			@Override
			public Object invoke(Object arg1) throws Exception {
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
			//vm.setInput(2, 0);
			vm.setInput(3, 0);
		}
	}

}
