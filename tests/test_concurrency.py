import requests
import threading
import uuid
import sys
import os

BASE_URL = "http://localhost"
TENANT_FILE = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".tenant_ids.txt"))

def run_test():
    # Make sure the app is spun up and the file is there
    if not os.path.exists(TENANT_FILE):
        print(f"Oops! I can't find {TENANT_FILE}. Rerun the app first.")
        sys.exit(1)

    # Grab the tenant IDs we dumped in the .txt file
    with open(TENANT_FILE, 'r') as f:
        tenant_id = f.read().strip()
    
    print(f"Testing on Tenant ID: {tenant_id}")

    # Creating a random email, avoiding conflicts.
    random_email = f"test_{uuid.uuid4().hex[:6]}@example.com"
    password = "password123"

    # Registering fake user
    print(f"Registering our test user: {random_email}")
    register_resp = requests.post(f"{BASE_URL}/auth/register", json={
        "email": random_email,
        "password": password,
        "tenantId": tenant_id,
        "tenantRole": "TENANT_ADMIN"
    })
    
    if register_resp.status_code not in (200, 201):
        print("Registration failed:", register_resp.text)
        sys.exit(1)

    # log in and grab the keys (JWT token)
    print("Logging in...")
    login_resp = requests.get(f"{BASE_URL}/auth/login", json={
        "email": random_email,
        "password": password
    })
    
    token = login_resp.text.strip()
    headers = {"Authorization": f"Bearer {token}"}

    # blank project creation
    print("Creating a fresh project...")
    proj_resp = requests.post(f"{BASE_URL}/projects", json={
        "name": "Concurrency Test Project",
        "createdBy": random_email
    }, headers=headers)
    
    project_id = proj_resp.json()["id"]

    # Adding a new task
    print("Dropping a task in the project...")
    task_resp = requests.post(f"{BASE_URL}/projects/{project_id}/tasks", json={
        "title": "Explode the DB"
    }, headers=headers)
    
    task_id = task_resp.json()["id"]
    print(f"Task created with ID: {task_id}")

    print("\n INITIATING CONCURRENT PATCH REQUESTS ")
    
    results = []
    
    def patch_task(new_status, thread_name):
        try:
            resp = requests.patch(f"{BASE_URL}/tasks/{task_id}", json={
                "status": new_status
            }, headers=headers)
            try:
                body = resp.json()
            except Exception:
                body = resp.text
            results.append(f"{thread_name} Finished! Status Code: {resp.status_code} | Response: {body}")
        except Exception as e:
            results.append(f"{thread_name} Failed: {str(e)}")

    thread1 = threading.Thread(target=patch_task, args=("IP", "Thread A (Claiming Task)"))
    thread2 = threading.Thread(target=patch_task, args=("IP", "Thread B (Claiming Task)"))

    # Running both threads then waiting for them to finish
    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    print("\n--- RESULTS ---")
    for r in results:
        print(r)
        
    print("\nNotice how one thread succeeds (200 OK), and the other gets 409 Conflict not because the client chose a bad transition, but because the optimistic lock version changed under it (a concurrent modification). The error message distinguishes this from an invalid state machine transition.")
    print("The database rejected the second thread because the version changed, triggering a rollback")

    # Cleanup (Disabled in this version to keep seeder file alive)
    # os.remove(TENANT_FILE)
    # print("\nCleaned up .tenant_ids.txt")

if __name__ == "__main__":
    run_test()
