## JEDIS

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


## Installation & Compilation

1. Clone the repository:
   git clone https://github.com/Zaid737/JEDIS.git

2. Locate repo cd jredis

3. Compile
   javac -d out src/miniredis/*.java

4. Run Server
   java -cp out miniredis.MiniRedisServer

5. Run client(WSL)
   ip route | grep default --Getting ip address
   redis-cli -h <ip> -p 6380
