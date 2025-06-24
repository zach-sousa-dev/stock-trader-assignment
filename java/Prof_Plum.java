import java.io.*;
import java.util.*;

public class Prof_Plum {
    private static final String FILE_PATH = "..\\data\\plum.txt";

    //  MODES
    private final boolean DEBUG_MODE = true;

    // Map<Symbol, Map<Variable, Map<DayNum, Value>>>
    private Map<String, Map<String, Map<Integer, Double>>> data;

    public Prof_Plum() {
        data = new HashMap<>();
    }

    public void setValue(String symbol, String variable, int dayNum, double value) {
        data.computeIfAbsent(symbol, k -> new HashMap<>())
                .computeIfAbsent(variable, k -> new HashMap<>())
                .put(dayNum, value);

        // Auto-save to file after every update
        saveToFile();
    }

    public Double getValue(String symbol, String variable, int dayNum) {
        Map<String, Map<Integer, Double>> symbolData = data.get(symbol);
        if (symbolData != null) {
            Map<Integer, Double> varData = symbolData.get(variable);
            if (varData != null) {
                Double value = varData.get(dayNum);
                if (value != null) {
                    return value;
                }
            }
        }

        // Return sentinel defaults for high and low
        if (variable.equals("high"))
            return -999.99;
        if (variable.equals("low"))
            return 999.99;

        return null;
    }

