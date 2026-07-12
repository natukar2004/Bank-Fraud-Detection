import gradio as gr
import pandas as pd
import hashlib
from datetime import datetime, timedelta

# ------------------------------------------------------------------
# "DATABASE" — module-level dicts shared across the Space
# (fine for a demo; resets when the Space restarts)
# ------------------------------------------------------------------
def hash_pw(pw):
    return hashlib.sha256(pw.encode()).hexdigest()


USERS = {
    "rahul@gmail.com": {
        "name": "Rahul",
        "password": hash_pw("password123"),
        "balance": 50000,
        "known_devices": {"Device-Home-Laptop"},
        "known_locations": {"Mumbai"},
        "recent_transfer_times": [],
    }
}
TRANSACTIONS = []   # list of dicts
FRAUD_ALERTS = []   # subset of TRANSACTIONS that were blocked
NEXT_ID = [1]

DEVICE_OPTIONS = ["Device-Home-Laptop", "Device-Office-PC", "Device-Unknown-Phone"]
LOCATION_OPTIONS = ["Mumbai", "Delhi", "Bengaluru", "Unknown City"]
RISK_THRESHOLD = 70
ADMIN_USER, ADMIN_PASS = "admin", "admin123"


# ------------------------------------------------------------------
# FRAUD DETECTION ENGINE (Step 5)
# ------------------------------------------------------------------
def calculate_risk(amount, device, location, user):
    score = 0
    reasons = []

    if amount > 50000:
        score += 40
        reasons.append("High Amount")

    if device not in user["known_devices"]:
        score += 20
        reasons.append("New Device")

    if location not in user["known_locations"]:
        score += 20
        reasons.append("New Location")

    now = datetime.now()
    recent = [t for t in user["recent_transfer_times"] if now - t < timedelta(minutes=1)]
    if len(recent) > 3:
        score += 20
        reasons.append("Too Many Transfers in 1 Minute")

    return score, reasons


# ------------------------------------------------------------------
# AUTH ACTIONS
# ------------------------------------------------------------------
def register(name, email, password):
    if not name or not email or not password:
        return "⚠ Please fill all fields."
    if email in USERS:
        return "⚠ An account with this email already exists."
    USERS[email] = {
        "name": name,
        "password": hash_pw(password),
        "balance": 50000,
        "known_devices": {"Device-Home-Laptop"},
        "known_locations": {"Mumbai"},
        "recent_transfer_times": [],
    }
    return f"✅ Account created for {name}. You can now log in."


def login(email, password):
    user = USERS.get(email)
    if user and user["password"] == hash_pw(password):
        welcome = f"## Welcome, {user['name']} 👋"
        balance = f"**Balance:** ₹{user['balance']:,}"
        return (
            "",                                   # login status
            email,                                 # user_email state
            False,                                 # is_admin state
            gr.update(visible=False),              # auth_group
            gr.update(visible=True),               # bank_group
            gr.update(visible=False),              # admin_group
            welcome,
            balance,
        )
    return (
        "❌ Invalid credentials.",
        None, False,
        gr.update(visible=True), gr.update(visible=False), gr.update(visible=False),
        "", "",
    )


def admin_login(username, password):
    if username == ADMIN_USER and password == ADMIN_PASS:
        return (
            "",
            None, True,
            gr.update(visible=False), gr.update(visible=False), gr.update(visible=True),
        )
    return (
        "❌ Invalid admin credentials.",
        None, False,
        gr.update(visible=True), gr.update(visible=False), gr.update(visible=False),
    )


def logout():
    return (
        None, False,
        gr.update(visible=True), gr.update(visible=False), gr.update(visible=False),
    )


