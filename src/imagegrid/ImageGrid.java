package imagegrid;

import java.io.File;

// import java.awt.Frame;
import netP5.NetAddress;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.IntList;
import imagegrid.MyGrid;
// import imagegrid.GridElement;
import imagegrid.Cerchio;
import oscP5.*;
// import fullscreen.*;

@SuppressWarnings("serial")
public class ImageGrid extends PApplet {
    
//	Frame fullScreenFrame;
	boolean isStarted;

	// cols > rows
	int rows = 5; // default 10
	int cols = 5; // default 10
	int imgNum = 15;
	int currImg; // = 0;
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

	PImage[] imgList;
	PImage img;
	PImage partImg;

	int minborder; // bordo minimo in pixel in caso di resize
	
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
	int deltaRedFFT = 3000;
	int deltaGreenFFT = 3000;
	int deltaBlueFFT = 25 * 50; // frames x seconds	

	Object[] redFFT, greenFFT, blueFFT; 

	boolean isRedFFT, isGreenFFT, isBlueFFT;
	int countRedFFT, countGreenFFT, countBlueFFT;

	/* circle network variables */
	Cerchio[] cerchi;
	int ccount = 0;
	int currentPointer = 0;
	boolean chcol = false;

	// picture fade control
	boolean isFade;
	boolean fadeIn = false;
	int fadeCount = 0;
	int fadeSteps = 75;
	int fadeTime, startFade;

	int startScreenshot, deltaScreenshot;
	
	// transition from picture to fade and from fade to new picture
	IntList imageElementsTrans;
	int startTrans, stepTrans, elementTrans, sizeTrans, countTrans, transImgNu;
	
	public void setup() {		
		
		isStarted = false;
		isRedFFT = false;

		frameRate(12); // we can change the frame rate according to our needs
		// size(1440,830, P2D);
		// size(1440,900, P2D);
		// minborder = 100;
		// size(1024,768, P2D);
		size(800,600, P2D);
		minborder = 30;
		// size(400, 300, P2D);
		// minborder = 20;
		// set the border accordingly to frame size
		
		 
//		frame.setLocation(0,0);
//		frame.setUndecorated(true);

		elNo = 0;
		frameStep = 1;
 
		// Inserire una funzione per la lettura di una cartella!!!
		/** image file selection read */
		this.imgList = new PImage[imgNum];
		for (int i=0; i<imgNum; i++) {
			imgList[i] = loadImage("wood_dumb_"+i+".png");
			imgList[i] = resizeImage(imgList[i]);
		}

		currImg = (int) random(imgList.length);
		
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

		this.pixelColors = readPixelColorColumn(1);
		println("Array size: "+pixelColors.length+" - sample: "+pixelColors[1]);

		// set OSC communication sender and receiver
		oscP5 = new OscP5(this, 12000);
		supercollider = new NetAddress("127.0.0.1",57120);

		int pixel = (int)random(img.pixels.length);
		float newCol = pixelColor(1, pixel);
		println("Selected pixel:"+pixel+" - Red value: "+newCol);
		sendColor(newCol, 1);

		// set the network array of object Cerchio
		cerchi = new Cerchio[grid.cols * grid.rows];

		isFade = false;
		fadeTime = 500;
		
		// startScreenshot, deltaScreenshot;
		startScreenshot = millis();
		deltaScreenshot = 15000;
		
		// transition 
		imageElementsTrans = new IntList();
		startTrans = frameCount; 
		stepTrans = 12;
		elementTrans = 0;
		countTrans = grid.rows * grid.cols;
		transImgNu = (int) random(imgList.length);
		
		  
		listLoad();

	}

