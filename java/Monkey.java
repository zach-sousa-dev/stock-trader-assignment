import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLConnection;
import java.net.URL;
import java.net.NoRouteToHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.HttpURLConnection;



/**
 * Monkey Stock Picker Program (MySQL DB version)
 *[
 * This program simulates a “Monkey” trader that runs through a historical date range,
 * using:
 * - Mr_Green for long buys,
 * - Miss_Scarlet for long sells,
 * - Mrs_Peacock for short sells,
 * - Col_Mustard for short covers.
 * 
 * It reads stock quotes day by day from the MMEngine.php, then applies strategies, and logs activity,
 * and reports total profit.
 * 
 * Features:
 *   Configurable strategy enables via config file.
 *   Retry logic when fetching quotes.
 *   Logging of errors and outcomes.
 * 
 * @author Dave Slemon
 * @version 215
 */
public class Monkey {
    private static final String version = "v215";
    public  static int verbose = 1;   //3-max verbosity  1-min verbosity  0-no verbosity
    //private static final int MAX_RETRIES = 5;
    

    // Enable/disable strategies via config file
    private static final boolean enable_Green    = Tools.getConfig("enable_Green").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Scarlet  = Tools.getConfig("enable_Scarlet").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Peacock  = Tools.getConfig("enable_Peacock").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Mustard  = Tools.getConfig("enable_Mustard").trim().equalsIgnoreCase("YES");
    private static final boolean enable_White    = Tools.getConfig("enable_White").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Plum     = Tools.getConfig("enable_Plum").trim().equalsIgnoreCase("YES");

