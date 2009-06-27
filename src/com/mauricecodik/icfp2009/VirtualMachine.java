package com.mauricecodik.icfp2009;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.logging.Logger;

public class VirtualMachine {
	
	private static final Logger log = Logger.getLogger("VirtualMachine");
	
	public static void main(String[] args) throws Exception {
		String filename = args[0];
		long configuration = Long.parseLong(args[1]);
		
		VirtualMachine vm = new VirtualMachine();
		vm.load(filename);
		vm.run(configuration);
	}
	
	public VirtualMachine() {
		input = new double[16385];
		output = new double[16385];
		data = new double[16385];
		program = new long[16385];
	}

	public double[] data; 
	public long[] program; 
	
	public double[] input;
	public double[] output;

	boolean status = false;
	
	public void load(String filename) throws Exception {

		Arrays.fill(data, 0.0);
		Arrays.fill(program, 0l);
		
		BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(filename));
		
		byte[] buf = new byte[12];
		int frame = 0;
		try {
			int read;
			while ((read = inputStream.read(buf, 0, 12)) > 0) {
				
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < read; i++) {
					sb.append(Integer.toHexString(0x000000FF & buf[i])).append(" ");
				}
				
				log.info("read " + read + " bytes: " + sb.toString());
				
				if (frame > data.length) {
					throw new Exception("reading more than " + data.length + " frames?");
				}
				
				long instruction;
				double datum;
				if (frame % 2 == 0) {
					datum = readDouble(buf, 0);
					instruction = readInt(buf, 8, true);
				}
				else {
					instruction = readInt(buf, 0,true);
					datum = readDouble(buf, 4);
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
		
		log.info("loaded " + frame + " frames from " + filename);
	}
	
	public double readDouble(byte[] buf, int startOffset) {
		long h = 0x000000FF & ((int)buf[startOffset]);
		long g = 0x000000FF & ((int)buf[startOffset+1]);
		long f = 0x000000FF & ((int)buf[startOffset+2]);
		long e = 0x000000FF & ((int)buf[startOffset+3]);
		long d = 0x000000FF & ((int)buf[startOffset+4]);
		long c = 0x000000FF & ((int)buf[startOffset+5]);
		long b = 0x000000FF & ((int)buf[startOffset+6]);
		long a = 0x000000FF & ((int)buf[startOffset+7]);
		
		long i = (a << 56) | (b << 48) | (c << 40) | (d << 32) 
		 			| (e << 24) | (f << 16) | (g << 8) | h;
		
		double doub = Double.longBitsToDouble(i);
		log.info("read double: " + Long.toHexString(i) + " " + doub);
		return doub;
	}

	public long readInt(byte[] buf, int startOffset) {
		return readInt(buf, startOffset, false);
	}
	
	public long readInt(byte[] buf, int startOffset, boolean loud) {
		long d = 0x000000FF & ((int)buf[startOffset]);
		long c = 0x000000FF & ((int)buf[startOffset+1]);
		long b = 0x000000FF & ((int)buf[startOffset+2]);
		long a = 0x000000FF & ((int)buf[startOffset+3]);

		long e = 0xFFFFFFFF & ((a << 24) | (b << 16) | (c << 8) | d);
		if (loud) {
			log.info("read int: " + Long.toHexString(e) + " " + e);
			log.info("   read int: d = " + Long.toHexString(d));
			log.info("   read int: c = " + Long.toHexString(c));
			log.info("   read int: b = " + Long.toHexString(b));
			log.info("   read int: a = " + Long.toHexString(a));
		}
		return e;
	}

	public double getData(int indx) {
		return data[indx];
	}
	
	public long getProgram(int indx) {
		return program[indx];
	}
	
	public void run(long configuration) {

		Arrays.fill(input, 0.0);
		Arrays.fill(output, 0.0);

		input[0x3e80] = configuration;
		
		log.info("Running configuration " + configuration);
		
		for (int iteration = 0; iteration < 1000; iteration++) {
			runIteration();
		}
	}
	
	public int opcode(long instruction) {
		return (int)((instruction >> 28) & 0xF);
	}
	
	public void runIteration() {
		for (int pc = 0; pc < program.length; pc++) {
			long instruction = program[pc];
			//log.info("running: " + Long.toHexString(instruction));

			int opcode = opcode(instruction); 
			if (opcode == 0) {
				
				int immediate = (int)((instruction >> 14)&2048);
				int addr = (int)(instruction & 32767);
				
				runSOp(pc, opcode, immediate, addr);
			}
			else {
				
				int addr1 = (int)(instruction & 32767);
				int addr2 = (int)((instruction >> 14) & 32767);
				
				runDOp(pc, opcode, addr1, addr2);
			}
		}
	}
	
	private void runSOp(int pc, int opcode, int immediate, int addr) {
		if (opcode == 0) {
			log.info(Long.toHexString(program[pc]) + ": NOOP");
			return; 
		}
		
		if (opcode == 1) { // CMP
			if (immediate == 0) {
				status = (data[addr] < 0);
				log.info(Long.toHexString(program[pc]) + ": mem[" + addr + "] < 0 == " + status);
			}
			else if (immediate == 1) {
				status = (data[addr] <= 0);
				log.info(Long.toHexString(program[pc]) + ": mem[" + addr + "] <= 0 == " + status);
			}
			else if (immediate == 2) {
				status = (data[addr] == 0);
				log.info(Long.toHexString(program[pc]) + ": mem[" + addr + "] == 0 == " + status);
			}
			else if (immediate == 3) {
				status = (data[addr] >= 0);
				log.info(Long.toHexString(program[pc]) + ": mem[" + addr + "] >= 0 == " + status);
			}
			else if (immediate == 1) {
				status = (data[addr] > 0);
				log.info(Long.toHexString(program[pc]) + ": mem[" + addr + "] > 0 == " + status);
			}
			else {
				throw new RuntimeException("Bad CMP instruction: opcode = " + opcode + "; imm = " + immediate + "; addr = " + addr);
			}
			return;
		}
		
		if (opcode == 2) {
			data[pc] = Math.sqrt( data[addr] );
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- sqrt(mem[" + addr + "]) == " + data[pc]);
			return;
		}
		
		if (opcode == 3) {
			data[pc] = data[addr];
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr + "] == " + data[pc]);
			return;
		}
		
		if (opcode == 4) {
			data[pc] = input[addr];
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- input[" + addr + "] == " + data[pc]);
			return;
		}
		
		throw new RuntimeException("Bad S op: opcode = " + opcode + "; imm = " + immediate + "; addr = " + addr);
	}
	
