
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.Math;
import java.awt.Color;
import java.util.Arrays;

public class ImageDisplay {

	JFrame frame = new JFrame();
	JLabel lbIm1 = new JLabel();
	JLabel lbIm2 = new JLabel();
	JLabel lbIm3 = new JLabel();
	JLabel lbText1 = new JLabel();
	JLabel lbText2 = new JLabel();
	JLabel lbText3 = new JLabel();
	BufferedImage orginalImage;
	BufferedImage dctImage;
	BufferedImage dwtImage;
	int width = 512;
	int height = 512;
	int[][] pixels = new int[height][width];
	int blocks[][][] = new int[4096][8][8];
	double dct_cosine[][] = new double[8][8];

	double dwt_Rlines[][] = new double[height][width];
	double dwt_Glines[][] = new double[height][width];
	double dwt_Blines[][] = new double[height][width];

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath)
	{
		try
		{
			int frameLength = width*height*3;
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);
			
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
					pixels[y][x]=pix;
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					orginalImage.setRGB(x,y,pix);
					ind++;
				}
			}

			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int i = ((int)Math.floor(y/8))*64 + (int)Math.floor(x/8);
					blocks[i][y%8][x%8] = pixels[y][x];
					int rgb = pixels[y][x];
			    	dwt_Rlines[y][x] = Double.valueOf((rgb>>16)&0x0ff);
					dwt_Glines[y][x] = Double.valueOf((rgb>>8) &0x0ff);
					dwt_Blines[y][x] = Double.valueOf((rgb)    &0x0ff);
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
	private double C(int i){
		if(i==0)
			return 1.0/Math.sqrt(2);
		else
			return 1.0;
	}
	private double[][] zigzagZeroOut(double[][] f, int m){

		double[][] res = new double[f.length][f[0].length];
		for(int i=0; i<f.length; i++){
			for(int j=0; j<f[0].length; j++){
				res[i][j] = f[i][j];
			}
		}

		if(m==(f.length)*(f[0].length))
			return res;
		

		int m0 = (f.length)*(f[0].length)-m;
		int i=0,j=0;
		int d=2*f.length-1;
		int count=0;
		while(d>=0 && count<m0){
        
	        if(d<f.length){
	            int temp = d;
	            //go right up
	            if(d%2==1){
	                i = d;
	                j = 0;
	                while(temp>=0 && count<m0){
	                    count++;
	                    res[i][j] = 0;
	                    i--;j++;
	                    temp--;
	                }
	            }
	            // go left down
	            else{
	                i = 0;
	                j = d;
	                while(temp>=0&& count<m0){
	                    count++;
	                    res[i][j] = 0;
	                    i++;j--;
	                    temp--;
	                }
	            }
	        }else{
	            int temp = 2*f.length-1-d-1;
	            //go right up
	            if(d%2==1){
	                i = f.length-1;
	                j = d-(f.length-1);
	                while(temp>=0&& count<m0){
	                    count++;
	                    res[i][j] = 0;
	                    i--;j++;
	                    temp--;
	                }
	            }
	            // go left down
	            else{
	                i = d-(f.length-1);
	                j = f.length-1;
	                while(temp>=0&& count<m0){
	                    count++;
	                    res[i][j] = 0;
	                    i++;j--;
	                    temp--;
	                }
	            }
	        }
	        d--;
	    }
	    return res;
	}

	private int checkOverflow(int channel){
		if(channel <= 0)
			channel = 0;
		else if(channel >= 255)
			channel = 255;
		return channel;
	}

	private double FDCT_formula(int u,int v,int[][] sourceBlock){
		
		double res = 0;
		for (int x = 0; x<=7; x++) {
	        for (int y = 0; y<=7; y++) {
	            res+=sourceBlock[x][y]*dct_cosine[x][u]*dct_cosine[y][v];
	        }
	    }
	    res = res*0.25*C(u)*C(v);
		return res;
	}
	private int IDCT_formula(int x,int y,double[][] dct){
		
		double res = 0;
		for (int u = 0; u<=7; u++) {
	        for (int v = 0; v<=7; v++) {
	            res+=C(u)*C(v)*dct[u][v]*dct_cosine[x][u]*dct_cosine[y][v];
	        }
	    }
	    res *= 0.25;
		return (int) Math.round(res);
	}

	private void DCT_Encode(int[][] sourceBlock,double[][] dct_Rblock,double[][] dct_Gblock,double[][] dct_Bblock){

		int Rblock[][]=new int[8][8];
		int Gblock[][]=new int[8][8];
		int Bblock[][]=new int[8][8];

		//convert source pixels block to RGB blocks
		for (int x = 0; x<=7; x++) {
	        for (int y = 0; y<=7; y++) {
	        	int rgb = sourceBlock[x][y];
	        	Rblock[x][y] = (rgb>>16)&0x0ff;
				Gblock[x][y] = (rgb>>8) &0x0ff;
				Bblock[x][y] = (rgb)    &0x0ff;
	        }
	    }
		//Perform DCT encoding for the source block for each R G B channel
	    for (int u = 0; u<=7; u++) {
	        for (int v = 0; v<=7; v++) {
	        	dct_Rblock[u][v] = (int) Math.round(FDCT_formula(u, v, Rblock));
	        	dct_Gblock[u][v] = (int) Math.round(FDCT_formula(u, v, Gblock));
	        	dct_Bblock[u][v] = (int) Math.round(FDCT_formula(u, v, Bblock));
	
	        }
	    }
	    
	}

	private void DCT_Decode(int[][] restructBlock,double[][] dct_Rblock,double[][] dct_Gblock,double[][] dct_Bblock){
		
		int rec_Rblock[][]=new int[8][8];
		int rec_Gblock[][]=new int[8][8];
		int rec_Bblock[][]=new int[8][8];

	    for (int x = 0; x<=7; x++) {
	        for (int y = 0; y<=7; y++) {
	            rec_Rblock[x][y]=checkOverflow(IDCT_formula(x, y, dct_Rblock));
	            rec_Gblock[x][y]=checkOverflow(IDCT_formula(x, y, dct_Gblock));
	            rec_Bblock[x][y]=checkOverflow(IDCT_formula(x, y, dct_Bblock));
	        }
	    }
	    for (int x = 0; x<=7; x++) {
	        for (int y = 0; y<=7; y++) {
	        	int rgb = rec_Rblock[x][y];
				rgb = (rgb << 8) + rec_Gblock[x][y];
				rgb = (rgb << 8) + rec_Bblock[x][y];
	            restructBlock[x][y] = rgb;
	        }
	    }
	}


	private void DCT_process(int m){


		
		for(int ind = 0;ind<4096;ind++){
			int sourceBlock[][]=blocks[ind];

			double dct_Rblock[][]=new double[8][8];
			double dct_Gblock[][]=new double[8][8];
			double dct_Bblock[][]=new double[8][8];
			
			DCT_Encode(sourceBlock,dct_Rblock,dct_Gblock,dct_Bblock);

			dct_Rblock=zigzagZeroOut(dct_Rblock,m);
			dct_Gblock=zigzagZeroOut(dct_Gblock,m);
			dct_Bblock=zigzagZeroOut(dct_Bblock,m);
			
			int restructBlock[][] = new int[8][8];
			
			DCT_Decode(restructBlock,dct_Rblock,dct_Gblock,dct_Bblock);

			int x0 = (ind%64)*8;
			int y0 = (ind - (int)Math.floor(x0/8))/8;
			for(int y = 0; y < 8; y++)
			{
				for(int x = 0; x < 8; x++)
				{
					dctImage.setRGB(x0+x,y0+y,restructBlock[y][x]);
					//img.setRGB(x,y,pixels[y][x]);
				}
			}
		}
	}


	private double[] DWT_Decomposition_Step(double[] line, int h) {
		
		double[] res = Arrays.copyOf(line, line.length);
		for(int i=0; i < h/2; i++) {
			//average
			res[i] = (line[2*i] + line[2*i + 1]) / 2;
			//difference			
			res[h/2 + i] = (line[2*i] - line[2*i + 1]) / 2;	
		}
			
		return res; 
	}

	private double[] DWT_Composition_Step(double[] dwtLine, int h) {
		
		double[] res = Arrays.copyOf(dwtLine, dwtLine.length);
		for(int i=0; i < h/2; i++) {
			res[2*i] = dwtLine[i] + dwtLine[h/2 + i];
			res[2*i + 1] = dwtLine[i] - dwtLine[h/2 + i];
		}		
		return res;
	}
	/*
	private double[] DWT_Decomposition(double[] line) {
		int size = line.length;
		while (size > 0) {
			double[] temp = Arrays.copyOf(line, line.length);
			for(int i=0; i < size/2; i++) {
				//average
				temp[i] = (line[2*i] + line[2*i + 1]) / 2;
				//difference			
				temp[size/2 + i] = (line[2*i] - line[2*i + 1]) / 2;	
			}
			line = temp;
			size = size/2;
		}
		return line; 
	}


	private double[] DWT_Composition(double[] dwtLine) {

		int size = 1;
		while (size <= dwtLine.length) {
			double[] temp = Arrays.copyOf(dwtLine, dwtLine.length);
			for(int i=0; i < size/2; i++) {
				temp[2*i] = dwtLine[i] + dwtLine[size/2 + i];
				temp[2*i + 1] = dwtLine[i] - dwtLine[size/2 + i];
			}		
			dwtLine = temp;
			size = size*2;
		}
		return dwtLine;
	}*/


	private double[][] transpose(double[][] matrix) {
		double[][] res = new double[height][width];
		for(int i=0; i<height; i++) {
			for(int j=0; j< width; j++) {
				res[i][j] = matrix[j][i]; 
			}
		}
		return res;
	}

	private double[][] copy2DArray(double[][] matrix){
		double [][] res = new double[matrix.length][];
		for(int i = 0; i < matrix.length; i++)
		{
			double[] temp = matrix[i];
		    res[i] = new double[temp.length];
		    System.arraycopy(temp, 0, res[i], 0, temp.length);
		}
		return res;
	}

	private double[][] DWT_Encode(double[][] lines){
		//encode columns first
		int h = lines.length;
		while(h>1){
			for(int i = 0;i<h;i++){
				lines[i] = DWT_Decomposition_Step(lines[i],h);
			}
			lines=transpose(lines);
			for(int i = 0;i<h;i++){
				lines[i] = DWT_Decomposition_Step(lines[i],h);
			}
			lines=transpose(lines);
			h = h/2;
		}
		return lines;
	}

	private double[][] changePartialValuesInLarger(double[][] larger, double[][] smaller, int row, int col){
		for(int i = 0;i<smaller.length;i++){
			for(int j = 0;j<smaller[i].length;j++){
				larger[i+row][j+col]=smaller[i][j];
			}
		}
		return larger;
	}

	private double[][] assignPartialValuesToSmaller(double[][] larger, double[][] smaller, int row, int col){
		for(int i = 0;i<smaller.length;i++){
			for(int j = 0;j<smaller[i].length;j++){
				smaller[i][j]=larger[i+row][j+col];
			}
		}
		return smaller;
	}

	private double[][] DWT_Decode(double[][] dwt_lines, int m){
		
		int h=1;
		int s = 1;
		int row=0;
		int col=0;
		int mleft = m;

		//Stage zigzag
		
		double [][] zigzag_dwt_lines = new double [dwt_lines.length][dwt_lines.length];
		for(int i = 0;i<zigzag_dwt_lines.length;i++){
			for(int j=0;j<zigzag_dwt_lines[i].length;j++){
				zigzag_dwt_lines[i][j]=0;
			}
		}
		//LL
		double [][] LL = new double[s][s];
		LL=assignPartialValuesToSmaller(dwt_lines,LL,row,col);
		if(mleft>s*s){
			LL=zigzagZeroOut(LL,s*s);
			mleft-=s*s;
		}
		zigzag_dwt_lines = changePartialValuesInLarger(zigzag_dwt_lines,LL,row,col);

		while(s<dwt_lines.length && mleft>=0){
			//LH
			row=0;col=s;
			double [][] LH = new double[s][s];
			LH=assignPartialValuesToSmaller(dwt_lines,LH,row,col);
			if(mleft>s*s){
				LH=zigzagZeroOut(LH,s*s);
				mleft-=s*s;
			}else{
				LH=zigzagZeroOut(LH,mleft);
				mleft=0;
			}
			zigzag_dwt_lines = changePartialValuesInLarger(zigzag_dwt_lines,LH,row,col);
			if(mleft==0)
				break;
				
			//HL
			row+=s;col-=s;
			double [][] HL = new double[s][s];
			HL=assignPartialValuesToSmaller(dwt_lines,HL,row,col);
			if(mleft>s*s){
				HL=zigzagZeroOut(HL,s*s);
				mleft-=s*s;
			}else{
				HL=zigzagZeroOut(HL,mleft);
				mleft=0;
			}
			zigzag_dwt_lines = changePartialValuesInLarger(zigzag_dwt_lines,HL,row,col);
			if(mleft==0)
				break;
			
			//HH
			col+=s;
			double [][] HH = new double[s][s];
			HH=assignPartialValuesToSmaller(dwt_lines,HH,row,col);
			if(mleft>s*s){
				HH=zigzagZeroOut(HH,s*s);
				mleft-=s*s;
			}else{
				HH=zigzagZeroOut(HH,mleft);
				mleft=0;
			}
			zigzag_dwt_lines = changePartialValuesInLarger(zigzag_dwt_lines,HH,row,col);
			if(mleft==0)
				break;


			s*=2;
			//System.out.println(s+" "+ mleft);
		}

		dwt_lines=zigzag_dwt_lines;
		
		//DWT Decode
		while(h<=dwt_lines.length){
			//decode columns first
			dwt_lines=transpose(dwt_lines);
			for(int i = 0;i<h;i++){
				dwt_lines[i] = DWT_Composition_Step(dwt_lines[i],h);
			}
			//then decode rows 
			dwt_lines=transpose(dwt_lines);
			for(int i = 0;i<h;i++){
				dwt_lines[i] = DWT_Composition_Step(dwt_lines[i],h);
			}
			
			h=h*2;
		}

		return dwt_lines;
		
	}

	private void DWT_process(int m){

		double Rlines[][]= copy2DArray(dwt_Rlines);
		double Glines[][]= copy2DArray(dwt_Glines);
		double Blines[][]= copy2DArray(dwt_Blines);

		Rlines = DWT_Encode(Rlines);
		Glines = DWT_Encode(Glines);
		Blines = DWT_Encode(Blines);

		/*Rlines = zigzagZeroOut(Rlines,m);
		Glines = zigzagZeroOut(Glines,m);
		Blines = zigzagZeroOut(Blines,m);*/

		Rlines = DWT_Decode(Rlines,m);
		Glines = DWT_Decode(Glines,m);
		Blines = DWT_Decode(Blines,m);


		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				int rgb = checkOverflow((int) Math.round(Rlines[y][x]));
					rgb = (rgb << 8) + checkOverflow((int) Math.round(Glines[y][x]));
					rgb = (rgb << 8) + checkOverflow((int) Math.round(Blines[y][x]));
		            
				dwtImage.setRGB(x,y,rgb);
			}
		}

	}

	private void DCTandDWTprocess(int n){

		for(int i = 0;i<8;i++) {
			for(int j = 0;j<8;j++) {
				dct_cosine[i][j] = Math.cos((2*i+1)*j*Math.PI/16.00);
			}
		}

		if(n==-1){

			for(int iter = 1;iter<=64;iter++){
				DCT_process(iter);
				DWT_process(iter*4096);

				try { 
						Thread.sleep(500);
					} catch (InterruptedException e) {}

				if(iter==1)
					displayIterationImages(n,iter);
				else{
					lbText1.setText("DCT " +iter+ " iteration");
					lbText3.setText("DWT " +iter+ " iteration");
					lbIm1.setIcon(new ImageIcon(dctImage));
					lbIm3.setIcon(new ImageIcon(dwtImage));
				}
			}
		}else{
			int m = (int) Math.round((double) n/4096);
			System.out.println(Math.round((double) n/4096));
			DCT_process(m);
			DWT_process(n);

			try { 
				Thread.sleep(1000);
			} 
			catch (InterruptedException e) {}
			
			displayIterationImages(n,m);
		}
	}


	public void displayIterationImages(int n,int iteration){

		lbText2.setText("Original Image");
		
		if(n==-1){
			lbText1.setText("DCT " +iteration+ " iteration");
			
			lbText3.setText("DWT " +iteration+ " iteration");

		}else{
			lbText1.setText("DCT");
			lbText3.setText("DWT");
		}
		JLabel space = new JLabel("			");

		lbIm1.setIcon(new ImageIcon(dctImage));
		lbIm2.setIcon(new ImageIcon(orginalImage));
		lbIm3.setIcon(new ImageIcon(dwtImage));
		
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);
		// Use label to display the image
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		//c.weightx = 0.3;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		//c.weightx = 0.3;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(space, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		//c.weightx = 0.3;
		c.gridx = 2;
		c.gridy = 0;
		frame.getContentPane().add(lbText3, c);


		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(space, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 1;
		frame.getContentPane().add(lbIm3, c);
/*
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 3;
		c.gridy = 1;
		frame.getContentPane().add(space, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 4;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);*/

		frame.pack();
		frame.setVisible(true);

	}
	public void showIms(String[] args){

		// Read a parameter from command line
		String param1 = args[1];
		int n = Integer.parseInt(param1);
		int m = (int) Math.round(n/4096);

		// Read in the specified image
		orginalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0]);

		dctImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		dwtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		

		DCTandDWTprocess(n);
		
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}