import sqlite3
import random
from datetime import datetime, timedelta

# Path to your SQLite DB
DB_PATH = 'nearmecommunity.db'

# Example notification messages
MESSAGES = [
    "Your business profile was viewed!",
    "You received a new review!",
    "A user asked a question about your business.",
    "Your business is trending this week!",
    "You have a new follower.",
    "A user liked your business.",
    "Your business was recommended to others.",
    "You received a new booking request."
]

def random_message():
    return random.choice(MESSAGES)

def random_date_after(start_dt):
    """Generate a random datetime after start_dt and before now."""
    now = datetime.now()
    if start_dt >= now:
        return now.strftime('%Y-%m-%d %H:%M:%S')
    delta = now - start_dt
    random_seconds = random.randint(1, int(delta.total_seconds()))
    result = start_dt + timedelta(seconds=random_seconds)
    return result.strftime('%Y-%m-%d %H:%M:%S')

# Connect to DB
conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all businesses and their owners
cur.execute("SELECT business_id, user_id, created_at FROM business")
businesses = cur.fetchall()

notif_count = 0
for business_id, user_id, b_created_at in businesses:
    n = random.randint(1, 3)  # 1 to 3 notifications per business
    # Parse business created_at as datetime object
    if isinstance(b_created_at, str):
        last_time = datetime.strptime(b_created_at, '%Y-%m-%d %H:%M:%S')
    else:
        last_time = b_created_at
    for _ in range(n):
        message = random_message()
        created_at = random_date_after(last_time)
        cur.execute(
            """
            INSERT INTO business_notification (business_id, user_id, message, created_at)
            VALUES (?, ?, ?, ?)
            """,
            (business_id, user_id, message, created_at)
        )
        notif_count += 1
        # Next notification must be after this one
        last_time = datetime.strptime(created_at, '%Y-%m-%d %H:%M:%S')

conn.commit()
conn.close()
print(f"Inserted {notif_count} fake business notifications.") 