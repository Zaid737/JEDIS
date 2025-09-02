#JEDIS

A lightweight Redis-like in-memory key-value store implemented in **Java**.  
Supports basic commands, persistence, and a simple client-server architecture.


## Features
- Basic key-value storage (`SET`, `GET`, `DEL`, `EXISTS`).
- Persistence using RDB-style snapshots.
- Simple client-server implementation over TCP sockets.
- Minimal dependencies, runs with standard Java.


## Requirements
- **Java 17** or higher (recommended).
- Works on **Windows, Linux, macOS**.
- Git (for cloning the repository).


## Structure
MiniRedis/
│── src/miniredis/       # Source code
│   ├── MiniRedisServer.java
│   ├── MiniRedisClient.java
│   ├── CommandHandler.java
│   ├── PersistenceManager.java
│   └── ...
│── out/                 # Compiled classes
│── README.md            # Project documentation


## Installation & Compilation

1. Clone the repository:
   git clone https://github.com/<your-username>/MiniRedis.git
   cd MiniRedis

2. Compile
  javac -d out src/miniredis/*.java

3. Run Server
  java -cp out miniredis.MiniRedisServer

4. Run client(WSL)
   ip route | grep default --Getting ip address
   redis-cli -h <ip> -p 6380


