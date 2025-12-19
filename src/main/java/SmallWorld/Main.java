package SmallWorld;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeListener;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Main class for the Small World Model Viewer.
 * Responsible for initializing the GUI and handling user input events.
 */
public class Main {

    // Default parameters for the Small-World model
    private static final int INITIAL_N = 20;   // Number of nodes
    private static final int INITIAL_K = 4;    // Mean degree (neighbors per node)
    private static final double INITIAL_P = 0.0; // Rewiring probability (0 = Regular Ring, 1 = Random)

    public static void main(String[] args) {
       SwingUtilities.invokeLater(() -> {
            try {
                // Apply the FlatLaf Dark theme for modern aesthetics
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            createAndShowGUI();
        });
    }

    /**
     * Constructs the User Interface layout and attaches event listeners.
     */
    private static void createAndShowGUI() {
        // Initialize the backend model and the drawing panel
        final GraphModel model = new GraphModel(INITIAL_N, INITIAL_K, INITIAL_P);
        final GraphPanel graphPanel = new GraphPanel(model);

        // Fetch initial statistics from the model for display
        double initialAvgPath = model.getAveragePathLength();
        int initialDiameter = model.getDiameter();
        double initialClustering = model.getClusteringCoefficient();

        // Status Panel Setup (Top of Window)
        // Labels to display graph metrics (Path Length, Diameter, Clustering Coefficient)
        final JLabel avgPathLabel = new JLabel(String.format("Avg. Path: %.2f", initialAvgPath));
        final JLabel diameterLabel = new JLabel("Diameter (Longest Path): " + initialDiameter);
        final JLabel clusteringLabel = new JLabel(String.format("Clustering Coefficient: %.2f", initialClustering));
        
        // Font styling for metrics
        Font statusFont = new Font("SansSerif", Font.BOLD, 14);
        avgPathLabel.setFont(statusFont);
        diameterLabel.setFont(statusFont);
        clusteringLabel.setFont(statusFont);

        avgPathLabel.setHorizontalAlignment(SwingConstants.CENTER);
        diameterLabel.setHorizontalAlignment(SwingConstants.CENTER);
        clusteringLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Container for the metric labels
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.setLayout(new GridLayout(1, 3)); // 1 row, 3 columns
        statusPanel.add(avgPathLabel);
        statusPanel.add(diameterLabel);
        statusPanel.add(clusteringLabel);

        // Control Panel Setup (Bottom of Window)
        
        // Slider for Number of Nodes (N)
        final JSlider nSlider = new JSlider(20, 200, INITIAL_N);
        nSlider.setMinorTickSpacing(20);
        nSlider.setPaintTicks(true);
        
        // Custom labels for the N slider to make it readable
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(20, new JLabel("20"));
        labelTable.put(100, new JLabel("100"));
        labelTable.put(200, new JLabel("200"));
        
        nSlider.setLabelTable(labelTable);
        nSlider.setPaintLabels(true);
        
        // Slider for Probability (p)
        final JSlider pSlider = new JSlider(0, 100, (int) (INITIAL_P * 100));
        pSlider.setMajorTickSpacing(25);
        pSlider.setMinorTickSpacing(5);
        pSlider.setPaintTicks(true);
        pSlider.setPaintLabels(true);

        // Dynamic text labels updating alongside sliders
        final JLabel nLabel = new JLabel("Nodes (N): " + INITIAL_N, JLabel.CENTER);
        final JLabel pLabel = new JLabel("<html>Probability (<i>p</i>): " + (int) (INITIAL_P * 100) + "%</html>", JLabel.CENTER);

        // Input field for K (Mean Degree)
        JLabel kLabel = new JLabel("K Value (even):");
        final JTextField kField = new JTextField(Integer.toString(INITIAL_K));
        kField.setColumns(5);
        kField.setHorizontalAlignment(JTextField.CENTER);

        // Panel grouping K-value controls
        JPanel kPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        kPanel.add(kLabel);
        kPanel.add(kField);

        // Grid panel for sliders and their labels
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 2));
        controlPanel.add(nLabel);
        controlPanel.add(pLabel);
        controlPanel.add(nSlider);
        controlPanel.add(pSlider);

        // Button to toggle between Ring View and Force-Directed View
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton analysisButton = new JButton("Switch to Force-Directed Layout");
        buttonPanel.add(analysisButton);

        // Combine all control elements into the southern region
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(controlPanel, BorderLayout.NORTH);
        southPanel.add(kPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Main Frame Assembly
        JFrame frame = new JFrame("Small World Model Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 750); 
        frame.setLayout(new BorderLayout());

        frame.add(statusPanel, BorderLayout.NORTH);  // Metrics at top
        frame.add(graphPanel, BorderLayout.CENTER);  // Visualizer in middle
        frame.add(southPanel, BorderLayout.SOUTH);   // Controls at bottom

        // Event Handling Logic

        // Runnable that regenerates the graph based on current UI settings
        Runnable updateGraphAction = () -> {
            int n = nSlider.getValue();
            int pPercent = pSlider.getValue();
            double p = pPercent / 100.0; // Convert 0-100 back to 0.0-1.0
            int k;

            // Validate K input (must be integer, even number, and < N)
            try {
                k = Integer.parseInt(kField.getText());
                if (k < 2 || k >= n) {
                    k = INITIAL_K;
                    kField.setText(Integer.toString(k));
                }
                if (k % 2 != 0) { // Enforce evenness for ring lattice symmetry
                    k = Math.max(2, k - 1);
                    kField.setText(Integer.toString(k));
                }
            } catch (NumberFormatException ex) {
                k = INITIAL_K;
                kField.setText(Integer.toString(k));
            }
            
            // Update Model Logic
            model.generateWattsStrogatz(n, k, p); 

            // Fetch New Metrics
            double avgPath = model.getAveragePathLength();
            int diameter = model.getDiameter();
            double clustering = model.getClusteringCoefficient(); 
            
            // Update UI Labels
            avgPathLabel.setText(String.format("Avg. Path: %.2f", avgPath));
            diameterLabel.setText("Diameter: " + diameter);
            clusteringLabel.setText(String.format("Clustering Coeffeicient: %.2f", clustering)); 
            
            // Redraw Graph
            graphPanel.initializeNodes(); 
            graphPanel.repaint();
        };

        // Listener for sliders to update text labels immediately, 
        // but only regenerate graph when user releases the slider handle
        ChangeListener sliderListener = e -> {
            JSlider source = (JSlider) e.getSource();

            nLabel.setText("Nodes (N): " + nSlider.getValue());
            pLabel.setText("<html>Probability (<i>p</i>): " + pSlider.getValue() + "%</html>");
            
            if (!source.getValueIsAdjusting()) {
                updateGraphAction.run();
            }
        };

        nSlider.addChangeListener(sliderListener);
        pSlider.addChangeListener(sliderListener);

        // Update graph when Enter is pressed in the K text field
        kField.addActionListener(e -> updateGraphAction.run());

        // Handle Layout Toggle Button
        analysisButton.addActionListener(e -> {
            graphPanel.toggleLayoutMode();
            boolean isAnalysis = graphPanel.isInForceDirectedMode();
            analysisButton.setText(isAnalysis ? "Switch to Ring View" : "Switch to Force-Directed Layout");
        });

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
    }
}