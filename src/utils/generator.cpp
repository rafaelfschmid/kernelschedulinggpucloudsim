#include <time.h>
#include <algorithm>
#include <math.h>
#include <cstdlib>
#include <stdio.h>
#include <iostream>
#include <vector>

#ifndef EXP_BITS_SIZE
#define EXP_BITS_SIZE 12
#endif

void vectors_gen(int num_elements, int num_gpus, int size_elements) {

	for (int i = 0; i < num_gpus; i++)
	{

		for (int j = 0; j < num_elements; j++)
		{
			std::cout << (rand() % 500)+1 << " "; // máximo 1000 segundos.
			std::cout << (rand() % size_elements)+1; // máximo de blocos por kernel
			std::cout << "\n";
		}
		//std::cout << "\n";
	}
}


int main(int argc, char** argv) {

	if (argc < 3) {
		printf(
				"Parameters needed: <number of kernels> <max_kernel_size>\n\n");
		return 0;
	}

	int number_of_kernels = atoi(argv[1]);
	int number_of_machines = atoi(argv[2]);

	srand(time(NULL));
	printf("%d\n", number_of_kernels);
	vectors_gen(number_of_kernels, number_of_machines, 32);
	printf("\n");

	return 0;
}

