import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.gpu.GpuCloudlet;
import org.cloudbus.cloudsim.gpu.GpuDatacenterBroker;
import org.cloudbus.cloudsim.gpu.GpuTask;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.VgpuTags;
import org.cloudbus.cloudsim.lists.VmList;

import de.vandermeer.asciitable.AsciiTable;

/**
 * This example demonstrates the use of gpu package in simulations. <br>
 * Performance Model: off <br>
 * Interference Model: off <br>
 * Power Model: off
 * 
 * @author Ahmad Siavashi
 * 
 */
public class TestesMultVM extends Testes {
	
	private static double [][]finishTimes = new double[4][200];
	private static int numberOfFiles = 200;
	private static int maxKernels = 256;
	private static int vmTotal = 8;
	private static int numOfVMs = 2;
	private static boolean differentVms = false;
	private static String dirFiles = "inputspermachine500";
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		
		/*for(numOfVMs = 2; numOfVMs <= vmTotal; numOfVMs*=2) {
			System.out.println("\n numvms="+numOfVMs);
			System.out.println("VMs\tVM0\tVM1\tVM2\tVM3\tVM4\tVM5\tVM6\tVM7");
			for(int k = 8; k <= maxKernels; k*=2) { // número de kernels
				String filename = dirFiles+"/"+numOfVMs+"_"+k+"_0.in";
				taskDistribution(filename);
			}
		}	
		
		for(numOfVMs = 2; numOfVMs <= vmTotal; numOfVMs*=2) {
			System.out.println("\n numvms="+numOfVMs);
			System.out.println("VMs\tVM0\tVM1\tVM2\tVM3\tVM4\tVM5\tVM6\tVM7");
			for(int k = 8; k <= maxKernels; k*=2) { // número de kernels
				String filename = dirFiles+"/"+numOfVMs+"_"+k+"_0.in";
				taskDistributionTime(filename);
			}
		}*/		
				
		for(numOfVMs = 2; numOfVMs <= vmTotal; numOfVMs*=2) {
			System.out.println("\n numvms="+numOfVMs);
			System.out.println("Dimension	FinishTime	AvgStartTime	AvgDurationTime	AvgEndTime	ProcessTime");
			for(int k = 8; k <= maxKernels; k*=2) { // número de kernels
				String filename = dirFiles+"/"+numOfVMs+"_"+k;
				execTimes(filename);
			}	
		}
		
		/*System.out.println("\n\nDimension	Speedup");
		for(int b = 32; b <= 32; b*=2) { // número máximo de blocos por kernel
			for(int k = 32; k <= maxKernels; k*=2) { // número de kernels
				String filename = b+"_"+k;
				execSpeedup(filename);
			}	
		}*/
		
