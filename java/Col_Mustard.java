import java.time.LocalDate;

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

        // Get the previous day's high (or today's if dayNum == 0).
        //String varname = "high";
        //double prevDayHigh = plum.getValue(symbol, varname, dayNum - 1);

        // Calculate the gap from previous day's high to current price.
        //double gap = (prevDayHigh - q.getPrice()) / prevDayHigh * 100.0;

        // Log details to mustard.txt for tracking/debugging.
        //String msg = String.format("%d\t%.2f\t%.2f\t%.1f\t%.2f\t%.2f\t%.2f\t",
         //       dayNum, q.getPrice(), spreadPercent, prevDayHigh, gap, q.getHigh(), q.getLow());
        //.log("mustard.txt", q.getDT(), msg);

        // ------------------------
        // Case M0: Late-day close on day 4
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,15);
        if ((dayNum == 4) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
            h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
            reasonCode = "M0";
            return true;
        }

        
        double low2 = plum.getValue(symbol,"low"  ,dayNum-2);  //ROCKY
        double low1 = plum.getValue(symbol,"low"  ,dayNum-1);
        double low0 = plum.getValue(symbol,"low"  ,dayNum);
        String msg = String.format("%d - Mustard sees %.2f, spreadPercent: %.2f  low2: %.2f  low1: %.2f  low0: %.2f", dayNum, q.getPrice(), spreadPercent, low2, low1, low0);
        Tools.log("mustard.txt", q.getDT(), msg);
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
                
                boolean ok = Tools.HLx3(x1,x2,x3,y1,y2,y3);     //  changed from detecing fall off to now detect rises
                String message = String.format("spread: %.2f\tdayNum: %d\tx1: %.2f x2: %.2f x3: %.2f y1: %.2f y2: %.2f y3:%.2f ok: %b",spreadPercent,dayNum,x1,x2,x3,y1,y2,y3, ok);
                Tools.log("HLx3.txt", q.getDT(), message);
                if (ok) {
                    
                                h.closeHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
                                msg = String.format("%d - Mustard covers short @ %.2f, spreadPercent: %.2f", dayNum, q.getPrice(), spreadPercent);
                                Tools.log("mustard.txt", q.getDT(), msg);
                                reasonCode = "M3";
                              
                                return true;
                }
            }
        }
     
        // No close signal.
        return false;
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
