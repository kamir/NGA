package wiki;

import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.algorithms.shortestpath.UnweightedShortestPath;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.shortestpath.DistanceStatistics;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.ObservableGraph;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;

import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.math3.stat.Frequency;

import wiki.charts.statistic.LogBinTool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRootPane;

import edu.uci.ics.jung.graph.util.Graphs;
import edu.uci.ics.jung.io.GraphMLWriter;
import edu.uci.ics.jung.io.PajekNetWriter;

public class GrowRateReader {
	
	/**
	 * Runtime parameter ...
	 */
	public static boolean useStructure = true;   // create a graph and calc structural properties
	
	public static boolean meassureDiameter = false;   // calc the diameter of the graph
	public static boolean storeNetworks = false;    
	
	public static final int TYPE_GRAPHML = 0;    
	public static final int TYPE_PAJEK = 1;    
	public static int net_strore_type = TYPE_PAJEK;    
	

	public static String delim = "" + (char)1;   // migth be \t in BROWSER Download Mode, but "(char)1" in my cluster setup as default 
	public static String lang = "68";			  // language code for wikipedia pages
	public static String type = "full";          // input file type
	public static int max = -100000;            // use a limit (max < 0 => no LIMIT)
	
	public static int YEAR_TS_FROM = 2000;	        // max year for structure analysis
	public static int YEAR_TS_TO = 2011;	        // max year for structure analysis
	
	/*
	 * Internal variables
	 */
	static File fIN = null;
	static BufferedReader brIN = null;
	
	static File fOUT1 = null;
	static File fOUT2 = null;
	static File fOUT3 = null;
			
	static BufferedWriter bw1 = null;
	static BufferedWriter bw2 = null;
	static BufferedWriter bw3 = null;
	
	static long t0 = 0;
	
	static File netsFolder= null;
	static File netsFolderNET= null;
	static File netsFolderDEGR= null;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {

		useStructure = false; 
				meassureDiameter = false;   // calc the diameter of the graph
				storeNetworks = false;    
				net_strore_type = TYPE_PAJEK;  
				LogBinTool.logAxis = true;
		
		long tT0 = System.currentTimeMillis();
		
	    run( 52 , args );   // de
//		run( 60 , args );   // en
//	    run( 68 , args );   // fi      
//		run( 88 , args );   // he      
//		run( 109 , args );  // ja
//	  	run( 122 , args );  // ko  
//	  	run( 166 , args );  // nl  
//		run( 222 , args );  // sv
//							
		String rt = (System.currentTimeMillis() - tT0)/1000 +" s";
		
		System.out.println();
		System.out.println( "# EVERYTHING done in :-) " + rt );
		
		System.exit(0);
	}
		
