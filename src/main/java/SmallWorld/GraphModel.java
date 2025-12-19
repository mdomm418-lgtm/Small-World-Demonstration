package SmallWorld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * The Model component. 
 * Manages the Adjacency Matrix and calculates graph metrics.
 */
public class GraphModel {

    private static final int K_COMMUNITIES = 4; // Target number of clusters for Spectral Clustering

    private int[][] matrix; // Adjacency matrix
    private int N;          // Number of nodes
    private Random rand;

    // Computed Metrics
    private double averagePathLength;
    private int diameter;
    private List<Integer> diameterPath = new ArrayList<>();
    private double clusteringCoefficient;
    private int[] nodeCommunities; // Stores the cluster ID for each node
    private int communityCount;

    public GraphModel(int N, int K, double p) {
        this.rand = new Random();
        generateWattsStrogatz(N, K, p);
    }

    /**
     * Core Algorithm: Generates the Small-World Network.
     * Creates a regular ring lattice and randomly rewires edges with probability p.
     */
    public void generateWattsStrogatz(int N, int K, double p) {
        this.N = N;
        this.matrix = new int[N][N];
        int halfK = K / 2;

        // Construct Regular Ring Lattice
        // Connect each node to its K nearest neighbors (K/2 on each side)
        for (int i = 0; i < N; i++) {
            for (int j = 1; j <= halfK; j++) {
                int neighbor = (i + j) % N;
                matrix[i][neighbor] = 1;
                matrix[neighbor][i] = 1;
            }
        }

        // Random Rewiring
        // Iterate through edges and rewire them based on probability p
        for (int i = 0; i < N; i++) {
            for (int j = 1; j <= halfK; j++) {
                int oldTarget = (i + j) % N;
                if (rand.nextDouble() < p) {
                    int newTarget;
                    // Find a new target that isn't self and isn't already connected
                    do {
                        newTarget = rand.nextInt(N);
                    } while (newTarget == i || matrix[i][newTarget] == 1);
                    
                    // Remove old edge
                    matrix[i][oldTarget] = 0;
                    matrix[oldTarget][i] = 0;
                    // Add new edge
                    matrix[i][newTarget] = 1;
                    matrix[newTarget][i] = 1;
                }
            }
        }

        // After generation, compute all statistics
        calculateGraphMetrics();
        calculateClusteringCoefficient();
        calculateCommunitiesSpectral(); 
    }

    /**
     * Calculates Average Path Length and Diameter using breadth-first search.
     * BFS is run from every node to find all shortest paths.
     */
    private void calculateGraphMetrics() {
        if (N == 0) {
            this.averagePathLength = 0;
            this.diameter = 0;
            return;
        }

        long totalPathLength = 0;
        int reachablePairs = 0;
        int maxPathFound = 0;
        
        this.diameterPath.clear();

        // Run BFS from every node 'startNode'
        for (int startNode = 0; startNode < N; startNode++) {
            int[] distances = new int[N];
            Arrays.fill(distances, -1); // -1 indicates unvisited
            int[] parent = new int[N];  // Used to reconstruct the path
            Arrays.fill(parent, -1);
            Queue<Integer> queue = new LinkedList<>();

            distances[startNode] = 0;
            queue.add(startNode);
            
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v = 0; v < N; v++) {
                    if (matrix[u][v] == 1 && distances[v] == -1) {
                        distances[v] = distances[u] + 1;
                        parent[v] = u;
                        queue.add(v);
                    }
                }
            }
            
