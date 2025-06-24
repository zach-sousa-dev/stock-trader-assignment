
// --- AllHoldings.java ---

import java.time.LocalDate;
import java.util.ArrayList;

public class AllHoldings {
    private ArrayList<SingleHolding> holdings = new ArrayList<>();
    private double recentProfit = 0.0;
    private ArrayList<SingleHolding> syntheticClosures = new ArrayList<>();

    public void openHolding(String symbol, int numShares, double avgCost, LocalDate dateOpened) {
        if (numShares != 0)
            holdings.add(new SingleHolding(symbol, numShares, avgCost, dateOpened));
    }

    public boolean closeHolding(String symbol, int sharesToClose, double closingPrice, LocalDate dateClosed) {
        int sharesRemaining = Math.abs(sharesToClose);
        int closeSign = Integer.signum(sharesToClose);
        double sessionProfit = 0.0;
        boolean closedAny = false;
        ArrayList<SingleHolding> closedList = new ArrayList<>();

        for (SingleHolding pos : holdings) {
            if (sharesRemaining <= 0)
                break;
            if (pos.getSymbol().equals(symbol) && pos.isOpen() && Integer.signum(pos.getNumShares()) == closeSign) {
                int posSharesAbs = Math.abs(pos.getNumShares());
                if (posSharesAbs <= sharesRemaining) {
                    pos.close(closingPrice, dateClosed);
                    closedList.add(pos);
                    sharesRemaining -= posSharesAbs;
                } else {
                    pos.reduceShares(sharesRemaining, closingPrice, dateClosed);
                    SingleHolding clone = new SingleHolding(pos.getSymbol(), sharesRemaining * closeSign,
                            pos.getAvgCost(), pos.getDateOpened());
                    clone.close(closingPrice, dateClosed);
                    clone.markSynthetic();
                    syntheticClosures.add(clone);
                    closedList.add(clone);
                    sharesRemaining = 0;
                }
                closedAny = true;
            }
        }

        if (symbol.equals("PDI") && sharesToClose < 0) {
            double dividendPerShare = 0.2205;
            double totalAdjustment = dividendPerShare * Math.abs(sharesToClose);
            double remainingAdjustment = totalAdjustment;

            for (SingleHolding pos : closedList) {
                if (remainingAdjustment <= 0)
                    break;
                double maxApply = remainingAdjustment;
                pos.applyDividendAdjustment(maxApply);
                remainingAdjustment -= maxApply;
            }
        }

        for (SingleHolding pos : closedList) {
            sessionProfit += pos.getProfit();
        }

        recentProfit = sessionProfit;
        return closedAny;
    }

    public void printAllHoldings() {
        for (SingleHolding p : holdings) {
            System.out.println(p);
        }
        for (SingleHolding s : syntheticClosures) {
            System.out.println(s);
        }
    }

    public void results() {
        ArrayList<Double> profitList = new ArrayList<>();
        for (SingleHolding h : holdings) {
            if (!h.isOpen())
                profitList.add(h.getProfit());
        }
        for (SingleHolding s : syntheticClosures) {
            profitList.add(s.getProfit());
        }
        if (!profitList.isEmpty()) {
            System.out.printf("%.2f\t%.2f\n", 0.00, profitList.get(0));
        }
        for (int i = 1; i < profitList.size(); i += 2) {
            String first = String.format("%.2f", profitList.get(i));
            String second = (i + 1 < profitList.size()) ? String.format("%.2f", profitList.get(i + 1)) : "";
            System.out.println(first + "\t" + second);
        }
    }

    public double totalProfit(String symbol) {
        return holdings.stream().filter(h -> h.getSymbol().equals(symbol)).mapToDouble(SingleHolding::getProfit).sum()
                + syntheticClosures.stream().filter(h -> h.getSymbol().equals(symbol))
                        .mapToDouble(SingleHolding::getProfit).sum();
    }

    public double totalPortfolioProfit() {
        return holdings.stream().filter(h -> !h.isOpen()).mapToDouble(SingleHolding::getProfit).sum()
                + syntheticClosures.stream().mapToDouble(SingleHolding::getProfit).sum();
    }

    public double getProfit() {
        return recentProfit;
    }

    public boolean hasHolding(String symbol) {
        return holdings.stream().anyMatch(h -> h.getSymbol().equals(symbol) && h.isOpen());
    }

    public int getNumShares(String symbol) {
        return holdings.stream().filter(h -> h.isOpen() && h.getSymbol().equals(symbol))
                .mapToInt(SingleHolding::getNumShares).sum();
    }

    public double getAvgCost(String symbol) {
        int totalShares = 0;
        double totalCost = 0.0;
        for (SingleHolding h : holdings) {
            if (h.isOpen() && h.getSymbol().equals(symbol)) {
                totalShares += Math.abs(h.getNumShares());
                totalCost += Math.abs(h.getNumShares()) * h.getAvgCost();
            }
        }
        return (totalShares == 0) ? 0.0 : totalCost / totalShares;
    }

    public ArrayList<SingleHolding> getAllHoldings() {
        ArrayList<SingleHolding> all = new ArrayList<>(holdings);
        all.addAll(syntheticClosures);
        return all;
    }
}