	public static void run( int langCode, String[] args ) throws IOException, ParseException { 	
		
		lang = langCode + "";
		
		nodes = new HashSet();
		
	 	nN = 0;
		lN = 0;
		NN = 0;
		LN = 0;
		
		current_year2 = 0;
		current_month2 = 0;
		current_day2 = 0;
		
		current_year = 0;
		current_month = 0;
		current_day = 0;
		current_hour = 0;
		current_minute = 0;
		
		// if ( !useStructure ) EditActivityReader.main( args );
		
		String dout = "data.out"; 
		
		int from = YEAR_TS_FROM;
		int to = YEAR_TS_TO;
		
		// lbtHIST = new LogBinTool();
		// lbtHIST.file = new File( dout+"/networks/"+ lang + "/" + lang + "_degree_hist_over_time_" + from + "_" + to + ".csv");
		
		lbtDISTR = new LogBinTool();
		lbtDISTR.file = new File( dout+"/networks/"+ lang + "/" + lang + "_degree_distr_over_time_" + from + "_" + to + ".csv");
		
		t0 = System.currentTimeMillis();
		t = t0;
		
		String[] header = { "pageid", "langid", "nsid", "pagename", 
				            "c1", "c2", "source", "dest", "revstart", "revend", 
				            "sourcename", "destname", "time", 
				            "lyear", "lmonth", "minute", "hour", "day", "month", "year" };
		
		netsFolder = new File( dout+"/networks/"+ lang + "/" );
		netsFolderNET = new File( dout+"/networks/"+ lang + "/NET" );
		netsFolderDEGR = new File( dout+"/networks/"+ lang + "/DEGR" );
		
		if ( !netsFolder.exists() ) netsFolder.mkdirs();
		if ( !netsFolderNET.exists() ) netsFolderNET.mkdirs();
		if ( !netsFolderDEGR.exists() ) netsFolderDEGR.mkdirs();
		
		
		
		
		fIN = new File( "data.in/" + lang + "_wiki_" + type + ".csv" );
		brIN = new BufferedReader( new FileReader( fIN ) );
		
		fOUT1 = new File(dout+"/" + lang + "-" + type + "_wiki.process.dat" );
		fOUT2 = new File(dout+"/" + lang + "_" + type + "_wiki.structure.dat" );
		fOUT3 = new File(dout+"/" + lang + "_" + type + "_wiki.rtlog.dat" );
				
		bw1 = new BufferedWriter( new FileWriter( fOUT1 ) );
		bw2 = new BufferedWriter( new FileWriter( fOUT2 ) );
		bw3 = new BufferedWriter( new FileWriter( fOUT3 ) );
		
	    String outline = "date\tNN\tnN\tLN\tlN"; 
		bw1.write( outline + "\n" );  // jeder Schritt

		outline = "date\tNN\tLN\tdiameter"; 
		bw2.write( outline + "\n" );  // jeder Monat

		outline = "yyyy-MM\tst\tdelt_st"; 
		bw3.write( outline + "\n" );  // jeder Monat

		
		int lc = 0;

		System.out.println( "GROW RATE READER > Process : " + lang + "        (max=" + max+")" );
		
		String line = null;
		line = brIN.readLine();
		
		if ( useStructure ) ig = Graphs.<String,String>synchronizedDirectedGraph(new DirectedSparseMultigraph<String,String>());

		boolean goOn = true;
		while( brIN.ready() && goOn ) {
			
			line = brIN.readLine();
			lc++;
			
			if ( max > 0 ) {
				if( lc < max ) processLine( line ); 
				else goOn = false;
			}	
			else processLine( line ); 
					
			bw1.flush();
			bw2.flush();
		}
		
		if ( useStructure ) lbtDISTR.initBinning( lang, lang + "_DISTR" );
		if ( useStructure ) lbtDISTR.createTable();

		// lbtHIST.initBinning(lang + "_HIST");
		// lbtHIST.createTableDISTR();

		
		/**
		 * ENDE
		 */
		brIN.close();
		bw1.close();
		bw2.close();
		bw3.close();
		
		System.out.println( "\n#links   :  " + lc );
		System.out.println( "#done in :-) " + getRuntime() );
				
		
		
	}
	
	private static String getRuntime() {
		long tn = System.currentTimeMillis();
		String rt = (tn-t0)/1000 +" s";
		return rt;
	}

 	static int nN = 0;
	static int lN = 0;
	static int NN = 0;
	static int LN = 0;
	
	static int current_year2 = 0;
	static int current_month2 = 0;
	static int current_day2 = 0;
	
	static int current_year = 0;
	static int current_month = 0;
	static int current_day = 0;
	static int current_hour = 0;
	static int current_minute = 0;
	
	static HashSet nodes = null;
	
	static Graph<String,String> ig = null;	
	static int nV = 0; // global counter for vertex
	
	static GregorianCalendar cal = new GregorianCalendar();
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	static String[] toks = new String[11]; 
	
