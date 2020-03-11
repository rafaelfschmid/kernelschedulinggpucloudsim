import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudbus.cloudsim.gpu.GpuCloudlet;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.VideoCardTags;

import escalonador.algorithm.knapsack.DynamicKnapsack;
import escalonador.algorithm.knapsack.KnapsackResource;
import escalonador.algorithm.knapsack.MemoryDynamicKnapsack;

/**
 * This example demonstrates the use of gpu package in simulations. <br>
 * Performance Model: off <br>
 * Interference Model: off <br>
 * Power Model: off
 * 
 * @author Ahmad Siavashi
 * 
 */
public class Knapsack extends Scheduler {
	
	public Knapsack(int numVms, boolean differentVms) {
		super(numVms, differentVms);
	}
	
	public Knapsack() {
		super();
	}

	protected void schedule() {
		scheduleMultipleVms();
	}

	protected double expectedTime(GpuCloudlet gpuCloudlet, GpuVm vm) {
		double gpuTime = ((double)gpuCloudlet.getGpuTask().getBlockLength()  /  vm.getVgpu().getPeMips());
		//double gpuTime = ((double)gpuCloudlet.getGpuTask().getTaskTotalLength()  / vm.getVgpu().getPeMips());
				//(double)VideoCardTags.NVIDIA_K1_CARD_PE_MIPS);
	
		return gpuTime;
	}

	protected void scheduleMultipleVms() {
		List<GpuCloudlet> cloudletListAux = new ArrayList<GpuCloudlet>();
		cloudletListAux.addAll(cloudletList);
		cloudletList.clear();
		
		int[] availableResources = new int[vmlist.size()];
		for(int vmId = 0; vmId < vmlist.size(); vmId++) {
			availableResources[vmId] = 32;
		}

		double currentTime = 0;
		List<GpuCloudlet> cloudletListExecuting = new ArrayList<GpuCloudlet>();
		
		scheduleInitial(cloudletListAux, availableResources, currentTime, cloudletListExecuting);
		
		scheduleRound(cloudletListAux, availableResources, currentTime, cloudletListExecuting);
	}
	
	private void scheduleInitial(List<GpuCloudlet> cloudletListAux, int[] availableResources, double currentTime,
			List<GpuCloudlet> cloudletListExecuting) {
		for(int vmId = 0; vmId < vmlist.size() && cloudletListAux.size() > 0; vmId++) {
			
			KnapsackResource resource = new KnapsackResource(availableResources[vmId]);
			float[] values = new float[cloudletListAux.size()];
			KnapsackResource[] weights = new KnapsackResource[cloudletListAux.size()];
			int k = 0;
			for(GpuCloudlet c : cloudletListAux) {
				values[k] = (float) (c.getGpuTask().getNumberOfBlocks() / ((c.getGpuTask().getBlockLength()/VideoCardTags.NVIDIA_K1_CARD_PE_MIPS)*32/100));
				weights[k] = new KnapsackResource(c.getGpuTask().getNumberOfBlocks());
				k++;
			}
			
			DynamicKnapsack knapsack = new MemoryDynamicKnapsack(weights, resource, values);
			int[] scheduledKernelIndexs = knapsack.execute();
			
			List<GpuCloudlet> cloudletToRemove = new ArrayList<GpuCloudlet>();
			// GpuCloudlet-VM assignment
			for (int i = 0; i < scheduledKernelIndexs.length; i++) {
				GpuCloudlet gpuCloudlet = cloudletListAux.get(scheduledKernelIndexs[i]);
				cloudletToRemove.add(gpuCloudlet);
				
				double startTime = currentTime + expectedTime(gpuCloudlet, vmlist.get(vmId));
													
				gpuCloudlet.getGpuTask().setExecStartTime(startTime);
				
				availableResources[vmId] -= gpuCloudlet.getGpuTask().getNumberOfBlocks(); //weights[scheduledKernelIndexs[i]].value(0);
				gpuCloudlet.setVmId(vmId);
			}

			cloudletListAux.removeAll(cloudletToRemove);
			cloudletList.addAll(cloudletToRemove);
			cloudletListExecuting.addAll(cloudletToRemove);
			
			// Escalona se houver algum SM livre.
			if(availableResources[vmId] > 0 && cloudletListAux.size() != 0) {
				
				double maxValue = 0;
				int maxIndex = 0;
				for (int i = 0; i < cloudletListAux.size(); i++) {
					GpuCloudlet c = cloudletListAux.get(i);
					
					double currentValue = c.getGpuTask().getNumberOfBlocks();
					
					if(currentValue > maxValue) {
						maxValue = currentValue;
						maxIndex = i;
					}
				}
				
				GpuCloudlet gpuCloudlet = cloudletListAux.get(maxIndex);
									
				double startTime = currentTime + expectedTime(gpuCloudlet, vmlist.get(vmId));
				gpuCloudlet.getGpuTask().setExecStartTime(startTime);

				availableResources[vmId] -= gpuCloudlet.getGpuTask().getNumberOfBlocks();
				gpuCloudlet.setVmId(vmId);
				
				cloudletListAux.remove(gpuCloudlet);
				cloudletList.add(gpuCloudlet);
				cloudletListExecuting.add(gpuCloudlet);
			}
		}
	}

