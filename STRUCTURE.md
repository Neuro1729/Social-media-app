# Project Structure

Modular backend: new features should mostly add code, and only touch small public contracts.

```
backend/src/main/java/com/socialmedia/
│
├── SocialMediaApp.java
│
├── shared/                         # Cross-cutting only
│   ├── config/WebConfig.java
│   └── security/                   # JWT + Spring Security
│
└── modules/
    ├── auth/                       # signup, login, logout, passwords
    │   ├── api/AuthController.java
    │   └── usecases/
    │       ├── SignUpUser.java
    │       ├── LoginUser.java
    │       └── LogoutUser.java
    │
    ├── users/                      # profile, username, bio, picture ref
    │   ├── api/ProfileController.java
    │   ├── domain/User.java
    │   ├── usecases/
    │   │   ├── GetMyProfile.java
    │   │   ├── EditProfile.java
    │   │   └── UploadProfilePicture.java
    │   ├── infrastructure/
    │   └── publicapi/              # contracts for other modules
    │       ├── PublicUserReader.java
    │       └── UserAccountPort.java
    │
    ├── posts/                      # create / view / edit / delete posts
    │   ├── api/PostController.java
    │   ├── domain/Post.java        # stores authorId + mediaIds only
    │   ├── usecases/
    │   │   ├── CreatePost.java
    │   │   ├── GetOwnPosts.java
    │   │   ├── GetUserPosts.java
    │   │   ├── EditPostCaption.java
    │   │   └── DeletePost.java
    │   └── infrastructure/
    │
    └── media/                      # upload, validate, store, URLs
        ├── domain/MediaFile.java
        ├── infrastructure/LocalMediaStorage.java
        └── publicapi/MediaStorage.java
```

## Module rules

| Module | Owns | Does not own |
|--------|------|--------------|
| auth | credentials, JWT login/logout | profile fields, file storage |
| users | profile fields, `profilePictureMediaId` | password login rules, post rows |
| posts | caption, `authorId`, `mediaIds` | User entity, file bytes |
| media | files on disk + media IDs/URLs | posts, profiles |

Dependency direction:

```
controllers → use cases → publicapi interfaces → infrastructure
posts → users.publicapi + media.publicapi
users → media.publicapi
auth  → users.publicapi
```

## Ports

| Service | Port |
|---------|------|
| Frontend | 3000 |
| Backend | 8080 |
| PostgreSQL | 5432 |

```bash
docker compose up --build
```
