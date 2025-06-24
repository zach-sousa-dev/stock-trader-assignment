import java.util.*;
import java.io.*;
import java.text.*;
import java.net.URLConnection;
import java.net.URL;
import java.net.NoRouteToHostException;
import java.time.LocalDate;

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
 * @version 214
 */
public class Monkey {
    private static final String version = "v214";
    private static final int MAX_RETRIES = 5;
    private static final String LOG_FILE = "errors.txt";

    // Enable/disable strategies via config file
    private static final boolean enable_Green    = Tools.getConfig("enable_Green").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Scarlet  = Tools.getConfig("enable_Scarlet").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Peacock  = Tools.getConfig("enable_Peacock").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Mustard  = Tools.getConfig("enable_Mustard").trim().equalsIgnoreCase("YES");
    private static final boolean enable_White    = Tools.getConfig("enable_White").trim().equalsIgnoreCase("YES");
    private static final boolean enable_Plum     = Tools.getConfig("enable_Plum").trim().equalsIgnoreCase("YES");

    public static void main(String[] args) {
        int numShares = 1000;
        String symbol = "PDI";
        boolean verbose = false;
        boolean alreadyDone = false;
        String startingDate = "2022-06-28"; // original
        //String startingDate = "2022-09-09";
        String endingDate   = "2023-09-14";   // original
        //String endingDate = "2022-10-18";     //  early end
        String theDT;
        String dataDirectory = "../data/";  // where config.txt and marketDates.txt are located
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
        String theDate = startingDate;
        String theTime = "09:29:00";
        String theQuote = "";

        Tools.log("scarlet.txt", theDate + " " + theTime, "dayNum\tprice\tprofit\tpercent\tspread\thigh\tlow\t");
        Tools.log("green.txt",   theDate + " " + theTime, "dayNum\tprice\tspreadPercent\tgap\tprevDayHigh\thigh\tlow\t");

        long startTimer = System.nanoTime(); // Start timer for total runtime

        // Load market date ranges
        MarketDates md = new MarketDates();
        md.loadFromFile(dataDirectory + "marketDates.txt", startingDate, endingDate);
        ArrayList<MarketDates.MarketDate> marketDates = md.getMarketDates();
        String startTime = md.getMarketOpenTime(theDate);
        String endTime = md.getMarketCloseTime(theDate);
        
       
        System.out.println("\n\nMonkey " + version + "  " + marketDates.size() + " days... over date(s) [" + startingDate + " to " + endingDate + "]\n");

        // Initialize holdings and strategy objects
        AllHoldings h = new AllHoldings();
        Prof_Plum plum = new Prof_Plum();
        plum.clearFile();  // Clear daily stats
        plum.getFromFile();
        
        Mr_Green green = new Mr_Green(symbol, theDate, theTime, (AllHoldings) h, plum, startTime, endTime);
        Miss_Scarlet scarlet = new Miss_Scarlet(symbol, theDate, theTime, h, plum, startTime, endTime);
        Mrs_Peacock peacock = new Mrs_Peacock(symbol, theDate, theTime, h, plum, startTime, endTime);
        Col_Mustard mustard = new Col_Mustard(symbol, theDate, theTime, h, plum, startTime, endTime);

        for (MarketDates.MarketDate a_day : marketDates) {
            int dayNum = Tools.getDayNum(a_day.getDate());
            
            
            
            //this section re-initializes the plum.txt statistics file for every dataset from dayNum = -7 to 4
            if (dayNum < -7 || dayNum > 4) {
                if (!alreadyDone) {
                    plum.clearFile();
                    alreadyDone= true;
                    System.out.println(dayNum + " plum.txt on-going statistics file cleared/reset."); //ROCKY
                }
                //  continue    // no longer needed
                String end = a_day.getMarketCloseTime();                // Thanks Eli Wood for this part
                long checkTime = Tools.ConvertTimeToLong(end) - 1;
                theTime = Tools.ConvertTimeToString(checkTime);
            }
            

 
            plum.getFromFile();
            
           
            theDate = a_day.getDate();
            theTime = "09:25:00";
            theDT = theDate + " " + theTime;
            
           
            
            green.setMarketOpenTime(md.getMarketOpenTime(theDate));
            scarlet.setMarketOpenTime(md.getMarketOpenTime(theDate));
            mustard.setMarketOpenTime(md.getMarketOpenTime(theDate));
            peacock.setMarketOpenTime(md.getMarketOpenTime(theDate));
            green.setMarketCloseTime(md.getMarketCloseTime(theDate));
            scarlet.setMarketCloseTime(md.getMarketCloseTime(theDate));
            mustard.setMarketCloseTime(md.getMarketCloseTime(theDate));
            peacock.setMarketCloseTime(md.getMarketCloseTime(theDate));
            

            System.out.printf("%2d [%s %s]  %s shares: %d\n", dayNum, theDate, theTime, symbol, h.getNumShares(symbol));

            while (true) {
                theTime = getValidQuoteTime(symbol, theDate, theTime);
                if (theTime == null || theTime.equals("null")) break;
                

                // Delay to reduce server load and avoid HTTPS errors
                try {
                    int delay = Integer.parseInt(Tools.getConfig("loopDelay"));
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    System.out.println("Error 103: Loop delay error");
                }

                String url = "http://localhost/MMEngine/MMEngine.php";
                String parameters = "symbol=" + symbol + "&theDate=" + theDate + "&theTime=" + theTime + "&justTheQuote=true";
                Vector<String> resp = VectorPost(url, parameters);

                if (resp != null && !(resp.size() == 1 && resp.get(0).trim().equalsIgnoreCase("null"))) {
                    theQuote = String.join("", resp);
                    Quote q = new Quote(theQuote);
                    theTime = q.getTime();

                    plum.updateStatistics(symbol, dayNum, q.getPrice());
                   
        
                    // ----------- Long Buy (Mr_Green) -----------
                    if (enable_Green && dayNum >= 0 && dayNum <= 4 && h.getNumShares(symbol) == 0) {
                        green.setTime(theTime);
                        green.setDate(theDate);
                        green.setQuote(q);
                        green.setDayNum(dayNum);
                        green.setNumShares(numShares);
                        if (verbose) System.out.println(dayNum + " " + green);
                        if (green.is_buying()) {
                            System.out.printf("\t(%3d) [%s %s] Mr Green just bought %4d shares of %s @%.2f Long  (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), green.getReasonCode());
                        }
                    }

                    // ----------- Long Sell (Miss_Scarlet) -----------
                    else if (enable_Scarlet && dayNum >= -7 && dayNum <= -1 && h.getNumShares(symbol) > 0) {
                        scarlet.setTime(theTime);
                        scarlet.setDate(theDate);
                        scarlet.setQuote(q);
                        scarlet.setDayNum(dayNum);
                        scarlet.setNumShares(green.getNumShares());
                        if (verbose) System.out.println(dayNum + " " + scarlet);
                        if (scarlet.is_selling()) {
                            System.out.printf("\t(%3d) [%s %s] Miss Scarlet just sold %4d shares of %s @%.2f Long (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), scarlet.getReasonCode());
                            System.out.printf("Profit: %.2f\tTotal Profit: %.2f\t%s Shares: %d\n",
                                    h.getProfit(), h.totalProfit(symbol), symbol, h.getNumShares(symbol));
                        }
                    }

                    // ----------- Short Sell (Mrs_Peacock) -----------
                    else if (enable_Peacock && dayNum >= -7 && dayNum <= -1 && h.getNumShares(symbol) == 0) {
                        peacock.setTime(theTime);
                        peacock.setDate(theDate);
                        peacock.setQuote(q);
                        peacock.setDayNum(dayNum);
                        peacock.setNumShares(-1 * numShares);
                        if (verbose) System.out.println(dayNum + " " + peacock);
                        if (peacock.is_selling()) {
                            System.out.printf("\t(%3d) [%s %s] Mrs Peacock just sold %4d shares of %s @%.2f Short (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), peacock.getReasonCode());
                        }
                    }

                    // ----------- Short Cover (Col_Mustard) -----------
                    else if (enable_Mustard && dayNum >= 0 && dayNum <= 4 && h.getNumShares(symbol) < 0) {
                        mustard.setTime(theTime);
                        mustard.setDate(theDate);
                        mustard.setQuote(q);
                        mustard.setDayNum(dayNum);
                        mustard.setNumShares(-1 * numShares);
                        if (verbose) System.out.println(dayNum + " " + mustard);
                        if (mustard.is_buying()) {
                            System.out.printf("\t(%3d) [%s %s] Col Mustard just bought %4d shares of %s @%.2f Short (%s)\n",
                                    dayNum, theDate, theTime, numShares, symbol, q.getPrice(), mustard.getReasonCode());
                            System.out.printf("Profit: %.2f\tTotal Profit: %.2f\t%s Shares: %d\n",
                                    h.getProfit(), h.totalProfit(symbol), symbol, h.getNumShares(symbol));
                                    
                          
                        }
                    }

                    // ----------- Update Trend (Prof_Plum) -----------
                    // this needs to happen EVERY day
                    // check for end of day
                    // this block was originally created by Eli Wood, however its been modified to instead fire off my update function instead of his (which doesn't exist here)
                    if (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(a_day.getMarketCloseTime())
                            || Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong("16:00:00")) {
                        System.out.println("end of day - " + theDate);
                        plum.updateTrend(q);
                        plum.logStoredGainLossHistory();
                        System.out.println(IOColors.BLUE + "Plum Trending says: " + IOColors.RESET + plum.isTrendingUp());
                        break;
                    }

                } else {
                    break;  // No valid response, break the day loop
                }
            }

            
            
            
            if (dayNum == 4) {
                System.out.println("---------------------------------------------------------------------------------------------");
                alreadyDone = false;
                
            }
           
                
           
        }

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

    /**
     * Attempts to retrieve a valid quote time, retrying up to MAX_RETRIES.
     * 
     * @param symbol Stock symbol.
     * @param date   Date string.
     * @param time   Time string.
     * @return Valid time string or null if failed.
     */
    private static String getValidQuoteTime(String symbol, String date, String time) {
        int retryCount = 0;
        String validTime = null;

        while (retryCount < MAX_RETRIES) {
            String url = "http://localhost/MMEngine/MMEngine.php";
            String parameters = "symbol=" + symbol + "&theDate=" + date + "&theTime=" + time + "&justTheQuote=true";
            Vector<String> resp = VectorPost(url, parameters);

            if (resp != null && !(resp.size() == 1 && resp.get(0).trim().equalsIgnoreCase("null"))) {
                String quote = String.join("", resp);
                Quote q = new Quote(quote);
                validTime = q.getTime();
                break;
            }

            retryCount++;
            log("Retry " + retryCount + " for time=" + time);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("Interrupted while retrying");
                break;
            }
        }

        if (validTime == null || validTime.equals("null")) {
            log("CRITICAL: No valid quote returned after " + MAX_RETRIES + " attempts for time=" + time);
        }

        return validTime;
    }

    
    
    
    
    /**
     * Logs a message to the LOG_FILE.
     */
    private static void log(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            out.println(new Date() + " - " + message);
        } catch (IOException e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
    
    
    
    

    /**
     * Sends a POST request and returns the response as a vector of strings.
     */
    private static Vector<String> VectorPost(String machine, String message) {
        Vector<String> data = new Vector<>(0);
        try {
            URL EregURL = new URL(machine);
            URLConnection URLConn = EregURL.openConnection();
            URLConn.setConnectTimeout(1500);
            URLConn.setDoInput(true);
            URLConn.setDoOutput(true);

            try (OutputStreamWriter out = new OutputStreamWriter(URLConn.getOutputStream())) {
                out.write(message);
                out.flush();
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(URLConn.getInputStream()))) {
                String temp_buff;
                while ((temp_buff = in.readLine()) != null) {
                    if (temp_buff.length() > 0)
                        data.add(temp_buff);
                }
            }

            return data;
        } catch (NoRouteToHostException e) {
            System.out.println("Error: 101 No Route to Host");
            return null;
        } catch (Exception e) {
            System.out.println("Error: 100 Can't reach MMEngine.php");
            return null;
        }
    }
}