	private void scheduleRound(List<GpuCloudlet> cloudletListAux, int[] availableResources, double currentTime,
			List<GpuCloudlet> cloudletListExecuting) {
		int vmId = 0;
		
		while(cloudletListAux.size() != 0) {
			// Next finished task
			double mintime = Double.MAX_VALUE;
			int minIndex = 0;
			int m = 0;
			for(GpuCloudlet c : cloudletListExecuting) {
				if(c.getGpuTask().getExecStartTime() < mintime) {
					mintime = c.getGpuTask().getExecStartTime();
					minIndex = m;
					vmId = c.getVmId(); 
				}
				m++;
			}

			currentTime = mintime;
			
			availableResources[vmId] += cloudletListExecuting.get(minIndex).getGpuTask().getNumberOfBlocks();
			cloudletListExecuting.remove(minIndex);
			
			// verifica se tem mais q 2 SMs livres, 
			// pois se tiver somente 1 livre não precisa rodar a mochila 
			if(availableResources[vmId] > 1) {
			
				KnapsackResource resource = new KnapsackResource(availableResources[vmId]);
				float[] values = new float[cloudletListAux.size()];
				KnapsackResource[] weights = new KnapsackResource[cloudletListAux.size()];
				int k = 0;
				for(GpuCloudlet c : cloudletListAux) {
					values[k] = (float) (c.getGpuTask().getNumberOfBlocks() / ((c.getGpuTask().getBlockLength()/VideoCardTags.NVIDIA_K1_CARD_PE_MIPS)*32/100));
					weights[k] = new KnapsackResource(c.getGpuTask().getNumberOfBlocks());
					k++;
				}
				
				DynamicKnapsack knapsack = new MemoryDynamicKnapsack(weights, resource, values);
				int[] scheduledKernelIndexs = knapsack.execute();
				
				List<GpuCloudlet> cloudletToRemove = new ArrayList<GpuCloudlet>();
				// GpuCloudlet-VM assignment
				for (int i = 0; i < scheduledKernelIndexs.length; i++) {
					GpuCloudlet gpuCloudlet = cloudletListAux.get(scheduledKernelIndexs[i]);
					cloudletToRemove.add(gpuCloudlet);
					
					double startTime = currentTime + expectedTime(gpuCloudlet, vmlist.get(vmId));
					gpuCloudlet.getGpuTask().setExecStartTime(startTime);
					
					availableResources[vmId] -= gpuCloudlet.getGpuTask().getNumberOfBlocks(); //weights[scheduledKernelIndexs[i]].value(0);
					gpuCloudlet.setVmId(vmId);
				}
				cloudletListAux.removeAll(cloudletToRemove);
				cloudletList.addAll(cloudletToRemove);
				cloudletListExecuting.addAll(cloudletToRemove);
			}
				
			// Escalona se houver algum SM livre.
			if(availableResources[vmId] > 0 && cloudletListAux.size() != 0) {
				
				double maxValue = 0;
				int maxIndex = 0;
				for (int i = 0; i < cloudletListAux.size(); i++) {
					GpuCloudlet c = cloudletListAux.get(i);
					
					double currentValue = c.getGpuTask().getNumberOfBlocks();
					
					if(currentValue > maxValue) {
						maxValue = currentValue;
						maxIndex = i;
					}
				}
				
				GpuCloudlet gpuCloudlet = cloudletListAux.get(maxIndex);
				
				double startTime = currentTime + expectedTime(gpuCloudlet, vmlist.get(vmId));
				gpuCloudlet.getGpuTask().setExecStartTime(startTime);

				availableResources[vmId] -= gpuCloudlet.getGpuTask().getNumberOfBlocks();
				gpuCloudlet.setVmId(vmId);
				
				cloudletListAux.remove(gpuCloudlet);
				cloudletList.add(gpuCloudlet);
				cloudletListExecuting.add(gpuCloudlet);
			}
		}
	}

	
/*protected void scheduleOneVm(int vmId, List<GpuCloudlet> cloudletList) {
	List<GpuCloudlet> cloudletListAux = new ArrayList<GpuCloudlet>();
	cloudletListAux.addAll(cloudletList);
	cloudletList.clear();
	
	int availableResources = 32;
	double currentTime = 0;
	
	List<GpuCloudlet> cloudletListExecuting = new ArrayList<GpuCloudlet>();
	while(cloudletListAux.size() != 0) {
		
		if(availableResources > 0) {
			KnapsackResource resource = new KnapsackResource(availableResources);
			float[] values = new float[cloudletListAux.size()];
			KnapsackResource[] weights = new KnapsackResource[cloudletListAux.size()];
			int k = 0;
			for(GpuCloudlet c : cloudletListAux) {
				values[k] = (float) (c.getGpuTask().getNumberOfBlocks() / ((c.getGpuTask().getBlockLength()/VideoCardTags.NVIDIA_K1_CARD_PE_MIPS)*32/100));
				weights[k] = new KnapsackResource(c.getGpuTask().getNumberOfBlocks());
				k++;
			}
			
			DynamicKnapsack knapsack = new MemoryDynamicKnapsack(weights, resource, values);
			int[] scheduledKernelIndexs = knapsack.execute();
			
			List<GpuCloudlet> cloudletToRemove = new ArrayList<GpuCloudlet>();
			// GpuCloudlet-VM assignment
			for (int i = 0; i < scheduledKernelIndexs.length; i++) {
				GpuCloudlet gpuCloudlet = cloudletListAux.get(scheduledKernelIndexs[i]);
				cloudletToRemove.add(gpuCloudlet);
				availableResources -= gpuCloudlet.getGpuTask().getNumberOfBlocks(); //weights[scheduledKernelIndexs[i]].value(0);
				
				double startTime = currentTime + expectedTime(gpuCloudlet, vmlist.get(vmId));
									
				gpuCloudlet.getGpuTask().setExecStartTime(startTime);
				
				gpuCloudlet.setVmId(vmId);
			}
			cloudletListAux.removeAll(cloudletToRemove);
			cloudletList.addAll(cloudletToRemove);
			cloudletListExecuting.addAll(cloudletToRemove);
			
			// Escalona se houver algum SM livre.
			if(availableResources > 0 && cloudletListAux.size() != 0) {
				
				double maxValue = 0;
				int maxIndex = 0;
				for (int i = 0; i < cloudletListAux.size(); i++) {
					GpuCloudlet c = cloudletListAux.get(i);

					double currentValue = c.getGpuTask().getNumberOfBlocks();
					
					if(currentValue > maxValue) {
						maxValue = currentValue;
						maxIndex = i;
					}
				}
				
				GpuCloudlet gpuCloudlet = cloudletListAux.get(maxIndex);

				double startTime = currentTime + expectedTime(gpuCloudlet, vmlist.get(vmId));
				gpuCloudlet.getGpuTask().setExecStartTime(startTime);

				availableResources -= gpuCloudlet.getGpuTask().getNumberOfBlocks();
				gpuCloudlet.setVmId(vmId);
				
				cloudletListAux.remove(gpuCloudlet);
				cloudletList.add(gpuCloudlet);
				cloudletListExecuting.add(gpuCloudlet);
			}
		}
		
		// Next finished task
		double mintime = Double.MAX_VALUE;
		int minIndex = 0;
		int m = 0;
		for(GpuCloudlet c : cloudletListExecuting) {
			if(c.getGpuTask().getExecStartTime() < mintime) {
				mintime = c.getGpuTask().getExecStartTime();
				minIndex = m; 
			}
			m++;
		}
		
		availableResources += cloudletListExecuting.get(minIndex).getGpuTask().getNumberOfBlocks(); //weights[scheduledKernelIndexs[i]].value(0);
		cloudletListExecuting.remove(minIndex);
	}*/

}