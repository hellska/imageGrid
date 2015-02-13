package imagegrid;

import java.io.File;

import netP5.NetAddress;
import processing.core.PApplet;
import processing.core.PImage;
import imagegrid.MyGrid;
import imagegrid.GridElement;
import oscP5.*;
// import fullscreen.*;

@SuppressWarnings("serial")
public class ImageGrid extends PApplet {
	
	boolean isStarted;

	int rows = 20;
	// cols > rows
	int cols = 30;
	int imgNum = 5;
	int currImg = 0;
	int drawmode = 0;
	
	int borderX, deltaImgX, borderY, deltaImgY;
	
	/** Set the transition characteristics from an image to another 
	 * @startFrame: the initial frame of the transition
	 * @frameStep: delta frames from a transition status to another
	 * @elNo: grid elements counter (reset on each transition) 
	 * */
	int startFrame, frameStep, elNo;
	
	File dir;
	MyGrid grid;
	
	GridElement[] gElement;
	
	PImage[] imgList;
	PImage img;
	PImage partImg;
	
	float[] pixelColors;
	
	OscP5 oscP5;
	NetAddress supercollider;
	
	// Variable for timing functions
	int startTred = millis();
	int startTblue = millis();
	int startGreen = millis();
	
	// Time between each list is send
	int deltaTred = 10000;
	int deltaTblue = 1000;
	int deltaTgreen = 12000;
	
	int startRedFFT, startGreenFFT, startBlueFFT;
	int deltaRedFFT = 300;
	int deltaGreenFFT = 300;
	int deltaBlueFFT = 300;
	
	Object[] redFFT, greenFFT, blueFFT; 
	
	boolean isRedFFT, isGreenFFT, isBlueFFT;
	int countRedFFT, countGreenFFT, countBlueFFT;
	
	public void setup() {
		
		isStarted = false;
		isRedFFT = false;
		
		frameRate(25);
		size(1440,830, P2D);
		// size(1840,870, P2D);
		
		
		elNo = 0;
		frameStep = 1;
		
		/** image file selection read */
		this.imgList = new PImage[imgNum];
		for (int i=0; i<imgNum; i++) {
			imgList[i] = loadImage("wood_dumb_"+i+".png");
		}
		
		// use fixed image for testing
		// img = loadImage("wood_dumb_0.png");
		img = imgList[currImg].get(0, 0, imgList[currImg].width, imgList[currImg].height);

		// this one set a local variable
		// MyGrid grid = new MyGrid(rows, cols); 
		this.grid = new MyGrid(rows, cols, img);
		
	    /** calculate border <<<--- put into MyGrid class */
		deltaImgX = width - img.width;
		deltaImgY = height - img.height;
		borderX = deltaImgX / 2;
		borderY = deltaImgY / 2;
		
		this.background(0);
		// show the selected image
		image(img, borderX, borderY);
		
		// imageGrid();
		
		// create array of object
		this.gElement = new GridElement[rows*cols];
		
		for (int r=0;r<rows;r++) {
			for (int c=0;c<cols;c++) {
				this.gElement[r * c + c] = new GridElement((grid.gridXstep * r) + borderX, (grid.gridYstep * c) + borderY);
			}
		}
		
		this.pixelColors = readPixelColorColumn(1);
		println("Array size: "+pixelColors.length+" - sample: "+pixelColors[500]);
		
		// set OSC communication sender and receiver
		oscP5 = new OscP5(this, 12000);
		supercollider = new NetAddress("127.0.0.1",57120);
		
		int pixel = (int)random(img.pixels.length);
		float newCol = pixelColor(1, pixel);
		println("colore: "+newCol);
		sendColor(newCol, 1);
		
	}

