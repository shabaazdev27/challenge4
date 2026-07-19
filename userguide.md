# 🏟️ StadiumMate: A Beginner's Guide

Welcome! If you're looking to understand what the **StadiumMate** project is all about, how it works, and how to get it running without needing a PhD in computer science, you're in the right place. 

---

## 🌟 What is StadiumMate and Why Was it Built?

**StadiumMate** is a smart, multilingual web application designed to help fans navigate large stadiums (specifically built for the FIFA World Cup 2026 at MetLife Stadium). 

**The Problem:** Finding your seat, the nearest restroom, or a food stand in a massive, crowded stadium can be overwhelming, especially if you don't speak the local language. 

**The Solution (Why it was built):** StadiumMate allows fans to simply open a web browser on their phone and ask for directions in their native language—either by typing or speaking. The app then figures out the best route, making sure to avoid areas with heavy crowds, and tells them exactly how to get there in their own language.

It serves two main groups:
1. **Fans:** Get a VIP, personalized concierge experience.
2. **Stadium Operators:** Can monitor and direct crowd flow to prevent bottlenecks.

---

## 🔄 How Does it Work? (The End-to-End Flow)

Imagine you are at Gate A and want to find a food court. Here is exactly what happens behind the scenes in plain English:

1. **The Request:** You open the app on your phone, tap the microphone, and say, *"¿Dónde está la comida?"* (Where is the food?).
2. **Understanding the Intent (The AI Brain):** The app sends your question to our server. We use Google's advanced AI (**Gemini 2.0 Flash**) to translate and figure out exactly what you want. It realizes: *"Ah, this user speaks Spanish and is looking for a food court."*
3. **Checking the Crowds:** The server quickly looks at real-time data to see if any hallways are currently jam-packed.
4. **Finding the Route (The Math Brain):** Instead of using AI to guess the route, we use a rock-solid mathematical formula (called the **A* algorithm**) to calculate the shortest path. If the main hallway is too crowded, the math automatically calculates a detour to save you time.
5. **Crafting the Response:** The system uses the AI one more time to take the calculated route and turn it into a friendly, natural-sounding sentence in Spanish.
6. **The Result:** The app draws the route on your phone's screen and reads the directions out loud to you in Spanish: *"Dirígete a la Puerta B y encontrarás la zona de comidas..."*

**The Magic:** The AI does the language translation and talking, while the math handles the map routing. This keeps the app incredibly smart, fast, and accurate!

---

## 🛠️ How to Set It Up (Simplified Setup)

If you want to run this project on your own computer, follow these high-level steps. *Note: You'll need some basic tools installed on your computer first, like Java, Node.js, and a Google Gemini API Key (which acts as a password to use the AI).*

### Step 1: Get the Code
Download (or "clone") the project code from GitHub to your computer.
- You will need to create a small configuration file called `.env.local` to store your secret Gemini API key.

### Step 2: Start the Backend (The Server)
The backend is the engine of the app, built in Java.
- Open your computer's terminal.
- Navigate to the `backend` folder.
- Run the command to start the server (e.g., `mvn spring-boot:run`).
- *It will quietly run in the background, listening for requests.*

### Step 3: Start the Frontend (The App)
The frontend is the visual app you see on the screen.
- Open a new terminal window.
- Navigate to the `frontend` folder.
- Install the required packages by running `npm install`.
- Start the app by running `npm run dev`.

### Step 4: Try it Out!
- Open your web browser and go to `http://localhost:3000`. 
- You should see the StadiumMate interface! Try typing or asking for directions and watch the magic happen.

> [!TIP]
> **Want to test the crowd feature?** The project includes a "simulate crowd" button. If you artificially block a hallway and ask for directions again, you'll see the app instantly reroute you around the traffic!

---

## 🧩 Summary of the Tech Used
*Just in case someone asks, here is the quick cheat sheet of the technology powering this:*
* **Frontend:** Next.js (A tool for building fast websites).
* **Backend:** Spring Boot (Java) (The robust engine handling logic).
* **AI:** Google Vertex AI / Gemini 2.0 Flash (Handles all the language understanding and translation).
* **Database:** Google Firestore (Stores the stadium map and live crowd data).
