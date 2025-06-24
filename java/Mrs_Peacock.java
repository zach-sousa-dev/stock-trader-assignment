import java.time.LocalDate;

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
 *   PP: Short if a strong upward pop (5.4%+) happens on days -6, -5, -4.
 *   PT: Short if on days -3, -2, -1, the price is very close to the day’s high.
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

    private final boolean DEBUG_MODE = false;

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
        double avgCost = h.getAvgCost(symbol);
        double profit = (q.getPrice() - avgCost) * numShares;
        double percent = ((q.getPrice() - avgCost) / avgCost) * 100;
        double spreadPercent = (q.getHigh() - q.getLow()) / q.getLow() * 100.0;

        // Log the key stats to peacock.txt for tracking.
        String msg = String.format("%d\t%.2f\t%.2f\t%.2f\t%.2f",
                dayNum, q.getPrice(), spreadPercent, q.getHigh(), q.getLow());
        Tools.log("peacock.txt", q.getDT(), msg);

        // ------------------------
        // Case P0: Late-day short on day -1
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,10);
        if ((dayNum == -1) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
            h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
            reasonCode = "P0";
            return true;
        }

        // --Zach's cases--

        //  CASE P1 - an early bail out case
        //  This case sells when the percent difference between the avgCost and the current price is within a small threshold
        //  AND if the price is on a declining trend the past 3 days.
        //  What we are checking for here is that the price has remained about the same and if not becoming worse.
        //  We sell before risking too far negative. This will not fire if we are already losing by a lot, as we can hope the price
        //  will come back up on the later days like -2 or -1.
        //  It's similar to SL however there are additional conditions required so that it is still possible to HOLD if there is
        //  a chance the price will improve
        //  Implementing this case brought the worst month from -640.50 to 179.50, however it did make 2022-12-09
        //  to 2023-01-19 significantly worse which I will take a look at next.
        //  Author: Zachary Sousa
        //  Version: 2
        final double SELL_WITHIN_PERCENT = 0.0113;   //  the period 2022-08-10 to 2022-09-15 was quite bad, so what I did
                                                    //  was find the worst possible buy and sell point (approximately as I
                                                    //  was just looking at the chart) and calculate the percent difference.
                                                    //  That's what this number is, granted I rounded up to the next ten-thousandth
                                                    //  because "it felt right". It may be used as both an upper and lower limit.
        double zPercent = (h.getAvgCost(symbol) - q.getPrice()) / h.getAvgCost(symbol); //  the percent gained or lost
        if(zPercent <= SELL_WITHIN_PERCENT && zPercent > 0 && !plum.isTrendingUp()) {
            h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
            reasonCode = "P1";
            if(DEBUG_MODE) {
                plum.logStoredGainLossHistory();
                System.out.println(IOColors.BLUE + "Plum Trending says: " + IOColors.RESET + plum.isTrendingUp());
            }
            return true;
        }

        //  CASE P3
        //  This one is very similar to the last, although it is triggered when after an upward trend rather than a downward trend.
        //  In order for this one to fire, we need to be within the same range of barely profiting, however it also needs to have been
        //  trending up the past couple days AND detecting that a new average gains is now lower than the previous average gains over the 
        //  past 3 days (plateau) AND that it is day -7 or -6. Essentially what we are checking for here is that on the dayNums 99 it
        //  sank, started to come back, but didn't really eclipse the cost at which we bought at. So we sell it, in fear it may crash
        //  back down again.
        //  Author: Zachary Sousa
        //  Version: 4
        //  Refer to SELL_WITHIN_PERCENT and zPercent declarations above for more information.
        if(plum.isTrendingUp()) {
            if(DEBUG_MODE) {
                System.out.println();
                System.out.print("PASSED 1 | ");
            }
            if(zPercent <= SELL_WITHIN_PERCENT && zPercent > 0) {
                if(DEBUG_MODE) System.out.print("PASSED 2 | ");
                if(plum.getTrendAverage() >= plum.calculateAverageWithNewFirst(q)) {
                    if(DEBUG_MODE) System.out.print("PASSED 3 | ");
                    if((dayNum == -7 || dayNum == -6 || dayNum == -5)) {
                        if(DEBUG_MODE) System.out.print("PASSED 4");
                        h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
                        reasonCode = "P3";
                        return true;
                    }
                }
            }
        }

        // --End of Zach's cases--

  /*      
        
        // ------------------------
        // Case PP: Sharp price pop on early days
        if (dayNum == -6 || dayNum == -5 || dayNum == -4) {
            double threshold = 5.4;  // 5.4% upward move
            if (percent >= threshold) {
                h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
                reasonCode = "PP";
                return true;
            }
        }


        // ------------------------
        // Case PL: Small negative percent range
        if (percent >= -3.45 && percent <= -3.00) {
            h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
            reasonCode = "PL";
            return true;
        }


        // ------------------------
        // Case PT: Near the top on later days
        if (dayNum == -1 || dayNum == -2 || dayNum == -3) {
            if (spreadPercent >= 1.0) {
                if (Math.abs(q.getHigh() - q.getPrice()) < 0.07) {
                    reasonCode = "PT";
                    h.openHolding(symbol, numShares, q.getPrice(), LocalDate.parse(theDate));
                    return true;
                }
            }
        }        
        
      
 */       
        
        
        
        
        
        
        
        // No short-sell signal.
        return false;
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
