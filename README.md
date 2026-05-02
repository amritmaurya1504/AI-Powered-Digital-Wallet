# 💳 Digital Wallet System (Spring Boot + AI)

## 📌 Overview

A scalable **Digital Wallet Web Application** built using **Java + Spring Boot (Monolith Architecture)**.

The system enables users to manage funds, perform secure transactions, and gain **AI-powered insights**. 

It is designed with **strong consistency, concurrency handling, and idempotent transaction processing** to ensure reliability in real-world financial scenarios.


## 🚀 Features

### Core Features

- Wallet management (create, check balance)
- Add money (mock)
- Send money (P2P transfer)
- Transaction history
- Balance validation

### ⚡ Advanced Backend Features

- Concurrency-safe transactions (race condition handling)
- Idempotency key support (prevents duplicate transactions)
- Atomic debit-credit operations
- Transactional rollback on failure
- Proper Logging of failed and successful transaction.

### 🤖 AI Features

- Smart spending insights
- Natural language queries  
  _(e.g., "How much did I spend this month?")_
- Extensible for RAG-based search (future scope)

------------------------------------------------------------------------

## 🧱 Architecture

### Current

Controller → Service → Repository → Database


