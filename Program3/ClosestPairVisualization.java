import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ClosestPairVisualization extends JFrame {

    static class Point2D {
        double x, y;
        int id;

        Point2D(double x, double y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }
    }

    static class Pair {
        Point2D a, b;
        double distance;

        Pair(Point2D a, Point2D b) {
            this.a = a;
            this.b = b;
            distance = Math.hypot(a.x - b.x, a.y - b.y);
        }
    }

    Point2D[] points = new Point2D[0];
    Pair brutePair, dcPair;
    double bruteTime, dcTime;
    double splitX;
    double stripWidth;
    boolean hasSplit;
    int n = 50;

    JTextField sizeField = new JTextField("50", 6);
    JLabel resultLabel = new JLabel("Generate points to begin.");
    JLabel timeLabel = new JLabel("Execution time will appear here.");
    PlotPanel plot = new PlotPanel();
    RuntimePanel runtime = new RuntimePanel();

    public ClosestPairVisualization() {
        setTitle("Closest Pair of Points - Algorithm Visualization");
        setSize(1150, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Number of points N:"));
        top.add(sizeField);

        JButton generate = new JButton("Generate & Compare");
        JButton benchmark = new JButton("Performance Analysis");

        top.add(generate);
        top.add(benchmark);

        generate.addActionListener(e -> generatePoints());
        benchmark.addActionListener(e -> runBenchmark());

        JPanel results = new JPanel(new GridLayout(2, 1));
        results.add(resultLabel);
        results.add(timeLabel);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.add(plot);
        center.add(runtime);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(results, BorderLayout.SOUTH);

        generatePoints();
    }

    void generatePoints() {
        try {
            n = Integer.parseInt(sizeField.getText().trim());

            if (n < 0 || n > 10000) {
                JOptionPane.showMessageDialog(
                    this, "Enter N between 0 and 10000.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this, "Please enter a valid integer.");
            return;
        }

        Random random = new Random();
        points = new Point2D[n];

        // Random integer coordinates, including possible duplicates.
        for (int i = 0; i < n; i++) {
            points[i] = new Point2D(
                random.nextInt(101) - 50,
                random.nextInt(101) - 50,
                i
            );
        }

        long start = System.nanoTime();
        brutePair = bruteForce(points);
        bruteTime = (System.nanoTime() - start) / 1_000_000.0;

        start = System.nanoTime();
        dcPair = divideAndConquer(points);
        dcTime = (System.nanoTime() - start) / 1_000_000.0;

        hasSplit = n >= 2;

        if (hasSplit) {
            Point2D[] sorted = points.clone();
            Arrays.sort(sorted,
                Comparator.comparingDouble(p -> p.x));
            splitX = sorted[n / 2].x;

            stripWidth = dcPair.distance;
        }

        resultLabel.setText(
            "<html><b>Brute Force:</b> " + describe(brutePair)
            + "&nbsp;&nbsp;&nbsp; <b>Divide & Conquer:</b> "
            + describe(dcPair) + "</html>"
        );

        timeLabel.setText(String.format(
            Locale.US,
            "Brute Force: %.4f ms     Divide & Conquer: %.4f ms"
            + "     Verification: %s",
            bruteTime, dcTime,
            n < 2 ? "N < 2" :
                (Math.abs(brutePair.distance - dcPair.distance) < 1e-9
                    ? "PASS" : "FAIL")
        ));

        plot.repaint();
    }

    String describe(Pair p) {
        if (p == null) return "Not enough points (N < 2)";

        return String.format(
            Locale.US,
            "d = %.6f, (%g,%g) - (%g,%g)",
            p.distance, p.a.x, p.a.y, p.b.x, p.b.y
        );
    }

    // Brute-force algorithm: O(N^2)
    static Pair bruteForce(Point2D[] pts) {
        Pair best = null;

        for (int i = 0; i < pts.length; i++) {
            for (int j = i + 1; j < pts.length; j++) {
                Pair current = new Pair(pts[i], pts[j]);

                if (best == null ||
                    current.distance < best.distance) {
                    best = current;
                }
            }
        }
        return best;
    }

    // Divide-and-conquer algorithm: O(N log N)
    static Pair divideAndConquer(Point2D[] pts) {
        if (pts.length < 2) return null;

        Point2D[] byX = pts.clone();
        Point2D[] byY = pts.clone();

        Arrays.sort(byX,
            Comparator.comparingDouble((Point2D p) -> p.x)
                      .thenComparingDouble(p -> p.y)
                      .thenComparingInt(p -> p.id));

        Arrays.sort(byY,
            Comparator.comparingDouble((Point2D p) -> p.y)
                      .thenComparingDouble(p -> p.x)
                      .thenComparingInt(p -> p.id));

        return closest(byX, byY);
    }

    static Pair closest(Point2D[] byX, Point2D[] byY) {
        int n = byX.length;

        if (n <= 3) return bruteForce(byX);

        int mid = n / 2;
        double midX = byX[mid].x;

        Point2D[] leftX = Arrays.copyOfRange(byX, 0, mid);
        Point2D[] rightX = Arrays.copyOfRange(byX, mid, n);

        boolean[] leftIds = new boolean[byX.length == 0
                                        ? 0 : maxId(byX) + 1];

        for (Point2D p : leftX) leftIds[p.id] = true;

        Point2D[] leftY = new Point2D[mid];
        Point2D[] rightY = new Point2D[n - mid];

        int li = 0, ri = 0;

        for (Point2D p : byY) {
            if (leftIds[p.id])
                leftY[li++] = p;
            else
                rightY[ri++] = p;
        }

        Pair left = closest(leftX, leftY);
        Pair right = closest(rightX, rightY);

        Pair best;

        if (left == null) best = right;
        else if (right == null) best = left;
        else best = left.distance <= right.distance
                    ? left : right;

        double d = best.distance;
        List<Point2D> strip = new ArrayList<>();

        for (Point2D p : byY) {
            if (Math.abs(p.x - midX) < d)
                strip.add(p);
        }

        for (int i = 0; i < strip.size(); i++) {
            for (int j = i + 1; j < strip.size(); j++) {
                if (strip.get(j).y - strip.get(i).y >= d)
                    break;

                Pair current = new Pair(strip.get(i), strip.get(j));

                if (current.distance < best.distance) {
                    best = current;
                    d = current.distance;
                }
            }
        }

        return best;
    }

    static int maxId(Point2D[] pts) {
        int max = -1;
        for (Point2D p : pts) max = Math.max(max, p.id);
        return max;
    }

    // Performance graph for increasing input sizes.
    void runBenchmark() {
        int[] sizes = {100, 250, 500, 1000, 2000, 5000};
        double[] brute = new double[sizes.length];
        double[] dc = new double[sizes.length];

        Random random = new Random(42);

        for (int k = 0; k < sizes.length; k++) {
            Point2D[] data = new Point2D[sizes[k]];

            for (int i = 0; i < data.length; i++) {
                data[i] = new Point2D(
                    random.nextDouble() * 1000,
                    random.nextDouble() * 1000, i);
            }

            // Warm-up both algorithms.
            divideAndConquer(data);
            bruteForce(Arrays.copyOf(data, Math.min(100, data.length)));

            long t = System.nanoTime();
            bruteForce(data);
            brute[k] = (System.nanoTime() - t) / 1_000_000.0;

            t = System.nanoTime();
            divideAndConquer(data);
            dc[k] = (System.nanoTime() - t) / 1_000_000.0;
        }

        runtime.setData(sizes, brute, dc);
    }

    // Drawing area: points, closest pairs, dividing line and strip.
    class PlotPanel extends JPanel {
        PlotPanel() {
            setBackground(Color.WHITE);
        }

        int sx(double x) {
            return 35 + (int)((x + 55) / 110.0 * (getWidth() - 65));
        }

        int sy(double y) {
            return getHeight() - 35 -
                (int)((y + 55) / 110.0 * (getHeight() - 65));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            g2.setColor(Color.BLACK);
            g2.drawString("Closest Pair Visualization", 15, 18);

            if (n < 2) {
                g2.drawString("N < 2: Not enough points", 30, 50);
                return;
            }

            int left = sx(-50), right = sx(50);
            int top = sy(50), bottom = sy(-50);

            // Grid and axes.
            g2.setColor(new Color(225, 225, 225));
            for (int v = -50; v <= 50; v += 10) {
                g2.drawLine(sx(v), top, sx(v), bottom);
                g2.drawLine(left, sy(v), right, sy(v));
            }

            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(left, top, right - left, bottom - top);
            g2.drawString("X", right - 10, bottom + 20);
            g2.drawString("Y", left - 20, top + 10);

            // Divide-and-conquer strip has width 2d.
            double d = dcPair.distance;
            int stripL = sx(splitX - d);
            int stripR = sx(splitX + d);

            g2.setColor(new Color(100, 220, 130, 65));
            g2.fillRect(stripL, top, stripR - stripL, bottom - top);

            g2.setColor(new Color(20, 150, 60));
            g2.setStroke(new BasicStroke(
                1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f, 5f}, 0f));

            g2.drawLine(stripL, top, stripL, bottom);
            g2.drawLine(stripR, top, stripR, bottom);

            // Dividing line.
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(
                1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                1f, new float[]{7f, 5f}, 0f));
            g2.drawLine(sx(splitX), top, sx(splitX), bottom);

            g2.setStroke(new BasicStroke(1f));

            // All points.
            g2.setColor(new Color(30, 90, 200));
            for (Point2D p : points)
                g2.fillOval(sx(p.x) - 4, sy(p.y) - 4, 8, 8);

            // Brute force pair: red.
            drawPair(g2, brutePair, Color.RED);

            // Divide and conquer pair: magenta.
            drawPair(g2, dcPair, Color.MAGENTA);

            int legendY = 35;
            g2.setColor(Color.BLUE);
            g2.drawString("Blue: Points", 15, legendY);
            g2.setColor(Color.RED);
            g2.drawString("Red: Brute-force pair", 15, legendY + 18);
            g2.setColor(Color.MAGENTA);
            g2.drawString("Magenta: D&C pair", 15, legendY + 36);
            g2.setColor(new Color(20, 150, 60));
            g2.drawString("Green: Strip (width 2d)", 15, legendY + 54);
            g2.setColor(Color.BLACK);
            g2.drawString("Dashed: Dividing line", 15, legendY + 72);
        }

        void drawPair(Graphics2D g2, Pair p, Color color) {
    if (p == null) return;

    int x1 = sx(p.a.x);
    int y1 = sy(p.a.y);
    int x2 = sx(p.b.x);
    int y2 = sy(p.b.y);

    // Draw the line joining the actual points.
    g2.setColor(color);
    g2.setStroke(new BasicStroke(2.0f));
    g2.drawLine(x1, y1, x2, y2);

    // Draw smaller, outlined markers at both endpoints.
    g2.setStroke(new BasicStroke(2.0f));
    g2.drawOval(x1 - 5, y1 - 5, 10, 10);
    g2.drawOval(x2 - 5, y2 - 5, 10, 10);

    // Draw small center dots.
    g2.fillOval(x1 - 2, y1 - 2, 4, 4);
    g2.fillOval(x2 - 2, y2 - 2, 4, 4);

    g2.setStroke(new BasicStroke(1.0f));
}
    }

    // Running-time vs N chart.
    class RuntimePanel extends JPanel {
        int[] sizes = {};
        double[] brute = {}, dc = {};

        RuntimePanel() {
            setBackground(Color.WHITE);
        }

        void setData(int[] s, double[] b, double[] d) {
            sizes = s;
            brute = b;
            dc = d;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            g2.setColor(Color.BLACK);
            g2.drawString("Running Time vs N", 15, 20);

            int l = 55, r = w - 20, t = 45, b = h - 55;

            g2.setColor(Color.GRAY);
            g2.drawLine(l, b, r, b);
            g2.drawLine(l, t, l, b);

            g2.drawString("Time (ms)", 5, 40);
            g2.drawString("Input size N", w / 2 - 30, h - 15);

            if (sizes.length == 0) {
                g2.drawString(
                    "Click Performance Analysis to plot the graph.",
                    25, 75);
                return;
            }

            double maxTime = 0;
            for (double v : brute) maxTime = Math.max(maxTime, v);
            for (double v : dc) maxTime = Math.max(maxTime, v);
            maxTime = Math.max(maxTime, 0.01) * 1.1;

            g2.setColor(new Color(225, 225, 225));
            for (int i = 0; i <= 5; i++) {
                int y = b - i * (b - t) / 5;
                g2.drawLine(l, y, r, y);
                g2.setColor(Color.BLACK);
                g2.drawString(
                    String.format("%.2f", maxTime * i / 5),
                    5, y + 5);
                g2.setColor(new Color(225, 225, 225));
            }

            for (int i = 0; i < sizes.length; i++) {
                int x = l + (int)(i * (r - l) /
                    (double)Math.max(1, sizes.length - 1));

                if (i > 0) {
                    int px = l + (int)((i - 1) * (r - l) /
                        (double)Math.max(1, sizes.length - 1));

                    g2.setColor(Color.RED);
                    g2.drawLine(px, yPos(brute[i - 1], maxTime, t, b),
                                x, yPos(brute[i], maxTime, t, b));

                    g2.setColor(Color.BLUE);
                    g2.drawLine(px, yPos(dc[i - 1], maxTime, t, b),
                                x, yPos(dc[i], maxTime, t, b));
                }

                g2.setColor(Color.RED);
                g2.fillOval(x - 4, yPos(brute[i], maxTime, t, b) - 4, 8, 8);

                g2.setColor(Color.BLUE);
                g2.fillRect(x - 4, yPos(dc[i], maxTime, t, b) - 4, 8, 8);

                g2.setColor(Color.BLACK);
                g2.drawString(String.valueOf(sizes[i]), x - 15, b + 18);
            }

            g2.setColor(Color.RED);
            g2.drawString("Red: Brute Force", l + 10, t + 15);
            g2.setColor(Color.BLUE);
            g2.drawString("Blue: Divide & Conquer", l + 10, t + 32);
        }

        int yPos(double value, double max, int top, int bottom) {
            return bottom - (int)(value / max * (bottom - top));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClosestPairVisualization app =
                new ClosestPairVisualization();
            app.setVisible(true);
        });
    }
}