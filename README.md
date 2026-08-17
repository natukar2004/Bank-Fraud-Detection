---
title: SecureBank Fraud Detection Demo (Java)
emoji: 🏦
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
---

# SecureBank — Fraud Detection Demo (Java / Spring Boot)

Java rewrite of the original Gradio/Python demo — same rule-based fraud engine
and human-in-the-loop admin review, now as a Spring Boot + Thymeleaf web app
running behind Docker.

**Why Docker and not "Java SDK":** Hugging Face Spaces only has built-in SDKs
for Gradio, Streamlit, and static HTML — there's no native Java runner. Any
JVM app (Spring Boot, plain servlets, etc.) has to ship as a Docker image
instead. That's what this project does.

## How to deploy this on Hugging Face Spaces

1. Go to https://huggingface.co/new-space
2. Name it (e.g. `securebank-fraud-demo-java`), choose **Docker** as the SDK
   (pick the "Blank" Docker template), and set visibility to Public.
3. Upload the whole project — `Dockerfile`, `pom.xml`, `README.md`, and the
   `src/` folder — via the web UI or `git push` (same as your other Spaces).
4. Hugging Face builds the Dockerfile (Maven compiles the jar, then it runs
   on a slim JRE) and gives you a live URL in a couple of minutes.

The app listens on port **7860** inside the container — that's the port
Spaces expects for Docker SDK apps (set via `app_port` above and `EXPOSE 7860`
/ `server.port=7860` in the code).

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
   by its ID in the Admin Dashboard.

## Fraud rules implemented

| Rule | Risk added |
|---|---|
| Amount > ₹50,000 | +40 |
| New device | +20 |
| New location | +20 |
| More than 3 transfers in 1 minute | +20 |

Risk score ≥ 70 → transaction is blocked and sent to the Admin Dashboard for
manual Approve/Reject.

## Architecture (Java version)

- **Spring Boot 3 / Java 17** web app, server-rendered with **Thymeleaf**
  (no separate frontend — this replaces Gradio's UI layer).
- Login state kept in the **HTTP session** (`userEmail` / `isAdmin`) — the
  Java equivalent of Gradio's `gr.State`.
- `DataStore` is an in-memory singleton bean (`ConcurrentHashMap` /
  `CopyOnWriteArrayList`) — the Java equivalent of the Python module-level
  dicts. Same caveat as before: data resets when the container restarts.
- `FraudService` mirrors `calculate_risk()` rule-for-rule.
- `BankService` mirrors `transfer()` / `admin_decide()` / history & alert
  lookups.
- Controllers (`AuthController`, `BankController`, `AdminController`) use the
  redirect-after-post pattern with Spring's flash attributes for one-time
  status messages (login errors, transfer results, admin decisions).

## Known simplifications (same spirit as the Python version)

- Data is in-memory and shared across visitors to the Space — it resets when
  the Space goes idle/restarts. Not how a real bank would store data.
- "Device" and "location" are dropdowns you pick manually to simulate
  context; in production these would come from session/device metadata.
- Session-cookie login only — no JWT, no password complexity rules, no HTTPS
  enforcement beyond what HF Spaces provides. Not a production auth layer.

## Local run (without Docker)

```bash
mvn spring-boot:run
```

Then open http://localhost:7860
