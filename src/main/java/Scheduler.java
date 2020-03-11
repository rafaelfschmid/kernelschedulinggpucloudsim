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
import org.cloudbus.cloudsim.gpu.GpuTaskSchedulerLeftoverCorrect;
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
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

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
public class Scheduler {
	
	/** The cloudlet list. */
	protected List<GpuCloudlet> cloudletList;
	/** The vmlist. */
	protected List<GpuVm> vmlist;
	
	/** number of VMs. */
	protected int numVms;
	protected boolean differentVms;
	
	/** number of gpuCloudlets */
	protected int numGpuCloudlets;
	
	protected List<GpuDatacenter> datacenterList;
	/**
	 * The resolution in which progress in evaluated.
	 */
	protected double schedulingInterval = 20;
	
	public GpuDatacenterBroker broker;

	public Scheduler() {
		this.numVms = 1;
		this.differentVms = false;
	}
	
	public Scheduler(int numVms, boolean differentVms) {
		this.numVms = numVms;
		this.differentVms = differentVms;
	}
	
	@SuppressWarnings("unused")
	public long run(String filename) {
		Log.disable();
		//Log.enable();
		
		try {
			// number of cloud users
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			// trace events
			boolean trace_flag = false;
			
			// CloudSim initialization
			CloudSim.init(num_user, calendar, trace_flag);

			datacenterList = new ArrayList<GpuDatacenter>();
			// Create one Datacenter
			GpuDatacenter datacenter = createDatacenter("Datacenter");
			datacenterList.add(datacenter);
			
			// Create one Broker
			broker = createBroker("Broker");
			int brokerId = broker.getId();

			// Create a list to hold created VMs
			vmlist = new ArrayList<GpuVm>();
			// Create a list to hold issued Cloudlets
			cloudletList = new ArrayList<GpuCloudlet>();

			// Create VMs
			for (int i = 0; i < numVms; i++) {
				int vmId = i;
				int vgpuId = i;
				// Create a VM
				GpuVm vm = createGpuVm(vmId, vgpuId, brokerId);
				// add the VM to the vmList
				vmlist.add(vm);
			}

			BufferedReader br = new BufferedReader(new FileReader(filename));
			
			if(br.ready()) {
				numGpuCloudlets = Integer.parseInt(br.readLine());
				//numGpus = Integer.parseInt(br.readLine());

				int vmId = 0;
				for (int j = 0; j < numVms; j++) {
					// Create gpuCloudlets
					for (int i = 0; i < numGpuCloudlets; i++) {
						int gpuCloudletId = i;
						int gpuTaskId = i;
						
						String kernelInfo = br.readLine();
						int kernelTime = Integer.parseInt(kernelInfo.split(" ")[0]);
						int kernelBlocks = Integer.parseInt(kernelInfo.split(" ")[1]);
						
						// Create Cloudlet
						GpuCloudlet gpuCloudlet = createGpuCloudlet(gpuCloudletId, gpuTaskId, brokerId, kernelTime, kernelBlocks);
						// add the cloudlet to the list
						gpuCloudlet.setVmId(vmId);
						vmId = (vmId+1) % numVms;
						
						cloudletList.add(gpuCloudlet);
					}
				}
				br.close();
			}
			else {
				System.out.println("ERRO ao abrir arquivo: input.txt");
			}
						
			long startTime = 0, endTime = 0;
			
			startTime = System.currentTimeMillis();
			
			schedule();
			
			endTime = System.currentTimeMillis();

			// submit vm list to the broker
			broker.submitVmList(vmlist);

			// submit cloudlet list to the broker
			broker.submitCloudletList(cloudletList);

			// Disable Logs
			//Log.disable();
			// Starts the simulation
			CloudSim.startSimulation();

			CloudSim.stopSimulation();
			Log.enable();
			
			//Final step: Print results when simulation is over
			//List<Cloudlet> newList = broker.getCloudletReceivedList();
			//printCloudletList(newList);
					
			return (long)endTime - startTime;
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
		
		return 0;
	}

	protected void schedule() {
		
	}
	
	private GpuCloudlet createGpuCloudlet(int gpuCloudletId, int gpuTaskId, int brokerId, int kernelTime, int kernelBlocks) {
		// Cloudlet properties
		long length = 0; //(long) (400 * GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS);
		long fileSize = 0;
		long outputSize = 0;
		int pesNumber = 1;
		UtilizationModel cpuUtilizationModel = new UtilizationModelFull();
		UtilizationModel ramUtilizationModel = new UtilizationModelFull();
		UtilizationModel bwUtilizationModel = new UtilizationModelFull();

		// GpuTask properties
		long taskLength = (long) (VideoCardTags.NVIDIA_K1_CARD_PE_MIPS * kernelTime); //tamanho de cada bloco do kernel
		int numberOfBlocks = kernelBlocks; // número de blocos do kernel
		
		long taskInputSize = 0;
		long taskOutputSize = 0;
		long requestedGddramSize = 0;
		
		UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
		UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
		UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();

		GpuTask gpuTask = new GpuTask(gpuTaskId, taskLength, numberOfBlocks, taskInputSize, taskOutputSize,
				requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);

		GpuCloudlet gpuCloudlet = new GpuCloudlet(gpuCloudletId, length, pesNumber, fileSize, outputSize,
				cpuUtilizationModel, ramUtilizationModel, bwUtilizationModel, gpuTask, false);

		gpuCloudlet.setUserId(brokerId);
		return gpuCloudlet;
	}

	/**
	 * Create a GpuVM
	 * 
	 * @param vmId
	 *            vm id
	 * @param vgpuId
	 *            vm's vgpu id
	 * @param brokerId
	 *            the broker to which this vm belongs
	 * @return the GpuVm
	 */
	private GpuVm createGpuVm(int vmId, int vgpuId, int brokerId) {
		// VM description
		double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;
		// image size (GB)
		int size = 10;
		// vm memory (GB)
		int ram = 2;
		long bw = 100;
		// number of cpus
		int pesNumber = 1;
		// VMM name
		String vmm = "vSphere";

		// Create a VM
		GpuVm vm = new GpuVm(vmId, brokerId, mips, pesNumber, ram, bw, size, vmm, GpuVmTags.GPU_VM_CUSTOM,
				new GpuCloudletSchedulerTimeShared());
		// Create GpuTask Scheduler
		GpuTaskSchedulerLeftoverCorrect gpuTaskScheduler = new GpuTaskSchedulerLeftoverCorrect();
		//GpuTaskSchedulerLeftover gpuTaskScheduler = new GpuTaskSchedulerLeftover();
		// Create a Vgpu
		int frequency = 850;
		if(differentVms) {
			frequency = 450+(50*vmId);
		}
		Vgpu vgpu = VgpuTags.getK180Q_Teste(vgpuId, gpuTaskScheduler, frequency);
		vm.setVgpu(vgpu);
		return vm;
	}

	/**
	 * Create a datacenter.
	 * 
	 * @param name
	 *            the name of the datacenter
	 * 
	 * @return the datacenter
	 */
	private GpuDatacenter createDatacenter(String name) {
		// We need to create a list to store our machines
		List<GpuHost> hostList = new ArrayList<GpuHost>();
		
		for(int vmId = 0; vmId < numVms; vmId++)
			hostList.add(createHost(vmId));

		// Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		// system architecture
		String arch = "x86";
		// operating system
		String os = "Linux";
		// VM Manager
		String vmm = "Horizen";
		// time zone this resource located (Tehran)
		double time_zone = +3.5;
		// the cost of using processing in this resource
		double cost = 0.0;
		// the cost of using memory in this resource
		double costPerMem = 0.00;
		// the cost of using storage in this resource
		double costPerStorage = 0.000;
		// the cost of using bw in this resource
		double costPerBw = 0.0;
		// we are not adding SAN devices by now
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, time_zone,
				cost, costPerMem, costPerStorage, costPerBw);

		// We need to create a Datacenter object.
		GpuDatacenter datacenter = null;
		try {
			datacenter = new GpuDatacenter(name, characteristics, new GpuVmAllocationPolicySimple(hostList),
					storageList, schedulingInterval);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	private GpuHost createHost(int hostId) {
		// Create a host
		//int hostId = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3;
		
		// Number of host's video cards
		int numVideoCards = 1;//GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_VIDEO_CARDS;
		// To hold video cards
		List<VideoCard> videoCards = new ArrayList<VideoCard>(numVideoCards);
		for (int videoCardId = 0; videoCardId < numVideoCards; videoCardId++) {
			List<Pgpu> pgpus = new ArrayList<Pgpu>();
			// Adding an NVIDIA K1 Card
			double mips = VideoCardTags.NVIDIA_K1_CARD_PE_MIPS;
			int gddram = VideoCardTags.NVIDIA_K1_CARD_GPU_MEM;
			long bw = VideoCardTags.NVIDIA_K1_CARD_BW_PER_BUS;
			for (int pgpuId = 0; pgpuId < VideoCardTags.NVIDIA_K1_CARD_GPUS; pgpuId++) {
				List<Pe> pes = new ArrayList<Pe>();
				for (int peId = 0; peId < VideoCardTags.NVIDIA_K1_CARD_GPU_PES; peId++) {
					pes.add(new Pe(peId, new PeProvisionerSimple(mips)));
				}
				pgpus.add(
						new Pgpu(pgpuId, pes, new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw)));
			}
			// Pgpu selection policy
			PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyBreadthFirst();
			// Vgpu Scheduler
			VgpuScheduler vgpuScheduler = new VgpuSchedulerFairShare(VideoCardTags.NVIDIA_K1_CARD,
					pgpus, pgpuSelectionPolicy);
			// PCI Express Bus Bw Provisioner
			VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(
					VideoCardTags.PCI_E_3_X16_BW);
			// Create a video card
			VideoCard videoCard = new VideoCard(videoCardId, VideoCardTags.NVIDIA_K1_CARD, vgpuScheduler,
					videoCardBwProvisioner);
			videoCards.add(videoCard);
		}
		
		// A Machine contains one or more PEs or CPUs/Cores.
		List<Pe> peList = new ArrayList<Pe>();

		// PE's MIPS power
		double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

		for (int peId = 0; peId < GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_NUM_PES; peId++) {
			// Create PEs and add these into a list.
			peList.add(new Pe(0, new PeProvisionerSimple(mips)));
		}

		// Create Host with its id and list of PEs and add them to the list of machines
		// host memory (MB)
		int ram = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_RAM;
		// host storage
		long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
		// host BW
		int bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW;
		// Set VM Scheduler
		VmScheduler vmScheduler = new VmSchedulerTimeShared(peList);
		// Video Card Selection Policy
		VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyBreadthFirst(videoCards);
		GpuHost newHost = new GpuHost(hostId, GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3, new RamProvisionerSimple(ram),
				new BwProvisionerRelaxed(bw), storage, peList, vmScheduler, videoCardAllocationPolicy);
		
		return newHost;
	}

	/**
	 * Create a broker.
	 * 
	 * * @param name the name of the broker
	 * 
	 * @return the datacenter broker
	 */
	private GpuDatacenterBroker createBroker(String name) {
		GpuDatacenterBroker broker = null;
		try {
			broker = new GpuDatacenterBroker(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	
	public void printCloudletList(List<Cloudlet> gpuCloudlets) {
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
								((GpuVm) VmList.getById(vmlist, gpuTask.getCloudlet().getVmId())).getVgpu().getType()),
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
	/**
	 * Prints the GpuCloudlet objects.
	 * 
	 * @param list
	 *            list of GpuCloudlets
	 */
	private void printCloudletList1(List<Cloudlet> gpuCloudlets) {
		Log.printLine(String.join("", Collections.nCopies(100, "-")));
		DecimalFormat dft = new DecimalFormat("###.##");
		for (GpuCloudlet gpuCloudlet : (List<GpuCloudlet>) (List<?>) gpuCloudlets) {
			// Cloudlet
			AsciiTable at = new AsciiTable();
			at.addRule();
			at.addRow("Cloudlet ID", "Status", "Datacenter ID", "VM ID", "Time", "Start Time", "Finish Time");
			at.addRule();
			if (gpuCloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
				at.addRow(gpuCloudlet.getCloudletId(), "SUCCESS", gpuCloudlet.getResourceId(), gpuCloudlet.getVmId(),
						dft.format(gpuCloudlet.getActualCPUTime()).toString(),
						dft.format(gpuCloudlet.getExecStartTime()).toString(),
						dft.format(gpuCloudlet.getFinishTime()).toString());
				at.addRule();
			}
			GpuTask gpuTask = gpuCloudlet.getGpuTask();
			// Host-Device Memory Transfer
			AsciiTable atMT = new AsciiTable();
			atMT.addRule();
			atMT.addRow("Direction", "Time", "Start Time", "End Time");
			atMT.addRule();
			atMT.addRow("H2D", dft.format(gpuTask.getMemoryTransferHostToDevice().getTime()).toString(),
					dft.format(gpuTask.getMemoryTransferHostToDevice().startTime).toString(),
					dft.format(gpuTask.getMemoryTransferHostToDevice().endTime).toString());
			atMT.addRule();
			// Gpu Task
			at.addRow("Task ID", "Cloudlet ID", "Status", "vGPU Profile", "Time", "Start Time", "Finish Time");
			at.addRule();
			if (gpuTask.getTaskStatus() == GpuTask.FINISHED) {
				at.addRow(gpuTask.getTaskId(), gpuTask.getCloudlet().getCloudletId(), "SUCCESS",
						VgpuTags.getVgpuTypeString(
								((GpuVm) VmList.getById(vmlist, gpuTask.getCloudlet().getVmId())).getVgpu().getType()),
						dft.format(gpuTask.getActualGPUTime()).toString(),
						dft.format(gpuTask.getExecStartTime()).toString(),
						dft.format(gpuTask.getFinishTime()).toString());
				at.addRule();
			}
			// Device-Host Memory Transfer
			atMT.addRow("D2H", dft.format(gpuTask.getMemoryTransferDeviceToHost().getTime()).toString(),
					dft.format(gpuTask.getMemoryTransferDeviceToHost().startTime).toString(),
					dft.format(gpuTask.getMemoryTransferDeviceToHost().endTime).toString());
			atMT.addRule();
			at.getContext().setWidth(100);
			atMT.getContext().setWidth(100);
			Log.printLine(at.render());
			Log.printLine(atMT.render());
			Log.printLine(String.join("", Collections.nCopies(100, "-")));
		}
	}
}