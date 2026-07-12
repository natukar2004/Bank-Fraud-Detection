---
title: SecureBank Fraud Detection Demo
emoji: 🏦
colorFrom: blue
colorTo: indigo
sdk: gradio
sdk_version: "5.31.0"
app_file: app.py
pinned: false
---

# SecureBank — Fraud Detection Demo

A mini banking app with a rule-based fraud detection engine, built to demonstrate
end-to-end product + engineering thinking: register → login → transfer → fraud
scoring → block/allow → admin review.

## How to deploy this on Hugging Face Spaces

1. Go to https://huggingface.co/new-space
2. Name it (e.g. `securebank-fraud-demo`), choose **Gradio** as the SDK, and
   set visibility to Public.
3. Upload these 3 files to the Space: `app.py`, `requirements.txt`, `README.md`
   (same flow as your other HF Spaces — upload via the web UI or `git push`).
4. The Space will auto-build and give you a live URL in ~1 minute.

## Demo logins

- Customer: `rahul@gmail.com` / `password123`
- Admin: `admin` / `admin123`

## How to use it

1. Log in as the customer, go to **Transfer Money**.
2. Enter an amount and pick a device/location — these simulate what a real
   app would read automatically from the session/device.
3. If the risk score crosses the threshold, the transfer is blocked and shown
   under **Fraud Alerts**.
4. Log out, log in as **Admin**, and Approve or Reject the pending transaction
   by its ID in the Admin Dashboard tab.

## Fraud rules implemented

| Rule | Risk added |
|---|---|
| Amount > ₹50,000 | +40 |
| New device | +20 |
| New location | +20 |
| More than 3 transfers in 1 minute | +20 |

Risk score ≥ 70 → transaction is blocked and sent to the Admin Dashboard for
manual Approve/Reject.

## What this demonstrates (for interviews)

- **Product thinking**: identifies a real problem (fraudulent transfers), designs
  a rule engine and a human-in-the-loop review workflow instead of a blind
  block/allow.
- **Engineering**: state machine for a transaction (Success / Blocked / Approved
  / Rejected), scoring logic, role-based views (customer vs admin).
- **Scope for a fresher**: buildable and demoable in a single file, but the
  narrative (rules → scoring → decision → review) maps directly onto how a real
  fraud/risk system at a fintech like Juspay would be structured, just without
  the production infra (Spring Boot API layer, MySQL, real device fingerprinting).

## Troubleshooting

If the Space fails to build with an error like
`ImportError: cannot import name 'HfFolder' from 'huggingface_hub'`, it means
the pinned `gradio` version in `requirements.txt` is too old for the
`huggingface_hub` version Spaces installed alongside it. Fix: bump the
`gradio==` pin in `requirements.txt` to a newer 5.x release and rebuild the
Space (Settings → Factory rebuild).

## Known simplifications (good to say upfront in an interview)

- Data is in-memory and shared across visitors to the Space — it resets when
  the Space goes idle. Fine for a demo, not how a real bank would store data.
- "Device" and "location" are dropdowns you pick manually to simulate context,
  since there's no real device fingerprinting or IP geolocation here. In
  production these would come from session/device metadata automatically.
- No JWT/session auth — this is a UI-level login, not a secured API layer.
