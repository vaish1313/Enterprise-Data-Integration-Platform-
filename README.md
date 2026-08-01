# Enterprise Data Integration Platform (EDIP)

The **Enterprise Data Integration Platform** is a production-ready, full-stack application designed to streamline the ingestion, transformation, and synchronization of data across disparate systems. Built for data engineers, analysts, and enterprise operators, it offers a centralized dashboard to manage database connections, schedule asynchronous data synchronization jobs, apply real-time data transformation rules, and monitor system health.

---

## 🚀 Live Demo

- **Frontend:** [https://enterprise-data-integration-platfor.vercel.app/](https://enterprise-data-integration-platfor.vercel.app/)
- **Backend API Docs (Swagger):** `<Render URL>/swagger-ui.html` *(Note: replace `<Render URL>` with your actual deployment URL)*
- **Demo Credentials:** 
  - Username: `admin`
  - Password: `admin123`

---

## 🛠 Tech Stack

- **Backend:** Java 17, Spring Boot 3, Spring Security, JPA / Hibernate
- **Frontend:** React 18, Vite, Axios, React Router, TailwindCSS
- **Database:** PostgreSQL
- **Infrastructure & Hosting:** Docker, Render (Backend), Vercel (Frontend), Neon (Serverless Postgres)

---

## 🏗 Architecture Note

The application employs a deliberately decoupled cloud architecture:
- **Stateless Backend (Render):** The Spring Boot application is entirely stateless, managing authentication via JWTs and processing background tasks in-memory. This allows for horizontal scaling without session management overhead.
- **Managed Serverless Database (Neon):** Persistent state, business data, and Flyway schema history are delegated to Neon’s highly available serverless Postgres instance, ensuring separation of concerns and robust data durability independent of the backend lifecycle.

---

## ✨ Key Technical Highlights

- **Asynchronous Job Processing:** Leverages Spring's `@Async` alongside a configured `ThreadPoolTaskExecutor` to decouple long-running ingestion and synchronization tasks from the HTTP request-response cycle.
- **Robust Retry Logic:** Implements programmatic retry mechanisms with exponential backoff (1s → 2s → 4s) to handle transient network faults during external API integrations.
- **Circuit Breaker Pattern:** Protects system stability by monitoring data source health; failing data sources are isolated to prevent cascading failures across the execution thread pool.
- **Advanced Security:** Secures endpoints with stateless JWT authentication, role-based access control (RBAC), and a robust server-side logout mechanism.
- **Automated Schema Evolution:** Employs Flyway to manage database schema migrations (V1 through V8), ensuring deterministic, version-controlled database updates across environments.

---

## 💻 Local Setup

To run the application locally with a dedicated Dockerized PostgreSQL instance:

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/Enterprise-Data-Integration-Platform.git
   cd Enterprise-Data-Integration-Platform
   ```

2. **Start the database via Docker Compose**
   ```bash
   cd database
   docker-compose up -d
   ```

3. **Run the Backend (Spring Boot)**
   ```bash
   cd ../backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

4. **Run the Frontend (React)**
   ```bash
   cd ../frontend
   npm install
   npm run dev
   ```

5. Access the local app at `http://localhost:5173`.

---

## ⚠️ Known Limitations (Free-Tier Hosting)

This project is currently hosted on free-tier cloud platforms. As a result:
- **Cold Starts:** The Spring Boot backend (hosted on Render) will automatically spin down after 15 minutes of inactivity to conserve resources.
- **Initial Load Time:** If the backend is asleep, the **first API request or login attempt may take 30–60 seconds** while the server wakes up. Subsequent requests will be fast and responsive. 
