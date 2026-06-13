// GuardianEye Parental Control Signal & Control Server
const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Map to track rooms and their connected participants
// Key: roomCode -> Value: { parent: socketId, child: socketId }
const rooms = {};

// Map to track active socket rooms
// Key: socketId -> Value: roomCode
const socketToRoom = {};

function getRoomBySocket(socketId) {
  const roomCode = socketToRoom[socketId];
  return roomCode ? rooms[roomCode] : null;
}

io.on("connection", (socket) => {
  console.log(`Socket connected: ${socket.id}`);

  // Join Room
  socket.on("join-room", (data) => {
    const { roomCode, role } = data;
    if (!roomCode || !role) {
      console.log(`Failed attempt to join room: invalid data`);
      return;
    }

    // Initialize room if it doesn't exist
    if (!rooms[roomCode]) {
      rooms[roomCode] = { parent: null, child: null };
    }

    // Register active participant
    rooms[roomCode][role] = socket.id;
    socketToRoom[socket.id] = roomCode;
    socket.join(roomCode);

    console.log(`Socket ${socket.id} joined room [${roomCode}] as [${role}]`);

    // Let the room know about successful registration
    io.to(roomCode).emit("joined", {
      roomCode,
      role,
      socketId: socket.id
    });

    // If both parent and child are connected, signal to start WebRTC session / status triggers
    if (rooms[roomCode].parent && rooms[roomCode].child) {
      console.log(`Room [${roomCode}] fully connected. Notifying parent.`);
      io.to(rooms[roomCode].parent).emit("start-call", { childId: rooms[roomCode].child });
    }
  });

  // WebRTC Signaling routing
  socket.on("webrtc-offer", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Routing offer from ${socket.id} to child ${room.child}`);
      io.to(room.child).emit("webrtc-offer", {
        from: "parent",
        sdp: data.sdp
      });
    }
  });

  socket.on("webrtc-answer", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.parent) {
      console.log(`Routing answer from ${socket.id} to parent ${room.parent}`);
      io.to(room.parent).emit("webrtc-answer", {
        from: "child",
        sdp: data.sdp
      });
    }
  });

  socket.on("ice-candidate", (data) => {
    const { to, candidate } = data;
    const room = getRoomBySocket(socket.id);
    if (room) {
      const targetSocketId = to === "parent" ? room.parent : room.child;
      if (targetSocketId) {
        io.to(targetSocketId).emit("ice-candidate", {
          from: to === "parent" ? "child" : "parent",
          candidate
        });
      }
    }
  });

  socket.on("remote-control", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      io.to(room.child).emit("remote-control", data);
    }
  });

  // FEATURE 1 — FILE BROWSER
  socket.on("request-files", (type) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Parent requesting files of type: ${type}`);
      io.to(room.child).emit("request-files", type);
    }
  });

  socket.on("file-list", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.parent) {
      console.log(`Child sending back file-list of length: ${Array.isArray(data) ? data.length : "dynamic"}`);
      io.to(room.parent).emit("file-list", data);
    }
  });

  socket.on("request-file-data", (path) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Parent requesting raw byte stream of file: ${path}`);
      io.to(room.child).emit("request-file-data", path);
    }
  });

  socket.on("file-data", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.parent) {
      console.log(`Child sending base64 file block`);
      io.to(room.parent).emit("file-data", data);
    }
  });

  // FEATURE 2 — SCREEN LOCK
  socket.on("lock-screen", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Lock block status modification requested:`, data);
      io.to(room.child).emit("lock-screen", data);
    }
  });

  // FEATURE 4 — NOTIFICATION FORWARDER
  socket.on("child-notification", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.parent) {
      console.log(`Forwarding child notification from: ${data.app || "unknown"}`);
      io.to(room.parent).emit("child-notification", data);
    }
  });

  // FEATURE 5 — VOICE MESSAGE & TEXT-TO-SPEECH
  socket.on("play-audio", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Parent requested audio segment broadcast in child device`);
      io.to(room.child).emit("play-audio", data);
    }
  });

  socket.on("speak-text", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Parent requested text broadcast speech synthesize: ${data.text}`);
      io.to(room.child).emit("speak-text", data);
    }
  });

  // FEATURE 6 — APP BLOCKER
  socket.on("request-apps", () => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Parent requesting packages list`);
      io.to(room.child).emit("request-apps");
    }
  });

  socket.on("app-list", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.parent) {
      console.log(`Child broadcasting packages count: ${Array.isArray(data) ? data.length : "dynamic"}`);
      io.to(room.parent).emit("app-list", data);
    }
  });

  socket.on("block-apps", (data) => {
    const room = getRoomBySocket(socket.id);
    if (room && room.child) {
      console.log(`Parent updating list of blocked packages:`, data);
      io.to(room.child).emit("block-apps", data);
    }
  });

  // Disconnection handler
  socket.on("disconnect", () => {
    console.log(`Socket disconnected: ${socket.id}`);
    const roomCode = socketToRoom[socket.id];
    if (roomCode && rooms[roomCode]) {
      const room = rooms[roomCode];
      let droppedRole = "";

      if (room.parent === socket.id) {
        room.parent = null;
        droppedRole = "parent";
      } else if (room.child === socket.id) {
        room.child = null;
        droppedRole = "child";
      }

      // Notify the remaining client
      io.to(roomCode).emit("peer-disconnected", { role: droppedRole });

      // Clean up room memory if both are gone
      if (!room.parent && !room.child) {
        delete rooms[roomCode];
        console.log(`Room [${roomCode}] fully cleared`);
      }
    }
    delete socketToRoom[socket.id];
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`GuardianEye signalling service running on port ${PORT}`);
});
