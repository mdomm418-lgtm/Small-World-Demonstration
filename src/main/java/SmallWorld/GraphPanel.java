package SmallWorld;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The View component. 
 * Renders the graph and handles the physics simulation for the Analysis View.
 */
public class GraphPanel extends JPanel {

    private GraphModel model;
    private Node[] nodes;

    // Simulation State
    private boolean isForceDirected = false; // Toggle between Ring and Force-Directed view
    private Timer simulationTimer; // Timer to trigger animation frames
    private double[] velocitiesX;  // Physics velocity X
    private double[] velocitiesY;  // Physics velocity Y

    // Physics Constants
    private static final double BASE_REPEL = 1000.0 * 100.0; // repulsion strength
    private static final double BASE_ATTRACT = 0.05 * 100.0; // spring strength
    private static final double IDEAL_LENGTH = 100.0;        // resting length of springs
    private static final double DAMPING = 0.50;              // friction to stop movement
    private static final int SIM_SPEED = 30;                 // milliseconds per frame

    private static final double CENTERING_FORCE = 0.005;     // gravity pulling nodes to center

    // Visual Constants
    private static final int BASE_NODE_DIAMETER = 12;
    private static final int MIN_NODE_DIAMETER = 4;

    private static final int PADDING = 40;
    private static final double CURVE_FACTOR = 0.4; // How much edges curve in Ring view

    private final Color[] communityColors;

    public GraphPanel(GraphModel model) {
        this.model = model;
        
        setBackground(new Color(60, 63, 65)); // Dark Grey background
        
        // Initialize the physics loop
        simulationTimer = new Timer(SIM_SPEED, this::stepSimulation);
        simulationTimer.setInitialDelay(0);
        
        // Palette for coloring communities (Clusters)
        communityColors = new Color[] {
            new Color(0, 180, 255), // Bright Blue
            new Color(255, 0, 100), // Pink
            new Color(30, 200, 30), // Green
            new Color(255, 130, 0), // Orange
            new Color(150, 50, 255), // Purple
            new Color(240, 240, 0), // Yellow
        };
        
        initializeNodes();
    }

    /**
     * Resets node positions. 
     * Defaults to the Circular Layout whenever the graph is regenerated.
     */
    public void initializeNodes() {
        int n = model.getSize();
        if (n == 0) {
            nodes = null;
            velocitiesX = null;
            velocitiesY = null;
            return;
        }

        boolean needsResize = (nodes == null || nodes.length != n);

        if (needsResize) {
            nodes = new Node[n];
            velocitiesX = new double[n];
            velocitiesY = new double[n];
        }

        // Reset velocities
        Arrays.fill(velocitiesX, 0);
        Arrays.fill(velocitiesY, 0);
        
        calculateCircularLayout();
    }

