
public class Node {
	// The number of nodes which is required to start the algorithm.
	static final int expectedNetworkSize = 8;

	// NOTE: In the beginning start the registry with the rmiregistry command.
	public static void main(String args[]) throws Exception {
		// 1. Before starting the nodes, manually create the registry with the
		// rmiregistry command.
		// 2. Connect to the registry. Call getRegistry method of the
		// NodeImplementation class on port registryPort.
		// 3. Register in the registry, notify the others that you've joined the
		// network.
		// 4. If there are enough nodes have joined then start the algorithm.

		int registryPort = Integer.parseInt(args[0]);
		int nodePort = Integer.parseInt(args[1]);

		NodeImplementation node = new NodeImplementation(registryPort, nodePort, expectedNetworkSize);
		node.notifyOthers();
		System.out.println("Waiting for the incoming messages...");
	}
}
