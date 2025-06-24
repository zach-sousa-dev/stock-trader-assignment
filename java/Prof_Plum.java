import java.io.*;
import java.util.*;

public class Prof_Plum {
    private static final String FILE_PATH = "..\\data\\plum.txt";
    
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
    if (variable.equals("high")) return -999.99;
    if (variable.equals("low"))  return 999.99;

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
    Double currentLow  = getValue(symbol, "low", dayNum);

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
    
    
    
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
    
        for (String symbol : data.keySet()) {
            Map<String, Map<Integer, Double>> variableMap = data.get(symbol);
            for (String variable : variableMap.keySet()) {
                Map<Integer, Double> dayNumMap = variableMap.get(variable);
                for (Map.Entry<Integer, Double> entry : dayNumMap.entrySet()) {
                    int dayNum = entry.getKey();
                    double value = entry.getValue();
                    sb.append(String.format("%s[%d] = %.4f, ", variable, dayNum, value));
                }
            }
        }
    
        return sb.toString();
    }

    
    
    
    
}
