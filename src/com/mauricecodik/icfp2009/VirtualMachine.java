package com.mauricecodik.icfp2009;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import clojure.lang.IFn;

public class VirtualMachine {
	
	public static void main(String[] args) throws Exception {
		String filename = args[0];
		int configuration = Integer.parseInt(args[1]);
		
		VirtualMachine vm = new VirtualMachine();
		vm.load(filename);
		vm.run(configuration, 100, null);
	}
	
	public VirtualMachine() {
		input = new double[16385];
		output = new double[16385];
		data = new double[16385];
		program = new long[16385];
	}

	private double[] data; 
	private long[] program; 
	
	private double[] input;
	private double[] output;
	
	private int iteration;

	boolean status = false;
	
	private List<InputAction> trace;
	private int configuration;
	
	private class InputAction {
		int t;
		int addr;
		double value;
	}
	
	public int getCurrentIteration() {
		return iteration;
	}
	
	public void setInput(int addr, double value) {
		if (trace == null) {
			trace = new ArrayList<InputAction>();
		}
		
		input[addr] = value;
		
		InputAction ia = new InputAction();
		ia.t = iteration;
		ia.addr = addr;
		ia.value = value;
		
		trace.add(ia);
	}
	
	public void emitTrace(String filename) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
		try {
			byte[] buf = new byte[4096];
			ByteBuffer traceBuffer = ByteBuffer.wrap(buf);
			traceBuffer.order(ByteOrder.LITTLE_ENDIAN);
			traceBuffer.putInt(0xCAFEBABE);
			traceBuffer.putInt(668);
			traceBuffer.putInt(configuration);
			
			out.write(buf, 0, traceBuffer.position());
			
			traceBuffer.clear();
			
			List<InputAction> currentFrame = null; 
			for (int i = 0; i < trace.size(); i += currentFrame.size()) {
				int time = trace.get(i).t;
				traceBuffer.putInt(time);
				currentFrame = new ArrayList<InputAction>();
				currentFrame.add(trace.get(i));
				
				for (int k = i+1; k < trace.size(); k++) {
					InputAction ia = trace.get(k);
					if (ia.t == time) {
						currentFrame.add(ia);
					}
				}
				
				traceBuffer.putInt(currentFrame.size());
				for (InputAction cfia : currentFrame) {
					traceBuffer.putInt(cfia.addr);
					traceBuffer.putDouble(cfia.value);
				}
				
				out.write(buf, 0, traceBuffer.position());
				traceBuffer.clear();
			}
				
			traceBuffer.putInt(iteration+1);
			traceBuffer.putInt(0);
			out.write(buf, 0, traceBuffer.position());
			traceBuffer.clear();

			out.flush();
		}
		finally {
			out.close();
		}
	}
	
	public double getOutput(int addr) {
		return output[addr];
	}
	
	public void load(String filename) throws Exception {

		Arrays.fill(data, 0.0);
		Arrays.fill(program, 0l);
		
		BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filename));
		
		byte[] buf = new byte[12];
		int frame = 0;
		try {
			while (inputStream.read(buf, 0, 12) > 0) {
				
				if (frame > data.length) {
					throw new Exception("reading more than " + data.length + " frames?");
				}
				
				int instruction;
				double datum;
				
				ByteBuffer bb = ByteBuffer.wrap(buf);
				bb.order(ByteOrder.LITTLE_ENDIAN);
				
				if (frame % 2 == 0) {
					datum = bb.getDouble();

//					//System.out.println("read double: " + datum);
					instruction = bb.getInt();

					////System.out.println("read int: " + Integer.toHexString(instruction));
				}
				else {
					instruction = bb.getInt();
					////System.out.println("read int: " + Integer.toHexString(instruction));

					datum = bb.getDouble();

					////System.out.println("read double: " + datum);
				}
				
				data[frame] = datum;
				program[frame] = instruction;
				
				frame++;
				
				Arrays.fill(buf, (byte)0);
			}
		} 
		finally {
			inputStream.close();
		}
		
		//System.out.println("loaded " + frame + " frames from " + filename);
	}
	
	public void run(int configuration, int maxIterations, IFn callback) {

		this.configuration = configuration;
		
		Arrays.fill(input, 0.0);
		Arrays.fill(output, 0.0);

		setInput(0x3e80, configuration);
		
		//System.out.println("Running configuration " + configuration);
		
		for (iteration = 0; iteration < maxIterations; iteration++) {
			
			runIteration();
			
			if (getOutput(0) != 0.0) {
				System.err.println("Configuration " + configuration + " done after " + iteration + " iterations, score: " + getOutput(0));
				try {
					emitTrace("traces/" + configuration + "-" + getOutput(0) + ".trace");
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
			else {
				try {
					if (callback != null) {
						callback.invoke(this);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}

			}
		}
		
		System.err.println("Aborting configuration " + configuration + " after max " + iteration + " iterations");
	}
	
	
	
	public int opcode(long instruction) {
		return (int)((instruction >> 28) & 0xF);
	}
	
	public void runIteration() {
		for (int pc = 0; pc < program.length; pc++) {
			long instruction = program[pc];

			int opcode = opcode(instruction);
			if (opcode == 0) {
				
				int immediate = (int)((instruction >> 14)&1023);
				int addr = (int)(instruction & 16383);
				int sopcode = (int)((instruction >> 24)&15); 
				
				runSOp(pc, sopcode, immediate, addr);
			}
			else {
				
				int addr2 = (int)(instruction & 16383);
				int addr1 = (int)((instruction >> 14) & 16383);
				
				runDOp(pc, opcode, addr1, addr2);
			}
		}
	}
	
	private void runSOp(int pc, int opcode, int immediate, int addr) {
		try {
			if (opcode == 0) {
				if (program[pc] != 0) {
					//System.out.println(Long.toHexString(program[pc]) + ": NOOP");
				}
				return; 
			}
			
			if (opcode == 1) { // CMP
				int comparator = (int)((immediate>>7)&7);
				if (comparator == 0) {
					status = (data[addr] < 0);
					//System.out.println(Long.toHexString(program[pc]) + ": mem[" + addr + "] < 0 == " + status);
				}
				else if (comparator == 1) {
					status = (data[addr] <= 0);
					//System.out.println(Long.toHexString(program[pc]) + ": mem[" + addr + "] <= 0 == " + status);
				}
				else if (comparator == 2) {
					status = (data[addr] == 0);
					//System.out.println(Long.toHexString(program[pc]) + ": mem[" + addr + "] == 0 == " + status);
				}
				else if (comparator == 3) {
					status = (data[addr] >= 0);
					//System.out.println(Long.toHexString(program[pc]) + ": mem[" + addr + "] >= 0 == " + status);
				}
				else if (comparator == 1) {
					status = (data[addr] > 0);
					//System.out.println(Long.toHexString(program[pc]) + ": mem[" + addr + "] > 0 == " + status);
				}
				else {
					throw new RuntimeException("Bad CMP instruction: opcode = " + opcode + "; imm = " + immediate + "; addr = " + addr);
				}
				return;
			}
			
			if (opcode == 2) {
				data[pc] = Math.sqrt( data[addr] );
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- sqrt(mem[" + addr + "]) == " + data[pc]);
				return;
			}
			
			if (opcode == 3) {
				data[pc] = data[addr];
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr + "] == " + data[pc]);
				return;
			}
			
			if (opcode == 4) {
				data[pc] = input[addr];
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- input[" + addr + "] == " + data[pc]);
				return;
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("Out of bounds while running: opcode = " + opcode + "; imm = " + immediate + "; addr = " + addr, e);
		}
		throw new RuntimeException("Bad S op: opcode = " + opcode + "; imm = " + immediate + "; addr = " + addr);
	}
	
	private void runDOp(int pc, int opcode, int addr1, int addr2) {
		try { 
			if (opcode == 1) {
				data[pc] = data[addr1] + data[addr2];
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] + mem[" + addr2 + "] == " + data[pc]);
				return;
			}
			
			if (opcode == 2) {
				data[pc] = data[addr1] - data[addr2];
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] - mem[" + addr2 + "] == " + data[pc]);
				return;
			}
			
			if (opcode == 3) {
				data[pc] = data[addr1] * data[addr2];
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] * mem[" + addr2 + "] == " + data[pc]);
				return;
			}
			
			if (opcode == 4) {
				
				if (data[addr2] == 0.0) {
					data[pc] = 0.0;
				}
				else {
					data[pc] = data[addr1] / data[addr2];
				}
				
				//System.out.println(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] / mem[" + addr2 + "] == " + data[pc]);
				return;
			}
			
			if (opcode == 5) {
				output[addr1] = data[addr2];
				//System.out.println(Long.toHexString(program[pc]) + ": output[" + addr1 + "] <- mem[" + addr1 + "] == " + output[addr1]);
				return;
			}
			
			if (opcode == 6) {
				
				int src = status ? addr1 : addr2;
				data[pc] = data[src];
	
				//System.out.println(Long.toHexString(program[pc]) + ": if " + status + ": mem[" + pc + "] <- mem[" + src + "] == " + data[pc]);
				return;			
			}
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new RuntimeException("Out of bounds while running: opcode = " + opcode + "; addr1 = " + addr1 + "; addr2 = " + addr2, e);
		}
		
		throw new RuntimeException("Bad D instruction: opcode = " + opcode + "; addr1 = " + addr1 + "; addr2 = " + addr2);
	}
}
