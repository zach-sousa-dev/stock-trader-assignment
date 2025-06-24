import java.time.LocalDate;

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

    private final boolean DEBUG_MODE = false;

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
     * Case SP (Sharp Pop):
     *   - Sell if a large profit threshold is reached on days -6, -5, or -4.
     * 
     * Case ST (Near Top):
     *   - Sell if, on days -3, -2, or -1, the price spread is wide AND 
     *     the current price is very close to the day's high.
     * 
     * @return true if a sell action is taken; false otherwise.
     */
    public boolean is_selling() {
        double avgCost = h.getAvgCost(symbol);
        double profit = (q.getPrice() - avgCost) * numShares;
        double percent = ((q.getPrice() - avgCost) / avgCost) * 100;
        double spreadPercent = (q.getHigh() - q.getLow()) / q.getLow() * 100.0;

        // Log key stats to scarlet.txt for tracking.
        String msg = String.format("%d\t%.2f\t%.2f\t%.1f\t%.2f\t%.2f\t%.2f",
                dayNum, q.getPrice(), profit, percent, spreadPercent, q.getHigh(), q.getLow());
        Tools.log("scarlet.txt", q.getDT(), msg);

        // ------------------------
        // Case S0: Late-day sell on day -1
        String fifteen_minutes_earlier = Tools.subtractMinutesFromTime(marketCloseTime,15);
        if ((dayNum == -1) &&
            (Tools.ConvertTimeToLong(theTime) >= Tools.ConvertTimeToLong(fifteen_minutes_earlier)) &&
            (Tools.ConvertTimeToLong(theTime) < Tools.ConvertTimeToLong(marketCloseTime))) {
            h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
            reasonCode = "S0";
            return true;
        }


      
        // ------------------------
        // Case SL: Stop loss recovery zone (if in small loss range)
        if (dayNum == -1 || dayNum == -2 || dayNum == -3) {
            if (percent >= sl_lowerlimit && percent <= sl_upperlimit) {
                h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
                reasonCode = "SL";
                return true;
            }
        }

        // --Zach's cases--

        //  CASE S1 - an early bail out case
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
            h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
            reasonCode = "S1";
            if(DEBUG_MODE) {
                plum.logStoredGainLossHistory();
                System.out.println(IOColors.BLUE + "Plum Trending says: " + IOColors.RESET + plum.isTrendingUp());
            }
            return true;
        }

        //  CASE S2
        //  This one didn't do too well.
        //  Author: Zachary Sousa
        //  Version: 2
        // final double HIGH_SLOPE = 0.20;
        // if((dayNum == -7 || dayNum == -6) && plum.getTrendAverage() >= HIGH_SLOPE) {
        //     h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
        //     reasonCode = "S2";
        //     return true;
        // }

        //  CASE S3
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
                        h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
                        reasonCode = "S3";
                        return true;
                    }
                }
            }
        }

        // --End of Zach's cases--

        // if(plum.isTrendingUp() && zPercent <= SELL_WITHIN_PERCENT && zPercent > -SELL_WITHIN_PERCENT && plum.getTrendAverage() >= plum.calculateAverageWithNewFirst(q) && (dayNum == -7 || dayNum == -6)) {
        //     h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
        //     reasonCode = "S3";
        //     return true;
        // }
 /* 
        // ------------------------
        // Case SP: Sharp profit pop on early days
        if (dayNum == -6 || dayNum == -5 || dayNum == -4) {
            if (percent >= sp_threshold) {
                h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
                reasonCode = "SP";
                return true;
            }
        }

        // ------------------------
        // Case ST: Near the top on later days
        if (dayNum == -1 || dayNum == -2 || dayNum == -3) {
            if (spreadPercent >= st_spreadPercent) {
                if (Math.abs(q.getHigh() - q.getPrice()) < st_closeness) {
                    reasonCode = "ST";
                    h.closeHolding(symbol, getNumShares(), q.getPrice(), LocalDate.parse(theDate));
                    return true;
                }
            }
        }
*/


        // No sell signal.
        return false;
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
