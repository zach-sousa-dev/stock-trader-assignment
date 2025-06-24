import java.time.LocalDate;
import java.io.*;
import java.util.*;
/**
 * The Mrs_Peacock class represents a trading strategy entity that 
 * decides whether to open short positions (i.e., betting the stock will go down) 
 * on a given symbol based on price movements and thresholds.
 * 
 * It works with:
 * - AllHoldings: manages current holdings, including short positions.
 * - Prof_Plum: provides historical stats (not directly used here but passed in).
 * - Quote: provides current market data (price, high, low, etc.).
 * 
 * There are four main short-selling rules:
 *   P0: Always short-sell in the last 10 minutes of day -1.
 *   PL: Short if the percent change is in a small negative range.
 * 
 * IMPORTANT: For shorting, make sure setNumShares() sets a **negative number**.
 */
public class Mrs_Peacock {
    private String theDate;            // Current date in yyyy-MM-dd format.
    private String theTime;            // Current time in HH:mm:ss format.
    private int dayNum;                // Current day number (negative days counting back).
    private Quote q;                   // Current quote data.
    private AllHoldings h;             // Portfolio of current holdings.
    private String symbol;             // Stock symbol (e.g., "AAPL").
    private String reasonCode;         // Code explaining why a short-sell decision was made.
    private int numShares;             // Number of shares to short (negative number).
    private Prof_Plum plum;            // Historical stats provider (not directly used here).
    private String marketOpenTime;
    private String marketCloseTime;

    /**
     * Constructor for Mrs_Peacock.
     * 
     * @param symbol  The stock symbol.
     * @param date    The current date.
     * @param time    The current time.
     * @param h       The portfolio of current holdings.
     * @param plum    Historical stats provider.
     */
    public Mrs_Peacock(String symbol, String date, String time, AllHoldings h, Prof_Plum plum, String marketOpenTime, String marketCloseTime) {
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
        this.numShares = n;  // NOTE: should be negative for short-selling.
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
     * Decides whether to open a short position under four main cases:
     * 
     * Case P0:
     *   - Always short during the last 10 minutes of day -1 (15:50:00 to 16:00:00).
     * 
     * 
     * 
     * @return true if a short-sell action is taken; false otherwise.
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
                

        


        msg = String.format("Peacock sees\t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
        Tools.log("peacock.txt", q.getDT(), msg);
        
        
        
        
        
        // ------------------------
        // Case P0: Late-day short on day -1
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,10);
        if ((dayNum == -1) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
                reasonCode = "P0";
                h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
            
                msg = String.format("Peacock sells, (%s) \t%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", reasonCode,dayNum, q.getPrice(), h.getAvgCost(symbol), profit, percent, spreadPercent, q.getHigh(), q.getLow());
                Tools.log("peacock.txt", q.getDT(), msg);
                
                return true;
        }

  
        // No short-sell signal.
        return false;
    }

    
     
   //empties the peacock.txt file, but does not delete the file.  peacock.txt just stores activity by peacock
    public void clearFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("..\\logs\\peacock.txt"))) {
            // Truncate file by writing nothing
        } catch (IOException e) {
            System.err.println("Error clearing peacock file: " + e.getMessage());
        }
    }

    
       
    
    
    
    /**
     * Returns a string summary of the current Mrs_Peacock status, 
     * including potential profit if there's an active holding.
     * 
     * @return Summary string.
     */
    public String toString() {
        String str = String.format("Mrs Peacock [%s %s]\t%.2f\t", theDate, theTime, q.getPrice());

        if (h.hasHolding(symbol)) {
            double profit = (q.getPrice() - h.getAvgCost(symbol)) * numShares;
            str += String.format("profit: %.2f", profit);
        }

        return str;
    }
}
