package imagegrid;

import processing.core.PApplet;

public class Cerchio {
	PApplet parent;

	    float posX, posY, radA, radB;
	    boolean crc;
	    int colore;
	    Cerchio(float x, float y, float rad1, PApplet p ) {
	        parent = p;
	    	posX = x;
	        posY = y;
	        radA = rad1;
	        radB = -1;
	        crc = true;
	        colore = parent.color(255, 30);
	    }
	    public void show() {
	      if (radB == -1) {
	        radB = radA;
	      }
	      parent.noStroke();
	      parent.fill(this.colore);
	      parent.ellipse(posX, posY, radA, radB);
	    }
	    void setEllipse(float rad2) {
	      radB = rad2;
	      if (radB != radA) {
	        crc = false;
	      }
	    }
	    void setColor(int c) {
	      this.colore = c;
	    }
	}
