import sqlite3
import random
from datetime import datetime, timedelta

# Path to your SQLite DB
DB_PATH = 'nearmecommunity.db'

# Example answers
ANSWERS = [
    "Thank you for your question! Yes, we are open from 9am to 7pm.",
    "We do offer home delivery within a 5km radius.",
    "Yes, there is parking available nearby.",
    "We accept cash, credit cards, and UPI payments.",
    "Currently, we have a 10% discount on select items.",
    "You can book an appointment through our website or by calling us.",
    "Yes, pets are welcome at our business!",
    "Our facility is wheelchair accessible.",
    "You can contact us at the number listed on our profile.",
    "Our most popular service is express delivery."
]

def random_answer():
    return random.choice(ANSWERS)

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
businesses = cur.fetchall()

# Bulk fetch all existing (question_id, user_id) pairs in business_answers
cur.execute("SELECT question_id, user_id FROM business_answers")
existing_answers = set(cur.fetchall())

answers_to_insert = []
answer_count = 0
for business_id, owner_id in businesses:
    # Get all questions for this business, including their created_at
    cur.execute("SELECT question_id, created_at FROM business_questions WHERE business_id = ?", (business_id,))
    questions = cur.fetchall()
    if not questions:
        continue
    n = min(len(questions), random.randint(1, 2))
    selected_questions = random.sample(questions, n)
    for question_id, q_created_at in selected_questions:
        # Check if an answer already exists for this question by this owner
        if (question_id, owner_id) in existing_answers:
            continue
        answer_text = random_answer()
        # Parse question created_at as datetime object
        if isinstance(q_created_at, str):
            q_created_at_dt = datetime.strptime(q_created_at, '%Y-%m-%d %H:%M:%S')
        else:
            q_created_at_dt = q_created_at
        created_at = random_date_after(q_created_at_dt)
        answers_to_insert.append((question_id, owner_id, answer_text, created_at))
        answer_count += 1

# Batch insert all new answers
if answers_to_insert:
    cur.executemany(
        """
        INSERT INTO business_answers (question_id, user_id, answer_text, created_at)
        VALUES (?, ?, ?, ?)
        """,
        answers_to_insert
    )

conn.commit()
conn.close()
print(f"Inserted {answer_count} fake business answers.") 