		/*System.out.println("\n\nDimension	ANTT	STP");
		for(int b = 32; b <= 32; b*=2) { // número máximo de blocos por kernel
			for(int k = 32; k <= maxKernels; k*=2) { // número de kernels
				String filename = b+"_"+k;
				execMetrics(filename);
			}	
		}*/
	}
	
	private static void taskDistributionTime(String filename) {

		System.out.print(numOfVMs);
		Standard std = new Standard(numOfVMs, differentVms);
		std.run(filename);
		List<Cloudlet> stdGpuCloudlets = std.broker.getCloudletReceivedList();
		System.out.print("\tP");
		tasksTime(stdGpuCloudlets);
		
		Knapsack kna = new Knapsack(numOfVMs, differentVms);
		kna.run(filename);
		List<Cloudlet> moGpuCloudlets = kna.broker.getCloudletReceivedList();
		System.out.print("\tKna");
		tasksTime(moGpuCloudlets);
				
		MinMin minmin = new MinMin(numOfVMs, differentVms);
		minmin.run(filename);
		List<Cloudlet> minGpuCloudlets = minmin.broker.getCloudletReceivedList();
		System.out.print("\tMinMin");
		tasksTime(minGpuCloudlets);
		
		MinMinKnapsack minmink = new MinMinKnapsack(numOfVMs, differentVms);
		minmink.run(filename);
		List<Cloudlet> minkGpuCloudlets = minmink.broker.getCloudletReceivedList();
		System.out.print("\tMinKna");
		tasksTime(minkGpuCloudlets);
	}
	
	private static void tasksTime(List<Cloudlet> stdGpuCloudlets) {
		double []time = new double[numOfVMs];
		for(int i = 0; i < numOfVMs; i++)
			time[i] = 0;
		
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) stdGpuCloudlets) {
			
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			
			if(gpuTask.getFinishTime() > time[gpuCloudlet.getVmId()])
				time[gpuCloudlet.getVmId()] = gpuTask.getFinishTime();	
		}
		
		DecimalFormat dft = new DecimalFormat("###.##");
		
		for(int i = 0; i < numOfVMs; i++)
			System.out.print("\t" + dft.format(time[i]));
		System.out.print("\n");
		
	}

	private static void taskDistribution(String filename) {

		System.out.print(numOfVMs);
		Standard std = new Standard(numOfVMs, differentVms);
		std.run(filename);
		List<Cloudlet> stdGpuCloudlets = std.broker.getCloudletReceivedList();
		System.out.print("\tP");
		countTasks(stdGpuCloudlets);
		
		Knapsack kna = new Knapsack(numOfVMs, differentVms);
		kna.run(filename);
		List<Cloudlet> moGpuCloudlets = kna.broker.getCloudletReceivedList();
		System.out.print("\tKna");
		countTasks(moGpuCloudlets);
				
		MinMin minmin = new MinMin(numOfVMs, differentVms);
		minmin.run(filename);
		List<Cloudlet> minGpuCloudlets = minmin.broker.getCloudletReceivedList();
		System.out.print("\tMin");
		countTasks(minGpuCloudlets);
		
		MinMinKnapsack minmink = new MinMinKnapsack(numOfVMs, differentVms);
		minmink.run(filename);
		List<Cloudlet> minkGpuCloudlets = minmink.broker.getCloudletReceivedList();
		System.out.print("\tMinKna");
		countTasks(minkGpuCloudlets);
	}

	private static void countTasks(List<Cloudlet> stdGpuCloudlets) {
		int []count = new int[numOfVMs];
		for(int i = 0; i < numOfVMs; i++)
			count[i] = 0;
		
		for(Cloudlet c : stdGpuCloudlets) {
			count[c.getVmId()]++;
		}
		
		for(int i = 0; i < numOfVMs; i++)
			System.out.print("\t" + count[i]);
		System.out.print("\n");
	}


	
	private static void execTimes(String dimension) {
		
		ResultTime stdGlobalResultTime = new ResultTime();
		ResultTime moGlobalResultTime = new ResultTime();
		ResultTime minGlobalResultTime = new ResultTime();
		ResultTime minkGlobalResultTime = new ResultTime();
		
		for(int i = 0; i < numberOfFiles; i++) { // número de kernels
			
			String filename = dimension+"_"+i+".in";
			
			Standard std = new Standard(numOfVMs, differentVms);
			double stdProcessTime = std.run(filename);
			List<Cloudlet> stdGpuCloudlets = std.broker.getCloudletReceivedList();
			ResultTime stdResultLocal = calc(stdGpuCloudlets,i,0);
			stdResultLocal.processTime = stdProcessTime;
			stdGlobalResultTime.sum(stdResultLocal);

			Knapsack kna = new Knapsack(numOfVMs, differentVms);
			double moProcessTime = kna.run(filename);
			List<Cloudlet> moGpuCloudlets = kna.broker.getCloudletReceivedList();
			ResultTime moResultLocal = calc(moGpuCloudlets,i,1);
			moResultLocal.processTime = moProcessTime;
			moGlobalResultTime.sum(moResultLocal);
			
			MinMin minmin = new MinMin(numOfVMs, differentVms);
			double minProcessTime = minmin.run(filename);
			List<Cloudlet> minGpuCloudlets = minmin.broker.getCloudletReceivedList();
			ResultTime minResultLocal = calc(minGpuCloudlets,i,2);
			minResultLocal.processTime = minProcessTime;
			minGlobalResultTime.sum(minResultLocal);
			
			MinMinKnapsack minmink = new MinMinKnapsack(numOfVMs, differentVms);
			double minkProcessTime = minmink.run(filename);
			List<Cloudlet> minkGpuCloudlets = minmink.broker.getCloudletReceivedList();
			ResultTime minkResultLocal = calc(minkGpuCloudlets,i,3);
			minkResultLocal.processTime = minkProcessTime;
			minkGlobalResultTime.sum(minkResultLocal);
		}
		
		stdGlobalResultTime.div(numberOfFiles);
		moGlobalResultTime.div(numberOfFiles);
		minGlobalResultTime.div(numberOfFiles);
		minkGlobalResultTime.div(numberOfFiles);
		
		double finalMeanP = calcMean(finishTimes[0]);
		double finalMeanKna = calcMean(finishTimes[1]);
		double finalMeanMin = calcMean(finishTimes[2]);
		double finalMeanMinKna = calcMean(finishTimes[3]);
				
		DecimalFormat dft = new DecimalFormat("###.##");
		
		System.out.println("P"+dimension 
				+ "\t" + dft.format(stdGlobalResultTime.finishTime).toString()
				+ "\t" + dft.format(finalMeanP).toString()
//				 + "\t" + dft.format(stdGlobalResultTime.avgStartTime).toString()
//				 + "\t" + dft.format(stdGlobalResultTime.avgDurationTime).toString()
//				 + "\t" + dft.format(stdGlobalResultTime.avgEndTime).toString()
//				 + "\t" + dft.format(stdGlobalResultTime.processTime).toString());	
	);
		System.out.println("MT"+dimension 
				+ "\t" + dft.format(moGlobalResultTime.finishTime).toString()
				+ "\t" + dft.format(finalMeanKna).toString()
//				 + "\t" + dft.format(moGlobalResultTime.avgStartTime).toString()
//				 + "\t" + dft.format(moGlobalResultTime.avgDurationTime).toString()
//				 + "\t" + dft.format(moGlobalResultTime.avgEndTime).toString()
//				 + "\t" + dft.format(moGlobalResultTime.processTime).toString());	
	);
		System.out.println("Min"+dimension 
				+ "\t" + dft.format(minGlobalResultTime.finishTime).toString()
				+ "\t" + dft.format(finalMeanMin).toString()
				 //+ "\t" + dft.format(minGlobalResultTime.avgStartTime).toString()
				 //+ "\t" + dft.format(minGlobalResultTime.avgDurationTime).toString()
				 //+ "\t" + dft.format(minGlobalResultTime.avgEndTime).toString()
				 //+ "\t" + dft.format(minGlobalResultTime.processTime).toString());	
		);
		System.out.println("MinKna"+dimension 
				+ "\t" + dft.format(minkGlobalResultTime.finishTime).toString()
				+ "\t" + dft.format(finalMeanMinKna).toString()
//				 + "\t" + dft.format(minkGlobalResultTime.avgStartTime).toString()
//				 + "\t" + dft.format(minkGlobalResultTime.avgDurationTime).toString()
//				 + "\t" + dft.format(minkGlobalResultTime.avgEndTime).toString()
//				 + "\t" + dft.format(minkGlobalResultTime.processTime).toString());
				);
		
	}
	
	private static PerformanceMetrics calcMetrics(List<Cloudlet> stdGpuCloudlets, List<Cloudlet> moGpuCloudlets) {
		PerformanceMetrics pm = new PerformanceMetrics();
		
		for (GpuCloudlet stdCloudlet : (List<GpuCloudlet>) (List<?>) stdGpuCloudlets) {
			GpuTask stdGpuTask = stdCloudlet.getGpuTask();
			
			for (GpuCloudlet moCloudlet : (List<GpuCloudlet>) (List<?>) moGpuCloudlets) {
				GpuTask moGpuTask = moCloudlet.getGpuTask();
				
				if(moGpuTask.getCloudlet().getCloudletId() == stdGpuTask.getCloudlet().getCloudletId()) {
					double stdResp = stdGpuTask.getExecStartTime() + stdGpuTask.getActualGPUTime();
					double moResp = moGpuTask.getExecStartTime() + moGpuTask.getActualGPUTime();
					pm.antt += moResp/stdResp;
					
					pm.stp += stdResp/moResp;
					
					break;
				}
				
			}
		}
		
		pm.antt /= moGpuCloudlets.size();
		
		return pm;
		
	}

	
	private static ResultTime calc(List<Cloudlet> gpuCloudlets, int i, int j) {

		ResultTime r = new ResultTime();
		
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			
			r.avgStartTime += gpuTask.getExecStartTime();
			r.avgDurationTime += gpuTask.getActualGPUTime();
			r.avgEndTime += gpuTask.getFinishTime();
			
			if(gpuTask.getFinishTime() > r.finishTime)
				r.finishTime = gpuTask.getFinishTime();	
			
			//System.out.println(gpuTask.getExecStartTime());
		}
		
		r.avgStartTime /= gpuCloudlets.size();
		r.avgDurationTime /= gpuCloudlets.size();
		r.avgEndTime /= gpuCloudlets.size();
		
		finishTimes[j][i] = r.finishTime;
		
		return r;
	}
	
	private static PerformanceMetrics calcSpeedup(List<Cloudlet> stdGpuCloudlets, List<Cloudlet> moGpuCloudlets) {
		PerformanceMetrics pm = new PerformanceMetrics();
		
		for (GpuCloudlet stdCloudlet : (List<GpuCloudlet>) (List<?>) stdGpuCloudlets) {
			GpuTask stdGpuTask = stdCloudlet.getGpuTask();
			
			for (GpuCloudlet moCloudlet : (List<GpuCloudlet>) (List<?>) moGpuCloudlets) {
				GpuTask moGpuTask = moCloudlet.getGpuTask();
				
				if(moGpuTask.getCloudlet().getCloudletId() == stdGpuTask.getCloudlet().getCloudletId()) {
					double stdResp = stdGpuTask.getExecStartTime() + stdGpuTask.getActualGPUTime();
					double moResp = moGpuTask.getExecStartTime() + moGpuTask.getActualGPUTime();
					pm.antt += moResp/stdResp;
					
					pm.stp += stdResp/moResp;
					
					break;
				}
				
			}
		}
		
		pm.antt /= moGpuCloudlets.size();
		
		return pm;
		
	}
	
	private static void printGpuCloudletList(GpuDatacenterBroker broker) {
		// Print results when simulation is over
		List<Cloudlet> gpuCloudlets = broker.getCloudletReceivedList();
		
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		DecimalFormat dft = new DecimalFormat("###.##");
		
		AsciiTable gpu = new AsciiTable();
		gpu.addRule();
		gpu.addRow("Task ID", "Cloudlet ID", "Status", "vGPU Profile", "Time", "Start Time", "Finish Time", "GDDRAM");
		gpu.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			// Cloudlet
			GpuTask gpuTask = gpuCloudlet.getGpuTask();			
			if (gpuTask.getTaskStatus() == GpuTask.FINISHED) {
				gpu.addRow(gpuTask.getTaskId(), gpuTask.getCloudlet().getCloudletId(), "SUCCESS",
						VgpuTags.getVgpuTypeString(
								((GpuVm) VmList.getById(broker.getVmList(), gpuTask.getCloudlet().getVmId())).getVgpu().getType()),
						dft.format(gpuTask.getActualGPUTime()).toString(),
						dft.format(gpuTask.getExecStartTime()).toString(),
						dft.format(gpuTask.getFinishTime()).toString(),
						dft.format(gpuTask.getRequestedGddramSize()).toString());
				gpu.addRule();
			}
		}
		gpu.getContext().setWidth(100);
		Log.printLine(gpu.render());
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		
	}
	
	private static void printCloudletList(GpuDatacenterBroker broker) {
		// Print results when simulation is over
		List<Cloudlet> gpuCloudlets = broker.getCloudletReceivedList();
		
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		DecimalFormat dft = new DecimalFormat("###.##");
		// Host
		AsciiTable cpu = new AsciiTable();
		cpu.addRule();
		cpu.addRow("Cloudlet ID", "Status", "Datacenter ID", "VM ID", "Time", "Start Time", "Finish Time");
		cpu.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {

			if (gpuCloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				cpu.addRow(gpuCloudlet.getCloudletId(), "SUCCESS", gpuCloudlet.getResourceId(), gpuCloudlet.getVmId(),
						dft.format(gpuCloudlet.getActualCPUTime()).toString(),
						dft.format(gpuCloudlet.getExecStartTime()).toString(),
						dft.format(gpuCloudlet.getFinishTime()).toString());
				cpu.addRule();
			}
		}
		cpu.getContext().setWidth(100);
		Log.printLine(cpu.render());
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		
		// Host-Device Memory Transfer
		AsciiTable h2d = new AsciiTable();
		h2d.addRule();
		h2d.addRow("Direction", "Time", "Start Time", "End Time");
		h2d.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			h2d.addRow("H2D", dft.format(gpuTask.getMemoryTransferHostToDevice().getTime()).toString(),
					dft.format(gpuTask.getMemoryTransferHostToDevice().startTime).toString(),
					dft.format(gpuTask.getMemoryTransferHostToDevice().endTime).toString());
			h2d.addRule();
		}
		h2d.getContext().setWidth(100);
		Log.printLine(h2d.render());
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		
		AsciiTable gpu = new AsciiTable();
		gpu.addRule();
		gpu.addRow("Task ID", "Cloudlet ID", "Status", "vGPU Profile", "Time", "Start Time", "Finish Time", "GDDRAM");
		gpu.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			// Cloudlet
			GpuTask gpuTask = gpuCloudlet.getGpuTask();			
			if (gpuTask.getTaskStatus() == GpuTask.FINISHED) {
				gpu.addRow(gpuTask.getTaskId(), gpuTask.getCloudlet().getCloudletId(), "SUCCESS",
						VgpuTags.getVgpuTypeString(
								((GpuVm) VmList.getById(broker.getVmList(), gpuTask.getCloudlet().getVmId())).getVgpu().getType()),
						dft.format(gpuTask.getActualGPUTime()).toString(),
						dft.format(gpuTask.getExecStartTime()).toString(),
						dft.format(gpuTask.getFinishTime()).toString(),
						dft.format(gpuTask.getRequestedGddramSize()).toString());
				gpu.addRule();
			}
		}
		gpu.getContext().setWidth(100);
		Log.printLine(gpu.render());
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		
		
		AsciiTable d2h = new AsciiTable();
		d2h.addRule();
		d2h.addRow("Direction", "Time", "Start Time", "End Time");
		d2h.addRule();
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			// Device-Host Memory Transfer
			d2h.addRow("D2H", dft.format(gpuTask.getMemoryTransferDeviceToHost().getTime()).toString(),
					dft.format(gpuTask.getMemoryTransferDeviceToHost().startTime).toString(),
					dft.format(gpuTask.getMemoryTransferDeviceToHost().endTime).toString());
			d2h.addRule();
		}
		d2h.getContext().setWidth(100);
		Log.printLine(d2h.render());
		Log.printLine(String.join("", Collections.nCopies(100, "-")));

	}
	
}