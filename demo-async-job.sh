#!/bin/bash

# WorkHub - Async Job Creation and Completion Demo Script
# Make sure the application and Kafka are running before executing this script.
# (e.g. `docker-compose up -d kafka` and `./mvnw spring-boot:run`)

BASE_URL="http://localhost:8080"
echo "======================================================"
echo "WorkHub Async Job Demo"
echo "======================================================"
echo ""

# 1. Read Tenant ID from the seeder file
if [ ! -f .tenant_ids.txt ]; then
    echo "Error: .tenant_ids.txt not found. Ensure the application has started and seeded the data."
    exit 1
fi
TENANT_ID=$(cat .tenant_ids.txt)
echo "1. Using Seeded Tenant ID: $TENANT_ID"

# 2. Register a TENANT_ADMIN user
echo "2. Registering an Admin User..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
        "email": "admin_demo@workhub.local",
        "password": "password123",
        "tenantId": "'$TENANT_ID'",
        "tenantRole": "TENANT_ADMIN"
      }')
      
if [ -z "$TOKEN" ] || [[ "$TOKEN" == *"error"* ]]; then
  echo "Registration failed or user already exists. Attempting login..."
  # Login (Note: AuthController uses GET for login with request body)
  TOKEN=$(curl -s -X GET "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
          "email": "admin_demo@workhub.local",
          "password": "password123"
        }')
fi

echo "   JWT Token Received: ${TOKEN:0:20}..."

# 3. Create a Project
echo ""
echo "3. Creating a Project..."
PROJECT_RESPONSE=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Async Workflow Demo Project",
        "createdBy": "admin_demo@workhub.local"
      }')
      
PROJECT_ID=$(echo $PROJECT_RESPONSE | grep -o '"id":"[^"]*' | grep -o '[^"]*$')
echo "   Project created with ID: $PROJECT_ID"

# 4. Trigger Async Report Generation
echo ""
echo "4. Triggering Async Report Generation Job..."
JOB_RESPONSE=$(curl -s -X POST "$BASE_URL/projects/$PROJECT_ID/generate-report" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

JOB_ID=$(echo $JOB_RESPONSE | grep -o '"jobId":"[^"]*' | grep -o '[^"]*$')
STATUS=$(echo $JOB_RESPONSE | grep -o '"status":"[^"]*' | grep -o '[^"]*$')

echo "   API Response returned immediately."
echo "   Job ID: $JOB_ID"
echo "   Initial Status: $STATUS"

# 5. Poll for Job Completion
echo ""
echo "5. Polling for Job Completion (Kafka Consumer is processing...)"

MAX_RETRIES=10
ATTEMPT=1
FINAL_STATUS="PENDING"

while [ $ATTEMPT -le $MAX_RETRIES ]; do
  sleep 1
  REPORTS_RESPONSE=$(curl -s -X GET "$BASE_URL/projects/$PROJECT_ID/reports" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json")
    
  # Extract the status of the specific job
  CURRENT_STATUS=$(echo $REPORTS_RESPONSE | grep -o "\"id\":\"$JOB_ID\",\"status\":\"[^\"]*" | grep -o '[^"]*$')
  
  echo "   [Attempt $ATTEMPT] Job Status: $CURRENT_STATUS"
  
  if [ "$CURRENT_STATUS" == "COMPLETED" ]; then
    FINAL_STATUS="COMPLETED"
    break
  fi
  
  ATTEMPT=$((ATTEMPT+1))
done

echo ""
if [ "$FINAL_STATUS" == "COMPLETED" ]; then
  echo "✅ SUCCESS: Async Job completed successfully via Kafka messaging!"
else
  echo "❌ TIMEOUT: Async Job did not complete within expected time. Check Kafka logs."
fi
echo "======================================================"
