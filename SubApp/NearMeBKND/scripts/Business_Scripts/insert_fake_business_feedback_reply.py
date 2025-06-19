import sqlite3
import random
from datetime import datetime, timedelta

# Path to your SQLite DB
DB_PATH = 'nearmecommunity.db'

# Example replies
REPLIES = [
    "Thank you for your feedback!",
    "We appreciate your kind words.",
    "Glad you had a good experience!",
    "Thank you for visiting us.",
    "We hope to see you again soon!",
    "Your feedback means a lot to us.",
    "Thanks for recommending us!",
    "We are happy to serve you.",
    "Let us know if you need anything else.",
    "We value your support!"
]

def random_reply():
    return random.choice(REPLIES)

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
cur.execute("SELECT business_id, user_id FROM business")
businesses = {business_id: owner_id for business_id, owner_id in cur.fetchall()}

# Get all feedbacks for all businesses
cur.execute("SELECT feedback_id, business_id, created_at FROM business_feedback")
feedbacks = cur.fetchall()

reply_count = 0
for feedback_id, business_id, feedback_created_at in feedbacks:
    owner_id = businesses.get(business_id)
    if not owner_id:
        continue
    # Check if a reply already exists for this feedback
    cur.execute(
        "SELECT 1 FROM business_feedback_reply WHERE feedback_id = ?",
        (feedback_id,)
    )
    if cur.fetchone() is None:
        reply_text = random_reply()
        # Parse feedback_created_at as datetime
        if isinstance(feedback_created_at, str):
            try:
                feedback_created_at_dt = datetime.strptime(feedback_created_at, '%Y-%m-%d %H:%M:%S')
            except Exception:
                continue
        elif isinstance(feedback_created_at, datetime):
            feedback_created_at_dt = feedback_created_at
        else:
            continue
        created_at = random_date_after(feedback_created_at_dt)
        cur.execute(
            """
            INSERT INTO business_feedback_reply (feedback_id, user_id, reply_text, created_at)
            VALUES (?, ?, ?, ?)
            """,
            (feedback_id, owner_id, reply_text, created_at)
        )
        reply_count += 1

conn.commit()
conn.close()
print(f"Inserted {reply_count} fake business feedback replies.") 