    public void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String symbol : data.keySet()) {
                for (String variable : data.get(symbol).keySet()) {
                    for (Map.Entry<Integer, Double> entry : data.get(symbol).get(variable).entrySet()) {
                        writer.write(String.format("%s\t%s\t%d\t%.4f%n",
                                symbol, variable, entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public void getFromFile() {
        data.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\t");
                if (parts.length == 4) {
                    String symbol = parts[0];
                    String variable = parts[1];
                    int dayNum = Integer.parseInt(parts[2]);
                    double value = Double.parseDouble(parts[3]);
                    setValue(symbol, variable, dayNum, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading from file: " + e.getMessage());
        }
    }

    public void updateStatistics(String symbol, int dayNum, double currentPrice) {

        Double currentHigh = getValue(symbol, "high", dayNum);
        Double currentLow = getValue(symbol, "low", dayNum);

        // If no high or low exists yet, initialize both to currentPrice
        if (currentHigh == null || currentLow == null) {
            setValue(symbol, "high", dayNum, currentPrice);
            setValue(symbol, "low", dayNum, currentPrice);
            return;
        }

        if (currentPrice > currentHigh) {
            setValue(symbol, "high", dayNum, currentPrice);
        }

        if (currentPrice < currentLow) {
            setValue(symbol, "low", dayNum, currentPrice);
        }
    }

    public void clearFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            // Truncate file by writing nothing
        } catch (IOException e) {
            System.err.println("Error clearing plum file: " + e.getMessage());
        }
    }


    //  Zach's trend stuff
    private Deque<Quote> quotes = new ArrayDeque<>();
    private final int MAX_TREND_SIZE = 4;

    /**
     * Adds a quote to Plum's Quote Deque
     * @param q     the LAST quote of the day (very important it should be the last as we need the overall day's high and low)
     * @author Zachary Sousa
     */
    public void updateTrend(Quote q) {
        quotes.addFirst(q);
        if(quotes.size() > MAX_TREND_SIZE) {
            quotes.removeLast();
        }
        if(DEBUG_MODE) System.out.println("Trend Average: " + (isTrendingUp() ? (IOColors.BOLD_GREEN + "+") : IOColors.BOLD_RED) + getTrendAverage() + IOColors.RESET);
    }

    /**
     * Calculates the average gains between the past 4 days.
     * @return the average gains
     * @author Zachary Sousa
     */
    public double getTrendAverage() {
        double sum = 0;
        if(quotes.size() <= 1) {
            return 0;
        }

        Quote[] quotesArr = quotes.toArray(new Quote[0]);
        for(int i = 0; true; i++) {

            double today = (quotesArr[i].getHigh() + quotesArr[i].getLow())/2;
            double yesterday = (quotesArr[i+1].getHigh() + quotesArr[i+1].getLow())/2;

            sum += (today - yesterday);
            //System.out.println("Calculation "+today+"-"+yesterday);
            //System.out.println("New sum: " + sum);

            if(i+1 == quotes.size()-1) {
                break;
            }
        }
        // for(Quote curQ : quotes) {
        //     sum += ((curQ.getHigh() + curQ.getLow()) / 2);
        // }
        //System.out.println("sum: " + sum);
        //System.out.println("divisor: " + (quotes.size() - 1));
        return (sum / (double)(quotes.size() - 1));
    }

    /**
     * Similar to getTrendAverage, but it temporarily removes the oldest day's data and 
     * pushes the newQuote into the Deque. This way you can calculate the trend based
     * on the current intraday price. This won't affect the original Deque.
     * Note: This probably should be refactored but it isn't important right now.
     * @param newQuote  the new quote
     * @return ~~same as trend average calculation but using newQuote as the latest~~ UPDATE: while, the calculation is largely the same, it does NOT use the high low average for the newQuote
     * @author Zachary Sousa
     */
    public double calculateAverageWithNewFirst(Quote newQuote) {
        double sum = 0;
        Deque<Quote> tempQuotes = new ArrayDeque<>(quotes);
        if(tempQuotes.size() <= 1) {
            return 0;
        }

        tempQuotes.removeLast();
        tempQuotes.addFirst(newQuote);

        Quote[] quotesArr = tempQuotes.toArray(new Quote[0]);
        for(int i = 0; true; i++) {

            double today = (quotesArr[i].getPrice());
            if(i > 0) {
                today = (quotesArr[i].getHigh() + quotesArr[i].getLow())/2;
            }
            double yesterday = (quotesArr[i+1].getHigh() + quotesArr[i+1].getLow())/2;

            sum += (today - yesterday);
            //System.out.println("Calculation "+today+"-"+yesterday);
            //System.out.println("New sum: " + sum);

            if(i+1 == tempQuotes.size()-1) {
                break;
            }
        }
        // for(Quote curQ : quotes) {
        //     sum += ((curQ.getHigh() + curQ.getLow()) / 2);
        // }
        //System.out.println("sum: " + sum);
        //System.out.println("divisor: " + (quotes.size() - 1));
        return (sum / (double)(tempQuotes.size() - 1));
    }

    /**
     * Check if we have been going up.
     * @return  true if increasing gains, false if losing
     * @author Zachary Sousa
     */
    public boolean isTrendingUp() {
        if(getTrendAverage() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Simply calculates the average price from yesterday's high and low.
     * @return  average price from yesterday's high and low
     * @author Zachary Sousa
     */
    public double getYesterdaysAvg() {
        if(quotes.size() < 1) {
            return 0;
        }
        return (quotes.getFirst().getHigh() + quotes.getFirst().getLow()) / 2;
    }
    
    /**
     * Logs the gains and losses used in trend calculation for debugging purposes.
     * The first output is the most recent gain/loss and the last is the oldest.
     * Also, this should be refactored too.
     * @author Zachary Sousa
     */
    public void logStoredGainLossHistory() {
        if(quotes.size() <= 1) {
            System.out.println(IOColors.BOLD_RED + "Plum message: No data to log." + IOColors.RESET);
            return;
        }

        Quote[] quotesArr = quotes.toArray(new Quote[0]);
        for(int i = 0; true; i++) {

            double today = (quotesArr[i].getHigh() + quotesArr[i].getLow())/2;
            double yesterday = (quotesArr[i+1].getHigh() + quotesArr[i+1].getLow())/2;

            System.out.println("GAIN/LOSS " + i + ": " + ((today - yesterday) > 0 ? (IOColors.BOLD_GREEN + "+") : IOColors.BOLD_RED) + (today - yesterday) + IOColors.RESET);
            //System.out.println("Calculation "+today+"-"+yesterday);
            //System.out.println("New sum: " + sum);

            if(i+1 == quotes.size()-1) {
                break;
            }
        }
        System.out.println("DEQUE DAY AVERAGES: ");
        for(Quote i : quotes) {
            System.out.println(IOColors.BLUE + (i.getHigh() + i.getLow())/2 + IOColors.RESET);
        }
    }
}
