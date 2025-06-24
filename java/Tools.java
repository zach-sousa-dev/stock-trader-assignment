import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
import java.net.URLConnection;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.net.URL;
import java.io.InputStreamReader;
import java.net.NoRouteToHostException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Financial Tools
 *
 * @author Dave Slemon
 * @version v1
 */
public class Tools
{
    public static int getDayNum( String theDate ) {
/*      File: marketdates.txt
        2023-08-28    99    OBSERVE
        2023-08-29    -7    SELL
        2023-08-30    -6    SELL
        2023-08-31    -5    SELL
        2023-09-01    -4    SELL
        2023-09-05    -3    SELL
        2023-09-06    -2    SELL
        2023-09-07    -1    SELL
        2023-09-08    0    BUY
*/
         // Specify the path to your text file, e.g. path/to/your/text/file.txt
        String filePath = Tools.getConfig("dataDirectory") + "/marketdates.txt";
        int dayNum = 99;
        
        // Create a File object to represent the text file
        File filename = new File(filePath);

        try {
            // Create a Scanner to read from the file
            Scanner scanner = new Scanner(filename);

            // Read and print each line from the file
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] items = line.split("\t");
                
                if (theDate.equals(items[0]) )
                    return Integer.parseInt(items[1]);

            }

            // Close the scanner to release resources
            scanner.close();

        } catch (FileNotFoundException e) {
            // Handle the case where the file is not found
            System.err.println("getDayNum: File not found: " + filePath);
            e.printStackTrace();
        }
        return 99;
    }
    
    
    
    
    
    
    
    
    public static String getConfig(String targetVar) {
        String target = targetVar.toUpperCase().trim();
        String varname = "";
        String filePath = "../data/config.txt";
        // Create a File object to represent the text file
        File filename = new File(filePath);

        try {
            // Create a Scanner to read from the file
            Scanner scanner = new Scanner(filename);

            // Read and print each line from the file
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("#")) continue;
                if (line.length() <= 0) continue;
                
                String[] items = line.split("=");
                
                varname = items[0].toUpperCase().trim();
                
                if (target.equals(varname)) {
                    return items[1].replaceAll("\"", "").trim();
                }
                

            }

            // Close the scanner to release resources
            scanner.close();

        } catch (FileNotFoundException e) {
            // Handle the case where the file is not found
            System.err.println("configFile not found: " + filePath);
            e.printStackTrace();
        }  
        return "";
    }
    
    
    
    
    
    
    
    //all logs appear in the log folder
    //note: the message should be a tab delimited string
    public static void log(String filename, String dateTime, String message) {
        String theFileName = Tools.getConfig("logsDirectory") + "\\" + filename;
        try {
            FileWriter out = new FileWriter(theFileName,true);
            out.write(dateTime + "\t" + message + "\n");
            out.close();
        }
        catch (IOException e) {
            System.out.println("File " + theFileName + " can not be written to.  Is their a log folder?");
            e.printStackTrace();
        }
    }
    
    
    
    
    public static long ConvertTimeToLong(String t) {
        if (t == null || !t.matches("\\d{2}:\\d{2}:\\d{2}")) {
            System.out.println("Error parsing string: " + t);
            throw new IllegalArgumentException("Input must be in the format HH:mm:ss");
        }

        String[] parts = t.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return hours * 3600L + minutes * 60L + seconds;
    }
    
    
    
    
    //given s_time = "16:00:00"
    //returns 15:45:00"
    public static String subtractMinutesFromTime(String s_time, int number_minutes_to_subtract) {
        // Define the format of the input and output time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Parse the string into a LocalTime object
        LocalTime time = LocalTime.parse(s_time, formatter);

        // Subtract the minutes
        LocalTime newTime = time.minusMinutes(number_minutes_to_subtract);

        // Return the result as a formatted string
        return newTime.format(formatter);
    }
    
    
    //This routine looks for a pattern where candle1 is greater than candle2 and candle3
    public static boolean HLx3(double h1, double h2, double h3, double l1, double l2, double l3) {
        
        if (h1 > h2 && h1 > h3 && h2 > h3) {
            if (l1 > l2 && l1 > l3 && l2 > l3)
                return true;
        }
        return false;
    }
  
    /**
     * @author Eli Wood
     */
    public static String ConvertTimeToString(long t){
        String time = "";
        long hours = Math.floorDiv(t, 3600);
        long minutes = Math.floorDiv(t - (hours * 3600), 60);
        long seconds = t - ((hours * 3600) + (minutes * 60));

        time = String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);

        return time;
    }
  
}
