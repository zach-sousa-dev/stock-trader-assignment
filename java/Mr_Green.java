import java.time.LocalDate;
import java.io.*;
import java.util.*;

/**
 * The Mr_Green class represents a trading strategy entity that decides whether to buy shares 
 * of a given symbol (stock) based on market data and specific conditions.
 * 
 * It works with:
 * - AllHoldings: tracks currently open holdings.
 * - Prof_Plum: tracks historical statistics (like previous day's high).
 * - Quote: provides current market data (price, high, low, etc.).
 * 
 * There are two main buying rules:
 *   G0: Always buy in the last 15 minutes of day 4.
 *  
 */
public class Mr_Green {
    private String theDate;            // Current date in yyyy-MM-dd format.
    private String theTime;            // Current time in HH:mm:ss format.
    private int dayNum;                // Current day number (used for decision logic).
    private Quote q;                   // Current quote data.
    private AllHoldings h;             // Portfolio of current holdings.
    private String symbol;             // Stock symbol (e.g., "AAPL").
    private String reasonCode;         // Code explaining why a buy decision was made.
    private int numShares;             // Number of shares to buy.
    private Prof_Plum plum;            // Provides historical data for the symbol.
    private String marketOpenTime;
    private String marketCloseTime;

    // Threshold values read from configuration.
    private double g1_threshold = Double.parseDouble(Tools.getConfig("GREEN_G1_SPREADPERCENT"));
    private double g1_howNearLow = Double.parseDouble(Tools.getConfig("GREEN_G1_HOW_NEAR_LOW"));

    /**
     * Constructor for Mr_Green.
     * 
     * @param symbol  The stock symbol.
     * @param date    The current date.
     * @param time    The current time.
     * @param h       The portfolio of current holdings.
     * @param plum    Historical stats provider.
     */
    public Mr_Green(String symbol, String date, String time, AllHoldings h, Prof_Plum plum, String marketOpenTime, String marketCloseTime) {
        this.symbol = symbol;
        this.theDate = date;
        this.theTime = time;
        this.h = h;
        this.plum = plum;
        this.marketOpenTime = marketOpenTime;
        this.marketCloseTime = marketCloseTime;
    }

    // Setters for day number, time, date, and quote.
    public void setDayNum(int dayNum) {
        this.dayNum = dayNum;
    }

    public void setTime(String newTime) {
        this.theTime = newTime;
    }
    
    public void setMarketOpenTime(String newTime) {
        this.marketOpenTime = newTime;
    }
    
    public void setMarketCloseTime(String newTime) {
        this.marketCloseTime = newTime;
    }


    public void setDate(String date) {
        this.theDate = date;
    }

    public void setQuote(Quote q) {
        this.q = q;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setNumShares(int n) {
        this.numShares = n;
    }

    public int getNumShares() {
        return this.numShares;
    }

    /**
     * Decides whether to buy shares under two main cases:
     * 
     * Case G0:
     *   - Always buy during the last 15 minutes of day 4 (15:45:00 to 16:00:00).
     * 
     * Case G1:
     *   - Buy if today's price spread (high - low) is large enough, and 
     *     current price is close to the day's low.
     * 
     * @return true if a buy action is taken; false otherwise.
     */
    public boolean is_buying() {
        
        double profit = 0.0;
        double percent = 0.0;
        double spreadPercent = (q.getHigh() - q.getLow()) / q.getLow() * 100.0;
        String msg="";
        if (h.getAvgCost(symbol) > 0.000001) {
            profit = (q.getPrice() - h.getAvgCost(symbol)) * numShares;
            percent = (q.getPrice() - h.getAvgCost(symbol)) / h.getAvgCost(symbol) * 100.0;
        }
        else {
            profit = 0.00;
            percent = 0.00;
        }
        

        

        // "message\tdayNum\tprice\tprofit\tpercent\tspread\thigh\tlow\t");
        msg = String.format("Green sees\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
        Tools.log("green.txt", q.getDT(), msg);
        
        
        
        
        // ------------------------
        // Case G0: Late-day buy on day 4
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,15);
        if ((dayNum == 4) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
            reasonCode = "G0";
            
        
            h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
            
            //here Rest API placeOrder goes here
            
            msg = String.format("Green buys, (%s) \t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", reasonCode,dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
            Tools.log("green.txt", q.getDT(), msg);
                                
            return true;
        }

   
        
        
        // ------------------------
        // Case G3: Green    
        if (spreadPercent > 1.15) {
            
            if (dayNum == 2 || dayNum == 3 || dayNum == 4) {
                double x1 = plum.getValue(symbol,"high" ,dayNum-2);
                double y1 = plum.getValue(symbol,"low"  ,dayNum-2);
                double x2 = plum.getValue(symbol,"high" ,dayNum-1);
                double y2 = plum.getValue(symbol,"low"  ,dayNum-1);
                double x3 = plum.getValue(symbol,"high" ,dayNum);
                double y3 = plum.getValue(symbol,"low"  ,dayNum);
                
                boolean ok = Tools.HLx3(x1,x2,x3,y1,y2,y3);

                if (ok) {
                                reasonCode = "G3";
                                h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
                                msg = String.format("Green buys, (%s) \t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", reasonCode,dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
                                Tools.log("green.txt", q.getDT(), msg);
                                
                                return true;
                }
            }
        }
        
       
        // No buy signal.
        return false;
    }
    
    
    //empties the green.txt file, but does not delete the file.  green.txt just stores activity by green 
    public void clearFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("..\\logs\\green.txt"))) {
            // Truncate file by writing nothing
        } catch (IOException e) {
            System.err.println("Error clearing green file: " + e.getMessage());
        }
    }

    
    
    
    /**
     * Returns a string summary of the current Mr_Green status, including potential profit
     * if there's an active holding.
     * 
     * @return Summary string.
     */
    public String toString() {
        String str = String.format("Mr_Green [%s %s]\t%.2f\t", theDate, theTime, q.getPrice());

        if (h.hasHolding(symbol)) {
            double profit = (q.getPrice() - h.getAvgCost(symbol)) * numShares;
            str += String.format("profit: %.2f", profit);
        }

        return str;
    }
}