	public void draw() {
		
		if (isStarted) {
			
		int now = millis();
		
		switch(drawmode) {
		case 0:
			drawGridPos();
			elNo = 0;
			break;
		case 1:
			collageGrid();
			elNo = 0;
			break;
		case 2:
			// startFrame = this.frameCount;
			restoreImageGrid(elNo);
			if (elNo >= grid.elements - 1) {
				elNo = 0;
				drawmode = 0;
			} else {
				elNo = elNo + 1;
				startFrame = this.frameCount;
			}
			break;
		}
		
		if (now >= startTred+deltaTred) {
			
			int pixel = (int)random(img.pixels.length);
			float newCol = pixelColor(1, pixel);
			println("invio rosso: "+newCol);
			sendColor(newCol, 1);
			startTred = millis();
			
		}
		
		if (now >= startGreen+deltaTgreen) {
			
			int pixel = (int)random(img.pixels.length);
			float newCol = pixelColor(2, pixel); 
			println("invio verde: "+newCol);
			sendColor(newCol, 2);
			startGreen = millis();
			
		}
		
		 if (now >= startTblue+deltaTblue) {
		    
			int pixel = (int)random(img.pixels.length);
			float newCol = pixelColor(3, pixel);
			println("invio blu: "+newCol);
			sendColor(newCol, 3);
			startTblue = millis();
			
		}
		 
		if (isRedFFT) {
			
			if (now >= startRedFFT+deltaRedFFT) {
				
				if (countRedFFT >= redFFT.length-1 ) {
					
					isRedFFT = false;
					println("Fine elaborazione array redFFT");
					
				} else {
					
					startRedFFT = millis();
					countRedFFT += 1;
					float el = gridElementFromFFT(redFFT[countRedFFT]);
					int elem = (int)el - 1;
					// int[] coords = new int[0];
					int[] coords = coordsFromElement(elem);
					collageGridPos(coords);
					
					// println("Elemento Corrente: "+counterFFT);
					
				}
			}
		}
		
		if (isGreenFFT) {
			
			if ( now >= startGreenFFT + deltaGreenFFT) {
				
				println("green IN");
				for (int e=0;e<=greenFFT.length-1;e++) {
					
					startGreenFFT = millis();
					float el = gridElementFromFFT(greenFFT[e]);
					int elem = (int)el - 1;
					int[] coords = coordsFromElement(elem);
					// Chiamata alla funzione	
					collageGridPos(coords);
					
				}
				println("green OUT");
				isGreenFFT = false;
			}
		}
		 
		} // isStarted - Used to start manually the application and synchronize video capture and audio
		
	}
	
	/** draw a grid over the image area */
	void imageGrid() {
		
		stroke(155);
		
	    for (int r=1;r<cols;r++) {
	        line(grid.gridXstep * r + borderX, borderY, grid.gridXstep * r + borderX, height - borderY);
	        //println("row: "+r);
	    }
	    for (int c=1;c<rows;c++) {
	    	line(borderX, grid.gridYstep * c + borderY, width - borderX, grid.gridYstep * c + borderY);
	    	//println("col: "+c);
	    }
	    
	}
	
	/** draw a grid over the entire PApplet area */	
	void appletGrid() {

	    stroke(0);
	    
	    grid.gridXstep = floor(width/cols);
	    grid.gridYstep = floor(height/rows);
	    
	    for (int r=1;r<cols;r++) {
	        line(grid.gridXstep * r, 0, grid.gridXstep * r, height);
	    }
	    for (int c=1;c<rows;c++) {
	    	line(0, grid.gridYstep * c, width, grid.gridYstep * c);
	    }
	}

	/** select random image from the image list array and show in the PApplet */
	void randomImage() {
		
		currImg = (int) random(4);
		img = imgList[currImg].get(0, 0, imgList[currImg].width, imgList[currImg].height);
		image(img, borderX, borderY);
		
	}
	
	/** calculate the grid element from mouse position and return the coordinates in col/row */
	int[] gridPos() {
		
		int[] coords = new int[3];
		// if statements prevent drawing if click outside the picture
		if (mouseX>borderX && mouseX < (borderX + img.width)) {
			if (mouseY>borderY && mouseY < (borderY + img.height)) {

				coords[0] = (int) floor((mouseX - borderX) / grid.gridXstep);
				coords[1] = (int) floor((mouseY - borderY) / grid.gridYstep);
				coords[2] = 255;
				
			}
		} else {
			coords[2] = 0;
		}
		return coords;
	
	}

	/** Calculate the position relative to the value of the FFT array values
	 * The FFT amplitude array is sent by SuperCollider
	 *  */
	int gridElementFromFFT(Object fftBin) {
		
		int element = -1;
		
		// I M P L E M E N T   T H I S  ! ! !
		
		float el = Float.parseFloat(fftBin.toString());
		el = el * (grid.rows * grid.cols);
		element = (int) el;
		
		// println("oggetto: "+fftBin+" - elem: "+element);
		
		return element;
		
	}

	/** calculate position in row/col by element number */
	int[] coordsFromElement(int elementNumber) {
		
		int[] coords;
		// int elementNumber = elNo;
		coords = new int[3];
		if (frameCount >= startFrame + frameStep) {

			if (elementNumber >= grid.cols) {
				coords[1] = elementNumber % grid.cols;
			} else {
				coords[1] = elementNumber;
			}
			if (elementNumber >= grid.rows && elementNumber >= grid.cols) {
				coords[0] = elementNumber / grid.rows /  (grid.cols / grid.rows);
			} else {
				coords[0] = 0;
			}
			coords[2] = 255;
		}
		return coords;
		
	}
	
	/** not implemented yet */
	void gridToPixel(int row, int col, MyGrid gr) {
		// Implement this method
	}
	