            // Aggregate results for this startNode
            for (int endNode = 0; endNode < N; endNode++) {
                if (endNode == startNode) continue;
                int dist = distances[endNode];
                if (dist > 0) {
                    totalPathLength += dist;
                    reachablePairs++;
                    // Check if this is the longest path found so far (Diameter)
                    if (dist > maxPathFound) {
                        maxPathFound = dist;
                        this.diameterPath.clear();
                        // Backtrack to store the actual path nodes
                        int currentNode = endNode;
                        while (currentNode != -1) {
                            this.diameterPath.add(currentNode);
                            currentNode = parent[currentNode];
                        }
                    }
                }
            }
        }
        
        this.diameter = maxPathFound;
        this.averagePathLength = (reachablePairs > 0) ? (double)totalPathLength / reachablePairs : 0.0;
    }
    
    /**
     * Calculates the Clustering Coefficient (local clustering averaged over all nodes).
     * Measures how close a node's neighbors are to being a clique.
     */
    private void calculateClusteringCoefficient() {
        if (N == 0) {
            this.clusteringCoefficient = 0;
            return;
        }
        
        double totalCoeff = 0;
        
        for (int i = 0; i < N; i++) {
            // Find neighbors of node i
            List<Integer> neighbors = new ArrayList<>();
            for (int j = 0; j < N; j++) {
                if (matrix[i][j] == 1) {
                    neighbors.add(j);
                }
            }
            
            int degree = neighbors.size();
            if (degree < 2) {
                continue; // Coefficient is 0 for degree < 2
            }

            // Count edges existing between the neighbors
            int edgesBetweenNeighbors = 0;
            for (int j = 0; j < degree; j++) {
                for (int k = j + 1; k < degree; k++) {
                    if (matrix[neighbors.get(j)][neighbors.get(k)] == 1) {
                        edgesBetweenNeighbors++;
                    }
                }
            }
            
            // Formula: 2 * E / (k * (k-1))
            double localCoeff = (2.0 * edgesBetweenNeighbors) / (degree * (degree - 1.0));
            totalCoeff += localCoeff;
        }
        
        this.clusteringCoefficient = (N > 0) ? totalCoeff / N : 0.0;
    }

    /**
     * Constructs the Graph Laplacian Matrix.
     * L = D - A (Degree Matrix - Adjacency Matrix).
     */
    private double[][] buildLaplacian() {
        if (N == 0) return new double[0][0];

        double[][] laplacian = new double[N][N];
        
        for (int i = 0; i < N; i++) {
            int degree = 0;
            for (int j = 0; j < N; j++) {
                if (matrix[i][j] == 1) {
                    degree++;
                    laplacian[i][j] = -1.0; // -1 for adjacent
                } else {
                    laplacian[i][j] = 0.0;
                }
            }
            laplacian[i][i] = degree; // Diagonal is the degree
        }
        return laplacian;
    }

    /**
     * Performs Spectral Clustering using Eigenvectors of the Laplacian.
     */
    private void calculateCommunitiesSpectral() {
        try {
            // Build Laplacian
            double[][] laplacianData = buildLaplacian();
            RealMatrix laplacian = new Array2DRowRealMatrix(laplacianData);

            // Compute Eigenvalues and Eigenvectors
            EigenDecomposition eigen = new EigenDecomposition(laplacian);
            
            // Extract the first K eigenvectors to embed nodes in K-dimensional space
            double[][] kPoints = new double[N][K_COMMUNITIES];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < K_COMMUNITIES; j++) {
                    kPoints[i][j] = eigen.getEigenvector((N - 2) - j).getEntry(i);
                }
            }
            
            // Cluster these points using K-Means
            this.nodeCommunities = runKMeans(kPoints, K_COMMUNITIES);

            // Normalize community IDs
            Map<Integer, Integer> remapper = new HashMap<>();
            int communityId = 0;
            for (int i = 0; i < N; i++) {
                int oldLabel = nodeCommunities[i];
                if (!remapper.containsKey(oldLabel)) {
                    remapper.put(oldLabel, communityId++);
                }
                nodeCommunities[i] = remapper.get(oldLabel);
            }
            this.communityCount = communityId;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Standard K-Means implementation to group the Eigenvector points.
     */
    private int[] runKMeans(double[][] points, int k) {
        int n = points.length;
        int dimensions = points[0].length;
        
        // Randomly pick initial centroids
        double[][] centroids = new double[k][dimensions];
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        Collections.shuffle(indices);
        for (int i = 0; i < k; i++) {
            centroids[i] = points[indices.get(i)];
        }

        int[] assignments = new int[n];
        boolean changed = true;
        int maxIterations = 50;
        int iter = 0;

        // Iterate until convergence or max iterations
        while (changed && iter < maxIterations) {
            changed = false;
            iter++;

            // Assignment Step: Assign each point to nearest cluster
            for (int i = 0; i < n; i++) {
                double minDistance = Double.MAX_VALUE;
                int bestCluster = 0;
                for (int j = 0; j < k; j++) {
                    double dist = euclideanDistance(points[i], centroids[j]);
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestCluster = j;
                    }
                }
                if (assignments[i] != bestCluster) {
                    assignments[i] = bestCluster;
                    changed = true;
                }
            }

            // Recalculate clusters
            for (int j = 0; j < k; j++) {
                double[] newCentroid = new double[dimensions];
                int count = 0;
                for (int i = 0; i < n; i++) {
                    if (assignments[i] == j) {
                        for (int d = 0; d < dimensions; d++) {
                            newCentroid[d] += points[i][d];
                        }
                        count++;
                    }
                }
                if (count > 0) {
                    for (int d = 0; d < dimensions; d++) {
                        newCentroid[d] /= count;
                    }
                    centroids[j] = newCentroid;
                }
            }
        }
        return assignments;
    }

    private double euclideanDistance(double[] pointA, double[] pointB) {
        double sum = 0;
        for (int i = 0; i < pointA.length; i++) {
            sum += (pointA[i] - pointB[i]) * (pointA[i] - pointB[i]);
        }
        return Math.sqrt(sum);
    }
    
    // Getters for UI to access
    public int[][] getMatrix() { return matrix; }
    public int getSize() { return N; }
    public double getAveragePathLength() { return averagePathLength; }
    public int getDiameter() { return diameter; }
    public List<Integer> getDiameterPath() { return diameterPath; }
    public double getClusteringCoefficient() { return clusteringCoefficient; }
    public int[] getNodeCommunities() { return nodeCommunities; }
}