import java.text.DecimalFormat;
import java.util.Arrays;
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
public class Testes {


	public static class ResultTime {
		public double avgStartTime;
		public double finishTime;
		public double avgDurationTime;
		public double avgEndTime;
		public double processTime;
		
		public ResultTime() {
			this.avgStartTime = 0;
			this.finishTime = 0;
			this.avgDurationTime = 0;
			this.avgEndTime = 0;
			this.processTime = 0;
		}	
		
		public void sum(ResultTime stdResultLocal) {
			avgStartTime += stdResultLocal.avgStartTime;
			finishTime += stdResultLocal.finishTime;
			avgDurationTime += stdResultLocal.avgDurationTime;
			avgEndTime += stdResultLocal.avgEndTime;
			processTime += stdResultLocal.processTime;
		}
		
		public void div(int n) {
			avgStartTime /= n;
			finishTime /= n;
			avgDurationTime /= n;
			avgEndTime /= n;
			processTime /= n;
		}
	}
	
	public static class PerformanceMetrics {
		public double antt;
		public double stp;
		
		public PerformanceMetrics() {
			this.antt = 0;
			this.stp = 0;
		}	
		
		public void sum(PerformanceMetrics pm) {
			antt += pm.antt;
			stp += pm.stp;
		}
		
		public void div(int n) {
			antt /= n;
			stp /= n;
		}
	} 
	
	private static double [][]finishTimes = new double[2][300]; 
	private static int numberOfFiles = 300;
	private static int maxKernels = 256;
	private static String dirFiles = "inputspermachine500";
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		
		System.out.println("Dimension	FinishTime	AvgStartTime	AvgDurationTime	AvgEndTime	ProcessTime");
		
		for(int k = 8; k <= maxKernels; k*=2) { // número de kernels
			String filename = dirFiles+"/"+1+"_"+k;
			execTimes(filename);
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
	
	public static double stdev(double[] list, double mean) {
		
		double num = 0.0;
		double numi = 0.0;

		
		for (double value : list) {
			numi = Math.pow(((double) value - mean), 2);
			num += numi;
		}
		
		return Math.sqrt(num / list.length);
	}
	
	public static double mean(double[] list, int inicio, int fim) {
		double sum = 0.0;

		for (int i = inicio; i < fim; i++) {
			sum += list[i]/(fim-inicio);
		}
	
		return sum;// / fim-inicio;
	}
	
	public static int findIndex(double [] list, double mean) {
		
		int currentIndex = 0;
		
		for (int i = 0; i < list.length; i++) {
			if(list[i] <= mean && list[i] > list[currentIndex]) {
				currentIndex = i;
			}
		}
		
		return currentIndex;
	}
	
	public static double calcMean(double []finishTimes) {
		double mean = mean(finishTimes, 0, finishTimes.length);
		//int indexMean = findIndex(finishTimes, mean);
		double std = stdev(finishTimes, mean);
		Arrays.sort(finishTimes);
		int indexStdMenor = findIndex(finishTimes, mean-3*std);
		int indexStdMaior = findIndex(finishTimes, mean+3*std);
		
		return mean(finishTimes,indexStdMenor,indexStdMaior);
	}
	
	private static void execTimes(String dimension) {
		
		ResultTime stdGlobalResultTime = new ResultTime();
		ResultTime moGlobalResultTime = new ResultTime();
		
		//System.out.println("P"+dimension);
		for(int i = 0; i < numberOfFiles; i++) { // número de kernels
			
			String filename = dimension+"_"+i+".in";
						
			Standard std = new Standard();
			double stdProcessTime = std.run(filename);
			List<Cloudlet> stdGpuCloudlets = std.broker.getCloudletReceivedList();
			ResultTime stdResultLocal = calc(stdGpuCloudlets,i,0);
			stdResultLocal.processTime = stdProcessTime;
			stdGlobalResultTime.sum(stdResultLocal);

			Knapsack kna = new Knapsack();
			double moProcessTime = kna.run(filename);
			List<Cloudlet> moGpuCloudlets = kna.broker.getCloudletReceivedList();
			ResultTime moResultLocal = calc(moGpuCloudlets,i,1);
			moResultLocal.processTime = moProcessTime;
			moGlobalResultTime.sum(moResultLocal);
		}
		
		double finalMeanP = calcMean(finishTimes[0]);
		double finalMeanKna = calcMean(finishTimes[1]);
				
		stdGlobalResultTime.div(numberOfFiles);
		moGlobalResultTime.div(numberOfFiles);
				
		DecimalFormat dft = new DecimalFormat("###.##");
		
		System.out.println("P"+dimension + "\t" + dft.format(stdGlobalResultTime.finishTime).toString()
				+ "\t" + dft.format(finalMeanP).toString()
				 + "\t" + dft.format(stdGlobalResultTime.avgStartTime).toString()
				 + "\t" + dft.format(stdGlobalResultTime.avgDurationTime).toString()
				 + "\t" + dft.format(stdGlobalResultTime.avgEndTime).toString()
				 + "\t" + dft.format(stdGlobalResultTime.processTime).toString());	
		
		System.out.println("MT"+dimension + "\t" + dft.format(moGlobalResultTime.finishTime).toString()
				+ "\t" + dft.format(finalMeanKna).toString()
				 + "\t" + dft.format(moGlobalResultTime.avgStartTime).toString()
				 + "\t" + dft.format(moGlobalResultTime.avgDurationTime).toString()
				 + "\t" + dft.format(moGlobalResultTime.avgEndTime).toString()
				 + "\t" + dft.format(moGlobalResultTime.processTime).toString());	

		
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
			
		}
		
		r.avgStartTime /= gpuCloudlets.size();
		r.avgDurationTime /= gpuCloudlets.size();
		r.avgEndTime /= gpuCloudlets.size();

		finishTimes[j][i] = r.finishTime;
		//System.out.println(r.finishTime);
		
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