	static boolean sf = true;
	private static void processLine(String line) throws ParseException, IOException {
		
		line = line.replaceAll("\",\""  , "\t" );
		
//		String delim = "\t";		
		
 		StringTokenizer st = new StringTokenizer( line, delim );
		
		//System.out.println( st.countTokens() + " : " + line );
		
		
		for( int i = 0; i < 11; i++ ) {
			toks[i] = st.nextToken();
		}
		
		// System.out.println( toks[12] );
			
	    cal.setTime(sdf.parse( toks[10] ) );
	    
		/**
		 * Add the new NODES ...
		 */
		// do we have new nodes?
		// System.out.println( toks[6] + " " + toks[7] );
	    if ( !nodes.contains( toks[6] ) ) {
			nodes.add( toks[6] );
			nN++;
			//nV++;
			if ( useStructure ) ig.addVertex( toks[6] );
        }
	
		if ( !nodes.contains( toks[7] ) ) {
			nodes.add( toks[7] );
			nN++;
			//nV++;
			if( useStructure ) ig.addVertex( toks[7] );
		}
		
		lN++;
		
		/**
		 * Add the new LINKS
		 */
		
		if ( useStructure ) ig.addEdge(""+ig.getEdgeCount(), toks[6], toks[7] );
	    
		
		if ( isNewHour( cal ) ) {
			NN = NN + nN;
			LN = LN + lN;
					
		    String outline = cal.getTimeInMillis() + "\t" + cal.getTime().toString() + "\t" + NN + "\t" + nN + "\t" + LN + "\t" + lN; 
					
			bw1.write( outline + "\n" );	 
			
			lN = 0;
			nN = 0;
								
		}
		
		if ( isNewMonth( cal ) ) {
			
			double ec = 0; 
 			double lc = 0; 
			
 			String structDAT = "";
 			
 			// calc DistanceStatistics
			if ( useStructure ) {
				ec = ig.getEdgeCount(); 
				lc = ig.getVertexCount(); 
				
				if ( current_year2 >= YEAR_TS_FROM && current_year2 <= YEAR_TS_TO ) {
	 				
					storeDegreeDistribution( ig, cal ); 	
	 				// storeDegreeHistogram( ig, cal );
	 				
	 				if ( storeNetworks ) storeNetwork( ig, cal );
	 				if ( meassureDiameter) structDAT = getStructureData( ig ); 
	 				
	 			}	
			}
			

			 			 			
		    String outline = cal.getTime().toString() + "\t" + NN + "\t" + LN + "\t" + ec + "\t" +  lc + "\t" + structDAT;
			bw2.write( outline + "\n" );
			bw2.flush();
									
			String state = getCurrentState() + " NN=" + NN;

			bw3.write( state + "\n" );
			bw3.flush();
			
			System.out.println( state );

		}
		
	}

	
	private static String getStructureData(Graph<String, String> ig2) {
		String line = "";
		
		UnweightedShortestPath<String, String> dist = new UnweightedShortestPath<String, String>( ig2 );
		
	    double d = DistanceStatistics.diameter( ig2, dist, true);
	    line = line + " " + d;
	    
		return line;
	}

	private static void storeNetwork(Graph<String, String> ig2, GregorianCalendar cal) throws IOException {
		
		File netFile = null;

		
		
		
		switch ( net_strore_type ) {
		
			case TYPE_PAJEK : {	
				netFile = new File( netsFolderNET.getAbsolutePath() + "/" + lang + "_" + getCurrenrtTime() + ".net" );
				
				PrintWriter out = new PrintWriter(
	                     new BufferedWriter(
	                         new FileWriter( netFile.getAbsolutePath() )));
				
				PajekNetWriter<String,String> graphWriter2 = new PajekNetWriter<String, String> ();
				
				graphWriter2.save(ig2, out);
				
				out.flush();
				out.close();
				break;
			}
			
			case TYPE_GRAPHML : {
				netFile = new File( netsFolderNET.getAbsolutePath() + "/" + lang + "_" + getCurrenrtTime() + ".graphml" );

				GraphMLWriter<String, String> graphWriter1 = new GraphMLWriter<String, String> ();
				
				PrintWriter out = new PrintWriter(
	                     new BufferedWriter(
	                         new FileWriter( netFile.getAbsolutePath() )));
				
				graphWriter1.save(ig2, out);
				
				out.flush();
				out.close();
		     	break;
			}

		}
					
		
		
	}