	public void draw() {

		if (isStarted) {

			int now = millis();
			
			/** print out screenshots each deltaScreeshot (default 5000msec) */
			if (now >= startScreenshot+deltaScreenshot) {
				
				saveFrame("/Volumes/ssdData/saved_frames/imangeGrid-######.png");
				startScreenshot = millis();
				
			}

			if (isFade) {

				if (now <= startFade+fadeTime) {

					fadeToBlack();
					startFade = millis();
					fadeCount += 1;
					if (fadeCount >= fadeSteps) {

						isFade = false;
						fadeCount = 0;

						if (fadeIn == true) {

							fadeIn = false;
							restartProcess();

						} else {

							fadeIn = true;
							this.background(0);

						}

					}

				}

			} else {

				/** send color to SuperCollider */
				if (now >= startTred+deltaTred) {

					int pixel = (int)random(img.pixels.length);
					float newCol = pixelColor(1, pixel);
					//println("invio rosso: "+newCol);
					sendColor(newCol, 1);
					startTred = millis();

				}

				/** send color to SuperCollider */
				if (now >= startGreen+deltaTgreen) {

					int pixel = (int)random(img.pixels.length);
					float newCol = pixelColor(2, pixel); 
					sendColor(newCol, 2);
					startGreen = millis();

				}

				 if (now >= startTblue+deltaTblue) {

					int pixel = (int)random(img.pixels.length);
					float newCol = pixelColor(3, pixel);
					sendColor(newCol, 3);
					startTblue = millis();

				}

				if (isRedFFT) { // substitute grid element with parts from other picture

					if (now >= startRedFFT + deltaRedFFT) {
						
						if (countRedFFT >= redFFT.length-1 ) {
							
							resetRed();
							
						} else {
							
							// float el = Float.parseFloat(redFFT[countRedFFT].toString()) * (grid.rows * grid.cols);
							float el = random(grid.rows * grid.cols);
							int[] coords = coordsFromElement((int) el);
							int selectedimg = (int) random(imgNum-1);
							collageGridPos(coords, selectedimg);
							countRedFFT += 1;
							startRedFFT = millis();
							
						}
					}
				} // red fft manipulation
				
				if (isGreenFFT) { // creates transparent veil over grid element
					
					if ( now >= startGreenFFT + deltaGreenFFT) {
						
						if (countGreenFFT >= greenFFT.length-1) {

							resetGreen();
							
						} else {

							// draw rect
							float el = Float.parseFloat(greenFFT[countGreenFFT].toString()) * (grid.rows * grid.cols);
							int[] coords = coordsFromElement((int) el);
							noStroke();
							// abilita collage
							//v1 collageGridPos(coords);
							drawRect(coords);
							countGreenFFT += 1;
							startGreenFFT = millis();

						}
					}
				} // green fft manipulation
				
				if (isBlueFFT) {
					
					if ( now >= startBlueFFT + deltaBlueFFT) {
						
						if (countBlueFFT >= blueFFT.length-1) {
							
							resetBlue();
							
						} else {
							
							if (ccount == 0) {
								
								float el = Float.parseFloat(blueFFT[countBlueFFT].toString()) * (grid.rows * grid.cols);
								int[] coords = coordsFromElement((int) el); 
								// center the circle into the image area
								int ics, ipsilon;
								ics = ((grid.gridXstep * (coords[0]-1)) + borderX) + (int) random(grid.gridXstep/2);
								ipsilon = ((grid.gridYstep * (coords[1]-1)) + borderY) + (int) random(grid.gridYstep/2);
								if (ics < borderX) {
									
									ics = borderX;
									
								} else if (ics > img.width + borderX) {
									
									ics = img.width + borderX;
									
								}
								if (ipsilon < borderY) {
									
									ipsilon = borderY;
									
								} else if (ipsilon > img.height + borderY) {
									
									ipsilon = img.height - borderY;
									
								}
								cerchi[ccount] = new Cerchio(ics, ipsilon, random(50) + 20, this);
								cerchi[ccount].show();
								// println("Current circle, x: "+ics+" y: "+ipsilon);
								ccount += 1;
								

							} else if ( ccount <= cerchi.length-1) {
								
								float el = Float.parseFloat(blueFFT[countBlueFFT].toString()) * (grid.rows * grid.cols);
								int[] coords = coordsFromElement((int) el); 
								
								// println("Current element blue: "+el);
								
								// center the circle into the image area								
								int ics, ipsilon;
								ics = ((grid.gridXstep * (coords[0]-1)) + borderX) + (int) random(grid.gridXstep/2);
								ipsilon = ((grid.gridYstep * (coords[1]-1)) + borderY) + (int) random(grid.gridYstep/2);;
								if (ics < borderX) {
									
									ics = borderX;
									
								} else if (ics > img.width + borderX) {
									
									ics = img.width + borderX;
									
								}
								if (ipsilon < borderY) {
									
									ipsilon = borderY;
									
								} else if (ipsilon > img.height + borderY) {
									
									ipsilon = img.height - borderY;
									
								}
								// cerchi[ccount] = new Cerchio((grid.gridXstep * (coords[0]-1) + random(grid.gridXstep/2) ) + borderX, (grid.gridYstep * (coords[1]-1) + random(grid.gridYstep/2)) + borderY, random(50) + 20, this);
								cerchi[ccount] = new Cerchio(ics, ipsilon, random(50) + 20, this);
								cerchi[ccount].show();
								// println("Current circle, x: "+cerchi[ccount].posX+" y: "+cerchi[ccount].posY);
								
								// connect each circle with the precedent one
								smooth();
								strokeWeight(random(2)+0.5f);
								stroke(255, random(60) + 60);
								line(cerchi[ccount].posX, cerchi[ccount].posY, cerchi[ccount-1].posX, cerchi[ccount-1].posY);

								strokeWeight(1f);
								// reset or increment counter
								if (ccount == cerchi.length-1) {
								
									ccount = 0;
									isBlueFFT = false;
									
								} else {
									
									ccount += 1;
									
								}
								
							}
							
							countBlueFFT += 1;
							startBlueFFT = millis();
							
						}	
					}
				} // blue fft picture manipulation
				
				int thisFrame = frameCount;
				if (thisFrame >= startTrans+stepTrans ) {
				    
					int[] coords = coordsFromElement(imageElementsTrans.get(0));
					collageGridPos(coords, transImgNu);
					// println(imageElementsTrans.get(0));
				    imageElementsTrans.remove(0);
				    startTrans = frameCount;
				    
				    if ( elementTrans == sizeTrans-1  ) {
				    	
				    	elementTrans = 0;
						if (isFade) {
							isFade = false;
							listLoad();
						} else {
							isFade = true;
							startFade = millis();
							listLoad();
						}
						
				    } else {
				    	elementTrans+=1;
				    }
				} // transition management
				
			} // fade or not to fade
		} // isStarted - Used to start manually the application and synchronize video capture and audio
	} // end draw()

