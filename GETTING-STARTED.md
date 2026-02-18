# Getting Started - Unified Notification Platform

A detailed step-by-step guide to run and use the notification platform.

---

## Part 1: Prerequisites

### 1.1 Install Docker Desktop

1. Download Docker Desktop for Windows: https://www.docker.com/products/docker-desktop/
2. Run the installer and follow the prompts
3. Restart your computer if prompted
4. Open **Docker Desktop** from the Start menu
5. Wait until Docker shows **"Docker Desktop is running"** (green icon in system tray)

### 1.2 Verify Docker is Working

Open **PowerShell** or **Command Prompt** and run:

```powershell
docker --version
docker-compose --version
```

You should see version numbers. If you get "command not found", Docker may not be in your PATH—restart your terminal or computer.

---

## Part 2: Starting the Project

### 2.1 Open the Project Folder

1. Open **File Explorer**
2. Go to: `C:\Users\shrey\OneDrive\Desktop\Important\Projects\unified-notification-platform`

### 2.2 Start All Services

1. Open **PowerShell** (right-click Start → Windows PowerShell)
2. Run these commands one by one:

```powershell
# Navigate to the infra folder
cd "C:\Users\shrey\OneDrive\Desktop\Important\Projects\unified-notification-platform\infra"

# Start all services (Kafka, MongoDB, PostgreSQL, API, Worker)
docker compose up -d
```

3. **Wait 2–3 minutes** for everything to start. The first time may take longer (downloading images).

### 2.3 Check That Services Are Running

```powershell
docker-compose ps
```

You should see something like:

| Name               | State   |
|--------------------|---------|
| infra-kafka-1      | running |
| infra-mongodb-1    | running |
| infra-postgres-1   | running |
| infra-zookeeper-1  | running |
| infra-notification-api-1    | running |
| infra-notification-worker-1 | running |

If any show "Exited" or "Restarting", wait a bit and run `docker-compose ps` again.

---

## Part 3: Accessing the Web App

### 3.1 Open the App in Your Browser

1. Open **Google Chrome** (or any browser)
2. In the address bar, type: **http://localhost:8080**
3. Press **Enter**

You should see the **Notification Platform** app with:
- A dark-themed interface
- Tabs: **Dashboard**, **Send Notification**, **Check Status**
- A footer showing "Connected" (green) when the API is running

### 3.2 If You See "This site can't be reached"

- **Docker Desktop not running?** Open Docker Desktop and wait until it’s ready
- **Services still starting?** Wait 2–3 minutes and refresh the page
- **Port 8080 in use?** Another app may be using it; stop that app or change the port in `infra/docker-compose.yml`

---

## Part 4: Using the App

### 4.1 Dashboard Tab

**What it shows:**
- **Total Notifications** – Count of all notifications
- **Success Rate** – Percentage of successful sends
- **Avg Latency** – Average delivery time in milliseconds
- **Last Hour** – Notifications sent in the last hour

**How to use:**
- Click **Dashboard** in the top nav
- Click **Refresh** to update the numbers

### 4.2 Send Notification Tab

**Step-by-step:**

1. Click **Send Notification** in the top nav
2. Fill in the form:
   - **Client ID:** Leave as `client1` (pre-configured)
   - **Channel:** Choose **Email** or **SMS**
   - **Recipient:**
     - Email: `your-email@gmail.com`
     - SMS: `+1234567890` (with country code)
   - **Template:** Choose based on channel:
     - Email: `welcome-email`
     - SMS: `welcome-sms` or `otp-sms`
   - **Variables (JSON):**
     - For `welcome-email` / `welcome-sms`: `{"name":"John"}`
     - For `otp-sms`: `{"code":"1234"}`
   - **Idempotency Key:** Optional; leave blank for now
3. Click **Send Notification**
4. You should see: **"Notification queued! Request ID: xxxxx"**
5. **Copy the Request ID** – you’ll use it to check status

**Note:** For real email, you need SMTP credentials. For SMS, the project uses a mock simulator (no real SMS sent). See "Configuration" below.

### 4.3 Check Status Tab

**Step-by-step:**

1. Click **Check Status** in the top nav
2. Paste the **Request ID** from the send response (or type it)
3. Click **Check Status**
4. You’ll see:
   - **Status:** QUEUED → PROCESSING → SENT → DELIVERED or FAILED
   - Channel, recipient, created time
   - Delivery logs (attempts, provider, latency)

---

## Part 5: Configuration (Optional)

### 5.1 Sending Real Emails

To send real emails, set SMTP environment variables before starting:

1. Create a file `infra/.env` (in the infra folder) with:

```
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
```

2. For Gmail, use an **App Password** (not your normal password):
   - Go to Google Account → Security → 2-Step Verification → App passwords
   - Generate a password for "Mail"

3. Restart the worker:
   ```powershell
   cd infra
   docker compose up -d notification-worker
   ```

### 5.2 Changing the Port

If port 8080 is in use, edit `notification-api/src/main/resources/application.yml`:

```yaml
server:
  port: 9090   # Change from 8080
```

Then rebuild: `docker compose up -d --build notification-api`

Access the app at: **http://localhost:9090**

---

## Part 6: Stopping the Project

When you’re done:

```powershell
cd "C:\Users\shrey\OneDrive\Desktop\Important\Projects\unified-notification-platform\infra"
docker-compose down
```

This stops all containers. Your data (in MongoDB and PostgreSQL) is kept in Docker volumes.

To remove data as well:

```powershell
docker-compose down -v
```

---

## Part 7: Troubleshooting

### "ERR_CONNECTION_REFUSED" when opening localhost:8080

| Cause | Solution |
|-------|----------|
| Docker Desktop not running | Start Docker Desktop, wait until it’s ready |
| Services not started | Run `docker compose up -d` from the `infra` folder |
| API container crashed | Run `docker-compose logs notification-api` to see errors |
| Port 8080 in use | Stop the other app or change the port (see 5.2) |

### API container keeps restarting

```powershell
docker-compose logs notification-api
```

Common causes:
- **PostgreSQL not ready** – Wait 1–2 minutes and try again
- **Kafka not ready** – Wait for Kafka to start
- **MongoDB connection failed** – Ensure MongoDB container is running

### "Template not found" or "Client not found"

The database is seeded with:
- **Client:** `client1`
- **Templates:** `welcome-email`, `welcome-sms`, `otp-sms`

Use `client1` as Client ID. If you changed the database, re-run `infra/init.sql`.

### Metrics show "—" (dashes)

- No notifications have been sent yet
- Or the API can’t reach MongoDB – check `docker-compose logs notification-api`

---

## Quick Reference

| Action | Command / URL |
|--------|---------------|
| Start project | `cd infra` then `docker compose up -d` |
| Open app | http://localhost:8080 |
| Check containers | `docker-compose ps` |
| View API logs | `docker-compose logs -f notification-api` |
| Stop project | `docker-compose down` |

---

## Summary Flow

1. **Start Docker Desktop**
2. **Run** `docker compose up -d` **from the infra folder**
3. **Wait 2–3 minutes**
4. **Open** http://localhost:8080 **in your browser**
5. **Use** Dashboard, Send Notification, and Check Status tabs
