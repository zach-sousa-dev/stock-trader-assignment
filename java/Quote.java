
/**
 * Quote Class
 *
 * @author Dave
 * @version v1
 */
public class Quote
{
    private String myDT=null;           // 2023-04-28.13:46.40
    private String mySymbol=null;       // PDI
    private String myType=null;         // STK
    private double myPrice=0.00;        // 17.99
    private String mySource=null;       // L7-1007
    private long   myVolume=0;          // 5060
    private double myBid=0.00;          // 17.98
    private double myAsk=0.00;          // 17.99
    private int    myBidSize=0;         // 53
    private int    myAskSize=0;         // 95
    private double myHigh=0.00;         // 18.12
    private double myLow=0.00;          // 17.98
    //myPrice 17.99
    private double myOpen=0.00;         // 18.07
    //myClose is myPrice at 16:00:00


    public Quote( String quoteStr ) {
        if (quoteStr.length() < 50) return;

        //      0                1     2        3       4      5        6       7     8        9       10      11      12         13
        //2023-08-28.09:30:24    PDI   STK    18.28    L7-1007    436    18.27    18.3    26    16    18.28    18.27    18.28    18.27

        String[] str = quoteStr.split("\t");
        if (str.length != 14) return;
        //System.out.println("quotelen=" + quoteStr.length());
        //System.out.println("len=" + str.length);

       
        this.myDT = str[0];
        this.mySymbol = str[1];
        this.myType = str[2];
        this.myPrice = Double.parseDouble(str[3]);
        this.mySource = str[4];
        this.myVolume = Long.parseLong(str[5]);
        this.myBid = Double.parseDouble(str[6]);
        this.myBidSize = Integer.parseInt(str[8]);
        this.myAsk = Double.parseDouble(str[7]);
        this.myAskSize = Integer.parseInt(str[9]);
        this.myHigh = Double.parseDouble(str[10]);
        this.myLow = Double.parseDouble(str[11]);
        this.myOpen =  Double.parseDouble(str[13]);
        
    }

    public String getDT() {
        return myDT;
    }
    
    
    
    public String getTime() {
        // Normalize by replacing '.' with ' '
        
        //String normalized = myDT.replace('.', ' ');
        if (myDT == null) return myDT;
        String[] parts = myDT.split(" ");

        if (parts.length < 2) {
            return "";  
        }

    return parts[1];
    }
    
    
    
    public String getDate() {
        if (myDT == null || myDT.isEmpty()) return "";

        // Replace '.' with space to standardize the delimiter
        String normalized = myDT.replace('.', ' ');
        String[] parts = normalized.split(" ");

        if (parts.length < 1) {
            return "";
        }

        return parts[0];
    }
    



    public String getSymbol() {
        return mySymbol;
    }

    public String getType() {
        return myType;
    }

    public double getPrice() {
        return myPrice;
    }

    public String getSource() {
        return mySource;
    }

    public long getVolume() {
        return myVolume;
    }

    public double getBid() {
        return myBid;
    }

    public double getAsk() {
        return myAsk;
    }

    public int getBidSize() {
        return myBidSize;
    }

    public int getAskSize() {
        return myAskSize;
    }

    public double getHigh() {
        return myHigh;
    }

    public double getLow() {
        return myLow;
    }

    public double getOpen() {
        return myOpen;
    }
    
    public String toString() {
        String str = myDT;
        str += "\t";
        str += mySymbol;
        str += "\t";
        str += String.format("%.2f",myPrice);
        return str;
        
    }
}