	// static LogBinTool lbtHIST = new LogBinTool();
	static LogBinTool lbtDISTR = new LogBinTool();

//	/**
//	 * Just the DegreeHistogramm, no division by total number ...
//	 * 
//	 * @param ig2
//	 * @param cal
//	 * @throws IOException
//	 */
//	private static void storeDegreeHistogram(Graph<String, String> ig2, GregorianCalendar cal) throws IOException {
//		
//		File degrFile = new File( netsFolderDEGR.getAbsolutePath() + "/" + lang + "_" + getCurrenrtTime() + "_degree_distribution.dat" );
//
//		PrintWriter out = new PrintWriter(
//          new BufferedWriter(
//            new FileWriter( 
//              degrFile.getAbsolutePath() 
//                          ) 
//                            ) 
//                  
//                                         );
//		
//		int i = 0;
//		
//		Hashtable<Integer,Integer> degrees = new Hashtable<Integer,Integer>();
//				
//		int n = ig2.getVertexCount();
//		Collection cV =  ig2.getVertices();
//		Iterator<String> it = cV.iterator();
//		while ( it.hasNext() ) {
//			String v = it.next();
//			int degree = ig2.degree( v );
//			
//			addDegree( degrees, degree ); 
//		}
//		
//		lbtHIST.nextRecord(  );
//		
//		Iterator iter = degrees.keySet().iterator();
//		while( iter.hasNext() ) {
//
//			if ( i != 0 ) out.write( ", " );
//						
//			int x = (Integer) iter.next();
//			int y = degrees.get( x );
//			
//			lbtHIST.add( x, y );
//			
//			
//			String point = Math.log( x ) + ", " + Math.log( y );
//		    out.write( "[" + point + "]" ) ;
//
//		    i++;
//		}
//		
//		lbtHIST.flush(getCurrenrtTime());
//	    
//		
//		out.flush();
//		out.close();
//		
//	}
	
	private static void storeDegreeDistribution(Graph<String, String> ig2, GregorianCalendar cal) throws IOException {
		
		File degrFile = new File( netsFolderDEGR.getAbsolutePath() + "/" + lang + "_" + getCurrenrtTime() + "_degree_histogram.dat" );

		PrintWriter out = new PrintWriter(
          new BufferedWriter(
            new FileWriter( 
              degrFile.getAbsolutePath() 
                          ) 
                            ) 
                  
                                         );
		
		int i = 0;
		
		Hashtable<Integer,Integer> degrees = new Hashtable<Integer,Integer>();
				
		int n = ig2.getVertexCount();
		Collection cV =  ig2.getVertices();
		Iterator<String> it = cV.iterator();
		while ( it.hasNext() ) {
			String v = it.next();
			int degree = ig2.degree( v );
			
			addDegree( degrees, degree ); 
		}
		
		lbtDISTR.nextRecord(  );
		
		Iterator iter = degrees.keySet().iterator();
		while( iter.hasNext() ) {

			if ( i != 0 ) out.write( ", " );
						
			int x = (Integer) iter.next();
			int y = degrees.get( x );
			
			lbtDISTR.add( x, y );
			
			
			String point = Math.log( x ) + ", " + Math.log( y );
		    out.write( "[" + point + "]" ) ;

		    i++;
		}
		
		lbtDISTR.flush(getCurrenrtTime());
	    
		
		out.flush();
		out.close();
		
	}

	private static void addDegree(Hashtable<Integer,Integer> degrees, int degree) {
		Integer integer = degrees.get( degree );
		if ( integer == null ) { 
			degrees.put( degree, 1 );
		}		
		else {
			degrees.put( degree, integer + 1 );
		}
	}

	static long t = 0;
	static long t1 = 0;
	
	private static String getCurrenrtTime() {
		
		int cmm = current_month2 + 1;
		String cm = "??";
		if ( cmm < 10 ) cm = "0" + cmm;
		else cm = "" + cmm;
		
		String date = current_year2 + "-" + cm;
		
		return date;
	}

	private static String getCurrentState() {
		
		long now = System.currentTimeMillis();
		
		int cmm = current_month2 + 1;
		String cm = "??";
		if ( cmm < 10 ) cm = "0" + cmm;
		else cm = "" + cmm;
		
		String date = current_year2 + "-" + cm  + "\t" + now + "\t" + ( (now-t) + " ms" );

		t = now;
		
		return date;
	}

	private static boolean isNewMonth(GregorianCalendar cal) {
		int month = cal.get( Calendar.MONTH);
		int year = cal.get( Calendar.YEAR);
		
		if ( month == current_month2 && year == current_year2 ) return false;
 
		current_month2 = month;
		current_year2 = year;
					
		return true;
	}

	private static boolean isNewHour(GregorianCalendar cal) {
		
		int hour = cal.get( Calendar.HOUR);
		int day = cal.get( Calendar.DAY_OF_MONTH);
		int month = cal.get( Calendar.MONTH);
		int year = cal.get( Calendar.YEAR);
		
		if ( year == current_year &&
			 month == current_month &&
			 day == current_day &&
			 hour == current_hour ) return false;
		
		current_day = day;
		current_month = month;
		current_year = year;
		current_hour = hour;
				
		return true;
	}



}
