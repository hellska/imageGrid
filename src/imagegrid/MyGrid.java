package imagegrid;

import processing.core.PApplet;
import processing.core.PImage;

@SuppressWarnings("serial")
public class MyGrid extends PApplet{

	public int rows;
	public int cols;
	public int elements;
	public int gridXstep, gridYstep;
	PImage immagine;
	
	public boolean[] gridPosStatus;

	public MyGrid(int r, int c, PImage img) {

		this.rows = r;
		this.cols = c;
		this.elements = r * c;
		this.gridXstep  = floor(img.width / c);
		this.gridYstep = floor(img.height / r);
		this.immagine = img.get(0, 0, img.width, img.height);
		
		this.gridPosStatus = new boolean[r*c];
		for (int i=0;i < this.gridPosStatus.length;i++) {
			this.gridPosStatus[i] = false;
		}
	}
	
	public void image(PImage img) {
		
		this.immagine = img;
		
	}

}