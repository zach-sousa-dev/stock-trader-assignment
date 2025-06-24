import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * MarketDates manages the loading and storage of market dates from a file.
 */
public class MarketDates {

    
    
    public static class MarketDate {
        private String date;
        private int dayNum;
        private String action;
        private String start_time;  //on this date, start_time is when market opens and end_time is when market closes
        private String end_time;
        
        
        public MarketDate(String date, int dayNum, String action, String start_time, String end_time) {
            this.date = date;
            this.dayNum = dayNum;
            this.action = action;
            this.start_time = start_time;
            this.end_time = end_time;
            
        }

        public String getDate() {
            return date;
        }
        
      

        public void setDate(String date) {
            this.date = date;
        }

        public int getDayNum() {
            return dayNum;
        }

        public void setDayNum(int dayNum) {
            this.dayNum = dayNum;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
        
        public String getMarketOpenTime() { return start_time; }
        public String getMarketCloseTime() { return end_time; }

        @Override
        public String toString() {
            return String.format("%s\t%d\t%s", date, dayNum, action);
        }
    }

    
    
    
    private ArrayList<MarketDate> marketDates = new ArrayList<>();

    /**
     * Loads market dates from a file within a specified date range.
     */
    public void loadFromFile(String filePath, String startDate, String endDate) {
        try (Scanner scanner = new Scanner(new File(filePath))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\t");
                if (parts.length >= 5) {
                    String date = parts[0];
                    String start_time = parts[3];
                    String end_time = parts[4];
                    int dayNum = Integer.parseInt(parts[1]);
                    String action = parts[2];

                    if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0) {
                        marketDates.add(new MarketDate(date, dayNum, action, start_time, end_time));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found — " + filePath);
        } catch (Exception e) {
            System.out.println("Error reading market dates: " + e.getMessage());
        }
    }

    public ArrayList<MarketDate> getMarketDates() {
        return marketDates;
    }

    public void setMarketDates(ArrayList<MarketDate> marketDates) {
        this.marketDates = marketDates;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MarketDate md : marketDates) {
            sb.append(md.toString()).append("\n");
        }
        return sb.toString();
    }
    
    
    
      // In outer class MarketDates
        public String getMarketOpenTime(String theDate) {
            for (MarketDate md : marketDates) {
                if (md.getDate().equals(theDate)) {
                    return md.getMarketOpenTime();
                }
            }
            return "09:30:00"; // Default or fallback
        }
        
        public String getMarketCloseTime(String theDate) {
            for (MarketDate md : marketDates) {
                if (md.getDate().equals(theDate)) {
                    return md.getMarketCloseTime();
                }
            }
            return "16:00:00"; // Default or fallback
        }

}