	/** Resize image accordingly with canvas size maintaining proportion */
	PImage resizeImage(PImage iimg) {
		  float resRatio, imgDimRatio;
		  int resX, resY;
		  imgDimRatio = width/height;
		  if (iimg.height<iimg.width) {

		    resRatio = (float) iimg.width / iimg.height;
		    if (resRatio<imgDimRatio) {
		    	resX = width - minborder; // cut the border
		    	resY = (int) floor(resX / resRatio);
		    } else {
		    	resY = height - minborder;
		    	resX = (int) floor(resY * resRatio);
		    }
		  } else { 
		    resRatio = (float) iimg.height / iimg.width;
		    resY = height - minborder; // cut the border
		    resX = (int) floor(resY / resRatio);
		  };
		  
		  println("larghezza: "+resX+" - altezza: "+resY);
		  iimg.resize(resX, resY);
		  return iimg;
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
		
		currImg = (int) random(imgList.length);
		img = imgList[currImg].get(0, 0, imgList[currImg].width, imgList[currImg].height);
		// change the current grid background
		grid.image(img);
		image(grid.immagine, borderX, borderY);
		
	}
	
	/** calculate the grid element from mouse position and return the coordinates in col/row */
	int[] gridPos() {
		
		int[] coords = new int[3];
		// if statements prevent drawing if click outside the picture
		if (mouseX>borderX && mouseX < (borderX + (cols * grid.gridXstep ))) {
			if (mouseY>borderY && mouseY < (borderY + (rows * grid.gridYstep))) {

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
		coords = new int[3];
		coords[0] = elementNumber % grid.cols;
		coords[1] = elementNumber / grid.rows;
		coords[2] = 255;
		return coords;
		
	}
	
	/** draw a random colored rectangle over a specific grid element */ 
	void drawRect(int[] coords) {
		
		rectMode(CORNER);
		fill(random(255), random(30));
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
	
	void collageGridPos(int[] gridCoord, int collageGridPosImgNum) {
		
		if (gridCoord[2] == 255) {
			
			// partImg = imgList[(int) random(imgNum-1)].get((grid.gridXstep * gridCoord[0]), (grid.gridYstep * gridCoord[1]), grid.gridXstep, grid.gridYstep);
			partImg = imgList[collageGridPosImgNum].get((grid.gridXstep * gridCoord[0]), (grid.gridYstep * gridCoord[1]), grid.gridXstep, grid.gridYstep);
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
		
		this.loadPixels();
		// int pixel = (int)random(img.pixels.length);
		float pixelCol = 0;
		
		switch (colore){
		case 1:
			pixelCol = red(this.pixels[pixel]);
			break;
		case 2:
			pixelCol = green(this.pixels[pixel]);
			break;
		case 3:
			pixelCol = blue(this.pixels[pixel]);
			break;
		}
		
		return pixelCol;
	}
	
	/** mask the current image using another image as alpha channel */
	void maskera() {
		
		PImage tempImg = imgList[2].get(0,0,imgList[2].width,imgList[2].height);
		img.mask(tempImg);
		image(img, borderX, borderY);
		
	}
	
	/** creates a fade effect (use 25 or more time to get the fade) */
	void fadeToBlack() {
		
		noStroke();
		fill(0, 10);
		rectMode(CORNER);
		rect(0,  0, this.width, this.height);
		
	}
	/** tree functions to reset the picture manipulation process */
	void resetRed() {
		
		isRedFFT = false;
		countRedFFT = 0;
		startRedFFT = millis();
		
	}
	void resetGreen() {
		
		startGreenFFT = millis();
		isGreenFFT = false;
		countGreenFFT = 0;
		
	}
	void resetBlue() {
		
		isBlueFFT = false;
		countBlueFFT = 0;
		startBlueFFT = millis();
		ccount = 0;
		
	}
	
	/** Crea una lista di interi con gli indici degli elementi della griglia
	 *  La lista viene utilizzata per:
	 *  modificare l'immagine e 
	 *  impostare la transizione tra un imagine e la successiva
	 *  */
	void listLoad() {
		  
		println("Ricarico la lista");
		for (int i=0;i<countTrans;i++) {  
			imageElementsTrans.append(i);
		}
		sizeTrans = imageElementsTrans.size();
		imageElementsTrans.shuffle();
		println("Elementi nella lista: "+sizeTrans);
		  
	}
	
	void restartProcess() {
		
		this.background(0);
		
		rows = (int) random(45) + 5;
		cols = (int) random(45) + 5;
		
		if (cols < rows ) {
			int tmp = rows;
			rows = cols;
			cols = tmp;
		}
		
		transImgNu = (int) random(imgList.length-1);
		currImg = (int) random(imgList.length-1);
		
		img = imgList[currImg].get(0, 0, imgList[currImg].width, imgList[currImg].height);

		grid = new MyGrid(rows, cols, img);
		cerchi = new Cerchio[cols * rows];

		this.resetRed();
		this.resetGreen();
		this.resetBlue();
		
		randomImage(); // this should be changed to transition image
		int gel = rows * cols;
		println("righe: "+rows+" - colonne: "+cols+" - grid elem: "+gel);
		println("Number of circles: "+cerchi.length);
		println("x step: "+grid.gridXstep+" - y step: "+grid.gridYstep+" - image size: "+img.width+","+img.height);
		countTrans = grid.rows * grid.cols;
		listLoad();
			
	}
	
	//////////////// O S C   M E T H O D S //
	
	/** 
	 * send a single frequency
	 * trigger a single note synthesizer in SuperCollider 
	 * */
	void sendColor(float freq, int colore) {
		OscMessage redmess;
		// println("frequenza inviata: "+freq);
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
			
			if (!isRedFFT) {
				// Set the global variables
				redFFT = incomingOscMessage.arguments();
				isRedFFT = true;
				startRedFFT = millis();
				countRedFFT = 0;
			}
		    return;
		    
		}
		if (incomingOscMessage.checkAddrPattern("/greenFFT")==true) {
			
			// the array content: 
			greenFFT = incomingOscMessage.arguments();
			isGreenFFT = true;
			startGreenFFT = millis();
			countGreenFFT = 0;
			
			return;
			
		}
		if (incomingOscMessage.checkAddrPattern("/blueFFT")==true) {
			
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
		// randomImage();
		// println("Mouse click disabled!");
		int[] coords = gridPos();
		println("Coord x: "+coords[0]+" - y: "+coords[1]);
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
		if (key == 'i' ) {
			saveFrame("./saved_frames/imangeGrid-######.png");
		}
		if (key == 'f') {
			
			// this code will become a transformation function depending on internal timing
			if (isFade) {
				isFade = false;
			} else {
				isFade = true;
				startFade = millis();
			}
		}
		if (key == 'p'){
			
			// this code will become a transformation function depending on internal timing
			this.background(0);
			
			rows = (int) random(45) + 5;
			cols = (int) random(45) + 5;
			
			if (cols < rows ) {
				int tmp = rows;
				rows = cols;
				cols = tmp;
			}

			this.grid = new MyGrid(rows, cols, img);	
			this.cerchi = new Cerchio[grid.cols * grid.rows];

			this.resetRed();
			this.resetGreen();
			this.resetBlue();

			randomImage();
			int gel = grid.rows * grid.cols;
			println("righe: "+rows+" - colonne: "+cols+" - grid elem: "+gel);
			println("Number of circles: "+cerchi.length);

		}

	}

	/** This function set the frame caracteristics */
	public void init(){
        if(frame!=null){
          frame.removeNotify();//make the frame not displayable
          frame.setResizable(false);
          frame.setUndecorated(true);
          println("frame is at "+frame.getLocation());
          frame.addNotify();
        }
      super.init();
	}
	
	/** program entry point */
	public static void main(String args[]) {


		// PApplet.main(new String[] { "--present", imagegrid.ImageGrid.class.getName() });
		// PApplet.main(new String[] {imagegrid.ImageGrid.class.getName()} );
		PApplet.main(new String[] {"--hide-stop", imagegrid.ImageGrid.class.getName()});

	}
}