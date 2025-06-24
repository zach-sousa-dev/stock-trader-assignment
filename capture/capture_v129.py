# capture program, capture_v129.py
# Dave Slemon
# June 2, 2025

import websocket
import time
import ssl
import json
import sys
from datetime import datetime
import mysql.connector

# --- Configuration ---

CONID_SYMBOL_MAP = {
    "73128548": "DIA",
    "756733": "SPY",
    "107976119": "PDI",
    "416921": "TNX",
    "13455763": "VIX",
    "320227571": "QQQ",
    "479624278": "BTC",
    "15016062": "USD.CAD"
}

FIELDS = ["31", "83", "85", "84", "86", "88", "89", "293"]  # removed mark price (37)

START_TIME = "09:29:00"
END_TIME = "16:01:00"

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '',
    'database': 'quotes'
}

TABLE_NAME = f"q{datetime.now().strftime('%Y%m%d')}"
CAPTURE_LOG_FILE = "capture.log"
ERROR_LOG_FILE = "error.log"

conn = mysql.connector.connect(**DB_CONFIG)
cursor = conn.cursor()

cursor.execute(f"""
    CREATE TABLE IF NOT EXISTS {TABLE_NAME} (
        myDT datetime NOT NULL,
        mySymbol varchar(10) NOT NULL,
        myType varchar(5) NOT NULL,
        myPrice decimal(10,4) NOT NULL,
        myComment varchar(20) NOT NULL,
        myVolume int(11) NOT NULL,
        myBid decimal(10,4) NOT NULL,
        myAsk decimal(10,4) NOT NULL,
        myBidSize int(11) NOT NULL,
        myAskSize int(11) NOT NULL,
        myHigh decimal(10,4) NOT NULL,
        myLow decimal(10,4) NOT NULL,
        myClose decimal(10,4) NOT NULL,
        myOpen decimal(10,4) NOT NULL,
        PRIMARY KEY (myDT, mySymbol)
    ) ENGINE=InnoDB;
""")

symbol_tracker = {}

# Initialize tracker from existing table data if table exists
try:
    cursor.execute(f"SELECT mySymbol, myHigh, myLow, myOpen FROM {TABLE_NAME} ORDER BY myDT DESC")
    rows = cursor.fetchall()
    seen = set()
    for row in rows:
        symbol, high, low, open_price = row
        if symbol not in seen:
            symbol_tracker[symbol] = {
                "high": float(high),
                "low": float(low),
                "open": float(open_price),
                "close": 0.0,
                "open_set": True,
                "last_price": float(open_price),
                "last_bid": 0.0,
                "last_ask": 0.0,
                "last_bid_size": 0,
                "last_ask_size": 0
            }
            seen.add(symbol)
except Exception as e:
    print(f"No initialization from DB: {e}")

def write_to_capture_log(timestamp, raw_message):
    with open(CAPTURE_LOG_FILE, "a") as f:
        f.write(f"{timestamp}\t{raw_message}\n")

def write_to_error_log(message):
    with open(ERROR_LOG_FILE, "a") as f:
        f.write(message + "\n")

def convert_timestamp(epoch_ms):
    try:
        ms = int(epoch_ms)
        if ms < 946684800000:
            raise ValueError("Bad timestamp")
        ts = datetime.fromtimestamp(ms / 1000)
        return ts.strftime("%Y-%m-%d %H:%M:%S")
    except:
        now = datetime.now()
        return now.strftime("%Y-%m-%d %H:%M:%S")

def extract_numeric(data, key, default):
    value = data.get(key)
    if value is None:
        return default
    try:
        val = float(str(value).replace(",", "").replace("M", "000000"))
        return val
    except:
        return default

def extract_int(data, key):
    value = data.get(key)
    if value is None:
        return 0
    try:
        clean = str(value).replace(",", "").replace("M", "000000")
        if "." in clean:
            clean = str(int(float(clean)))
        return int(clean)
    except:
        return 0

def update_tracker(symbol, price, bid, ask, bid_size, ask_size):
    if symbol not in symbol_tracker:
        symbol_tracker[symbol] = {
        "high": price,
        "low": price,
        "open": price,
        "close": 0.0,
        "open_set": False,
        "last_price": price,
        "last_bid": bid,
        "last_ask": ask,
        "last_bid_size": bid_size,
        "last_ask_size": ask_size
    }
    tracker = symbol_tracker[symbol]
    tracker["last_price"] = price
    if bid > 0:
        tracker["last_bid"] = bid
    if ask > 0:
        tracker["last_ask"] = ask
    if bid_size > 0:
        tracker["last_bid_size"] = bid_size
    if ask_size > 0:
        tracker["last_ask_size"] = ask_size
        tracker["last_bid"] = bid
    if ask > 0:
        tracker["last_ask"] = ask
    if not tracker["open_set"]:
        tracker["open"] = price
        tracker["open_set"] = True
    if price > tracker["high"]:
        tracker["high"] = price
    if price < tracker["low"]:
        tracker["low"] = price

