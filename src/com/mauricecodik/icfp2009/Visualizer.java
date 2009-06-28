package com.mauricecodik.icfp2009;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Visualizer extends JFrame {
	
	int dim;
	double scale;
	VizPanel panel;
	
	public Visualizer(int dim, double scale) {
		super();
		this.dim = dim;
		this.scale = scale;
		
		panel = new VizPanel();
		panel.setPreferredSize( new Dimension(dim, dim) );
		
		add(panel);
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public int toPx(double pos) {
		return (int) ((pos * scale) + (dim/2));
	}
	
	private Set<Ellipse> ellipses = new HashSet<Ellipse>();
	
	private class Ellipse {
		int x; int y; int r1; int r2;
		public Ellipse(double x, double y, double r1, double r2) {
			this.x = toPx(x);
			this.y = toPx(y);
			this.r1 = (int)Math.round(r1 * scale);
			this.r2 = (int)Math.round(r2 * scale);
		}
		public Ellipse(double x, double y, double r1) {
			this(x,y,r1,r1);
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + r1;
			result = prime * result + r2;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Ellipse other = (Ellipse) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (r1 != other.r1)
				return false;
			if (r2 != other.r2)
				return false;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}
		private Visualizer getOuterType() {
			return Visualizer.this;
		}
	}

	private Map<String, List<Point>> points = new HashMap<String, List<Point>>();
	
	private class Point {
		int x; int y;
		public Point(double x, double y) {
			this.x = toPx(x);
			this.y = toPx(y);
		}
	}
	
	public synchronized void addCircle(double centerx, double centery, double radius) {
		ellipses.add(new Ellipse(centerx, centery, radius));
	}
	
	public synchronized void addPoint(String name, double x, double y) {
		List<Point> pts = points.get(name);
		if (pts == null) {
			pts = new ArrayList<Point>();
			points.put(name, pts);
		}
		
		if (pts.size() > 200) {
			pts.subList(0, 199).clear();
		}
		
		Point p = new Point(x,y);
		pts.add(p);
	}
	
	private class VizPanel extends JPanel {
		@Override
		public void paint(Graphics g) {
			super.paint(g);

			BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
			
			Graphics bg = img.getGraphics();
			bg.setColor(Color.WHITE);
			bg.fillRect(0, 0, dim, dim);
			
			Color[] colors = new Color[] { Color.BLACK, Color.RED, Color.ORANGE, Color.GREEN, Color.MAGENTA };

			int i = 0;
			//System.out.println("draw!");
			synchronized (Visualizer.this) {
				for (Ellipse c : ellipses) {
					if (i > colors.length-1) {
						i = 0;
					}
					bg.setColor(colors[i]);
					bg.drawOval(c.x-(c.r1), c.y-(c.r2), 2*c.r1, 2*c.r2);
					//System.out.println("drawing oval of radius: " + c.r1 + " at (" + c.x + ", " + c.y + ")");
					i++;
				}
				
				int k = 0;
				
				for (String name : points.keySet()) {
					if (i > colors.length) {
						i = 0;
					}
	
					for (Point p : points.get(name)) {
						bg.setColor(colors[i]);
						bg.fillRect(p.x, p.y, 2, 2);
						k++;
					//	System.out.println("  drawing point: " + p.x + ", " + p.y);
					}
	
					i++;
				}
			}
			g.drawImage(img, 0, 0, null);
		}
	}
}
