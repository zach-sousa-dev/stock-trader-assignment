import java.time.LocalDate;
import java.io.*;
import java.util.*;
/**
 * The Col_Mustard class represents a trading strategy entity 
 * that decides whether to close holdings (i.e., sell shares) 
 * of a given symbol based on specific price movements.
 * 
 * It works with:
 * - AllHoldings: manages current portfolio positions.
 * - Prof_Plum: provides historical statistics (like previous day’s high).
 * - Quote: supplies the current market data (price, high, low, etc.).
 * 
 * There are two main closing (sell) rules:
 *   M0: Always close holdings in the last 15 minutes of day 4.
 *  
 */
public class Col_Mustard {
    private String theDate;            // Current date in yyyy-MM-dd format.
    private String theTime;            // Current time in HH:mm:ss format.
    private int dayNum;                // Current day number.
    private Quote q;                   // Current quote data.
    private AllHoldings h;             // Portfolio of current holdings.
    private String symbol;             // Stock symbol (e.g., "AAPL").
    private String reasonCode;         // Code explaining why a closing decision was made.
    private int numShares;             // Number of shares to close.
    private Prof_Plum plum;            // Historical stats provider.
    private String marketOpenTime;
    private String marketCloseTime;
    

    /**
     * Constructor for Col_Mustard.
     * 
     * @param symbol  The stock symbol.
     * @param date    The current date.
     * @param time    The current time.
     * @param h       The portfolio of current holdings.
     * @param plum    Historical stats provider.
     */
    public Col_Mustard(String symbol, String date, String time, AllHoldings h, Prof_Plum plum, String marketOpenTime, String marketCloseTime) {
        this.symbol = symbol;
        this.theDate = date;
        this.theTime = time;
        this.h = h;
        this.plum = plum;
        this.marketOpenTime = marketOpenTime;
        this.marketCloseTime = marketCloseTime;
    }

    // Setters for day number, time, date, quote, and number of shares.
    public void setDayNum(int dayNum) {
        this.dayNum = dayNum;
    }

    public void setTime(String newTime) {
        this.theTime = newTime;
    }

    public void setDate(String date) {
        this.theDate = date;
    }

    public void setQuote(Quote q) {
        this.q = q;
    }

  
    
    public void setMarketOpenTime(String newTime) {
        this.marketOpenTime = newTime;
    }
    
    public void setMarketCloseTime(String newTime) {
        this.marketCloseTime = newTime;
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
     * Decides whether to close (sell) holdings under two main cases:
     * 
     * Case M0:
     *   - Always close during the last 15 minutes of day 4 (15:45:00 to 16:00:00).
     * 
     * Case M1:
     *   - Close if today’s price spread is ≥1.5% AND 
     *     the current price is within 0.05 of today’s low.
     * 
     * @return true if a close action is taken; false otherwise.
     */
    public boolean is_buying() {
        // Calculate spread percentage (high - low relative to low).
        double spreadPercent = (q.getHigh() - q.getLow()) / q.getLow() * 100.0;
        double profit = 0.0;
        double percent = 0.0;
        String msg = "";
        
        if (h.getAvgCost(symbol) > 0.000001) {
            profit = (q.getPrice() - h.getAvgCost(symbol)) * numShares;
            percent = (q.getPrice() - h.getAvgCost(symbol)) / h.getAvgCost(symbol) * 100.0;
        }
        else {
            profit = 0.00;
            percent = 0.00;
        }
        
        

        // "message\tdayNum\tprice\tprofit\tpercent\tspread\thigh\tlow\t");
        msg = String.format("Mustard sees\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
        Tools.log("mustard.txt", q.getDT(), msg);
        
                
        
        // ------------------------
        // Case M0: Late-day close on day 4
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,15);
        if ((dayNum == 4) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
                reasonCode = "M0";
                h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
 
                msg = String.format("Mustard buys, (%s) \t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", reasonCode,dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
                Tools.log("mustard.txt", q.getDT(), msg);
            return true;
        }

        

        double low2 = plum.getValue(symbol,"low"  ,dayNum-2);  //ROCKY
        double low1 = plum.getValue(symbol,"low"  ,dayNum-1);
        double low0 = plum.getValue(symbol,"low"  ,dayNum);
        // ------------------------
        // Case G3: 
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
                                reasonCode = "M3";
                                h.closeHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
                                msg = String.format("%d - Mustard covers short @ %.2f, spreadPercent: %.2f", dayNum, q.getPrice(), spreadPercent);
                                Tools.log("mustard.txt", q.getDT(), msg);
                                
                                return true;
                }
            }
        }
     
        // No close signal.
        return false;
    }

    
    
     
    //empties the mustard.txt file, but does not delete the file.  mustard.txt just stores activity by mustard
    public void clearFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("..\\logs\\mustard.txt"))) {
            // Truncate file by writing nothing
        } catch (IOException e) {
            System.err.println("Error clearing mustard file: " + e.getMessage());
        }
    }

    
       
    
    
    
    
    /**
     * Returns a string summary of the current Col_Mustard status, 
     * including potential profit (minus commission cost) if there’s an active holding.
     * 
     * @return Summary string.
     */
    public String toString() {
        String str = String.format("Col_Mustard [%s %s]\t%.2f\t", theDate, theTime, q.getPrice());

        if (h.hasHolding(symbol)) {
            double profit = (q.getPrice() - h.getAvgCost(symbol)) * numShares - (0.2205 * numShares);
            str += String.format("profit: %.2f", profit);
        }

        return str;
    }
}