# ------------------------------------------------------------------
# STEP 4/5/6: TRANSFER + FRAUD DECISION
# ------------------------------------------------------------------
def transfer(user_email, receiver, amount, device, location):
    if not user_email:
        return "⚠ Please log in first.", "", ""
    user = USERS[user_email]
    amount = int(amount)

    if amount > user["balance"]:
        return "❌ Insufficient balance.", f"**Balance:** ₹{user['balance']:,}", ""

    score, reasons = calculate_risk(amount, device, location, user)
    user["recent_transfer_times"].append(datetime.now())

    txn = {
        "id": NEXT_ID[0],
        "user_email": user_email,
        "user_name": user["name"],
        "receiver": receiver,
        "amount": amount,
        "device": device,
        "location": location,
        "risk_score": score,
        "reasons": reasons,
        "timestamp": datetime.now(),
        "status": None,
    }
    NEXT_ID[0] += 1

    if score < RISK_THRESHOLD:
        user["balance"] -= amount
        txn["status"] = "Success"
        TRANSACTIONS.append(txn)
        result = f"✅ **Transaction Successful** — Risk Score: {score}"
    else:
        txn["status"] = "Blocked - Pending Review"
        TRANSACTIONS.append(txn)
        FRAUD_ALERTS.append(txn)
        reason_lines = "\n".join(f"- {r}" for r in reasons)
        result = (
            f"### ⚠ Suspicious Transaction\n"
            f"**Amount:** ₹{amount:,}\n\n"
            f"**Risk Score:** {score}\n\n"
            f"**Reason:**\n{reason_lines}\n\n"
            f"_Transaction is under review by the bank's fraud team._"
        )

    balance_md = f"**Balance:** ₹{user['balance']:,}"
    return result, balance_md, ""


# ------------------------------------------------------------------
# HISTORY / ALERTS (customer view)
# ------------------------------------------------------------------
def get_history(user_email):
    if not user_email:
        return pd.DataFrame()
    rows = [
        {
            "ID": t["id"],
            "Receiver": t["receiver"],
            "Amount (₹)": t["amount"],
            "Risk Score": t["risk_score"],
            "Status": t["status"],
            "Time": t["timestamp"].strftime("%d %b %Y, %I:%M %p"),
        }
        for t in TRANSACTIONS if t["user_email"] == user_email
    ]
    return pd.DataFrame(rows[::-1])


def get_fraud_alerts(user_email):
    if not user_email:
        return "Please log in."
    my_alerts = [t for t in FRAUD_ALERTS if t["user_email"] == user_email]
    if not my_alerts:
        return "No fraud alerts on your account."
    blocks = []
    for t in reversed(my_alerts):
        reason_lines = "\n".join(f"- {r}" for r in t["reasons"])
        blocks.append(
            f"⚠ **₹{t['amount']:,}** to {t['receiver']} — status: **{t['status']}**\n\n"
            f"**Reason:**\n{reason_lines}"
        )
    return "\n\n---\n\n".join(blocks)


# ------------------------------------------------------------------
# STEP 8/9: ADMIN DASHBOARD
# ------------------------------------------------------------------
def get_pending_df():
    rows = [
        {
            "ID": t["id"],
            "Customer": t["user_name"],
            "Amount (₹)": t["amount"],
            "Risk Score": t["risk_score"],
            "Reasons": ", ".join(t["reasons"]),
        }
        for t in FRAUD_ALERTS if t["status"] == "Blocked - Pending Review"
    ]
    return pd.DataFrame(rows)


def get_resolved_df():
    rows = [
        {
            "ID": t["id"],
            "Customer": t["user_name"],
            "Amount (₹)": t["amount"],
            "Status": t["status"],
        }
        for t in FRAUD_ALERTS if t["status"] != "Blocked - Pending Review"
    ]
    return pd.DataFrame(rows[::-1])


def admin_decide(txn_id, decision):
    txn_id = int(txn_id) if txn_id not in (None, "") else None
    match = next((t for t in FRAUD_ALERTS if t["id"] == txn_id and t["status"] == "Blocked - Pending Review"), None)
    if not match:
        return "⚠ Enter a valid pending Transaction ID.", get_pending_df(), get_resolved_df()

    if decision == "approve":
        sender = USERS[match["user_email"]]
        sender["balance"] -= match["amount"]
        match["status"] = "Approved - Transferred"
        msg = f"✅ Transaction #{txn_id} approved and transferred."
    else:
        match["status"] = "Rejected - Money Retained"
        msg = f"❌ Transaction #{txn_id} rejected. Money stays with the customer."

    for t in TRANSACTIONS:
        if t["id"] == txn_id:
            t["status"] = match["status"]

    return msg, get_pending_df(), get_resolved_df()


