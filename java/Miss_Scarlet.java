import java.time.LocalDate;
import java.io.*;
import java.util.*;

/**
 * The Miss_Scarlet class represents a trading strategy entity that decides whether 
 * to sell shares of a given symbol (stock) based on profit/loss conditions and price patterns.
 * 
 * It works with:
 * - AllHoldings: tracks currently open holdings.
 * - Prof_Plum: provides historical data (not used directly here but passed in).
 * - Quote: provides current market data (price, high, low, etc.).
 * 
 * There are four main selling rules:
 *   S0: Always sell in the last 15 minutes of day -1.

 */
public class Miss_Scarlet {
    private String theDate;            // Current date in yyyy-MM-dd format.
    private String theTime;            // Current time in HH:mm:ss format.
    private int dayNum;                // Current day number (negative days counting back).
    private Quote q;                   // Current quote data.
    private AllHoldings h;             // Portfolio of current holdings.
    private String symbol;             // Stock symbol (e.g., "AAPL").
    private String reasonCode;         // Code explaining why a sell decision was made.
    private int numShares;             // Number of shares to sell.
    private Prof_Plum plum;            // Historical stats provider (not used directly here).
        private String marketOpenTime;
    private String marketCloseTime;

    // Thresholds loaded from configuration (Tools.getConfig reads them as strings).
    private double sl_lowerlimit = Double.parseDouble(Tools.getConfig("SCARLET_SL_LOWERLIMIT"));
    private double sl_upperlimit = Double.parseDouble(Tools.getConfig("SCARLET_SL_UPPERLIMIT"));
    private double sp_threshold = Double.parseDouble(Tools.getConfig("SCARLET_SP_THRESHOLD"));
    private double st_spreadPercent = Double.parseDouble(Tools.getConfig("SCARLET_ST_PERCENT"));
    private double st_closeness = Double.parseDouble(Tools.getConfig("SCARLET_ST_CLOSENESS"));

    /**
     * Constructor for Miss_Scarlet.
     * 
     * @param symbol  The stock symbol.
     * @param date    The current date.
     * @param time    The current time.
     * @param h       The portfolio of current holdings.
     * @param plum    Historical stats provider.
     */
    public Miss_Scarlet(String symbol, String date, String time, AllHoldings h, Prof_Plum plum, String marketOpenTime, String marketCloseTime) {
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


    
    public void setMarketOpenTime(String newTime) {
        this.marketOpenTime = newTime;
    }
    public void setMarketCloseTime(String newTime) {
        this.marketCloseTime = newTime;
    }
    
    public void setDate(String date) {
        this.theDate = date;
    }

    public void setNumShares(int n) {
        this.numShares = n;
    }

    public int getNumShares() {
        return this.numShares;
    }

    public void setQuote(Quote q) {
        this.q = q;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    /**
     * Decides whether to sell shares under four main cases:
     * 
     * Case S0:
     *   - Always sell during the last 15 minutes of day -1 (15:45:00 to 16:00:00).
     * 
     * Case SL (Stop Loss):
     *   - Sell if the percentage gain/loss moves within a certain small negative range 
     *     (e.g., recovering from a small loss).
     * 
     * 
     * @return true if a sell action is taken; false otherwise.
     */
    public boolean is_selling() {
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
                
        


        

        msg = String.format("Scarlet sees\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
        Tools.log("scarlet.txt", q.getDT(), msg);
        
        
        
        
        // ------------------------
        // Case S0: Late-day sell on day -1
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,15);
        if ((dayNum == -1) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
                reasonCode = "S0";
                h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
            
                return true;
        }


      
        // ------------------------
        // Case SL: Stop loss recovery zone (if in small loss range)
        if (dayNum == -1 || dayNum == -2 || dayNum == -3) {
            if (percent >= sl_lowerlimit && percent <= sl_upperlimit) {
                reasonCode = "SL";
                h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
                            
                msg = String.format("Scarlet sells, (%s) \t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", reasonCode,dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
                Tools.log("scarlet.txt", q.getDT(), msg);

                return true;
            }
        }
 
        
        
        
        // No sell signal.
        return false;
    }

    

    
   //empties the scarlet.txt file, but does not delete the file.  scarlet.txt just stores activity by scarlet
    public void clearFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("..\\logs\\scarlet.txt"))) {
            // Truncate file by writing nothing
        } catch (IOException e) {
            System.err.println("Error clearing scarlet file: " + e.getMessage());
        }
    }

        
    
    
    
    
    /**
     * Returns a string summary of the current Miss_Scarlet status, including potential profit 
     * if there's an active holding.
     * 
     * @return Summary string.
     */
    public String toString() {
        String str = String.format("Miss_Scarlet [%s %s]\t%.2f\t", theDate, theTime, q.getPrice());

        if (h.hasHolding(symbol)) {
            double profit = (q.getPrice() - h.getAvgCost(symbol)) * numShares;
            str += String.format("profit: %.2f", profit);
        }

        return str;
    }
}