	private void runDOp(int pc, int opcode, int addr1, int addr2) {
		
		if (opcode == 1) {
			data[pc] = data[addr1] + data[addr2];
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] + mem[" + addr2 + "] == " + data[pc]);
			return;
		}
		
		if (opcode == 2) {
			data[pc] = data[addr1] - data[addr2];
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] - mem[" + addr2 + "] == " + data[pc]);
			return;
		}
		
		if (opcode == 3) {
			data[pc] = data[addr1] * data[addr2];
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] * mem[" + addr2 + "] == " + data[pc]);
			return;
		}
		
		if (opcode == 4) {
			
			if (data[addr2] == 0.0) {
				data[pc] = 0.0;
			}
			else {
				data[pc] = data[addr1] / data[addr2];
			}
			
			log.info(Long.toHexString(program[pc]) + ": mem[" + pc + "] <- mem[" + addr1 + "] / mem[" + addr2 + "] == " + data[pc]);
			return;
		}
		
		if (opcode == 5) {
			output[addr1] = data[addr2];
			log.info(Long.toHexString(program[pc]) + ": output[" + addr1 + "] <- mem[" + addr1 + "] == " + output[addr1]);
			return;
		}
		
		if (opcode == 6) {
			
			int src = status ? addr1 : addr2;
			data[pc] = data[src];

			log.info(Long.toHexString(program[pc]) + ": if " + status + ": data[" + pc + "] <- mem[" + src + "] == " + data[pc]);
			return;			
		}
		
		throw new RuntimeException("Bad D instruction: opcode = " + opcode + "; addr1 = " + addr1 + "; addr2 = " + addr2);
	}
}