	/** draw a random colored rectangle over a specific grid element */ 
	void drawRect(int[] coords) {
		
		rectMode(CORNER);
		fill(random(80), random(255), random(255), random(255));
		rect((grid.gridXstep * coords[0]) + borderX, (grid.gridYstep * coords[1]) + borderY, grid.gridXstep, grid.gridYstep);
		
	}
	
	/** draw transparent colored layer over the image in the grid position
	* 		OR
	* overwrite colored layer with the current image portion
	*/
	void drawGridPos() {
		
		int gridCoord[] = gridPos();
		if (gridCoord[2] == 255) {
			if ( grid.gridPosStatus[gridCoord[0]*gridCoord[1]+gridCoord[1]] == false) {
				
				noStroke();
				drawRect(gridCoord);
				grid.gridPosStatus[gridCoord[0]*gridCoord[1]+gridCoord[1]] = true;
		
			} else {
	
				partImg = img.get((grid.gridXstep * gridCoord[0]), (grid.gridYstep * gridCoord[1]), grid.gridXstep, grid.gridYstep);
				image(partImg, (grid.gridXstep * gridCoord[0]) + borderX, (grid.gridYstep * gridCoord[1]) + borderY);
				grid.gridPosStatus[gridCoord[0]*gridCoord[1]+gridCoord[1]] = false;
		
			}
			
		}
	}
	
	/** select the grid element from another picture */
	void collageGrid() {
		
		int gridCoord[] = gridPos();
		
		if (gridCoord[2] == 255) {
			partImg = imgList[(int) random(4)].get((grid.gridXstep * gridCoord[0]), (grid.gridYstep * gridCoord[1]), grid.gridXstep, grid.gridYstep);
			image(partImg, (grid.gridXstep * gridCoord[0]) + borderX, (grid.gridYstep * gridCoord[1]) + borderY);		
		}
			
	}
	
	void collageGridPos(int[] gridCoord) {
		
		if (gridCoord[2] == 255) {
			partImg = imgList[(int) random(4)].get((grid.gridXstep * gridCoord[0]), (grid.gridYstep * gridCoord[1]), grid.gridXstep, grid.gridYstep);
			image(partImg, (grid.gridXstep * gridCoord[0]) + borderX, (grid.gridYstep * gridCoord[1]) + borderY);		
		}
			
	}
	
	/** restore the current image element in the grid */
	void restoreImageGrid(int elNo) {
		
		int col, row;
		int elementNumber = elNo;
		if (frameCount >= startFrame + frameStep) {

			if (elementNumber >= grid.cols) {
				col = elementNumber % grid.cols;
			} else {
				col = elementNumber;
			}
			if (elementNumber >= grid.rows && elementNumber >= grid.cols) {
				row = elementNumber / grid.rows / (grid.cols / grid.rows);
			} else {
				row = 0;
			}

			partImg = imgList[currImg].get(grid.gridXstep*col, grid.gridYstep*row,grid.gridXstep,grid.gridYstep);
			image(partImg, (grid.gridXstep*col) + borderX, (grid.gridYstep*row) + borderY);
			
		}
		
	}

	/** this randomly select an image pixels column */
	float[] readPixelColorColumn(int colore) {
		img.loadPixels();

		int col = (int)(random(img.height));
		float[] pixelCol = new float[0];

		switch (colore) {
		case 1:
			for (int r = 0;r < img.height-1;r++) {
				pixelCol = append(pixelCol, red(img.pixels[col * r + 1]));
			}
			break;
		case 2:
			for (int r = 0;r < img.height-1;r++) {
				pixelCol = append(pixelCol, green(img.pixels[col * r + 1]));
			}
			break;
		case 3:
			for (int r = 0;r < img.height-1;r++) {
				pixelCol = append(pixelCol, blue(img.pixels[col * r + 1]));
			}
			break;
		}
		
		return pixelCol;
		
	}
	
	/** return one color value of a single pixel, color value in RGB color mode */
	float pixelColor(int colore, int pixel) {
		img.loadPixels();
		// int pixel = (int)random(img.pixels.length);
		float pixelCol = 0;
		
		switch (colore){
		case 1:
			pixelCol = red(img.pixels[pixel]);
			break;
		case 2:
			pixelCol = green(img.pixels[pixel]);
			break;
		case 3:
			pixelCol = blue(img.pixels[pixel]);
			break;
		}
		
		return pixelCol;
	}
	
	/** mask the current image using aother image as alpha channel */
	void maskera() {
		
		PImage tempImg = imgList[2].get(0,0,imgList[2].width,imgList[2].height);
		img.mask(tempImg);
		image(img, borderX, borderY);
		
	}
	
	//////////////// O S C   M E T H O D S //
	
