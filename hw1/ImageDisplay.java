
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.ArrayList;

import java.awt.Color;
import java.lang.IllegalArgumentException;
import java.lang.IndexOutOfBoundsException;
import java.lang.Math;

/*
Seam Carving code link:
https://www.programcreek.com/java-api-examples/index.php?source_dir=MOOC-master/Algorithms/seamCarving/SeamCarver.java#
*/

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 960;
	int height = 540;
	int frameLength = width*height*3;
	int newWidth;
	int newHeight;
	int[][] averagePixels;

    //Width of the picture
    private double[] energy; 
    private int[] pathTo; 
 
    // width of current picture 
    public int width() { 
        return imgOne.getWidth(); 
    } 
 
    // height of current picture 
    public int height() { 
        return imgOne.getHeight(); 
    } 
     
    private double gradient(Color x, Color y) { 
        double r = x.getRed() - y.getRed(); 
        double g = x.getGreen() - y.getGreen(); 
        double b = x.getBlue() - y.getBlue(); 
        return r*r + g*g + b*b; 
    } 
 
    // energy of pixel at column x and row y 
    public double energy(int x, int y) { 
        if (x < 0 || x >= width() || y < 0 || y >= height()) 
            throw new IndexOutOfBoundsException(); 
        if (x == 0 || y == 0 || x == width()-1 || y == height()-1) 
            return 195075; 
        return gradient(new Color(imgOne.getRGB(x-1, y)), new Color(imgOne.getRGB(x+1, y))) + gradient(new Color(imgOne.getRGB(x, y-1)), new Color(imgOne.getRGB(x, y+1))); 
    } 
 
    private double energy(int x, int y, int flag) { 
        if (flag == 1) 
            return energy(y, x); 
        else 
            return energy(x, y); 
    } 
 
    private void computeEnergy(int w, int h, int flag) { 
        //double maxE = 0; 
        energy = new double[w*h]; 
        for (int r = 0; r < h; r++) 
            for (int c = 0; c < w; c++) { 
                energy[r*w + c] = energy(c, r, flag); 
                //maxE = Math.max(maxE, energy[r*w + c]); 
            } 
    } 
 
    private int[] computePath(int w, int h) { 
        pathTo = new int[w*h]; 
        for (int i = 0; i < w; i++) 
            pathTo[i] = -1; 
        for (int r = 1, i = w; r < h; r++) { 
            if (energy[i-w] <= energy[i-w+1]) pathTo[i] = i-w; 
            else pathTo[i] = i-w+1; 
            energy[i] += energy[pathTo[i]]; i++; 
            for (int c = 1; c < w-1; c++, i++) { 
                if (energy[i-w-1] <= energy[i-w]) { 
                    if (energy[i-w-1] <= energy[i-w+1]) pathTo[i] = i-w-1; 
                    else pathTo[i] = i-w+1; 
                } else { 
                    if (energy[i-w] <= energy[i-w+1]) pathTo[i] = i-w; 
                    else pathTo[i] = i-w+1; 
                } 
                energy[i] += energy[pathTo[i]]; 
            } 
            if (energy[i-w-1] <= energy[i-w]) pathTo[i] = i-w-1; 
            else pathTo[i] = i-w; 
            energy[i] += energy[pathTo[i]]; i++; 
        } 
 
        int pathEnd = w*(h-1); 
        double minE = energy[w*(h-1)]; 
        for (int i = w*(h-1); i < w*h; i++) { 
            if (minE > energy[i]) { 
                minE = energy[i]; 
                pathEnd = i; 
            } 
        } 
 
        int[] path = new int[h]; 
        for (int p = pathEnd; p >= 0; p = pathTo[p]) 
            path[p/w] = p % w; 
        return path; 
    } 
 
    // sequence of indices for horizontal seam 
    public   int[] findHorizontalSeam() { 
        int w = height(), h = width(); 
        computeEnergy(w, h, 1); 
        return computePath(w, h); 
    } 
 
    // sequence of indices for vertical seam 
    public   int[] findVerticalSeam() { 
        int w = width(), h = height(); 
        computeEnergy(w, h, 0); 
        return computePath(w, h); 
    } 
 
    // remove horizontal seam from picture 
    public    void removeHorizontalSeam(int[] a) { 
        if (height() <= 1) 
            throw new IllegalArgumentException(); 
        if (a.length != width()) 
            throw new IllegalArgumentException("Wrong Length"); 
 
        BufferedImage p = new BufferedImage(width(), height()-1,BufferedImage.TYPE_INT_RGB); 
        //Picture seamPic = new Picture(pic); 
        int prerow = a[0]; 
        for (int c = 0; c < width(); c++) { 
            if (Math.abs(a[c] - prerow) > 1) 
                throw new IllegalArgumentException("Non-valid seam"); 
            if (a[c] < 0 || a[c] >= height()) 
                throw new IndexOutOfBoundsException(); 
            //seamPic.set(c, a[c], java.awt.Color.red); 
            prerow = a[c]; 
            for (int r = 0; r < height()-1; r++) 
                if (r < a[c]) 
                    p.setRGB(c, r, imgOne.getRGB(c, r)); 
                else 
                    p.setRGB(c, r, imgOne.getRGB(c, r+1)); 
        } 
        imgOne = p; 
        energy = null; 
        pathTo = null; 
        //seamPic.show(); 
    } 
 
    // remove vertical seam from picture 
    public void removeVerticalSeam(int[] a) { 
        if (width() <= 1) 
            throw new IllegalArgumentException(); 
        if (a.length != height()) 
            throw new IllegalArgumentException("Wrong Length"); 
 
        BufferedImage p = new BufferedImage(width()-1, height(),BufferedImage.TYPE_INT_RGB); 
        //Picture seamPic = new Picture(pic); 
        int precol = a[0]; 
        for (int r = 0; r < height(); r++) { 
            if (Math.abs(a[r] - precol) > 1) 
                throw new IllegalArgumentException("Non-valid seam"); 
            if (a[r] < 0 || a[r] >= width()) 
                throw new IndexOutOfBoundsException(); 
            //seamPic.set(a[r], r, java.awt.Color.red); 
            precol = a[r]; 
            for (int c = 0; c < width()-1; c++) 
                if (c < a[r]) 
                    p.setRGB(c, r, imgOne.getRGB(c, r)); 
                else 
                    p.setRGB(c, r, imgOne.getRGB(c+1, r)); 
        } 
        imgOne = p; 
        energy = null; 
        pathTo = null; 
        //seamPic.show(); 
    } 

