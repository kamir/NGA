package wiki.charts.statistic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class LogBinTool {

	Hashtable<String,Vector<KVPair>> hash = new Hashtable<String,Vector<KVPair>>();
 
	int minx = 0;
	int maxx = 0;
	int miny = 0;
	int maxy = 0;
	
	public void add(int x, int y) {
		
		if ( x < minx ) minx = x;
		if ( x > maxx ) maxx = x;
		if ( y < miny ) miny = y;
		if ( y > maxy ) maxy = y;
		
		KVPair pv = new KVPair( x,y );
		v.add(pv);
		
	}

	Vector<KVPair> v = new Vector<KVPair>();
 
	public void nextRecord() { 
		v = new Vector<KVPair>();
	} 
	
	public void initBinning( String lang, String ext ) {
		
		file = new File( "./data.out/networks/" + lang + "/" + ext + "_log_bin_data.csv" );
			
		System.out.println( "minx=" + minx );
		System.out.println( "maxx=" + maxx );
		System.out.println( "miny=" + miny );
		System.out.println( "maxy=" + maxy );
		
		
	}

	public void flush(String currenrtTime) {
		hash.put(currenrtTime, v);		
		System.out.println( "s=" + hash.size() + " ct=" + currenrtTime );
	}

	public void createTable() throws IOException {
		initBins( maxx, maxy, 1.5, LogBinTool.normalize );
	}
	
	public void createTableDISTR() throws IOException {
		initBins( maxx, maxy, 1.5, true );
	}
	
	
	
	
	double[][] dataF = null;
    double[][] dataB = null;
    
    static double[] widthOfBin = null;
    static double[] widthOfBinCounted = null;
    static double[] upperBorder = null;
    static double[] midOfBin = null;
    
    static double[] conuterBin = null;
    static double[] conuterBin2 = null;
    
    static int logSkipped = 0;
    static int logUsed = 0;
	
    private void initBins(double maxX, double maxY, double f, boolean normalize) throws IOException {

    	System.out.println("NORM : " + normalize );
    	double totalNr = 1.0;
    	
    	double max = 10;
    	if ( maxX > maxY ) max = maxX;
    	else max = maxY;
    	
        //
        // Anzahl der BINS
        //
        double z = 1.0;
        int c = 0;
        while (z < max) {
            z = z * f;
            // System.err.println(c + "\t" + z);
            c++;
        }
        
        widthOfBin = new double[c];
        upperBorder = new double[c];
        midOfBin = new double[c];
        
        conuterBin = new double[c];
        conuterBin2 = new double[c];
        widthOfBinCounted = new double[c];

        // fŸr alle Reihen nun das log binning rechnen 
        Set<String> keys = hash.keySet();
        int zz = keys.size();
        
        double b = 1.0;
        double mid = 0.5;
        double lastBorder = 0;
        
        dataF = new double[c][zz];
        dataB = new double[c][zz];
        
        double[] tN = new double[zz];
        
        for (int i = 0; i < c; i++) {
        	
        	widthOfBin[i] = b;
        	midOfBin[i] = mid;
        	upperBorder[i] = lastBorder + b;
        	
        	b = b * f;
        	mid = lastBorder + 0.5 * b;
            lastBorder = b;
        	
            conuterBin[i] = 0.0;
            conuterBin2[i] = 0.0;
            widthOfBinCounted[i] = 0.0;
            
            for (int j = 0; j < zz; j++) {
                dataF[i][j] = 0.0;
                dataB[i][j] = 0.0;
            }
        }

        for (int i = 0; i < c; i++) {
 			System.out.println("\n(" + i + ") " + widthOfBin[i] + " " + upperBorder[i] + " " + midOfBin[i]);
		}
        
        ArrayList<String> l = new ArrayList<String>();
        for( String s : keys )  { 
        	l.add(s);        	
        }
     
        DecimalFormat df = new DecimalFormat("0.000");
        
        int line = 0;
        Collections.sort( l );
        Iterator<String> it = l.iterator();
        String head = "#bin_mitte";
        
        while( it.hasNext() ) {
        	
        	double tn = 0;

        	String key = it.next();
        	
        	String lineA =  "\n (" + line + ") >>> " + key;
        	String lineB =  "B:: ";
        	String lineC =  "C:: ";
                	
        	head = head + "\t" + key;
        	
        	Vector<KVPair> pairs = hash.get(key);
        	for( KVPair kvp : pairs ) {
            	int i = getBin( kvp.x );
            	dataB[i][line] = dataB[i][line] + kvp.y; 
        	}
        	
        	for (int i = 0; i < c; i++) {
        		dataF[i][line] = dataB[i][line] / widthOfBin[i]; 
        		tn = tn + dataB[i][line];
        		lineB = lineB + " " + df.format( dataF[i][line] );
        		lineC = lineC + " " + df.format( dataF[i][line] / tn  );
        	}

        	tN[line] = tn;
        	
        	System.out.println( lineA + "  TN=" + tn );
    		System.out.println( lineB );
    		System.out.println( lineC );

        	line++;
        }
                       
                
        BufferedWriter bw = new BufferedWriter( new FileWriter( file ) );
        bw.write( "#normalized=" + normalize + "\n" );
        bw.write( "#log_axis=" + logAxis + "\n" );
        
        bw.write( "#tN\t" + tN[0] + "\t" );
    	for( int j = 1; j < hash.size() ; j++ ) 
            bw.write( tN[j] + "\t" );
        bw.write( "\n" + head + "\n" );
        
        for( int i = 0; i < c ; i++ ) {
    		bw.write( df.format( midOfBin[i]) + "\t" );
        	for( int j = 0; j < hash.size() ; j++ ) {
        		double v = dataF[i][j];
        		
        		if ( normalize ) v = v / tN[j];
        		if ( logAxis && v!= 0.0 ) v = Math.log( v );
        		
        		bw.write(  df.format( v ) + "\t"  );
        	}
    		bw.write( "\n"  );
        }	
        bw.flush();
        bw.close();
        
    }
    
    public static boolean logAxis = false;
    public static boolean normalize = true;
    
    public File file = new File( "./data.out/out.csv" );
    
    private static int getBin(double d) {
        //System.out.print( "--- d=" + d );
        
        boolean notCorrectBin = true;
        
        int z = 1;
        int b = 0;
        
        while (z < conuterBin.length) {

            if (d > upperBorder[z-1] ) {
               b = z-1;
            }    
            z++;      
        }
        //System.out.print( "bin=>" + b );
        
        return b;
    }
}



class KVPair {
	
	public KVPair(int x2, int y2) {
		x = x2;
		y = y2;
	}
	
	int x = 0;
	int y = 0;
	
}