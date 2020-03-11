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
public class MinMinKnapsack extends MinMin {
	
	public MinMinKnapsack(int numVms, boolean differentVms) {
		super(numVms, differentVms);
	}
	
	protected double expectedTimeSMs(GpuCloudlet gpuCloudlet, GpuVm vm) {
		double gpuTime = ((double)gpuCloudlet.getGpuTask().getBlockLength()  /  vm.getVgpu().getPeMips());
		//double gpuTime = ((double)gpuCloudlet.getGpuTask().getTaskTotalLength()  / vm.getVgpu().getPeMips());
				//(double)VideoCardTags.NVIDIA_K1_CARD_PE_MIPS);
	
		return gpuTime;
	}

	protected void schedule() {
		minmin();
		
		List<GpuCloudlet> cloudletListAll = new ArrayList<GpuCloudlet>();
		cloudletListAll.addAll(cloudletList);
		cloudletList.clear();
				
		for(int vmId = 0; vmId < numVms; vmId++) {
		
			List<GpuCloudlet> currentList = new ArrayList<GpuCloudlet>();
			
			for(GpuCloudlet gpuCloudlet : cloudletListAll) {
				if(gpuCloudlet.getVmId() == vmId)
					currentList.add(gpuCloudlet);
			}
			
			currentList = scheduleOneVm(vmId, currentList);
			
			cloudletListAll.removeAll(currentList);
			cloudletList.addAll(currentList);
		}
	}
	
	protected List<GpuCloudlet> scheduleOneVm(int vmId, List<GpuCloudlet> currentList) {
		List<GpuCloudlet> cloudletListAux = new ArrayList<GpuCloudlet>();
		cloudletListAux.addAll(currentList);
		currentList.clear();
		
		int availableResources = 32;
		//int vmId = 0;
		double currentTime = 0;
		
		List<GpuCloudlet> cloudletListExecuting = new ArrayList<GpuCloudlet>();
		while(cloudletListAux.size() != 0) {
			
			if(availableResources > 0) {
				KnapsackResource resource = new KnapsackResource(availableResources);
				float[] values = new float[cloudletListAux.size()];
				KnapsackResource[] weights = new KnapsackResource[cloudletListAux.size()];
				int k = 0;
				for(GpuCloudlet c : cloudletListAux) {
					values[k] = (float) (c.getGpuTask().getNumberOfBlocks() / ((c.getGpuTask().getBlockLength()/vmlist.get(vmId).getVgpu().getPeMips())*32/100));
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
					
					double startTime = currentTime +
							expectedTimeSMs(gpuCloudlet, vmlist.get(vmId));
							//gpuCloudlet.getGpuTask().getBlockLength() / 
							//(VideoCardTags.NVIDIA_K1_CARD_PE_MIPS);
										
					gpuCloudlet.getGpuTask().setExecStartTime(startTime);
					
					gpuCloudlet.setVmId(vmId);
				}
				cloudletListAux.removeAll(cloudletToRemove);
				currentList.addAll(cloudletToRemove);
				cloudletListExecuting.addAll(cloudletToRemove);
				
				// Escalona se houver algum SM livre.
				// Escalona se houver algum SM livre.
				if(availableResources > 0 && cloudletListAux.size() != 0) {
					
					double maxValue = 0;
					int maxIndex = 0;
					for (int i = 0; i < cloudletListAux.size(); i++) {
						GpuCloudlet c = cloudletListAux.get(i);

						//double currentValue = c.getGpuTask().getNumberOfBlocks();
						double currentValue = (float) (c.getGpuTask().getNumberOfBlocks() / ((c.getGpuTask().getBlockLength()/vmlist.get(vmId).getVgpu().getPeMips())*32/100));
						
						if(currentValue > maxValue) {
							maxValue = currentValue;
							maxIndex = i;
						}
					}
					
					GpuCloudlet gpuCloudlet = cloudletListAux.get(maxIndex);
										
					double startTime = currentTime + expectedTimeSMs(gpuCloudlet, vmlist.get(vmId));
										
					gpuCloudlet.getGpuTask().setExecStartTime(startTime);

					availableResources -= gpuCloudlet.getGpuTask().getNumberOfBlocks();
					gpuCloudlet.setVmId(vmId);
					
					cloudletListAux.remove(gpuCloudlet);
					currentList.add(gpuCloudlet);
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
			
			currentTime = mintime;
			
			availableResources += cloudletListExecuting.get(minIndex).getGpuTask().getNumberOfBlocks(); //weights[scheduledKernelIndexs[i]].value(0);
			cloudletListExecuting.remove(minIndex);
		}
		
		return currentList;
	}
}