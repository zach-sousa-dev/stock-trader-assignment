// --- SingleHolding.java ---

import java.time.LocalDate;

public class SingleHolding {
    private String symbol;
    private boolean isOpen;
    private int numShares;
    private double avgCost;
    private LocalDate dateOpened;
    private LocalDate dateClosed;
    private double closePrice;

    private double profit;
    private double partialProfit;
    private double dividendAdjustment = 0.0;
    private boolean isSynthetic = false; // true for synthetic partial clones

    public SingleHolding(String symbol, int numShares, double avgCost, LocalDate dateOpened) {
        this.symbol = symbol;
        this.numShares = numShares;
        this.avgCost = avgCost;
        this.dateOpened = dateOpened;
        this.isOpen = true;
        this.profit = 0.0;
        this.partialProfit = 0.0;
    }

    public void close(double closingPrice, LocalDate dateClosed) {
        if (!isOpen && !isSynthetic) throw new IllegalStateException("Position is already closed.");
        this.closePrice = closingPrice;
        this.dateClosed = dateClosed;
        this.profit += calcProfit(numShares, avgCost, closingPrice);
        this.isOpen = false;
        this.numShares = 0;
    }

    public void reduceShares(int sharesToReduce, double closingPrice, LocalDate date) {
        if (!isOpen) throw new IllegalStateException("Position is already closed.");
        int positionSign = Integer.signum(numShares);
        int absReduce = Math.min(Math.abs(sharesToReduce), Math.abs(numShares));
        double thisProfit = calcProfit(absReduce * positionSign, avgCost, closingPrice);
        this.partialProfit = thisProfit;
        this.profit += thisProfit;
        this.numShares -= absReduce * positionSign;
        if (this.numShares == 0) {
            this.isOpen = false;
            this.dateClosed = date;
            this.closePrice = closingPrice;
        }
    }

    private double calcProfit(int shares, double entryPrice, double exitPrice) {
        return shares > 0 ? (exitPrice - entryPrice) * shares : (entryPrice - exitPrice) * Math.abs(shares);
    }

    public void applyDividendAdjustment(double adjustment) {
        this.dividendAdjustment += adjustment;
    }

    public double getProfit() { return profit - dividendAdjustment; }
    public double getRawProfit() { return profit; }
    public String getSymbol() { return symbol; }
    public boolean isOpen() { return isOpen; }
    public int getNumShares() { return numShares; }
    public double getAvgCost() { return avgCost; }
    public LocalDate getDateOpened() { return dateOpened; }
    public LocalDate getDateClosed() { return dateClosed; }
    public double getPartialProfit() { return partialProfit - dividendAdjustment; }
    public boolean isSynthetic() { return isSynthetic; }
    public void markSynthetic() { this.isSynthetic = true; }

    @Override
    public String toString() {
        return String.format("Symbol: %s | Shares: %d | Avg Cost: %.2f | Profit: %.2f | Open: %b",
                symbol, numShares, avgCost, getProfit(), isOpen);
    }
}