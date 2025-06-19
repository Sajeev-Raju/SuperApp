import sqlite3
import random
from datetime import datetime, timedelta
import math
from collections import defaultdict

# Path to your SQLite DB
DB_PATH = 'nearmecommunity.db'

# Radius in kilometers
RADIUS_KM = 10

# Target number of questions
TARGET_QUESTIONS = 50_000

# Grid cell size
GRID_SIZE_KM = 2

# Example questions
QUESTIONS = [
    "What are your business hours?",
    "Do you offer home delivery?",
    "Is parking available nearby?",
    "What payment methods do you accept?",
    "Are there any discounts currently?",
    "Can I book an appointment online?",
    "Is your business pet-friendly?",
    "Do you have wheelchair access?",
    "How can I contact you?",
    "What are your most popular services?"
]

def random_question():
    return random.choice(QUESTIONS)

def random_date():
    # Random date within the last 180 days
    return (datetime.now() - timedelta(days=random.randint(0, 180))).strftime('%Y-%m-%d %H:%M:%S')

def haversine(lat1, lon1, lat2, lon2):
    # Calculate the great circle distance between two points on the earth (specified in decimal degrees)
    R = 6371  # Radius of earth in kilometers
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi/2)**2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def grid_cell(lat, lon, grid_size_km=GRID_SIZE_KM):
    # Approximate: 1 degree lat ~ 111km, 1 degree lon ~ 111km * cos(lat)
    lat_cell = int(lat / (grid_size_km / 111))
    lon_cell = int(lon / (grid_size_km / 111))
    return (lat_cell, lon_cell)

def neighboring_cells(cell):
    lat_cell, lon_cell = cell
    for dlat in [-1, 0, 1]:
        for dlon in [-1, 0, 1]:
            yield (lat_cell + dlat, lon_cell + dlon)

def random_date_after(start_dt):
    """Generate a random datetime after start_dt and before now."""
    now = datetime.now()
    if start_dt >= now:
        return now.strftime('%Y-%m-%d %H:%M:%S')
    delta = now - start_dt
    random_seconds = random.randint(0, int(delta.total_seconds()))
    result = start_dt + timedelta(seconds=random_seconds)
    return result.strftime('%Y-%m-%d %H:%M:%S')

# Connect to DB
conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get all user locations
cur.execute("SELECT user_id, latitude, longitude FROM user_locations")
user_locations = cur.fetchall()
# Uncomment the next line to test with a smaller subset:
# user_locations = user_locations[:1000]
user_dict = {user_id: (lat, lon) for user_id, lat, lon in user_locations}

# Get all businesses
cur.execute("SELECT business_id, user_id, latitude, longitude, created_at FROM business")
businesses = cur.fetchall()
# Uncomment the next line to test with a smaller subset:
# businesses = businesses[:1000]

# Build grid index for businesses and store created_at
business_grid = defaultdict(list)
business_created_at = {}
for b_id, owner_id, b_lat, b_lon, b_created_at in businesses:
    cell = grid_cell(b_lat, b_lon)
    business_grid[cell].append((b_id, owner_id, b_lat, b_lon))
    # Parse created_at as datetime object
    if isinstance(b_created_at, str):
        b_created_at_dt = datetime.strptime(b_created_at, '%Y-%m-%d %H:%M:%S')
    else:
        b_created_at_dt = b_created_at
    business_created_at[b_id] = b_created_at_dt

questions = []
question_count = 0
users = list(user_dict.keys())
random.shuffle(users)

for user_id in users:
    if question_count >= TARGET_QUESTIONS:
        break
    lat, lon = user_dict[user_id]
    user_cell = grid_cell(lat, lon)
    # Gather businesses in neighboring cells
    candidate_businesses = []
    for cell in neighboring_cells(user_cell):
        candidate_businesses.extend(business_grid.get(cell, []))
    # Filter by radius and not owned by user
    eligible = [ (b_id, b_lat, b_lon) for b_id, owner_id, b_lat, b_lon in candidate_businesses
                if owner_id != user_id and haversine(lat, lon, b_lat, b_lon) <= RADIUS_KM ]
    if not eligible:
        continue
    num_questions = random.choice([1, 2])
    num_questions = min(num_questions, len(eligible))
    selected = random.sample(eligible, num_questions)
    for b_id, b_lat, b_lon in selected:
        b_created_at = business_created_at[b_id]
        questions.append((b_id, user_id, random_question(), random_date_after(b_created_at)))
        question_count += 1
        if question_count >= TARGET_QUESTIONS:
            break

# Batch insert
BATCH_SIZE = 1000
for i in range(0, len(questions), BATCH_SIZE):
    batch = questions[i:i+BATCH_SIZE]
    cur.executemany(
        """
        INSERT INTO business_questions (business_id, user_id, question_text, created_at)
        VALUES (?, ?, ?, ?)
        """,
        batch
    )

conn.commit()
conn.close()
print(f"Inserted {question_count} fake business questions.") 