# ------------------------------------------------------------------
# UI
# ------------------------------------------------------------------
with gr.Blocks(title="SecureBank | Fraud Detection Demo") as demo:
    gr.Markdown("# 🏦 SecureBank — Fraud Detection Demo")
    gr.Markdown(
        "Demo customer login → `rahul@gmail.com` / `password123`  \n"
        "Demo admin login → `admin` / `admin123`"
    )

    user_email_state = gr.State(None)
    is_admin_state = gr.State(False)

    # ---------------- AUTH GROUP ----------------
    with gr.Group(visible=True) as auth_group:
        with gr.Tab("Customer Login"):
            login_email = gr.Textbox(label="Email")
            login_password = gr.Textbox(label="Password", type="password")
            login_btn = gr.Button("Login", variant="primary")
            login_status = gr.Markdown()

        with gr.Tab("Register"):
            reg_name = gr.Textbox(label="Name")
            reg_email = gr.Textbox(label="Email")
            reg_password = gr.Textbox(label="Password", type="password")
            reg_btn = gr.Button("Register")
            reg_status = gr.Markdown()

        with gr.Tab("Admin Login"):
            admin_username = gr.Textbox(label="Admin Username")
            admin_password = gr.Textbox(label="Admin Password", type="password")
            admin_login_btn = gr.Button("Login as Admin")
            admin_login_status = gr.Markdown()

    # ---------------- BANK GROUP (customer) ----------------
    with gr.Group(visible=False) as bank_group:
        welcome_md = gr.Markdown()
        balance_md = gr.Markdown()
        logout_btn_bank = gr.Button("Logout")

        with gr.Tab("Transfer Money"):
            receiver = gr.Textbox(label="Receiver Account", value="1234567890")
            amount = gr.Number(label="Amount (₹)", value=10000)
            gr.Markdown("_Simulated context — in production this comes from the device/session automatically:_")
            device = gr.Dropdown(DEVICE_OPTIONS, label="Device used", value=DEVICE_OPTIONS[0])
            location = gr.Dropdown(LOCATION_OPTIONS, label="Location", value=LOCATION_OPTIONS[0])
            transfer_btn = gr.Button("Transfer", variant="primary")
            transfer_result = gr.Markdown()

        with gr.Tab("Transaction History"):
            history_refresh_btn = gr.Button("Refresh")
            history_df = gr.Dataframe()

        with gr.Tab("Fraud Alerts"):
            alerts_refresh_btn = gr.Button("Refresh")
            alerts_md = gr.Markdown()

    # ---------------- ADMIN GROUP ----------------
    with gr.Group(visible=False) as admin_group:
        gr.Markdown("## Admin Dashboard")
        logout_btn_admin = gr.Button("Logout")

        gr.Markdown("### Blocked Transactions (Pending Review)")
        admin_refresh_btn = gr.Button("Refresh")
        pending_df = gr.Dataframe()

        with gr.Row():
            decide_id = gr.Number(label="Transaction ID")
            approve_btn = gr.Button("✅ Approve", variant="primary")
            reject_btn = gr.Button("❌ Reject")
        admin_status = gr.Markdown()

        gr.Markdown("### Resolved Alerts")
        resolved_df = gr.Dataframe()

    # ---------------- WIRING ----------------
    reg_btn.click(register, [reg_name, reg_email, reg_password], reg_status)

    login_btn.click(
        login,
        [login_email, login_password],
        [login_status, user_email_state, is_admin_state, auth_group, bank_group, admin_group, welcome_md, balance_md],
    )

    admin_login_btn.click(
        admin_login,
        [admin_username, admin_password],
        [admin_login_status, user_email_state, is_admin_state, auth_group, bank_group, admin_group],
    )

    logout_btn_bank.click(
        logout, None, [user_email_state, is_admin_state, auth_group, bank_group, admin_group]
    )
    logout_btn_admin.click(
        logout, None, [user_email_state, is_admin_state, auth_group, bank_group, admin_group]
    )

    transfer_btn.click(
        transfer,
        [user_email_state, receiver, amount, device, location],
        [transfer_result, balance_md, transfer_result],
    )

    history_refresh_btn.click(get_history, user_email_state, history_df)
    alerts_refresh_btn.click(get_fraud_alerts, user_email_state, alerts_md)

    admin_refresh_btn.click(get_pending_df, None, pending_df)
    approve_btn.click(lambda tid: admin_decide(tid, "approve"), decide_id, [admin_status, pending_df, resolved_df])
    reject_btn.click(lambda tid: admin_decide(tid, "reject"), decide_id, [admin_status, pending_df, resolved_df])

if __name__ == "__main__":
    demo.launch()
