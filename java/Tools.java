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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.io.PrintWriter;
import java.util.Date;
import java.net.HttpURLConnection;


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
    //      actualDT is the real time of day
    //      dateTime, is based on the quote 
    //
    // returns the actualDT
    //
    public static String log(String filename, String dateTime, String message) {
        String theFileName = Tools.getConfig("logsDirectory") + "\\" + filename;
        
        DateTimeFormatter s_today_dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String actualDT = now.format(s_today_dt);
        
        
        try {
            FileWriter out = new FileWriter(theFileName,true);
            out.write(actualDT+ "\t" + dateTime + "\t" + message + "\n");
            out.close();
            return actualDT;
        }
        catch (IOException e) {
            System.out.println("File " + theFileName + " can not be written to.  Is their a log folder?");
            
            e.printStackTrace();
            return actualDT;
        }
       
    }
    
    
    
    /**
     * Logs a message to the LOG_FILE.
     */
    public static void log(String message) {
        try (PrintWriter out = new PrintWriter(new FileWriter("errors.txt", true))) {
            out.println(new Date() + " - " + message);
        } catch (IOException e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
    
    
    
    public static long ConvertTimeToLong(String t) {
        if (t == null || !t.matches("\\d{2}:\\d{2}:\\d{2}")) {
            
            throw new IllegalArgumentException("ConvertTimeToLong Error 16: Input time must be in the format HH:mm:ss  Received: ["+t+"]");
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
  
    //  ZS UPDATE

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

    // END ZS UPDATE
    
    
    
    
    public static String getValidQuoteTime(Vector<String> resp) {
    if (resp != null && resp.size() > 0) {
        String dateTime = resp.get(0).trim();  // e.g., "2022-08-22 09:31:09"
        int spaceIndex = dateTime.indexOf(' ');
        if (spaceIndex != -1 && spaceIndex + 1 < dateTime.length()) {
            return dateTime.substring(spaceIndex + 1);  // returns "09:31:09"
        }
    }
    return null;  // if resp is null, empty, or malformed
    }
    
    
    
    
    /**
     * Adds a specified number of minutes to a given time string in "HH:mm:ss" format.
     *
     * @param prevTime the original time, e.g. "09:25:00"
     * @param minutesToAdd number of minutes to add
     * @return the new time string in "HH:mm:ss" format
     */
    public static String addMinutesToTime(String prevTime, int minutesToAdd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime time = LocalTime.parse(prevTime, formatter);
        LocalTime newTime = time.plusMinutes(minutesToAdd);
        return newTime.format(formatter);
    }
    
    
    
    
    
    //There are two vector posts, this one is for URLs
    public static Vector<String> VectorURLPost(String machine, String message) {
        Vector<String> data = new Vector<>(0);
        try {
            URL EregURL = new URL(machine);
            URLConnection URLConn = EregURL.openConnection();
            URLConn.setConnectTimeout(1500);
            URLConn.setDoInput(true);
            URLConn.setDoOutput(true);

            try (OutputStreamWriter out = new OutputStreamWriter(URLConn.getOutputStream())) {
                out.write(message);
                out.flush();
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(URLConn.getInputStream()))) {
                String temp_buff;
                while ((temp_buff = in.readLine()) != null) {
                    if (temp_buff.length() > 0)
                        data.add(temp_buff);
                }
            }

            return data;
        } catch (NoRouteToHostException e) {
            System.out.println("VectorPost Error: 101 No Route to Host");
            return null;
        } catch (Exception e) {
            System.out.println("VectorPost Error: 100 Can't reach MMEngine API");
            return null;
        }
    }    
    
    
    
    
    
    //There are two vector posts, this one is for URIs, ie. REST APIs
    public static Vector<String> VectorURIPost(String uri) {
        Vector<String> data = new Vector<>(0);

       

        try {
            URL url = new URL(uri);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1500);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            
            

            // Read JSON response
            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }

            // Extract the quote object manually
            String json = response.toString();
            int start = json.indexOf("\"quote\"");
            if (start == -1) return data;

            int braceStart = json.indexOf('{', start);
            int braceEnd = json.lastIndexOf('}');
            if (braceStart == -1 || braceEnd == -1 || braceEnd <= braceStart) return data;

            String quoteBody = json.substring(braceStart + 1, braceEnd);

            String[] fields = quoteBody.split(",");
            for (String field : fields) {
                String[] pair = field.split(":", 2);
                if (pair.length == 2) {
                    String value = pair[1].trim();

                    // Manually remove all quotes
                    StringBuilder clean = new StringBuilder();
                    for (int i = 0; i < value.length(); i++) {
                        char ch = value.charAt(i);
                        if (ch != '"') {
                            clean.append(ch);
                        }
                    }

                    // Remove any trailing brace and trim again
                    value = clean.toString().replace("}", "").trim();

                    data.add(value);
                }
            }

            return data;

        } catch (NoRouteToHostException e) {
            System.out.println("VectorPost Error: 101 No Route to Host");
            return null;
        } catch (Exception e) {
            System.out.println("VectorPost Error: 100 Can't reach MMEngine API");
            e.printStackTrace();  // Show error details
            return null;
        }
    }  
  
  
  

    
    
  
}