    public static void main(String[] args) throws InterruptedException {
        int numShares = 1000;
        String symbol = "PDI";        
        boolean alreadyDone = false;
        boolean is_simulator = true;    //properly set below
        long l_theTime;
        
        
        //for real mode, make sure the two dates below are the same day, also make sure
        //this date is in the data/marketdates.txt file.
        String startingDate = "2022-06-28";
        String endingDate   = "2023-09-14";

/*       
       Full Range =        2022-06-28 to   2023-09-14
   
        //Month Ex-Date    startingDate  endingDate  
        1    2022-07-08    2022-06-28    2022-07-14
        2    2022-08-10    2022-07-08    2022-08-15
        3    2022-09-09    2022-08-10    2022-09-15
        4    2022-10-12    2022-09-09    2022-10-18
        5    2022-11-10    2022-10-12    2022-11-16
        6    2022-12-09    2022-11-10    2022-12-20
        7    2023-01-12    2022-12-09    2023-01-19
        8    2023-02-10    2023-01-12    2023-02-16
        9    2023-03-10    2023-02-10    2023-03-16
        10    2023-04-12    2023-03-10    2023-04-18
        11    2023-05-10    2023-04-12    2023-05-16
        12    2023-06-09    2023-05-10    2023-06-15
        13    2023-07-12    2023-06-09    2023-07-18
        14    2023-08-10    2023-07-12    2023-08-16
        15    2023-09-08    2023-08-10    2023-09-14

*/      
        
        
        String theDT;
        String dataDirectory = "../data/";  // where config.txt and marketDates.txt are located
        String logDirectory = "../logs/";  // where config.txt and marketDates.txt are located
        String transcriptDirectory = "../logs/transcripts/";  // where config.txt and marketDates.txt are located
        String transcriptFileName;
        String unixTimeStamp = "";
        String prevUnixTimeStamp = "1";
        String msg = "";
        String transcriptFile;
        String prevTime = "";
        Path filePath;
        
        
        
        //Open the transcript log file...and state which mode the monkey is in....
        LocalDate today = LocalDate.now();
        LocalDate sDate = LocalDate.parse(startingDate);
        DateTimeFormatter s_today_dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String str = now.format(s_today_dt);
        
        
        
        //decide on which mode the monkey is in, either  simulator or realtime mode (realmode test is:  startDate and endingDate must be on today's date, and startDate must be in marketDates.txt
        if (sDate.equals(today)) {
            is_simulator = false;
            msg = "Monkey\tmain()\t\t\t\t" + "Monkey "+ "SYSTEM" +  version + " starting up in real mode on " + startingDate;
        }
        else
        {
            is_simulator = true;
            msg = "Monkey\tmain()\t\t\t\t" + "Monkey "+ version + " starting up in simulator mode. Day(s) " + startingDate + " to " + endingDate;
        } 
        
       
        
  
        String theDate = startingDate;
        String theTime = "09:29:00";
        String theQuote = "";

 

        long startTimer = System.nanoTime(); // Start timer for total runtime

        // Load market date ranges
        MarketDates md = new MarketDates();
        md.loadFromFile(dataDirectory + "marketDates.txt", startingDate, endingDate);
        ArrayList<MarketDates.MarketDate> marketDates = md.getMarketDates();
        String startTime = md.getMarketOpenTime(theDate);
        String endTime = md.getMarketCloseTime(theDate);
        
       
       
        System.out.println("\n\nMonkey " + version + " in " + (is_simulator?"SIMULATOR mode":"REAL mode") +" "+ marketDates.size() + " days... over date(s) [" + startingDate + " to " + endingDate + "]");
       

 
        
        // Initialize holdings and strategy objects
        AllHoldings h = new AllHoldings();
        Prof_Plum plum = new Prof_Plum();
        plum.clearFile();  // Clear daily stats
        plum.getFromFile();
        
        Mr_Green green = new Mr_Green(symbol, theDate, theTime, h, plum, startTime, endTime);
        Miss_Scarlet scarlet = new Miss_Scarlet(symbol, theDate, theTime, h, plum, startTime, endTime);
        Mrs_Peacock peacock = new Mrs_Peacock(symbol, theDate, theTime, h, plum, startTime, endTime);
        Col_Mustard mustard = new Col_Mustard(symbol, theDate, theTime, h, plum, startTime, endTime);

        green.clearFile();
        scarlet.clearFile();
        peacock.clearFile();
        mustard.clearFile();
        Tools.log("green.txt"   , theDate + " " + theTime,      "message\tdayNum\tprice\tavgcost\tprofit\tpercent\tspread\thigh\tlow\t");
        Tools.log("mustard.txt" , theDate + " " + theTime,      "message\tdayNum\tprice\tavgcost\tprofit\tpercent\tspread\thigh\tlow\t");
        Tools.log("scarlet.txt" ,   theDate + " " + theTime,    "message\tdayNum\tprice\tspreadPercent\tgap\tprevDayHigh\thigh\tlow\t");
        Tools.log("peacock.txt" ,   theDate + " " + theTime,    "message\tdayNum\tprice\tspreadPercent\tgap\tprevDayHigh\thigh\tlow\t");
        
        //******************************
        // continuous loop starts here
        //******************************
        nextDayLabel:
        //for every date (i.e. 2022-10-13 in the date range [startingDate, endingDate]
        for (MarketDates.MarketDate a_day : marketDates) 
        { //for each date in the dayrange [startingDate, endingDate]
            
            
            int dayNum = Tools.getDayNum(a_day.getDate());
            transcriptFileName = "transcript_"+ a_day.getDate() + ".txt";
            transcriptFile = "transcripts\\" + transcriptFileName;
            
            
            
            
            theDate = a_day.getDate();
            theTime = "09:29:01";
            theDT = theDate + " " + theTime;
            
            
            
            //Does the transcript file exist?
            File logTranscriptFile = new File(transcriptFile);
            if (!logTranscriptFile.exists()) {
                //set up header row 0 for the transcript file
                Tools.log(transcriptFile, theDT, "ROUTINE\tTYPE\tSYMBOL\tDAYNUM\tPRICE\tMESSAGE\tmyDT\tmySymbol\tmyType\tmyPrice\tmyComment\tmyVolume\tmyBid\tmyAsk\tmyBidSize\tmyAskSize\tmyHigh\tmyLow\tmyClose\tmyOpen");   //header row
            }
           Tools.log(transcriptFile, theDT, msg);
            
          
           
           
             //this section re-initializes the plum.txt statistics file for every new dataset that runs from dayNum = -7 to 4
            if (dayNum < -7 || dayNum > 4) {
                if (!alreadyDone) {
                    plum.clearFile();  
                    alreadyDone= true;   
                    msg = String.format("Monkey\tmain()\t\t\t\tPlum's stats file is emptied at %s",theDT);
                    Tools.log(transcriptFile, theDT, msg);
                }
                continue;  
            }
            
            
            
            //recall into plum object which contains stats from former days
            plum.getFromFile();
            
            
           

            //each clue character needs to know when the market opens and closes
            green.setMarketOpenTime(md.getMarketOpenTime(theDate));
            scarlet.setMarketOpenTime(md.getMarketOpenTime(theDate));
            mustard.setMarketOpenTime(md.getMarketOpenTime(theDate));
            peacock.setMarketOpenTime(md.getMarketOpenTime(theDate));
            green.setMarketCloseTime(md.getMarketCloseTime(theDate));
            scarlet.setMarketCloseTime(md.getMarketCloseTime(theDate));
            mustard.setMarketCloseTime(md.getMarketCloseTime(theDate));
            peacock.setMarketCloseTime(md.getMarketCloseTime(theDate));
            
            
            //stopTime is the time when the market closes
            String stopTime = md.getMarketCloseTime(theDate);
            long l_stopTime = Tools.ConvertTimeToLong(stopTime);
            

            int delay = Integer.parseInt(Tools.getConfig("loopDelay"));
            
            
            //output to the terminal window
            System.out.printf("%2d [%s %s]  %s shares: %d\n", dayNum, theDate, theTime, symbol, h.getNumShares(symbol));
            
            
            //for the current date, visit every quote available....
            String prevDT = "";
            while (true) 
            { //for each quote
                
                
                //Creating the URI to go to MMEngine's REST API for the next quote...
                //example REST API calls
                //simulator:    http://localhost/MMEngine/api/v2/quote?symbol=PDI&theDate=2022-08-22&theTime=09:30:00
                //
                //real:         http://localhost/MMEngine/api/v2/quote?symbol=PDI&theDate=today   {where today is today's date}
                //
                
                
                //http://localhost/MMEngine/api/v2/quote?symbol=PDI&theDate=2023-09-19&theTime=09:40:00
                String uri = "http://localhost/MMEngine/api/v2/quote?symbol=" + symbol + "&theDate=" + theDate + "&theTime=" + theTime;   
                Vector<String> resp = Tools.VectorURIPost(uri);  //this is where the URI POST goes to the REST API at MMEngine occurs...
                
                
                
                //update theTime variable, which is how the uri knows to go to the next quote
                //NB: occasionally theTime becomes null, in which case we take the prevTime and add 5 minutes to it 
                prevTime = theTime;
                theTime = Tools.getValidQuoteTime(resp);   //this parser method, scrapes the time, i.e. 09:31:00 from array element 0 of the resp array
                
                if (theTime == null) theTime = Tools.addMinutesToTime(prevTime, 5);
                
                    
                //check if the quote received from the API is a good quote...
                if (theTime != null && resp != null && !(resp.size() == 1 && resp.get(0).trim().equalsIgnoreCase("null"))) 
                { //good quote
                    
                    
                    //concatenate all the strings in the resp vector into one single string, with no separator, called theQuote
                    theQuote = String.join("\t", resp);
                    Quote q = new Quote(theQuote);
                    
                    
                    if (prevDT.equals(q.getDT())) {
                        //delay for 5 seconds...
                        try {
                            int delayInSeconds = 5000;
                            Thread.sleep(delayInSeconds);   //delay is set in the config.txt file
                            msg = String.format("Monkey\tDelay\t\t\t\tMonkey having to wait for a new real-time quote. Wait time is %d ms.",delayInSeconds);
                            Tools.log(transcriptFile, q.getDT(), msg);
                        } catch (InterruptedException e) {
                            System.out.println("Monkey Error 99: unable to delay.");
                        }
                        continue;
                    }
                    
                    if (q.getDT() != null)
                        prevDT = q.getDT();
                    else
                        prevDT = "";
                    
                    
                    
                    //end of day loop (ie. getting quotes from the day) STOPPING condition
                    l_theTime = Tools.ConvertTimeToLong(theTime);
                    if (l_theTime >= l_stopTime) {
                        msg = String.format("Monkey\tmain()\t\t\t\tend of day loop ends at %s",stopTime);
                        Tools.log(transcriptFile, q.getDT(), msg);
                        
                         //Check if the current date set i.e. [-7,4] is over ...
                        if (dayNum == 4) 
                        {
                            System.out.println("---------------------------------------------------------------------------------------------");
                            alreadyDone = false;
                        }
                       continue nextDayLabel;
                    }
                    
                    
                    
                    //update the on-going collected stats
                    plum.updateStatistics(symbol, dayNum, q.getPrice());
                    msg = String.format("Monkey\tPlum\t\t\t\tStats obtained from Plum %s",plum);
                    Tools.log(transcriptFile, q.getDT(), msg);
                    
                    
                    // Delay to reduce server load and avoid HTTPS errors
                    try {
                        Thread.sleep(delay);   //delay is set in the config.txt file
                    } catch (InterruptedException e) {
                        delay=2;                //if loopdelay variable not in config file, default to 2 ms.
                    }

                    
                    
                    //log to transcript
                    msg = String.format("Monkey\tQUOTE\t%s\t%d\t%.2f\t%s\t%s",symbol,dayNum,q.getPrice(),uri,theQuote);
                    Tools.log(transcriptFile, q.getDT(), msg);
                    
                   
                    
                    
                    
                    
                           
                    // ----------- Long Buy (Mr_Green) -----------
                    if (enable_Green && dayNum >= 0 && dayNum <= 4 && h.getNumShares(symbol) == 0) 
                    { //green
                        green.setTime(theTime);
                        green.setDate(theDate);
                        green.setQuote(q);
                        green.setDayNum(dayNum);
                        green.setNumShares(numShares);
                        
                        boolean green_buying = green.is_buying();
                        if (verbose > 2) System.out.println(dayNum + " " + green);
                        if (green_buying) { 
                            
                            str = String.format("Green buys %d shares @ %.2f",numShares,q.getPrice());
                            msg = String.format("Green\tOPEN\t%s\t%d\t%.2f\t%s%s\t%s",symbol,dayNum,q.getPrice(),str,"","");
                            Tools.log(transcriptDirectory + transcriptFileName, q.getDT(), msg);
                            if (verbose > 0) {
                                msg = String.format("\t(%3d) [%s %s] Mr Green just bought %4d shares of %s @%.2f Long  (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), green.getReasonCode());
                                System.out.printf(msg);
                            }
                        }
                      
                    } //green

                    // ----------- Long Sell (Miss_Scarlet) -----------
                    else if (enable_Scarlet && dayNum >= -7 && dayNum <= -1 && h.getNumShares(symbol) > 0) 
                    { //scarlet
                        scarlet.setTime(theTime);
                        scarlet.setDate(theDate);
                        scarlet.setQuote(q);
                        scarlet.setDayNum(dayNum);
                        scarlet.setNumShares(green.getNumShares());
                        
                        boolean scarlet_selling = scarlet.is_selling();
                        if (verbose > 2) System.out.println(dayNum + " " + scarlet);
                        if (scarlet_selling) { 
                            
                            str = String.format("Scarlet sells %d shares @ %.2f",numShares,q.getPrice());
                            msg = String.format("Scarlet\tCLOSE\t%s\t%d\t%.2f\t%s%s\t%s",symbol,dayNum,q.getPrice(),str,"","");
                            Tools.log(transcriptDirectory + transcriptFileName, q.getDT(), msg);
                            if (verbose > 0) {
                                msg = String.format("\t(%3d) [%s %s] Miss Scarlet just sold %4d shares of %s @%.2f Long  (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), scarlet.getReasonCode());
                                System.out.printf(msg);
                                System.out.printf("Profit: %.2f\tTotal Profit: %.2f\t%s Shares: %d\n",
                                    h.getProfit(), h.totalProfit(symbol), symbol, h.getNumShares(symbol));
                            }
                        }
                    } //scarlet

                    // ----------- Short Sell (Mrs_Peacock) -----------
                    else if (enable_Peacock && dayNum >= -7 && dayNum <= -1 && h.getNumShares(symbol) == 0) 
                    { //peacock
                        peacock.setTime(theTime);
                        peacock.setDate(theDate);
                        peacock.setQuote(q);
                        peacock.setDayNum(dayNum);
                        peacock.setNumShares(-1 * numShares);
                                          
                        boolean peacock_selling = peacock.is_selling();
                        if (verbose > 2) System.out.println(dayNum + " " + peacock);
                        if (peacock_selling) { 
                            
                            str = String.format("Peacock sells %d shares @ %.2f",numShares,q.getPrice());
                            msg = String.format("Peacock\tOPEN\t%s\t%d\t%.2f\t%s%s\t%s",symbol,dayNum,q.getPrice(),str,"","");
                            Tools.log(transcriptDirectory + transcriptFileName, q.getDT(), msg);
                            if (verbose > 0) {
                                msg = String.format("\t(%3d) [%s %s] Mrs Peacock just sold short %4d shares of %s @%.2f Short  (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), peacock.getReasonCode());
                                System.out.printf(msg);
                            }
                        }
                    } //peacock

                    // ----------- Short Cover (Col_Mustard) -----------
                    else if (enable_Mustard && dayNum >= 0 && dayNum <= 4 && h.getNumShares(symbol) < 0) 
                    { //mustard
                        mustard.setTime(theTime);
                        mustard.setDate(theDate);
                        mustard.setQuote(q);
                        mustard.setDayNum(dayNum);
                        mustard.setNumShares(-1 * numShares);
                        
                        boolean mustard_buying = mustard.is_buying();
                        if (verbose > 2) System.out.println(dayNum + " " + mustard);
                        if (mustard_buying) { 
                            
                            str = String.format("Mustard buys %d shares @ %.2f",numShares,q.getPrice());
                            msg = String.format("Mustard\tOPEN\t%s\t%d\t%.2f\t%s%s\t%s",symbol,dayNum,q.getPrice(),str,"","");
                            Tools.log(transcriptDirectory + transcriptFileName, q.getDT(), msg);
                            if (verbose > 0) {
                                msg = String.format("\t(%3d) [%s %s] Col Mustard just bought %4d shares of %s @%.2f Short (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), mustard.getReasonCode());
                                System.out.printf(msg);
                                System.out.printf("Profit: %.2f\tTotal Profit: %.2f\t%s Shares: %d\n",
                                    h.getProfit(), h.totalProfit(symbol), symbol, h.getNumShares(symbol));
                            }
                        }
                 
                    } //mustard

                } //good quote
                    
                
                
            }  //for each quote 
            
            /*
               
            */
        } //for each date in the dayrange [startingDate, endingDate]
        
        
        
        
          
        //******************************
        // continuous loop ends here
        //******************************
             

        if (verbose > 0) {
            System.out.println("\nTransaction(s)");
            h.printAllHoldings();
            System.out.printf("Total Profit: %.2f\n", h.totalProfit(symbol));
            
            System.out.println("---------------------------------------------------------------------------------------------");
            System.out.println("\nCopy and Paste into your spreadsheet called,  results_v##.xlsx");
            h.results();
            System.out.println("---------------------------------------------------------------------------------------------");
    
            long endTimer = System.nanoTime(); // End timer
            double elapsedMinutes = (endTimer - startTimer) / 1_000_000_000.0 / 60.0;
    
            System.out.println("\n\nMonkey says good-bye on " + theDate + " " + theTime +
                    "\tElapsed time: " + String.format("%.4f", elapsedMinutes) + " minutes\n");
        }
    }

    
    

    

    
    
    
  
  
    
    
}
