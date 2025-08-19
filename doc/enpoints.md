# /api estara siempre delante de los siguientes endpoints.

# /auth

- /createtoken: Creates a new token.
    Request: ${service-ip}:30000/api/auth/token/createtoken
    Body: {
        "token": String,
        "userId": String,
        "createdAt": String,
        "expiresAt": String,
        "lastUsed": String,
        "status": String,
        "ipAddress": String,
        "sessiontype": String
    }