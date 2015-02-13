package imagegrid;

import processing.core.PApplet;

@SuppressWarnings("serial")
public class GridElement extends PApplet{
	
	public int x, y;
	public boolean colored;
	
	public GridElement(int ics, int ips) {
		x = ics;
		y = ips;
		colored = false;
	}
}