//////////////////////////////////////////////////////////////////////////////////////////////


	private int numberOfFrames(File file){		
		long len = frameLength;
		long fileLen = file.length();
		return (int) Math.floor(fileLen/len);
	}

	private void readImageRGBWithAnalysisA(File file, int width, int height, int frameIndex, double wScale, double hScale, int antiAliasing)
	{
		int wfactor = (int)Math.floor(1.0/wScale);
		int hfactor = (int)Math.floor(1.0/hScale);

		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "r");

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			long pos = frameIndex*frameLength;
			raf.seek(pos);
			raf.read(bytes);

			int[][] orginPixels= new int[width][height];

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					orginPixels[x][y]=pix;
					ind++;
				}
			}

			if(antiAliasing==1){
				CalculateAveragePixels(bytes);
				orginPixels = averagePixels;
			}

			
			double cenPercent=0.8;
			int cenfactor,cenWidth,cenHeight;
			if(wScale<hScale){
				
				cenWidth = (int)Math.floor(newWidth*cenPercent);
				cenHeight = (int)Math.floor(wScale*cenPercent*height);
				cenfactor= (int)Math.floor(width*cenPercent/cenWidth);
			}else{
				
				cenHeight = (int)Math.floor(newHeight*cenPercent);
				cenWidth = (int)Math.floor(hScale*cenPercent*width);
				cenfactor= (int)Math.floor(width*cenPercent/cenWidth);
			}

			int croppedWidth = (int)Math.floor(width*cenPercent);
			int croppedHeight = (int)Math.floor(height*cenPercent);
			int croppedX0 = (int)Math.floor(width*(1.0-cenPercent)/2);
			int croppedY0 = (int)Math.floor(height*(1.0-cenPercent)/2);
			
			//paint major central part maintaing the same pixel aspect ratio
			int xc=(newWidth/2);
			int yc=(newHeight/2);
			int x1=xc - cenWidth/2;
			int y1=yc - cenHeight/2;
			int x2=xc-1+cenWidth/2;
			int y2=yc-1+cenHeight/2;

			int origin_x=0,origin_y=0;
			for(int y = y1; y <=y2; y++)
			{
				origin_y = (int) (((y-y1)*croppedHeight/cenHeight)+croppedY0);				
				for(int x=x1;x<=x2;x++){
					origin_x = (int) (((x-x1)*croppedWidth/cenWidth)+croppedX0);					
					imgOne.setRGB(x,y,orginPixels[origin_x][origin_y]);	
				}
			}
			
			//paint nonlinearly on peripheral areas
			
			//handle the top and buttom peripheral areas
			int topheight = y1;
			int originTopHeight = (int) Math.floor(height*(1.0 - cenPercent)/2);
			origin_x=0;
			origin_y=0;
			for(int y = y1-1; y >=0; y--)
			{
				int xstart=y*x1/y1;
				int xend=newWidth-xstart;
				
				origin_y = (int) Math.floor(y*originTopHeight/topheight);
				int origin_xstart = origin_y*width/height;
				int origin_xend = width - origin_xstart-1;
				
				for(int x=xstart;x<xend;x++){

					origin_x = (int)(((x-xstart)*(origin_xend - origin_xstart+1))/(xend-xstart)+origin_xstart);
					imgOne.setRGB(x,y,orginPixels[origin_x][origin_y]);
					imgOne.setRGB(x,newHeight-y-1,orginPixels[origin_x][height - origin_y - 1]);
				}
			}

			//handle the left and right peripheral areas
			int leftwidth = x1;
			int originLeftWidth = (int) Math.floor(width*(1.0 - cenPercent)/2);
			origin_x=0;
			origin_y=0;
			for(int x = x1-1; x >=0; x--)
			{
				int ystart=x*y1/x1;
				int yend=newHeight-ystart;
				
				origin_x = (int) Math.floor(x*originLeftWidth/leftwidth);
				int origin_ystart = origin_x*height/width;
				int origin_yend = height - origin_ystart-1;
				
				for(int y=ystart;y<yend;y++){

					origin_y = (int)(((y - ystart)*(origin_yend - origin_ystart+1))/(yend-ystart)+origin_ystart);
					imgOne.setRGB(x,y,orginPixels[origin_x][origin_y]);
					imgOne.setRGB(newWidth -x-1,y,orginPixels[width - origin_x-1][origin_y]);
				}
			}
		
	
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}	

	private void readImageRGBWithAnalysisB(File file, int width, int height, int frameIndex, double wScale, double hScale, int antiAliasing)
	{	

		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "r");

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			long pos = frameIndex*frameLength;
			raf.seek(pos);
			raf.read(bytes);

			int ind = 0;


			if(antiAliasing==1){
				CalculateAveragePixels(bytes);
			}
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					if(antiAliasing==1){
						imgOne.setRGB(x,y,averagePixels[x][y]);

					}else{
						imgOne.setRGB(x,y,pix);
					}
					ind++;
				}
			}
			
			long start, end;
	        if(wScale<1){
	        	start = System.currentTimeMillis();
		        for (int i = 0; i < (width-newWidth); i++){ 
					removeVerticalSeam(findVerticalSeam()); 
		        }
		        end = System.currentTimeMillis(); 
	        	System.out.println("Time: "+(end-start)+"ms"); 
	    	}
	    	if(hScale<1){
		        start = System.currentTimeMillis(); 
		        for (int i = 0; i < (height-newHeight); i++){ 
		            removeHorizontalSeam(findHorizontalSeam()); 
		        }
		        end = System.currentTimeMillis(); 
		        System.out.println("Time: "+(end-start)+"ms"); 
			}		

		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	private void CalculateAveragePixels(byte[] bytes){
		int ind = 0;
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				// averaging 3x3 filter process
				if(x>0 && y>0 && x<width-1 && y<height-1){

					int r = bytes[ind] & 0xff;
				    int g = bytes[ind+height*width] & 0xff;
				    int b = bytes[ind+height*width*2] & 0xff;

					r += bytes[ind-1] & 0xff;
					g += bytes[ind-1+height*width] & 0xff;
					b += bytes[ind-1+height*width*2] & 0xff;
					
					r += bytes[ind+1] & 0xff;
					g += bytes[ind+1+height*width] & 0xff;
					b += bytes[ind+1+height*width*2] & 0xff;
					
					r += bytes[ind-1-width] & 0xff;
					g += bytes[ind-1-width+height*width] & 0xff;
					b += bytes[ind-1-width+height*width*2] & 0xff;
					
					r += bytes[ind-width] & 0xff;
					g += bytes[ind-width+height*width] & 0xff;
					b += bytes[ind-width+height*width*2] & 0xff;
					
					r += bytes[ind+1-width] & 0xff;
					g += bytes[ind+1-width+height*width] & 0xff;
					b += bytes[ind+1-width+height*width*2] & 0xff;
					
					r += bytes[ind-1+width] & 0xff;
					g += bytes[ind-1+width+height*width] & 0xff;
					b += bytes[ind-1+width+height*width*2] & 0xff;
					
					r += bytes[ind+width] & 0xff;
					g += bytes[ind+width+height*width] & 0xff;
					b += bytes[ind+width+height*width*2] & 0xff;
					
					r += bytes[ind+1+width] & 0xff;
					g += bytes[ind+1+width+height*width] & 0xff;
					b += bytes[ind+1+width+height*width*2] & 0xff;

					r/=9;
					g/=9;
					b/=9;
					
					int pix = 0xff000000 | ((r) << 16) | ((g) << 8) | (b);
					averagePixels[x][y]=pix;

				}else{

					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					averagePixels[x][y]=pix;
					
				}
				 
				ind++;
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				
			}
		}

	}	

	
	private void readImageRGB(File file, int width, int height, int frameIndex, double wScale, double hScale, int antiAliasing)
	{
		int wfactor = (int)Math.floor(1.0/wScale);
		int hfactor = (int)Math.floor(1.0/hScale);		

		try
		{
			RandomAccessFile raf = new RandomAccessFile(file, "r");

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			long pos = frameIndex*frameLength;
			raf.seek(pos);
			raf.read(bytes);

			int ind = 0;
			for(int y = 0; y < newHeight; y++)
			{
				ind=y*hfactor*width;
				for(int x = 0; x < newWidth; x++)
				{
					// averaging 3x3 filter process
					if(antiAliasing == 1 && x>0 && y>0 && x<newWidth-1 && y<newHeight-1){

						int r = bytes[ind] & 0xff;
					    int g = bytes[ind+height*width] & 0xff;
					    int b = bytes[ind+height*width*2] & 0xff;

						r += bytes[ind-1] & 0xff;
						g += bytes[ind-1+height*width] & 0xff;
						b += bytes[ind-1+height*width*2] & 0xff;
						
						r += bytes[ind+1] & 0xff;
						g += bytes[ind+1+height*width] & 0xff;
						b += bytes[ind+1+height*width*2] & 0xff;
						
						r += bytes[ind-1-width] & 0xff;
						g += bytes[ind-1-width+height*width] & 0xff;
						b += bytes[ind-1-width+height*width*2] & 0xff;
						
						r += bytes[ind-width] & 0xff;
						g += bytes[ind-width+height*width] & 0xff;
						b += bytes[ind-width+height*width*2] & 0xff;
						
						r += bytes[ind+1-width] & 0xff;
						g += bytes[ind+1-width+height*width] & 0xff;
						b += bytes[ind+1-width+height*width*2] & 0xff;
						
						r += bytes[ind-1+width] & 0xff;
						g += bytes[ind-1+width+height*width] & 0xff;
						b += bytes[ind-1+width+height*width*2] & 0xff;
						
						r += bytes[ind+width] & 0xff;
						g += bytes[ind+width+height*width] & 0xff;
						b += bytes[ind+width+height*width*2] & 0xff;
						
						r += bytes[ind+1+width] & 0xff;
						g += bytes[ind+1+width+height*width] & 0xff;
						b += bytes[ind+1+width+height*width*2] & 0xff;

						r/=9;
						g/=9;
						b/=9;
						
						int pix = 0xff000000 | ((r) << 16) | ((g) << 8) | (b);
						imgOne.setRGB(x,y,pix);

					}else{

						byte a = 0;
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 

						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						imgOne.setRGB(x,y,pix);
						
					}
					 
					ind+=wfactor;
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){

		// Read parameters from command line
		String imgPath = args[0];
		double wScale = Double.parseDouble(args[1]);
		double hScale = Double.parseDouble(args[2]);
		double frameRate = Double.parseDouble(args[3]);
		int antiAliasing = Integer.parseInt(args[4]);
		int analysis = Integer.parseInt(args[5]);

		// Caculate total number of frames in the file
		File file = new File(imgPath);
		int numberOfFrames = numberOfFrames(file);
		
		System.out.println("The number of frames is: " + numberOfFrames );

		// Read all frames
		ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
		for(int i=0; i<numberOfFrames; i++){
			// Read in the specified image
		//int i =50;
	
			newWidth = (int) Math.floor(width*wScale);
			newHeight = (int) Math.floor(height*hScale);
			if(antiAliasing==1){
				averagePixels = new int[width][height];
			}
			
			//resize and anti-aliasing without analysis implementation
			if(analysis==0||wScale==hScale){
				imgOne = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
				readImageRGB(file, width, height, i,wScale,hScale,antiAliasing);
			}
			//Analysis B: seam craving
			else if(analysis==2){
				imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				readImageRGBWithAnalysisB(file, width, height, i,wScale,hScale,antiAliasing);
			}
			//Analysis A: non-linear mapping
			else if(analysis==1){
				imgOne = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
				readImageRGBWithAnalysisA(file, width, height, i,wScale,hScale,antiAliasing);
			}
			
			images.add(imgOne);
		}
		
		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(images.get(0)));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);

		//Display the frames with specific frame rate
		for (int i = 1; i < images.size(); i++) {
	    	lbIm1.setIcon(new ImageIcon(images.get(i)));
		    try {
		    	if (i==images.size()-1){
		        	i=0;
		        }
		        Thread.sleep((long)(1000.0/frameRate));        
		    }catch (InterruptedException e) {
		        Thread.currentThread().interrupt();
		    }
	    }

	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}