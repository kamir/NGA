package wiki;


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

public class EditActivityReader {
	
	/**
	 * Runtime parameter ...
	 */
	static boolean useStructure = GrowRateReader.useStructure;   // create a graph and calc structural properties
	static String delim = GrowRateReader.delim;   				 // migth be \t in BROWSER Download Mode, but "(char)1" in my cluster setup as default 
	static String lang = GrowRateReader.lang;			  		 // langcode
	static String type = GrowRateReader.type;		             // input file type
	static int max = GrowRateReader.max;			             // use a limit (max < 0 => no LIMIT)
	
	
	
	/*
	 * Internal variables
	 */
	static File fIN = null;
	static BufferedReader brIN = null;
	
	static File fOUT1 = null;
	static File fOUT3 = null;
			
	static BufferedWriter bw1 = null;
	static BufferedWriter bw3 = null;
	
	static long t0 = 0;
	
	static File netsFolder= null;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws IOException, ParseException {
		
		lang = GrowRateReader.lang;
		pages = new Hashtable<String,Long>(); 
		
	 	eN = 0;
		EN = 0;
		tV = 0;
		TV = 0;
		
		current_year2 = 0;
		current_month2 = 0;
		current_day2 = 0;
		
		current_year = 0;
		current_month = 0;
		current_day = 0;
		current_hour = 0;
		current_minute = 0;
		
		t0 = System.currentTimeMillis();
		t = t0;
		
// CHECK this values		
		String[] header = { "pageid", "langid", "nsid", "pagename", 
				            "c1", "c2", "source", "dest", "revstart", "revend", 
				            "sourcename", "destname", "time", 
				            "lyear", "lmonth", "minute", "hour", "day", "month", "year" };
		
		netsFolder = new File( "data.out/networks/"+ lang + "/" );
		
		if ( !netsFolder.exists() ) netsFolder.mkdirs();
		
		fIN = new File( "data.in/" + lang + "_revisions_" + type + ".csv" );
		brIN = new BufferedReader( new FileReader( fIN ) );
		
		fOUT1 = new File("data.out/" + lang + "-" + type + "_revisions.process.dat" );
		fOUT3 = new File("data.out/" + lang + "_" + type + "_revisions.rtlog.dat" );
				
		bw1 = new BufferedWriter( new FileWriter( fOUT1 ) );
		bw3 = new BufferedWriter( new FileWriter( fOUT3 ) );
		
	    String outline = "date\tEN\teN\tTV\ttV"; 
		bw1.write( outline + "\n" );  // jeder Schritt


		outline = "yyyy-MM\tst\tdelt_st"; 
		bw3.write( outline + "\n" );  // jeder Monat

		/**
		 * 
		 */
		
		int lc = 0;

		System.out.println( "EDIT ACTIVITY READER > Process : " + lang + "        (max=" + max+")" );
		
		String line = null;
		line = brIN.readLine();
		
		

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
		}
		
		/**
		 * ENDE
		 */
		brIN.close();
		bw1.close();
		bw3.close();
		
		System.out.println( "\n#revisions   :  " + lc );
		System.out.println( "#done in :-) " + getRuntime() );
		
		System.out.println( "#done in :-) " + getRuntime() );
		
		
				
		// System.exit(0);
		pages = null;
		bw1 = null;
		bw3 = null;
		
	}
	
	private static String getRuntime() {
		long tn = System.currentTimeMillis();
		String rt = (tn-t0)/1000 +" s";
		return rt;
	}

 	static long eN = 0;
	static long EN = 0;
	static long tV = 0;
	static long TV = 0;
	
	static int current_year2 = 0;
	static int current_month2 = 0;
	static int current_day2 = 0;
	
	static int current_year = 0;
	static int current_month = 0;
	static int current_day = 0;
	static int current_hour = 0;
	static int current_minute = 0;
	
//	static HashSet nodes = null;
//	
//	static Graph<String,String> ig = null;	
	static int nV = 0; // global counter for vertex
	
	static GregorianCalendar cal = new GregorianCalendar();
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static Date dateD = null;
    
	private static void processLine(String line) throws ParseException, IOException {
		
		line = line.replaceAll("\",\""  , "\t" );
		
//		String delim = "\t";
		String delim = "" + (char)1;
		
 		StringTokenizer st = new StringTokenizer( line, delim );
		
		// System.out.println( st.countTokens() + " : " + line );
		
		String[] toks = new String[13]; 
		for( int i = 0; i < 13; i++ ) {
			toks[i] = st.nextToken();
			// System.out.print( toks[i] + "   " );
			
		}
		
		// System.out.println( toks[12] );
		Date d = null;
		try {
			dateD = sdf.parse( toks[8] ); 
			d = dateD;
		}
		
		catch( Exception ex) {
			if ( d == null ) d = dateD;
		}

		cal.setTime( d );
		dateD = d;

	    long deltaText = calcDeltaText( toks[3], toks[5] );
	    
	    eN = eN + 1;
	    tV = tV + deltaText;
	    
		if ( isNewHour( cal ) ) {
			EN = EN + eN;
			TV = TV + tV;
					
		    String outline = cal.getTimeInMillis() + "\t" + cal.getTime().toString() + "\t" + EN + "\t" + eN + "\t" + TV + "\t" + tV; 
					
			bw1.write( outline + "\n" );
			
			
			tV = 0;
			eN = 0;
								
		}
		
		if ( isNewMonth( cal ) ) {
			
			double ec = 0; 
 			double lc = 0; 
			 			
		    String outline = cal.getTime().toString() + "\t" + EN + "\t" + TV + "\t"; 
						
			String state = getCurrentState();
			System.out.println( state );
			bw3.write( state + "\n" );
			
			bw1.flush();

			
		}
		
	}

	
	static Hashtable<String,Long> pages = new Hashtable<String,Long>();
	
	
	private static long calcDeltaText(String key, String s) {
		Long newSize = Long.parseLong(s);
		Long oldSize = pages.get(key);
		long delta = 0;
		if ( oldSize == null ) {
			delta = newSize;
		}
		pages.put(key, newSize);
		
		return delta;
	}

	static long t = 0;
	private static String getCurrenrtTime() {
		long now = System.currentTimeMillis();
		
		int cmm = current_month2 + 1;
		String cm = "??";
		if ( cmm < 10 ) cm = "0" + cmm;
		else cm = "" + cmm;
		
		String date = current_year2 + "-" + cm;
		t = now;
		return date;
	}

	private static String getCurrentState() {
		long now = System.currentTimeMillis();
		
		int cmm = current_month2 + 1;
		String cm = "??";
		if ( cmm < 10 ) cm = "0" + cmm;
		else cm = "" + cmm;
		
		String date = current_year2 + "-" + cm  + "\t" + now + "\t" + ( now-t );
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