    /**
     * Positions nodes in a perfect circle (Ring Lattice).
     */
    private void calculateCircularLayout() {
        int n = model.getSize();
        if (n == 0 || nodes == null || nodes.length != n) return;

        int width = getWidth() > 0 ? getWidth() : 600;
        int height = getHeight() > 0 ? getHeight() : 600;
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) / 2 - PADDING;

        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i) / n;
            int x = (int) (centerX + radius * Math.cos(angle));
            int y = (int) (centerY + radius * Math.sin(angle));

            if (nodes[i] == null) {
                nodes[i] = new Node(x, y);
            } else {
                nodes[i].x = x;
                nodes[i].y = y;
            }
        }
    }
    
    public boolean isInForceDirectedMode() {
        return isForceDirected;
    }
    
    /**
     * Switches between static Ring View and Force-Directed View.
     */
    public void toggleLayoutMode() {
        this.isForceDirected = !this.isForceDirected;
        if (isForceDirected) {
            initializeNodes(); // Reset to circle before repelling out
            simulationTimer.start();
        } else {
            simulationTimer.stop();
            repaint();
        }
    }
    
    /**
     * Calculates forces acting on every node.
     * Uses a Force-Directed Graph Drawing algorithm.
     */
    private void stepSimulation(ActionEvent e) {
        int n = model.getSize();
        if (nodes == null || nodes.length != n || n == 0) return;
        
        int[][] matrix = model.getMatrix();
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int centerX = panelWidth / 2;
        int centerY = panelHeight / 2;

        // Scale forces based on number of nodes
        double kRepel = BASE_REPEL / n;
        double kAttract = BASE_ATTRACT / n;

        double[] forcesX = new double[n];
        double[] forcesY = new double[n];

        for (int i = 0; i < n; i++) {
            Node n1 = nodes[i];
            
            // Repulsion: Every node repels every other node
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                Node n2 = nodes[j];
                double dx = n1.x - n2.x;
                double dy = n1.y - n2.y;
                double distSq = dx*dx + dy*dy;
                if (distSq < 1) distSq = 1; // Prevent division by zero
                double dist = Math.sqrt(distSq);
                
                forcesX[i] += (dx / dist) * kRepel / distSq;
                forcesY[i] += (dy / dist) * kRepel / distSq;
            }

            // Attraction: Connected nodes pull together (Springs)
            for (int j = 0; j < n; j++) {
                if (matrix[i][j] == 1) {
                    Node n2 = nodes[j];
                    double dx = n1.x - n2.x;
                    double dy = n1.y - n2.y;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    if (dist < 1) dist = 1;
                    
                    double force = kAttract * (dist - IDEAL_LENGTH);
                    forcesX[i] -= (dx / dist) * force;
                    forcesY[i] -= (dy / dist) * force;
                }
            }

            // Centering: Weak gravity to keep nodes on screen
            double dxCenter = nodes[i].x - centerX;
            double dyCenter = nodes[i].y - centerY;
            forcesX[i] -= dxCenter * CENTERING_FORCE;
            forcesY[i] -= dyCenter * CENTERING_FORCE;
        }

        // Apply forces to velocity and position
        for (int i = 0; i < n; i++) {
            velocitiesX[i] = (velocitiesX[i] + forcesX[i]) * DAMPING;
            velocitiesY[i] = (velocitiesY[i] + forcesY[i]) * DAMPING;

            nodes[i].x += (int)velocitiesX[i];
            nodes[i].y += (int)velocitiesY[i];
        }

        repaint(); // Trigger a redraw
    }

    /**
     * Standard Java Swing painting method. 
     * Draws the edges and nodes based on current coordinates.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        int n = model.getSize();
        if (n == 0) return;

        if (nodes == null || nodes.length != n) {
            initializeNodes();
        }
        
        if (nodes == null) return; 

        // If ring view, ensure circle layout is enforced every frame
        if (!isForceDirected) {
            calculateCircularLayout();
        }

        Graphics2D g2d = (Graphics2D) g;
        // Antialiasing for smooth lines/circles
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                             RenderingHints.VALUE_STROKE_PURE);
        
        int[][] matrix = model.getMatrix();
        int[] communities = model.getNodeCommunities();
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // Draw Edges
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (matrix[i][j] == 1) {
                    Node n1 = nodes[i];
                    Node n2 = nodes[j];
                    
                    g2d.setColor(new Color(180, 180, 180, 150)); // Semi-transparent grey
                    g2d.setStroke(new BasicStroke(1.5f));
                    
                    if (isForceDirected) {
                        // Straight lines for physics view
                        g2d.draw(new Line2D.Double(n1.x, n1.y, n2.x, n2.y));
                    } else {
                        // Curves for Ring View:
                        int midX = (n1.x + n2.x) / 2;
                        int midY = (n1.y + n2.y) / 2;
                        double vecX = centerX - midX;
                        double vecY = centerY - midY;
                        double length = Math.sqrt(vecX * vecX + vecY * vecY);
                        if (length > 0) {
                            vecX /= length;
                            vecY /= length;
                        }
                        // Curve strength depends on distance between nodes in the ring
                        double angleDiff = Math.abs(i - j);
                        if (angleDiff > n / 2) angleDiff = n - angleDiff;
                        double curveStrength = CURVE_FACTOR * (1.0 - (angleDiff / (n / 2.0)));
                        if (curveStrength < 0) curveStrength = 0;
                        int controlX = (int) (midX + vecX * length * curveStrength);
                        int controlY = (int) (midY + vecY * length * curveStrength);
                        QuadCurve2D curve = new QuadCurve2D.Double(n1.x, n1.y, controlX, controlY, n2.x, n2.y);
                        g2d.draw(curve);
                    }
                }
            }
        }
        
        // Scale node size based on total number of nodes (smaller nodes for dense graphs)
        int nodeDiameter = (int) Math.max(MIN_NODE_DIAMETER, BASE_NODE_DIAMETER * (1.0 - (n / 1000.0)));
        if (n > 400) nodeDiameter = MIN_NODE_DIAMETER;
        int r = nodeDiameter / 2;

        // Draw Nodes
        for (int i = 0; i < n; i++) {
            Node node = nodes[i];
            
            // Color node based on its community (Spectral Analysis)
            int community = communities[i];
            if (community >= 0) {
                g2d.setColor(communityColors[community % communityColors.length]);
            } else {
                g2d.setColor(Color.DARK_GRAY);
            }
            
            g2d.fillOval(node.x - r, node.y - r, nodeDiameter, nodeDiameter);
            
            // White outline for visibility
            g2d.setColor(Color.WHITE);
            if (nodeDiameter > 6) {
                g2d.drawOval(node.x - r, node.y - r, nodeDiameter, nodeDiameter);
            }
        }
    }
}