def log_quote(symbol, fields, dt_str, raw_message):
    bid = extract_numeric(fields, "84", 0.0)
    ask = extract_numeric(fields, "86", 0.0)
    raw_last = extract_numeric(fields, "31", None)
    midpoint = (bid + ask) / 2 if bid > 0 and ask > 0 else None

    tracker = symbol_tracker.get(symbol, {})
    last_bid = tracker.get("last_bid", 0.0)
    last_ask = tracker.get("last_ask", 0.0)

    if raw_last is not None:
        last_price = raw_last
    elif midpoint is not None:
        last_price = midpoint
    else:
        last_price = tracker.get("last_price", 0.0)

    final_bid = bid if bid > 0 else last_bid
    final_ask = ask if ask > 0 else last_ask

    volume = extract_int(fields, "89")
    bid_size = extract_int(fields, "85")
    ask_size = extract_int(fields, "88")

    last_bid_size = tracker.get("last_bid_size", 0)
    last_ask_size = tracker.get("last_ask_size", 0)

    final_bid_size = bid_size if bid_size > 0 else last_bid_size
    final_ask_size = ask_size if ask_size > 0 else last_ask_size

    update_tracker(symbol, last_price, final_bid, final_ask, bid_size, ask_size)
    tracker = symbol_tracker[symbol]

    dt_obj = datetime.strptime(dt_str, "%Y-%m-%d %H:%M:%S")
    current_time = dt_obj.strftime("%H:%M:%S")

    if current_time >= END_TIME or current_time < START_TIME:
        print("Outside configured trading hours. Stopping stream.")
        sys.exit(0)

    if current_time >= END_TIME:
        tracker["close"] = last_price

    if symbol == "PDI":
        print(f"{dt_str}\tPDI\tFinalPrice: {last_price:.2f}\tLast: {raw_last}\tMid: {midpoint}")

    unix_timestamp = str(int(dt_obj.timestamp()))

    sql = f"""
        INSERT INTO {TABLE_NAME} (myDT, mySymbol, myType, myPrice, myComment, myVolume, myBid, myAsk,
        myBidSize, myAskSize, myHigh, myLow, myClose, myOpen)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE 
        myPrice=VALUES(myPrice), myComment=VALUES(myComment), myVolume=VALUES(myVolume),
        myBid=VALUES(myBid), myAsk=VALUES(myAsk), myBidSize=VALUES(myBidSize), myAskSize=VALUES(myAskSize),
        myHigh=VALUES(myHigh), myLow=VALUES(myLow), myClose=VALUES(myClose), myOpen=VALUES(myOpen);
    """
    vals = (dt_str, symbol, "STK", last_price, unix_timestamp, volume, final_bid, final_ask, final_bid_size, final_ask_size,
            tracker["high"], tracker["low"], tracker["close"], tracker["open"])

    try:
        cursor.execute(sql, vals)
        conn.commit()
    except Exception as e:
        error_msg = f"Error inserting into database: {e}"
        print(error_msg)
        write_to_error_log(error_msg)

def on_message(ws, message):
    try:
        if isinstance(message, bytes):
            message = message.decode("utf-8")
        data = json.loads(message)

        dt_str = convert_timestamp(data.get("83", time.time() * 1000))
        write_to_capture_log(dt_str, message)

        if isinstance(data, dict) and "conid" in data:
            conid = str(data["conid"])
            symbol = CONID_SYMBOL_MAP.get(conid, f"UNKNOWN-{conid}")
            log_quote(symbol, data, dt_str, message)
    except Exception as e:
        error_msg = f"Error processing message: {e}"
        print(error_msg)
        write_to_error_log(error_msg)

def on_error(ws, error):
    print("WebSocket Error:", error)
    write_to_error_log(f"WebSocket Error: {error}")

def on_close(ws, close_status_code, close_msg):
    print("## CLOSED ##")

def on_open(ws):
    print("Opened WebSocket connection.")
    time.sleep(2)
    for conid in CONID_SYMBOL_MAP:
        fields_json = json.dumps({"fields": FIELDS})
        request = f'smd+{conid}+{fields_json}'
        ws.send(request)

def run_stream():
    ws = websocket.WebSocketApp(
        url="wss://localhost:5000/v1/api/ws",
        on_open=on_open,
        on_message=on_message,
        on_error=on_error,
        on_close=on_close
    )
    ws.run_forever(sslopt={"cert_reqs": ssl.CERT_NONE})

if __name__ == "__main__":
    print(f"[INFO] Writing quotes to MySQL table: {TABLE_NAME}")
    # Wait until START_TIME before beginning the stream
    while True:
        now_time = datetime.now().strftime("%H:%M:%S")
        if now_time >= START_TIME:
            break
        print(f"Waiting for START_TIME ({START_TIME})... Current: {now_time}")
        time.sleep(10)

    run_stream()
    cursor.close()
    conn.close()
    sys.exit(0)
