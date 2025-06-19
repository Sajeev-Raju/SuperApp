import sqlite3
import random
import math
import string

# Path to your SQLite DB
DB_PATH = '../nearmecommunity.db'

# Area centers: (name, lat, lon, google maps url)
areas = [
    ("Patancheruvu", 17.5333, 78.2651, "https://www.google.com/maps?q=17.5333,78.2651"),
    ("Kokapet", 17.3849, 78.3317, "https://www.google.com/maps?q=17.3849,78.3317"),
    ("Hayathnagar", 17.3295, 78.6092, "https://www.google.com/maps?q=17.3295,78.6092"),
    ("Nagaram", 17.4863, 78.6235, "https://www.google.com/maps?q=17.4863,78.6235"),
]

USERS_PER_AREA = 75000
TOTAL_USERS = USERS_PER_AREA * len(areas)
RADIUS_KM = 2

# Generate all possible user IDs in the format AAA000-AAA999, AAB000-... etc.
def generate_user_ids(n):
    ids = []
    for a in string.ascii_uppercase:
        for b in string.ascii_uppercase:
            for c in string.ascii_uppercase:
                for d in range(1000):
                    ids.append(f"{a}{b}{c}{d:03d}")
                    if len(ids) == n:
                        return ids
    return ids[:n]

def random_point_within_radius(lat, lon, radius_km):
    # Random point within radius_km of (lat, lon)
    radius_in_degrees = radius_km / 111  # Approximate
    u = random.random()
    v = random.random()
    w = radius_in_degrees * math.sqrt(u)
    t = 2 * math.pi * v
    dx = w * math.cos(t)
    dy = w * math.sin(t)
    return lat + dy, lon + dx / math.cos(math.radians(lat))

user_ids = generate_user_ids(TOTAL_USERS)
random.shuffle(user_ids)

assert len(set(user_ids)) == TOTAL_USERS, "User IDs are not unique!"

conn = sqlite3.connect(DB_PATH)
cur = conn.cursor()

batch = []
user_idx = 0
for area_idx, (area_name, lat, lon, gmaps_url) in enumerate(areas):
    for i in range(USERS_PER_AREA):
        user_id = user_ids[user_idx]
        user_idx += 1
        user_lat, user_lon = random_point_within_radius(lat, lon, RADIUS_KM)
        batch.append((user_id, user_lat, user_lon, area_name))
        if len(batch) == 1000:
            cur.executemany(
                """
                INSERT OR REPLACE INTO user_locations (user_id, latitude, longitude, location_name)
                VALUES (?, ?, ?, ?)
                """,
                batch
            )
            batch = []
# Insert any remaining
if batch:
    cur.executemany(
        """
        INSERT OR REPLACE INTO user_locations (user_id, latitude, longitude, location_name)
        VALUES (?, ?, ?, ?)
        """,
        batch
    )

conn.commit()
conn.close()
print(f"Inserted {TOTAL_USERS} fake users into user_locations table.") 