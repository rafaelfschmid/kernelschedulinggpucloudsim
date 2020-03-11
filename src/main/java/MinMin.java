import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.gpu.GpuCloudlet;
import org.cloudbus.cloudsim.gpu.GpuCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.gpu.GpuDatacenter;
import org.cloudbus.cloudsim.gpu.GpuDatacenterBroker;
import org.cloudbus.cloudsim.gpu.GpuHost;
import org.cloudbus.cloudsim.gpu.GpuHostTags;
import org.cloudbus.cloudsim.gpu.GpuTask;
import org.cloudbus.cloudsim.gpu.GpuTaskSchedulerLeftover;
import org.cloudbus.cloudsim.gpu.GpuVm;
import org.cloudbus.cloudsim.gpu.GpuVmAllocationPolicySimple;
import org.cloudbus.cloudsim.gpu.GpuVmTags;
import org.cloudbus.cloudsim.gpu.Pgpu;
import org.cloudbus.cloudsim.gpu.Vgpu;
import org.cloudbus.cloudsim.gpu.VgpuScheduler;
import org.cloudbus.cloudsim.gpu.VgpuSchedulerFairShare;
import org.cloudbus.cloudsim.gpu.VgpuTags;
import org.cloudbus.cloudsim.gpu.VideoCard;
import org.cloudbus.cloudsim.gpu.VideoCardTags;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicy;
import org.cloudbus.cloudsim.gpu.allocation.VideoCardAllocationPolicyBreadthFirst;
import org.cloudbus.cloudsim.gpu.provisioners.BwProvisionerRelaxed;
import org.cloudbus.cloudsim.gpu.provisioners.GpuBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.provisioners.GpuGddramProvisionerSimple;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisioner;
import org.cloudbus.cloudsim.gpu.provisioners.VideoCardBwProvisionerShared;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicy;
import org.cloudbus.cloudsim.gpu.selection.PgpuSelectionPolicyBreadthFirst;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import de.vandermeer.asciitable.AsciiTable;
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
public class MinMin extends Scheduler {
	
	public MinMin(int numVms, boolean differentVms) {
		super(numVms, differentVms);
	}
	
	protected void schedule() {
		minmin();
	}

	protected double expectedTime(GpuCloudlet gpuCloudlet, GpuVm vm) {
		double gpuTime = ((double)gpuCloudlet.getGpuTask().getBlockLength()  /   vm.getVgpu().getPeMips());
		//double gpuTime = ((double)gpuCloudlet.getGpuTask().getTaskTotalLength()  / vm.getVgpu().getPeMips());
	
		return gpuTime;
	}
	
	protected void minmin() {
		double completion_times[] = new double[vmlist.size()];
		for(int i = 0; i < vmlist.size(); i++) {
			completion_times[i] = 0;
		}
		
		List<GpuCloudlet> cloudletAux = new ArrayList<GpuCloudlet>();
		cloudletAux.addAll(cloudletList);
		cloudletList.clear();
		
		int imin = 0;
		int jmin = 0;
		double min_value;
		while(cloudletAux.size() > 0) {

			min_value = (double)Double.MAX_VALUE;
			
			for (int i = 0; i < cloudletAux.size(); i++) {
				for (int j = 0; j < vmlist.size(); j++) {
					double time = expectedTime(cloudletAux.get(i), vmlist.get(j));
					if (completion_times[j] + time < min_value) {
						imin = i;
						jmin = j;
						min_value = completion_times[jmin] + time;
					}
				}
			}
			
			GpuCloudlet gpuCloudlet = cloudletAux.remove(imin);
			int vmId = vmlist.get(jmin).getId();
			gpuCloudlet.setVmId(vmId);
			cloudletList.add(gpuCloudlet);
			completion_times[jmin] = min_value;
		}		
	}
}