	/** send a single freq and trigger a single note synth in supercollider */
	void sendColor(float freq, int colore) {
		OscMessage redmess;
		println("frequenza inviata: "+freq);
		switch (colore) {
		case 1:
			redmess = new OscMessage("/redNote");
			redmess.add(freq);
			oscP5.send(redmess, supercollider);
			break;
		case 2:
			redmess = new OscMessage("/greenNote");
			redmess.add(freq);
			oscP5.send(redmess, supercollider);
			break;
		case 3:
			redmess = new OscMessage("/blueNote");
			redmess.add(freq);
			oscP5.send(redmess, supercollider);
			break;
		}
			
	}

	/** Incoming OSC messages management */
	void oscEvent(OscMessage incomingOscMessage) {
		//  print("### OSC Message Received ###");
		if (incomingOscMessage.checkAddrPattern("/column")==true) {
			// float inputVal = incomingOscMessage.get(0).floatValue();
			// println("Column input: "+inputVal+" testing!");
		    return;
		}
		// Manage input red delay from Supercollider
		if (incomingOscMessage.checkAddrPattern("/deltaTred")==true) {
			deltaTred = incomingOscMessage.get(0).intValue();
			// println("Next Red notes series sent in: "+deltaTred+" milliseconds!");
		    return;
		}
		// Manage input blue delay from Supercollider
		if (incomingOscMessage.checkAddrPattern("/deltaTblue")==true) {
			deltaTblue = incomingOscMessage.get(0).intValue();
			// println("Next Blue notes list sent in: "+deltaTblue+" milliseconds!");
		    return;
		} 
		if (incomingOscMessage.checkAddrPattern("/deltaTgreen")==true) {
		    deltaTgreen = incomingOscMessage.get(0).intValue();
		    //println("Next Greeb notes list sent in: "+deltaTgreen+" milliseconds!");
		    return;
		}
		if (incomingOscMessage.checkAddrPattern("/testFFT")==true) {
		    
		    Object[] input = incomingOscMessage.arguments();
		    int lungo = input.length;
		    println("ciao "+input[(int)random((float)lungo)]+" - lunghezza: "+lungo);
		    return;
		    
		}
		if (incomingOscMessage.checkAddrPattern("/redFFT")==true) {
			
			// Set the global variables
		    redFFT = incomingOscMessage.arguments();
		    isRedFFT = true;
		    startRedFFT = millis();
		    countRedFFT = 0;
		    
		    // int lungo = redFFT.length;
		    // println("Received FFT "+redFFT[(int)random((float)lungo)]+" - length: "+lungo);
		    return;
		    
		}
		if (incomingOscMessage.checkAddrPattern("/greenFFT")==true) {
			
			greenFFT = incomingOscMessage.arguments();
			isGreenFFT = true;
			startGreenFFT = millis();
			countGreenFFT = 0;
			
			return;
			
		}
		if (incomingOscMessage.checkAddrPattern("/blueFFF")==true) {
			
			blueFFT = incomingOscMessage.arguments();
			isBlueFFT = true;
			startBlueFFT = millis();
			countBlueFFT = 0;
			
		}
	}
	
	//////////////// P U B L I C   M E T H O D S //
	
	/** Change the current image
	 * the new image is selected from the list in imgList
	 * convenience method to manual test the application
	 *  */
	public void mouseClicked() {
		
		randomImage();
		
	}	
	
	/** change the draw mode with keyboard */
	public void keyPressed() {
		
		/** change the current drawing style */
		if ( key == '1') {
			drawmode = 0; 
		}
		if (key == '2') {
			drawmode = 1;
		}
		if (key == '3') {
			drawmode = 2;
			startFrame = this.frameCount;
		}
		/** Mask the current image with another image */
		if (key == 'm') {
			maskera();
		}
		/** test the array creation */
		if (key == 'a') {
			this.pixelColors = readPixelColorColumn(1);
			println("Array size: "+pixelColors.length+" - sample: "+pixelColors[500]);
		}
		if (key == 's') {
			isStarted = true;
		}
		// send colors to supercollider
		if (key == 'r') {
			int pixel = (int)random(img.pixels.length);
			float newCol = pixelColor(1, pixel); 
			println("colore: "+newCol);
			sendColor(newCol, 1);
		}
		if (key == 'g') {
			int pixel = (int)random(img.pixels.length);
			float newCol = pixelColor(2, pixel); 
			println("colore: "+newCol);
			sendColor(newCol, 2);
		}
		if (key == 'b') {
			int pixel = (int)random(img.pixels.length);
			float newCol = pixelColor(3, pixel); 
			println("colore: "+newCol);
			sendColor(newCol, 3);
		}
		if (key == 'c') {
			coordsFromElement(30);
		}
		
	}
	
	/** program entry point */
	public static void main(String _args[]) {
		
		PApplet.main(new String[] { imagegrid.ImageGrid.class.getName() });
		
	}
	
}