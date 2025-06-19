import sqlite3
import random
from datetime import datetime, timedelta

DB_PATH = r"../nearmecommunity.db"

AREAS = ["Patancheruvu", "Kokapet", "Hayathnagar", "Nagaram"]
USERS_PER_AREA = 5000
ANSWER_TEXTS = [
    "Thank you for the info!",
    "I agree with this answer.",
    "Here is what I know...",
    "This worked for me.",
    "I had a similar experience.",
    "Try checking the local shops.",
    "You can find more details online.",
    "Ask the community center.",
    "I recommend this place.",
    "Hope this helps!",
    "Contact the local authorities.",
    "I have a different suggestion.",
    "This is a great question!",
    "Let me know if you need more info.",
    "I found this useful too."
]

def random_answer():
    return random.choice(ANSWER_TEXTS)

def random_time_after(start_time_str):
    # start_time_str: 'YYYY-MM-DD HH:MM:SS'
    start_time = datetime.strptime(start_time_str, '%Y-%m-%d %H:%M:%S')
    # Add between 1 minute and 30 days
    delta = timedelta(minutes=random.randint(1, 43200))
    answer_time = start_time + delta
    # Don't allow future times
    now = datetime.now()
    if answer_time > now:
        answer_time = now - timedelta(minutes=random.randint(1, 60))
    return answer_time.strftime('%Y-%m-%d %H:%M:%S')

def main():
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    answers = []
    total_answers = 0
    for area in AREAS:
        # Get 5k random users from this area
        cur.execute("SELECT user_id FROM user_locations WHERE location_name = ?", (area,))
        users = [row[0] for row in cur.fetchall()]
        if len(users) > USERS_PER_AREA:
            users = random.sample(users, USERS_PER_AREA)
        # Get all questions from this area
        cur.execute("SELECT id, created_at FROM qna_questions WHERE latitude IN (SELECT latitude FROM user_locations WHERE location_name = ?) AND longitude IN (SELECT longitude FROM user_locations WHERE location_name = ?)", (area, area))
        # Instead, better: get all questions where the user is from this area
        cur.execute("SELECT id, created_at FROM qna_questions WHERE user_id IN (SELECT user_id FROM user_locations WHERE location_name = ?)", (area,))
        questions = cur.fetchall()
        if not questions:
            continue
        for user_id in users:
            num_answers = random.randint(1, 3)
            # Pick 1-3 random questions from this area
            q_choices = random.sample(questions, min(num_answers, len(questions)))
            for q_id, q_created_at in q_choices:
                answer_text = random_answer()
                answer_time = random_time_after(q_created_at)
                answers.append((q_id, user_id, answer_text, answer_time))
                total_answers += 1
    # Batch insert
    BATCH_SIZE = 1000
    for i in range(0, len(answers), BATCH_SIZE):
        batch = answers[i:i+BATCH_SIZE]
        cur.executemany(
            """
            INSERT INTO qna_answers (question_id, user_id, description, created_at)
            VALUES (?, ?, ?, ?)
            """,
            batch
        )
    conn.commit()
    conn.close()
    print(f"Inserted {total_answers} fake answers into qna_answers table.")

if __name__ == "__main__":
    main() 