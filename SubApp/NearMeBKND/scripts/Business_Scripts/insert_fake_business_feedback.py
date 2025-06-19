import sqlite3
import random
from datetime import datetime, timedelta
import math

# Path to your SQLite DB
DB_PATH = 'nearmecommunity.db'

# Feedback radius in kilometers (now 5km for performance)
RADIUS_KM = 5

# Example feedbacks
FEEDBACKS = [
    "Great service and friendly staff!",
    "Had a wonderful experience here.",
    "The place was clean and well maintained.",
    "Highly recommend this business to others.",
    "Prices are reasonable and quality is good.",
    "Customer support was very helpful.",
    "Will definitely visit again!",
    "The location is very convenient.",
    "Loved the variety of products offered.",
    "Quick and efficient service."
]

AREAS = ["Patancheruvu", "Kokapet", "Hayathnagar", "Nagaram"]
USERS_PER_AREA = 5000
MAX_FEEDBACKS = USERS_PER_AREA * len(AREAS)

def random_feedback():
    return random.choice(FEEDBACKS)

def random_date():
    # Random date within the last 180 days
    return (datetime.now() - timedelta(days=random.randint(0, 180))).strftime('%Y-%m-%d %H:%M:%S')

def random_date_after(start_dt):
    """Generate a random datetime after start_dt and before now."""
    now = datetime.now()
    if start_dt >= now:
        return now.strftime('%Y-%m-%d %H:%M:%S')
    delta = now - start_dt
    random_seconds = random.randint(1, int(delta.total_seconds()))
    result = start_dt + timedelta(seconds=random_seconds)
    return result.strftime('%Y-%m-%d %H:%M:%S')

def haversine(lat1, lon1, lat2, lon2):
    R = 6371  # Earth radius in km
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi/2)**2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c

def users_within_radius(b_lat, b_lon, user_dict, radius_km):
    # 1 degree latitude ~ 111km
    lat_margin = radius_km / 111.0
    # 1 degree longitude ~ 111km * cos(latitude)
    lon_margin = radius_km / (111.0 * math.cos(math.radians(b_lat)))
    candidates = [
        (user_id, u_lat, u_lon)
        for user_id, (u_lat, u_lon) in user_dict.items()
        if (b_lat - lat_margin) <= u_lat <= (b_lat + lat_margin)
        and (b_lon - lon_margin) <= u_lon <= (b_lon + lon_margin)
    ]
    # Now apply haversine only to candidates
    return [user_id for user_id, u_lat, u_lon in candidates if haversine(b_lat, b_lon, u_lat, u_lon) <= radius_km]

# Connect to DB
conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

# Get 5,000 random users from each area
selected_users = []
for area in AREAS:
    cur.execute("SELECT user_id, latitude, longitude FROM user_locations WHERE location_name = ?", (area,))
    area_users = cur.fetchall()
    if len(area_users) > USERS_PER_AREA:
        area_users = random.sample(area_users, USERS_PER_AREA)
    selected_users.extend(area_users)

# Get all businesses with their locations and created_at
cur.execute("SELECT business_id, latitude, longitude, created_at FROM business")
businesses = cur.fetchall()

# Bulk fetch all existing (business_id, user_id) pairs in business_feedback
cur.execute("SELECT business_id, user_id FROM business_feedback")
existing_feedbacks = set(cur.fetchall())

feedbacks_to_insert = []
feedback_count = 0

for area in AREAS:
    # Get first 5,000 users from this area
    cur.execute("SELECT user_id FROM user_locations WHERE location_name = ? ORDER BY id ASC LIMIT ?", (area, USERS_PER_AREA))
    area_users = [row[0] for row in cur.fetchall()]
    # Get all businesses in this area (address contains area name, case-insensitive)
    cur.execute("SELECT business_id, user_id, created_at FROM business WHERE LOWER(address) LIKE ?", (f"%{area.lower()}%",))
    area_businesses = cur.fetchall()
    if not area_businesses:
        continue
    for user_id in area_users:
        # Filter out businesses owned by this user
        eligible_businesses = [(b_id, b_created_at) for b_id, owner_id, b_created_at in area_businesses if owner_id != user_id]
        if not eligible_businesses:
            continue
        # Pick a random business from the area (not owned by the user)
        b_id, b_created_at = random.choice(eligible_businesses)
        # Check if feedback already exists
        if (b_id, user_id) in existing_feedbacks:
            continue
        feedback_text = random_feedback()
        if isinstance(b_created_at, str):
            try:
                b_created_at_dt = datetime.strptime(b_created_at, '%Y-%m-%d %H:%M:%S')
            except Exception:
                continue
        elif isinstance(b_created_at, datetime):
            b_created_at_dt = b_created_at
        else:
            continue
        created_at = random_date_after(b_created_at_dt)
        feedbacks_to_insert.append((b_id, user_id, feedback_text, created_at))
        feedback_count += 1
        if feedback_count >= MAX_FEEDBACKS:
            break
    if feedback_count >= MAX_FEEDBACKS:
        break

# Batch insert all new feedbacks
if feedbacks_to_insert:
    cur.executemany(
        """
        INSERT INTO business_feedback (business_id, user_id, feedback_text, created_at)
        VALUES (?, ?, ?, ?)
        """,
        feedbacks_to_insert
    )

conn.commit()
conn.close()
print(f"Inserted {feedback_count} fake business feedbacks.") 