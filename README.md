# MindQ — AI-Powered Learning Platform

MindQ is an intelligent learning and assessment platform that enables users to upload study materials, generate structured MCQs using AI, take timed quizzes, analyze performance, and receive adaptive quizzes targeting weak concepts.

---

## 🏗️ Architecture

MindQ is designed as a **Modular Monolith**:
* **Backend**: Spring Boot 3, Java 21, Spring Data JPA, Hibernate, MySQL, Groq/Gemini/OpenAI AI Router
* **Frontend**: React, TypeScript, Vite, Tailwind CSS, Axios
* **Database**: MySQL (`mindq_db`)

```
MindQ/
├── backend/          # Java 21 + Spring Boot 3 REST API
└── frontend/         # React + TypeScript + Vite + Tailwind CSS
```

---

## 🚀 Getting Started (Phase 1)

### Prerequisites
* Java 21 (JDK 21)
* Apache Maven 3.9+
* Node.js 18+ and npm
* MySQL 8.x+ Server

### 1. Database Setup
Ensure MySQL is running and create the database:
```sql
CREATE DATABASE IF NOT EXISTS mindq_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Backend Setup
Navigate to `backend/` and run:
```bash
cd backend
mvn spring-boot:run
```
The backend starts at `http://localhost:8080`.
* Health Endpoint: `http://localhost:8080/api/v1/health`

### 3. Frontend Setup
Navigate to `frontend/` and run:
```bash
cd frontend
npm install
npm run dev
```
The frontend starts at `http://localhost:5173`.
