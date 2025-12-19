# Small World Network Demonstration

This interactive Java application demonstrates the Watts-Strogatz Small-World Model. This tool visualizes how increasing the randomness of edge rewiring (p) transforms a regular ring lattice into a "small-world" network, characterized by high clustering and short average path lengths. 

## Key Features

* **Interactive Controls:** Real-time adjustment of Network Size (n), Mean Degree (k), and Rewiring Probability (p).
* **Ring Lattice View:** Classic circular layout to visualize the adjacency matrix.
* **Force-Directed View:** Physics-based simulation to reveal community structures.
* **Real-Time Graph Metrics:** Automatically calculates and displays:
    * Average Path Length
    * Network Diameter (Longest Shortest Path Between Nodes)
    * Clustering Coefficient

* **Spectral Clustering:** Implements spectral graph theory (using Eigenvectors of the Laplacian matrix) to automatically detect and color-code communities within the network.
* **UI:** Built with Java Swing and FlatLaf for a dark mode aesthetic.

## Prerequisites

* **Java Development Kit (JDK):** Version 17 or higher.
* **Maven:** For dependency management and building the project.

## How to Build and Run

1. **Clone or Download** the repository.

2. **Build the Project** using Maven. Open your terminal in the project root and run:
```bash
mvn clean package

```

3. **Run the Application**:
```bash
java -jar target/SmallWorldDemonstration-1.0-SNAPSHOT.jar

```

or simply find the executable jar labelled "SmallWorldDemonstration-1.0-SNAPSHOT.jar" located under the target directory and run it from your file viewer.

## Usage

### The Controls

* **Nodes (N):** The total number of vertices in the graph. (Slider: 20–200)
* **Probability (p):** The likelihood that an edge will be rewired to a random target.
    * **p = 0:** Regular Ring Lattice (High clustering, Long paths).
    * **0 < p < 1:** Small World (High clustering, Short paths).
    * **p = 1:** Random Graph (Low clustering, Short paths).


* **K Value:** The mean degree (must be an even integer). This determines how many neighbors each node starts with.

### The Views

* **Ring Lattice:** This is the default view and what most people expect and think of when visualizing an adjacency matrix.
* **Force-Directed Layout:** Click the button at the bottom to apply the physics simulation. This is useful for visualizing the clustering coefficient and watching communities form / collapse.

## Mathematical Concepts

### Watts-Strogatz Model

This application implements the algorithm described by Duncan Watts and Steven Strogatz in their paper titled **Collective dynamics of ‘small-world’ networks** in 1998. It starts with a regular ring lattice where each node is connected to it's nearest neighbors. It then rewires each edge based upon a probability (p).

### Spectral Clustering

To color the nodes, the application constructs the **Laplacian Matrix** and computes its Eigenvectors. By analyzing the eigenvectors corresponding to the smallest non-zero eigenvalues (specifically the Fiedler vector), the graph sorts nodes into communities.

## Dependencies

* Apache Commons Math 3 - For linear algebra.
* FlatLaf - For the Swing Look and Feel.

## Author

**Dominic Murray**